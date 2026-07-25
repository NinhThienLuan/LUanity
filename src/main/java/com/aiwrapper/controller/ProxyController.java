package com.aiwrapper.controller;

import com.aiwrapper.executor.TranslateExecutor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
public class ProxyController {

    private final TranslateExecutor translateExecutor;

    public ProxyController(TranslateExecutor translateExecutor) {
        this.translateExecutor = translateExecutor;
    }

    @GetMapping("/api/v1/proxy/translate")
    public CompletableFuture<ResponseEntity<String>> translateGet(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        if (!translateExecutor.isProxyActive()) {
            return CompletableFuture.completedFuture(ResponseEntity.ok(""));
        }
        if (text == null || text.trim().isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.ok(""));
        }
        String time = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(java.time.LocalTime.now());
        System.out.println(
                time + "  INFO --- [proxy] : HTTP GET Proxy request: [" + from + " -> " + to + "] text: " + text);
        return translateExecutor.translateSingleAsync(text, Map.of())
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("[ERROR] " + ex.getMessage()));
    }

    @PostMapping("/api/v1/proxy/translate")
    public CompletableFuture<ResponseEntity<String>> translatePost(
            @RequestBody(required = false) String rawBody,
            @RequestParam(value = "text", required = false) String textParam) {
        if (!translateExecutor.isProxyActive()) {
            return CompletableFuture.completedFuture(ResponseEntity.ok(""));
        }
        String query = textParam;
        if (query == null || query.trim().isEmpty()) {
            query = rawBody;
        }
        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.ok(""));
        }
        String time = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(java.time.LocalTime.now());
        System.out.println(time + "  INFO --- [proxy] : HTTP POST Proxy request: " + query);

        if (query.trim().startsWith("[")) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<String> texts = mapper.readValue(query, new TypeReference<List<String>>() {
                });
                if (texts != null && !texts.isEmpty()) {
                    List<CompletableFuture<String>> futures = texts.stream()
                            .map(item -> translateExecutor.translateSingleAsync(item, Map.of()))
                            .collect(Collectors.toList());

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> {
                                List<String> results = futures.stream()
                                        .map(CompletableFuture::join)
                                        .collect(Collectors.toList());
                                try {
                                    String jsonResponse = mapper.writeValueAsString(results);
                                    return ResponseEntity.ok(jsonResponse);
                                } catch (Exception ex) {
                                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                            .body("[ERROR] Serialization failed: " + ex.getMessage());
                                }
                            });
                }
            } catch (Exception ignored) {
            }
        }

        return translateExecutor.translateSingleAsync(query, Map.of())
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("[ERROR] " + ex.getMessage()));
    }

    @GetMapping("/translate_a/single")
    public CompletableFuture<ResponseEntity<String>> handleGoogleMock(
            @RequestParam(value = "q") String query,
            @RequestParam(value = "sl") String from,
            @RequestParam(value = "tl") String to) {
        if (!translateExecutor.isProxyActive()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.ok("[[[\"\", \"\", null, null, 3]]]"));
        }
        if (query == null || query.trim().isEmpty()) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.ok("[[[\"\", \"\", null, null, 3]]]"));
        }

        String time = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(java.time.LocalTime.now());
        System.out.println(
                time + "  INFO --- [proxy] : (Google Mock Batch) HTTP GET Request: [" + from + " -> " + to + "]");

        String[] originals = query.split("\n", -1);
        List<CompletableFuture<String>> futures = java.util.Arrays.stream(originals)
                .map(line -> translateExecutor.translateSingleAsync(line, Map.of()))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<List<Object>> entries = new java.util.ArrayList<>();
                    for (int i = 0; i < originals.length; i++) {
                        String orig = originals[i];
                        String trans = futures.get(i).join();
                        List<Object> entry = new java.util.ArrayList<>();
                        entry.add(trans);
                        entry.add(orig);
                        entry.add(null);
                        entry.add(null);
                        entry.add(3);
                        entries.add(entry);
                    }

                    List<Object> wrapper = List.of(entries);

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        String jsonResponse = mapper.writeValueAsString(wrapper);
                        return ResponseEntity.ok(jsonResponse);
                    } catch (Exception ex) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
                    }
                })
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(""));
    }
}
