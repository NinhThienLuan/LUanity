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
    public ResponseEntity<String> translateGet(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        if (!translateExecutor.isProxyActive()) {
            return ResponseEntity.ok("");
        }
        if (text == null || text.trim().isEmpty()) {
            return ResponseEntity.ok("");
        }
        System.out.println("HTTP GET Proxy request: [" + from + " -> " + to + "] text: " + text);
        return ResponseEntity.ok(translateExecutor.translateSingle(text, Map.of()));
    }

    @PostMapping("/translate")
    public ResponseEntity<String> translatePost(
            @RequestBody(required = false) String rawBody,
            @RequestParam(value = "text", required = false) String textParam) {
        if (!translateExecutor.isProxyActive()) {
            return ResponseEntity.ok("");
        }
        String query = textParam;
        if (query == null || query.trim().isEmpty()) {
            query = rawBody;
        }
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok("");
        }
        System.out.println("HTTP POST Proxy request: " + query);
        return ResponseEntity.ok(translateExecutor.translateSingle(query, Map.of()));
    }
}
