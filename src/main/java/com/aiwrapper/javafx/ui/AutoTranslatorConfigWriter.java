package com.aiwrapper.javafx.ui;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AutoTranslatorConfigWriter {

    public static void updateAutoTranslatorLanguages(File gameRoot, String fromLang, String targetLang) {
        File configDir = new File(gameRoot, "BepInEx/config");
        File configFile = new File(configDir, "AutoTranslatorConfig.ini");
        if (!configFile.exists()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
            List<String> output = new ArrayList<>();
            boolean generalBlock = false;
            boolean hasLanguage = false;
            boolean hasFromLanguage = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.equals("[General]")) {
                    generalBlock = true;
                    output.add(line);
                    continue;
                } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    generalBlock = false;
                }

                if (generalBlock) {
                    if (trimmed.startsWith("Language=")) {
                        output.add("Language=" + targetLang);
                        hasLanguage = true;
                    } else if (trimmed.startsWith("FromLanguage=")) {
                        output.add("FromLanguage=" + fromLang);
                        hasFromLanguage = true;
                    } else {
                        output.add(line);
                    }
                } else {
                    output.add(line);
                }
            }

            if (!hasLanguage || !hasFromLanguage) {
                for (int i = 0; i < output.size(); i++) {
                    if (output.get(i).trim().equals("[General]")) {
                        if (!hasLanguage) {
                            output.add(i + 1, "Language=" + targetLang);
                        }
                        if (!hasFromLanguage) {
                            output.add(i + 1, "FromLanguage=" + fromLang);
                        }
                        break;
                    }
                }
            }

            Files.write(configFile.toPath(), output, StandardCharsets.UTF_8);
            System.out.println(
                    "[AutoTranslatorConfigWriter] Updated AutoTranslatorConfig.ini language pair: " + fromLang + " -> "
                            + targetLang);
        } catch (Exception ex) {
            System.err.println(
                    "[AutoTranslatorConfigWriter] Failed to update AutoTranslatorConfig.ini: " + ex.getMessage());
        }
    }
}
