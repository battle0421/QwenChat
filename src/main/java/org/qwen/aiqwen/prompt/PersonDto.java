package org.qwen.aiqwen.prompt;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class PersonDto {

    @Description("姓名")
    private String name;
    @Description("年龄")
    private String age;
}
