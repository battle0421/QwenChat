package org.qwen.aiqwen.util;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.TokenCountEstimator;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 优化文档分块器 - 专注于提取型号和故障描述
 */
public class OptimizedWordSplitter implements DocumentSplitter {
    @Override
    public List<TextSegment> split(Document document) {
        List<TextSegment> segments = new ArrayList<>();
        String text = document.text();

        // 1. 按表格分割（型号和故障常在表格中）
//        List<String> tableContents = extractTableContent(text);

        // 2. 按段落分割
//        List<String> paragraphs = extractRelevantParagraphs(text);

        // 3. 合并处理
        //  segments.addAll(processTableContents(tableContents));
        List<String> paragraphs=new ArrayList<>();
        paragraphs.add(text);
        segments.addAll(processParagraphs(paragraphs));

        return segments;
    }

    private List<String> extractTableContent(String text) {
        // 简化的表格内容提取
        List<String> tables = new ArrayList<>();
        String[] lines = text.split("\\R");
        boolean inTable = false;
        StringBuilder currentTable = new StringBuilder();

        for (String line : lines) {
            if (line.contains("型号") || line.contains("故障")) {
                inTable = true;
            }

            if (inTable) {
                currentTable.append(line).append("\n");
                if (line.trim().isEmpty()) {
                    inTable = false;
                    tables.add(currentTable.toString());
                    currentTable = new StringBuilder();
                }
            }
        }

        return tables;
    }

    private List<String> extractRelevantParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        String[] parts = text.split("\\R{2,}");

        for (String part : parts) {
            if (!part.isEmpty() && !"\t".equals(part)) {
                paragraphs.add(part);
            }
        }

        return paragraphs;
    }

    private List<TextSegment> processTableContents(List<String> tableContents) {
        List<TextSegment> segments = new ArrayList<>();
        for (String table : tableContents) {
            segments.add(TextSegment.textSegment(table));
        }
        return segments;
    }

    private List<TextSegment> processParagraphs(List<String> paragraphs) {
        List<TextSegment> segments = new ArrayList<>();
        for (String paragraph : paragraphs) {
            segments.add(TextSegment.textSegment(paragraph));
        }
        return segments;
    }

}