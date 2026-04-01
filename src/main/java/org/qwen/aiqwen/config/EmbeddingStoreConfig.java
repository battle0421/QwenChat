package org.qwen.aiqwen.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import org.qwen.aiqwen.properties.QwenAPIkeyProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingStoreConfig {

    @Autowired
    public QwenAPIkeyProperties qwenAPIkeyProperties;
    @Autowired
    private EmbeddingModel embeddingModel;
//    @Bean
//    public EmbeddingStore<TextSegment> embeddingStore(){
//        EmbeddingStore<TextSegment> embeddingStore = PineconeEmbeddingStore.builder()
//                .apiKey(qwenAPIkeyProperties.getPineconeApiKey())
//                .index(qwenAPIkeyProperties.getPineconeEindexName())
//                .createIndex(PineconeServerlessIndexConfig.builder().cloud("AWS")
//                        .region("us-east-1").dimension(embeddingModel.dimension()).build())
//                .build();
//        return embeddingStore;
//    }
        @Bean
        public PineconeEmbeddingStore pineconeEmbeddingStore() throws Exception {


            return PineconeEmbeddingStore.builder().apiKey(qwenAPIkeyProperties.getPineconeApiKey())

                    .index(qwenAPIkeyProperties.getPineconeEindexName())
                    .createIndex(PineconeServerlessIndexConfig.builder().cloud("AWS")
                            .region("us-east-1").dimension(embeddingModel.dimension()).build())
                    .build();

    }
}
