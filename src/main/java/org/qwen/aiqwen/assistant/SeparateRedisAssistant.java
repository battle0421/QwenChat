package org.qwen.aiqwen.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import org.qwen.aiqwen.prompt.PersonDto;

@AiService(wiringMode = AiServiceWiringMode.EXPLICIT,
chatModel = "openAiChatModel",chatMemoryProvider = "redisChatMemoryProvider")
public interface SeparateRedisAssistant {
    @SystemMessage(fromResource="sysMessage.txt")
    String chat(@MemoryId String memoryId,@UserMessage String message);

    @UserMessage("extract information about a person from {{message}}")
    PersonDto extractPerson(@MemoryId String memoryId, @V("message") String message);

    @UserMessage("给出结果,好的或者不好的 {{it}}")
    Boolean isGoodOrBad( String message);


}
