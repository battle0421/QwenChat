package org.qwen.aiqwen.service.impl;

import dev.langchain4j.data.segment.TextSegment;
import io.pinecone.clients.Pinecone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SparseVectorService {

    private final Map<String, Integer> idf = new HashMap<>();
    private int totalDocuments = 0;

    public Map<String, Float> generateSparseVector(String text) {
        String[] tokens = tokenize(text);
        Map<String, Float> vector = new HashMap<>();

        for (String token : tokens) {
            vector.put(token, vector.getOrDefault(token, 0f) + 1.0f);
        }

        float totalTokens = tokens.length;
        for (Map.Entry<String, Float> entry : vector.entrySet()) {
            float tf = entry.getValue() / totalTokens;
            float idfValue = calculateIdf(entry.getKey());
            entry.setValue(tf * idfValue);
        }

        return vector;
    }

    private String[] tokenize(String text) {
        return text.toLowerCase()
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .split("\\s+");
    }

    private float calculateIdf(String term) {
        int docFreq = idf.getOrDefault(term, 0);
        return (float) Math.log((1.0 + totalDocuments) / (1.0 + docFreq)) + 1.0f;
    }

    public void updateIdf(List<String> documents) {
        totalDocuments += documents.size();

        for (String doc : documents) {
            String[] tokens = tokenize(doc);
            Map<String, Boolean> termInDoc = new HashMap<>();

            for (String token : tokens) {
                termInDoc.put(token, true);
            }

            for (String term : termInDoc.keySet()) {
                idf.put(term, idf.getOrDefault(term, 0) + 1);
            }
        }
    }
}
