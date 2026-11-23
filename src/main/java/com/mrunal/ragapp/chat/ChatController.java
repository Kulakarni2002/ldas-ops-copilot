package com.mrunal.ragapp.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatClient chatClient;

    // Constructor injection: ChatClient.Builder is a bean created by Spring AI's auto-configuration
    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String q) {
        long start = System.currentTimeMillis();

        ChatResponse response = chatClient.prompt()
                .user(q)                 // the question
                .call()                  // send it to Gemini, wait for the answer
                .chatResponse();         // full response: text + metadata

        Usage usage = response.getMetadata().getUsage();
        log.info("Tokens -> prompt: {}, completion: {}, total: {} | {} ms",
                usage.getPromptTokens(),
                usage.getCompletionTokens(),
                usage.getTotalTokens(),
                System.currentTimeMillis() - start);

        return response.getResult().getOutput().getText();
    }
}