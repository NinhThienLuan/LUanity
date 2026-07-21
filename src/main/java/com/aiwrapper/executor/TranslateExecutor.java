package com.aiwrapper.executor;

import com.aiwrapper.file.FileHandler;
import com.aiwrapper.file.FileHandlerFactory;
import com.aiwrapper.provider.AiProvider;
import com.aiwrapper.provider.AiProviderFactory;
import com.aiwrapper.template.PromptTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class TranslateExecutor implements BaseExecutor {

    private final AiProviderFactory aiFactory;
    private final PromptTemplate template = new PromptTemplate(
        "Dịch câu sau sang tiếng Việt, giữ văn phong game. CHỈ trả lời bằng bản dịch tiếng Việt, không giải thích gì thêm, không dùng ký tự Trung Quốc: {text}"
    );

    public TranslateExecutor(AiProviderFactory aiFactory) {
        this.aiFactory = aiFactory;
    }

    @Override
    public void execute(String inputPath, String outputPath, Map<String, Object> options) {
        FileHandler inHandler = FileHandlerFactory.get(inputPath);
        FileHandler outHandler = FileHandlerFactory.get(outputPath);
        AiProvider ai = aiFactory.get();

        List<Map<String, Object>> rows = inHandler.read(inputPath);
        List<Map<String, Object>> results = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Object textObj = row.get("text");
            if (textObj == null && !row.isEmpty()) {
                // Defensive fallback to first value if "text" key is absent
                textObj = row.values().iterator().next();
            }
            String text = textObj != null ? String.valueOf(textObj) : "";
            String prompt = template.render(Map.of("text", text));

            String translated;
            try {
                translated = ai.complete(prompt, options);
            } catch (Exception e) {
                translated = "[ERROR] " + e.getMessage();
            }

            Map<String, Object> resultRow = new LinkedHashMap<>(row);
            resultRow.put("translated", translated);
            results.add(resultRow);
        }

        outHandler.write(outputPath, results);
    }
}
