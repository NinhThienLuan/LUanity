package com.aiwrapper;

import com.aiwrapper.config.AiConfig;
import com.aiwrapper.ui.JavaFxLauncher;
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
