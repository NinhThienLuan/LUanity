package com.aiwrapper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class AppConfigManager {
    private static final File CONFIG_FILE = new File("data/app_config.json");
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static AppConfig currentConfig = null;

    public static synchronized AppConfig load() {
        if (currentConfig != null) {
            return currentConfig;
        }

        currentConfig = new AppConfig();

        // 1. Perform migrations if legacy files exist and config doesn't
        boolean migrated = false;
        if (!CONFIG_FILE.exists()) {
            File gamePathFile = new File("data/game_path.txt");
            if (gamePathFile.exists()) {
                try {
                    String path = Files.readString(gamePathFile.toPath(), StandardCharsets.UTF_8).trim();
                    currentConfig.setActiveGamePath(path);
                    migrated = true;
                } catch (Exception ignored) {
                }
            }

            File geminiKeyFile = new File("data/gemini_key.txt");
            if (geminiKeyFile.exists()) {
                try {
                    String key = Files.readString(geminiKeyFile.toPath(), StandardCharsets.UTF_8).trim();
                    currentConfig.setGeminiApiKey(key);
                    migrated = true;
                } catch (Exception ignored) {
                }
            }

            File openaiKeyFile = new File("data/openapi_key.txt");
            if (openaiKeyFile.exists()) {
                try {
                    String key = Files.readString(openaiKeyFile.toPath(), StandardCharsets.UTF_8).trim();
                    currentConfig.setOpenaiApiKey(key);
                    migrated = true;
                } catch (Exception ignored) {
                }
            }

            if (migrated) {
                save(currentConfig);
                // Clean up legacy files
                try {
                    Files.deleteIfExists(gamePathFile.toPath());
                    Files.deleteIfExists(geminiKeyFile.toPath());
                    Files.deleteIfExists(openaiKeyFile.toPath());
                    System.out.println("[AppConfigManager] Legacies migrated and removed successfully.");
                } catch (Exception ignored) {
                }
                return currentConfig;
            }
        }

        // 2. Load existing config mapping
        if (CONFIG_FILE.exists() && CONFIG_FILE.length() > 0) {
            try {
                currentConfig = MAPPER.readValue(CONFIG_FILE, AppConfig.class);
            } catch (Exception e) {
                System.err.println("[AppConfigManager] Failed to read app_config.json: " + e.getMessage());
            }
        }
        return currentConfig;
    }

    public static synchronized void save(AppConfig config) {
        currentConfig = config;
        try {
            File parent = CONFIG_FILE.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            MAPPER.writeValue(CONFIG_FILE, config);
        } catch (Exception e) {
            System.err.println("[AppConfigManager] Failed to save config: " + e.getMessage());
        }
    }
}
