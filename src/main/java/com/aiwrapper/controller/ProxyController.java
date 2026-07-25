package com.aiwrapper.controller;

import com.aiwrapper.executor.TranslateExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/proxy")
public class ProxyController {

    private final TranslateExecutor translateExecutor;

    public ProxyController(TranslateExecutor translateExecutor) {
        this.translateExecutor = translateExecutor;
    }

    @GetMapping("/translate")
    public java.util.concurrent.CompletableFuture<ResponseEntity<String>> translateGet(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        if (!translateExecutor.isProxyActive()) {
            return java.util.concurrent.CompletableFuture.completedFuture(ResponseEntity.ok(""));
        }
        if (text == null || text.trim().isEmpty()) {
            return java.util.concurrent.CompletableFuture.completedFuture(ResponseEntity.ok(""));
        }
        String time = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(java.time.LocalTime.now());
        System.out.println(
                time + "  INFO --- [proxy] : HTTP GET Proxy request: [" + from + " -> " + to + "] text: " + text);
        return translateExecutor.translateSingleAsync(text, Map.of())
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("[ERROR] " + ex.getMessage()));
    }

    @PostMapping("/translate")
    public java.util.concurrent.CompletableFuture<ResponseEntity<String>> translatePost(
            @RequestBody(required = false) String rawBody,
            @RequestParam(value = "text", required = false) String textParam) {
        if (!translateExecutor.isProxyActive()) {
            return java.util.concurrent.CompletableFuture.completedFuture(ResponseEntity.ok(""));
        }
        String query = textParam;
        if (query == null || query.trim().isEmpty()) {
            query = rawBody;
        }
        if (query == null || query.trim().isEmpty()) {
            return java.util.concurrent.CompletableFuture.completedFuture(ResponseEntity.ok(""));
        }
        String time = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(java.time.LocalTime.now());
        System.out.println(time + "  INFO --- [proxy] : HTTP POST Proxy request: " + query);
        return translateExecutor.translateSingleAsync(query, Map.of())
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("[ERROR] " + ex.getMessage()));
    }
}
