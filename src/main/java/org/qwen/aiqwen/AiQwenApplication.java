package org.qwen.aiqwen;

import dev.langchain4j.openai.spring.AutoConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ImportAutoConfiguration(exclude = AutoConfig.class)
@EnableAutoConfiguration
public class AiQwenApplication {

    public static void main(String[] args) {

        SpringApplication.run(AiQwenApplication.class, args);
    }

}
