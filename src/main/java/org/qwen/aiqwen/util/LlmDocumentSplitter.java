package org.qwen.aiqwen.util;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.List;

public class LlmDocumentSplitter implements DocumentSplitter {
    @Override
    public List<TextSegment> split(Document document) {
        List<TextSegment> textSegments = new ArrayList<>();
        String [] lines = document.text().split("\\s*\\R\\s*\\R\\s*");
        for (String line : lines){
            textSegments.add(TextSegment.textSegment(line));
        }
        return textSegments;
    }
}
