package com.mrunal.ragapp.ingest;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class IngestionService {

    private final VectorStore vectorStore;

    public IngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int ingest(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();

        // [1] READ: PDF bytes → one Document per page
        var resource = new ByteArrayResource(file.getBytes()) {
            @Override public String getFilename() { return fileName; }
        };
        List<Document> pages = new PagePdfDocumentReader(resource).get();

        // [2] CHUNK: pages → smaller pieces
        List<Document> chunks = new TokenTextSplitter().apply(pages);

        // tag every chunk with its source file (for citations later)
        chunks.forEach(c -> c.getMetadata().put("file_name", fileName));

        var filter = new org.springframework.ai.vectorstore.filter.FilterExpressionBuilder()
        .eq("file_name", fileName).build();
vectorStore.delete(filter);

        // [3] EMBED + [4] STORE: one call does both
        vectorStore.add(chunks);

        return chunks.size();
    }
}