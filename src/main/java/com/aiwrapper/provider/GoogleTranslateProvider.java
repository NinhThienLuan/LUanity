package com.aiwrapper.provider;

import com.aiwrapper.config.AiConfig;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleTranslateProvider implements AiProvider {
    private final AiConfig.GoogleTranslate config;
    private final RestTemplate restTemplate;

    public GoogleTranslateProvider(AiConfig.GoogleTranslate config) {
        this.config = config;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public String complete(String prompt, Map<String, Object> options) throws Exception {
        String textToTranslate = prompt;
        if (prompt != null && prompt.contains(": ")) {
            textToTranslate = prompt.substring(prompt.indexOf(": ") + 2).trim();
        }

        String apiKey = config.getApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            String url = "https://translation.googleapis.com/language/translate/v2?key=" + apiKey.trim();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("q", List.of(textToTranslate));
            String target = "vi";
            if (options != null && options.containsKey("targetLanguage")) {
                target = String.valueOf(options.get("targetLanguage"));
            }
            requestBody.put("target", target);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("data")) {
                Map<?, ?> data = (Map<?, ?>) response.get("data");
                if (data != null && data.containsKey("translations")) {
                    List<?> translations = (List<?>) data.get("translations");
                    if (!translations.isEmpty()) {
                        Map<?, ?> translation = (Map<?, ?>) translations.get(0);
                        if (translation.containsKey("translatedText")) {
                            return String.valueOf(translation.get("translatedText"));
                        }
                    }
                }
            }
            throw new RuntimeException("Invalid response from official Google Translate API: " + response);
        } else {
            String target = "vi";
            if (options != null && options.containsKey("targetLanguage")) {
                target = String.valueOf(options.get("targetLanguage"));
            }
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl={tl}&dt=t&q={q}";

            @SuppressWarnings("unchecked")
            List<?> response = restTemplate.getForObject(url, List.class, target, textToTranslate);
            if (response != null && !response.isEmpty()) {
                List<?> outerList = (List<?>) response.get(0);
                if (outerList != null && !outerList.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Object sentenceObj : outerList) {
                        if (sentenceObj instanceof List) {
                            List<?> sentenceList = (List<?>) sentenceObj;
                            if (!sentenceList.isEmpty()) {
                                sb.append(sentenceList.get(0));
                            }
                        }
                    }
                    return sb.toString();
                }
            }
            throw new RuntimeException("Invalid response from free Google Translate API");
        }
    }
}
