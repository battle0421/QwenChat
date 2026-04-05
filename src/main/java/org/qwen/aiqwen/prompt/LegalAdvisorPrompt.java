package org.qwen.aiqwen.prompt;

import dev.langchain4j.model.input.structured.StructuredPrompt;
import lombok.Data;

@Data
@StructuredPrompt("根据我国法律{{law}}法律，解答以下问题，{{question}}")
public class LegalAdvisorPrompt{

    private String question;
    private String law;
}
