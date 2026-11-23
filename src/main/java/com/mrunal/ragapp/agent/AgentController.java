package com.mrunal.ragapp.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentController {

    private static final String SYSTEM_PROMPT = """
        You are an ops copilot for the LDAS lab data platform.
        Use the tools to check live service status, look up runbook steps and manage incidents.
        Base fix steps only on runbook results and mention the runbook source.
        Never invent ticket IDs; only report IDs returned by the createIncidentTicket tool.
        """;

    private final ChatClient chatClient;
    private final OpsTools opsTools;

    public AgentController(ChatClient.Builder builder, OpsTools opsTools) {
        this.chatClient = builder.build();
        this.opsTools = opsTools;
    }

    @GetMapping("/agent")
    public String agent(@RequestParam String q) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(q)
                .tools(opsTools)          // ← the model can now call these methods
                .call()
                .content();
    }
}