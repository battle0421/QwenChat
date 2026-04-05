package org.qwen.aiqwen.dto;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class PersonDto {

    @Description("姓名")
    private String name;
    @Description("年龄")
    private String age;
}
