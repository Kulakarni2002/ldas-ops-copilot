package com.mrunal.ragapp.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class OpsTools {

    private static final Logger log = LoggerFactory.getLogger(OpsTools.class);

    private final VectorStore vectorStore;

    // Mock live status (in real life: call Prometheus / K8s / an actuator endpoint)
    private final Map<String, String> statuses = Map.of(
            "nifi", "DEGRADED - PutAzureDataLakeStorage failing with HTTP 403 since 10:40",
            "kafka-consumer", "HEALTHY - lag 12 messages",
            "ldas-api", "HEALTHY");

    private final List<String> tickets = new CopyOnWriteArrayList<>();
    private final AtomicInteger seq = new AtomicInteger(1000);

    public OpsTools(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "Get the current health status of an LDAS service. Valid names: nifi, kafka-consumer, ldas-api.")
    public String getServiceStatus(@ToolParam(description = "service name, e.g. nifi") String serviceName) {
        log.info("TOOL getServiceStatus({})", serviceName);
        return statuses.getOrDefault(serviceName.toLowerCase(), "UNKNOWN service: " + serviceName);
    }

    @Tool(description = "Search the ops runbooks for troubleshooting and fix steps for an error or symptom.")
    public String searchRunbook(@ToolParam(description = "the error or symptom to look up") String query) {
        log.info("TOOL searchRunbook({})", query);
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(3).similarityThreshold(0.5).build());
        if (docs.isEmpty()) return "No runbook entry found.";
        return docs.stream()
                .map(d -> "[" + d.getMetadata().get("file_name") + ", page "
                        + d.getMetadata().get("page_number") + "] " + d.getText())
                .collect(Collectors.joining("\n\n"));
    }

    @Tool(description = "Create an incident ticket. Use only when the user asks for one, or a service is DEGRADED or DOWN and the user wants action taken.")
    public String createIncidentTicket(
            @ToolParam(description = "affected service") String service,
            @ToolParam(description = "severity: P1, P2, P3 or P4") String severity,
            @ToolParam(description = "short summary including the suspected cause and fix steps") String summary) {
        String id = "INC-" + seq.incrementAndGet();
        tickets.add(id + " | " + severity + " | " + service + " | " + summary);
        log.info("TOOL createIncidentTicket -> {}", id);
        return "Created " + id;
    }

    @Tool(description = "List all open incident tickets.")
    public List<String> listOpenIncidents() {
        log.info("TOOL listOpenIncidents()");
        return tickets;
    }
}