package org.qwen.aiqwen.util;
import dev.langchain4j.data.document.Document; import dev.langchain4j.data.document.DocumentSplitter; import dev.langchain4j.data.segment.TextSegment; import lombok.extern.slf4j.Slf4j; import org.apache.poi.xwpf.usermodel.*;
import java.io.FileInputStream; import java.io.InputStream; import java.util.ArrayList; import java.util.HashMap; import java.util.List; import java.util.Map;
@Slf4j public class SmartWordDocumentSplitter implements DocumentSplitter {
    private static final int MAX_SEGMENT_LENGTH = 1500;
    private static final int MIN_SEGMENT_LENGTH = 200;
    private static final int OVERLAP_SIZE = 100;

    @Override
    public List<TextSegment> split(Document document) {
        String filePath = document.metadata().getString("filePath");
        if (filePath == null) {
            log.warn("文档缺少 filePath 元数据，使用默认分割器");
            return new LlmDocumentSplitter().split(document);
        }

        try {
            return splitWordDocument(filePath, document.metadata().toMap());
        } catch (Exception e) {
            log.error("Word 文档分割失败，降级为默认分割器: {}", e.getMessage());
            return new LlmDocumentSplitter().split(document);
        }
    }

    private List<TextSegment> splitWordDocument(String filePath, Map<String, Object> metadata) {
        List<WordContentBlock> contentBlocks = new ArrayList<>();

        try (InputStream fis = new FileInputStream(filePath);
             XWPFDocument document = new XWPFDocument(fis)) {

            log.info("开始解析 Word 文档: {}", filePath);

            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph) {
                    XWPFParagraph paragraph = (XWPFParagraph) element;
                    parseParagraph(paragraph, contentBlocks);
                } else if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;
                    parseTable(table, contentBlocks);
                }
            }

            log.info("Word 文档解析完成，共提取 {} 个内容块", contentBlocks.size());

            return mergeBlocksToSegments(contentBlocks, metadata);

        } catch (Exception e) {
            log.error("解析 Word 文档失败: {}", e.getMessage(), e);
            throw new RuntimeException("Word 文档解析失败", e);
        }
    }

    private void parseParagraph(XWPFParagraph paragraph, List<WordContentBlock> blocks) {
        String text = paragraph.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        WordContentBlock block = new WordContentBlock();
        block.setRawText(text);

        try {
            String alignment = paragraph.getParagraphText() != null ? paragraph.getParagraphText(): "LEFT";
            block.setAlignment(alignment);
        } catch (Exception e) {
            block.setAlignment("LEFT");
        }

        if (!paragraph.getRuns().isEmpty()) {
            XWPFRun firstRun = paragraph.getRuns().get(0);

            String styleId = paragraph.getStyleID();
            String styleName = paragraph.getStyle();

            boolean isTitle = isTitleStyle(styleId, styleName);
            boolean isHeading = isHeadingStyle(styleId, styleName);
            boolean isBold = firstRun.isBold();
            int fontSize = getFontSize(firstRun);

            if (isTitle) {
                block.setType(BlockType.TITLE);
                block.setPriority(10);
            } else if (isHeading) {
                block.setType(BlockType.HEADING);
                block.setPriority(8);
                block.setHeadingLevel(extractHeadingLevel(styleName));
            } else if (isBold && fontSize > 14) {
                block.setType(BlockType.SUBTITLE);
                block.setPriority(7);
            } else if (isBold) {
                block.setType(BlockType.BOLD_TEXT);
                block.setPriority(5);
            } else if (text.matches("^\\d+\\.\\s.*")) {
                block.setType(BlockType.NUMBERED_LIST);
                block.setPriority(4);
            } else if (text.matches("^[•\\-·]\\s.*")) {
                block.setType(BlockType.BULLET_LIST);
                block.setPriority(4);
            } else {
                block.setType(BlockType.PARAGRAPH);
                block.setPriority(3);
            }

            if (fontSize > 0) {
                block.setFontSize(fontSize);
            }
        }

        blocks.add(block);
    }

    private int getFontSize(XWPFRun run) {
        try {
            return run.getFontSize();
        } catch (Exception e) {
            return 0;
        }
    }

    private void parseTable(XWPFTable table, List<WordContentBlock> blocks) {
        StringBuilder tableContent = new StringBuilder();
        tableContent.append("[TABLE_START]\n");

        int rowNum = 0;
        for (XWPFTableRow row : table.getRows()) {
            tableContent.append("| ");
            for (XWPFTableCell cell : row.getTableCells()) {
                String cellText = cell.getText().trim().replace("\n", " ");
                tableContent.append(cellText).append(" | ");
            }
            tableContent.append("\n");
            rowNum++;
        }

        tableContent.append("[TABLE_END]");

        WordContentBlock block = new WordContentBlock();
        block.setType(BlockType.TABLE);
        block.setRawText(tableContent.toString());
        block.setPriority(6);
        block.setMetadata("table_rows", String.valueOf(rowNum));

        blocks.add(block);
        log.debug("解析表格，共 {} 行", rowNum);
    }

    private List<TextSegment> mergeBlocksToSegments(List<WordContentBlock> blocks, Map<String, Object> metadata) {
        List<TextSegment> segments = new ArrayList<>();
        StringBuilder currentSegment = new StringBuilder();
        String currentTitle = "";
        String currentHeading = "";
        int segmentIndex = 0;

        for (int i = 0; i < blocks.size(); i++) {
            WordContentBlock block = blocks.get(i);

            if (block.getType() == BlockType.TITLE) {
                currentTitle = block.getRawText();
                currentHeading = "";
            } else if (block.getType() == BlockType.HEADING) {
                currentHeading = block.getRawText();
            }

            if (block.getType() == BlockType.TABLE) {
                if (currentSegment.length() > MIN_SEGMENT_LENGTH) {
                    segments.add(createSegment(currentSegment.toString(), currentTitle, currentHeading, segmentIndex++, metadata));
                    currentSegment = new StringBuilder();
                }

                if (block.getRawText().length() <= MAX_SEGMENT_LENGTH) {
                    segments.add(createSegment(block.getRawText(), currentTitle, currentHeading, segmentIndex++, metadata));
                } else {
                    String tableText = block.getRawText();
                    while (tableText.length() > MAX_SEGMENT_LENGTH) {
                        int splitPos = tableText.lastIndexOf("\n", MAX_SEGMENT_LENGTH);
                        if (splitPos <= 0) splitPos = MAX_SEGMENT_LENGTH;
                        segments.add(createSegment(tableText.substring(0, splitPos), currentTitle, currentHeading, segmentIndex++, metadata));
                        tableText = tableText.substring(splitPos);
                    }
                    if (!tableText.isEmpty()) {
                        segments.add(createSegment(tableText, currentTitle, currentHeading, segmentIndex++, metadata));
                    }
                }
                continue;
            }

            int blockLength = block.getRawText().length();

            if (currentSegment.length() + blockLength > MAX_SEGMENT_LENGTH && currentSegment.length() > MIN_SEGMENT_LENGTH) {
                segments.add(createSegment(currentSegment.toString(), currentTitle, currentHeading, segmentIndex++, metadata));

                String overlap = "";
                if (currentSegment.length() > OVERLAP_SIZE) {
                    overlap = currentSegment.substring(currentSegment.length() - OVERLAP_SIZE);
                }
                currentSegment = new StringBuilder(overlap);
            }

            if (!currentSegment.isEmpty()) {
                currentSegment.append("\n\n");
            }

            currentSegment.append(formatBlock(block));
        }

        if (currentSegment.length() > MIN_SEGMENT_LENGTH) {
            segments.add(createSegment(currentSegment.toString(), currentTitle, currentHeading, segmentIndex++, metadata));
        }

        log.info("文档分割完成，共生成 {} 个片段", segments.size());
        return segments;
    }

    private String formatBlock(WordContentBlock block) {
        StringBuilder formatted = new StringBuilder();

        switch (block.getType()) {
            case TITLE:
                formatted.append("# ").append(block.getRawText());
                break;
            case HEADING:
                String hashes = "#".repeat(Math.min(block.getHeadingLevel() + 1, 6));
                formatted.append(hashes).append(" ").append(block.getRawText());
                break;
            case SUBTITLE:
                formatted.append("**").append(block.getRawText()).append("**");
                break;
            case BOLD_TEXT:
                formatted.append("**").append(block.getRawText()).append("**");
                break;
            case NUMBERED_LIST:
                formatted.append("- ").append(block.getRawText());
                break;
            case BULLET_LIST:
                formatted.append("- ").append(block.getRawText());
                break;
            case TABLE:
                formatted.append(block.getRawText());
                break;
            case PARAGRAPH:
            default:
                formatted.append(block.getRawText());
                break;
        }

        return formatted.toString();
    }

    private TextSegment createSegment(String text, String title, String heading, int index, Map<String, Object> metadata) {
        TextSegment segment = TextSegment.from(text);

        if (title != null && !title.isEmpty()) {
            segment.metadata().put("title", title);
        }
        if (heading != null && !heading.isEmpty()) {
            segment.metadata().put("heading", heading);
        }
        segment.metadata().put("segmentIndex", String.valueOf(index));
        segment.metadata().put("contentType", "word_document");

        metadata.forEach((key, value) -> {
            if (!segment.metadata().containsKey(key)) {
                segment.metadata().put(key, String.valueOf(value));
            }
        });

        return segment;
    }

    private boolean isTitleStyle(String styleId, String styleName) {
        if (styleId == null && styleName == null) return false;

        String check = (styleId != null ? styleId : styleName).toLowerCase();
        return check.contains("title") || check.contains("标题 1") || check.equals("title");
    }

    private boolean isHeadingStyle(String styleId, String styleName) {
        if (styleId == null && styleName == null) return false;

        String check = (styleId != null ? styleId : styleName).toLowerCase();
        return check.contains("heading") || check.contains("标题");
    }

    private int extractHeadingLevel(String styleName) {
        if (styleName == null) return 1;

        for (int i = 1; i <= 6; i++) {
            if (styleName.toLowerCase().contains(String.valueOf(i)) ||
                    styleName.contains("标题 " + i)) {
                return i;
            }
        }
        return 1;
    }

    enum BlockType {
        TITLE,
        HEADING,
        SUBTITLE,
        BOLD_TEXT,
        PARAGRAPH,
        NUMBERED_LIST,
        BULLET_LIST,
        TABLE
    }

    static class WordContentBlock {
        private BlockType type;
        private String rawText;
        private int priority;
        private String alignment;
        private int fontSize;
        private int headingLevel;
        private Map<String, String> metadata;

        public BlockType getType() { return type; }
        public void setType(BlockType type) { this.type = type; }

        public String getRawText() { return rawText; }
        public void setRawText(String rawText) { this.rawText = rawText; }

        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }

        public String getAlignment() { return alignment; }
        public void setAlignment(String alignment) { this.alignment = alignment; }

        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }

        public int getHeadingLevel() { return headingLevel; }
        public void setHeadingLevel(int headingLevel) { this.headingLevel = headingLevel; }

        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(String key, String value) {
            if (this.metadata == null) this.metadata = new HashMap<>();
            this.metadata.put(key, value);
        }
    }
}