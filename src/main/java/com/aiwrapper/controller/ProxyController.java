package com.aiwrapper.controller;

import com.aiwrapper.executor.TranslateExecutor;
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
    public String translateGet(
            @RequestParam(value = "text", required = false) String text,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        System.out.println("HTTP GET Proxy request: [" + from + " -> " + to + "] text: " + text);
        return translateExecutor.translateSingle(text, Map.of());
    }

    @PostMapping("/translate")
    public String translatePost(
            @RequestBody(required = false) String rawBody,
            @RequestParam(value = "text", required = false) String textParam) {
        String query = textParam;
        if (query == null || query.trim().isEmpty()) {
            query = rawBody;
        }
        if (query == null || query.trim().isEmpty()) {
            return "";
        }
        System.out.println("HTTP POST Proxy request: " + query);
        return translateExecutor.translateSingle(query, Map.of());
    }
}
