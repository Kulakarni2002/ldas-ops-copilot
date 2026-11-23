package com.mrunal.ragapp.ingest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
public class IngestionController {

    private final IngestionService ingestionService;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbc;

    public IngestionController(IngestionService ingestionService,
                               VectorStore vectorStore,
                               JdbcTemplate jdbc) {
        this.ingestionService = ingestionService;
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
    }

    // 1. Upload a PDF → chunks stored in pgvector
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
        int chunks = ingestionService.ingest(file);
        return Map.of("file", file.getOriginalFilename(), "chunksStored", chunks);
    }

    // 2. List uploaded documents with their chunk counts
    @GetMapping
    public List<Map<String, Object>> list() {
        return jdbc.queryForList("""
            SELECT metadata->>'file_name' AS file, COUNT(*) AS chunks
            FROM vector_store
            GROUP BY metadata->>'file_name'
            ORDER BY file
            """);
    }

    // 3. Delete all chunks of one document
    @DeleteMapping("/{fileName}")
    public Map<String, String> delete(@PathVariable String fileName) {
        var filter = new FilterExpressionBuilder().eq("file_name", fileName).build();
        vectorStore.delete(filter);
        return Map.of("deleted", fileName);
    }
}