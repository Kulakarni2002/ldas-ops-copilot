package com.mrunal.ragapp.rag;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private static final String SYSTEM_PROMPT = """
        You are an operations assistant for the LDAS lab data platform.
        Answer ONLY using the context below.
        If the answer is not in the context, say: "I couldn't find this in the uploaded documents."
        Keep the answer short and practical, as steps where possible.
        """;

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public RagService(VectorStore vectorStore, ChatClient.Builder builder) {
        this.vectorStore = vectorStore;
        this.chatClient = builder.build();
    }

    public RagAnswer ask(String question) {
        // 1 + 2. Embed the question and find the nearest chunks
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(4)
                        .similarityThreshold(0.5)
                        .build());

        // No relevant chunks → don't call the LLM at all
        if (docs.isEmpty()) {
            return new RagAnswer("I couldn't find this in the uploaded documents.", List.of());
        }

        // 3. Build the context, tagging each chunk with its source
        String context = docs.stream()
                .map(d -> "[" + d.getMetadata().get("file_name") + ", page "
                        + d.getMetadata().get("page_number") + "]\n" + d.getText())
                .collect(Collectors.joining("\n\n"));

        // 4. Ask Gemini with the context
        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user("CONTEXT:\n" + context + "\n\nQUESTION: " + question)
                .call()
                .content();

        // Sources for the UI / citations
        List<Source> sources = docs.stream()
                .map(d -> new Source(
                        String.valueOf(d.getMetadata().get("file_name")),
                        String.valueOf(d.getMetadata().get("page_number")),
                        d.getScore()))
                .toList();

        return new RagAnswer(answer, sources);
    }

    public record Source(String file, String page, Double score) {}
    public record RagAnswer(String answer, List<Source> sources) {}
}