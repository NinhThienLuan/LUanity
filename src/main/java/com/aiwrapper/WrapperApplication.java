package com.aiwrapper;

import com.aiwrapper.config.AiConfig;
import com.aiwrapper.javafx.service.JavaFxLauncher;
import javafx.application.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

@SpringBootApplication
@EnableConfigurationProperties(AiConfig.class)
public class WrapperApplication {
    public static void main(String[] args) {
        // Load .env file manually if exists
        java.io.File dotEnv = new java.io.File(".env");
        if (dotEnv.exists()) {
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(dotEnv.toPath(),
                        java.nio.charset.StandardCharsets.UTF_8);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = trimmed.indexOf('=');
                    if (eqIdx > 0) {
                        String key = trimmed.substring(0, eqIdx).trim();
                        String val = trimmed.substring(eqIdx + 1).trim();
                        if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        } else if (val.startsWith("'") && val.endsWith("'") && val.length() >= 2) {
                            val = val.substring(1, val.length() - 1);
                        }
                        System.setProperty(key, val);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        boolean launchGui = true;
        try (InputStream inputStream = WrapperApplication.class.getResourceAsStream("/application.yml")) {
            if (inputStream != null) {
                Yaml yaml = new Yaml();
                Map<String, Object> obj = yaml.load(inputStream);
                if (obj != null && obj.containsKey("app")) {
                    Map<?, ?> appMap = (Map<?, ?>) obj.get("app");
                    if (appMap != null && appMap.containsKey("gui")) {
                        launchGui = Boolean.parseBoolean(String.valueOf(appMap.get("gui")));
                    }
                }
            }
        } catch (Exception e) {
            // Fallback to true
        }

        for (String arg : args) {
            if ("--cli".equals(arg)) {
                launchGui = false;
            } else if ("--gui".equals(arg)) {
                launchGui = true;
            }
        }

        if (launchGui) {
            System.setProperty("java.awt.headless", "false");
            Application.launch(JavaFxLauncher.class, args);
        } else {
            System.setProperty("app.gui", "false");
            SpringApplication.run(WrapperApplication.class, args);
        }
    }
}
