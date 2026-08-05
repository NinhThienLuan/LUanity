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
        } catch (Exception ex) {
            System.err.println(
                    "[AutoTranslatorConfigWriter] Failed to update AutoTranslatorConfig.ini: " + ex.getMessage());
        }
    }

    public static void updateAutoTranslatorEndpoint(File gameRoot, String provider) {
        File configDir = new File(gameRoot, "BepInEx/config");
        File configFile = new File(configDir, "AutoTranslatorConfig.ini");
        if (!configFile.exists()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
            List<String> output = new ArrayList<>();
            boolean serviceBlock = false;
            boolean hasEndpoint = false;
            boolean hasGoogleV2 = false;

            String targetEndpoint = "CustomTranslate";
            if (provider != null && (provider.equalsIgnoreCase("googletranslate") || provider.equalsIgnoreCase("google")
                    || provider.equalsIgnoreCase("google translate"))) {
                targetEndpoint = "GoogleTranslateV2";
            }

            for (String line : lines) {
                String trimmed = line.trim();

                if (trimmed.equals("[Service]")) {
                    serviceBlock = true;
                    output.add(line);
                    continue;
                } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    serviceBlock = false;
                }

                if (trimmed.equals("[GoogleV2]")) {
                    hasGoogleV2 = true;
                }

                if (serviceBlock) {
                    if (trimmed.startsWith("Endpoint=")) {
                        output.add("Endpoint=" + targetEndpoint);
                        hasEndpoint = true;
                    } else {
                        output.add(line);
                    }
                } else {
                    output.add(line);
                }
            }

            if (!hasEndpoint) {
                for (int i = 0; i < output.size(); i++) {
                    if (output.get(i).trim().equals("[Service]")) {
                        output.add(i + 1, "Endpoint=" + targetEndpoint);
                        break;
                    }
                }
            }

            if (!hasGoogleV2) {
                output.add("");
                output.add("[Google]");
                output.add("ServiceUrl=");
                output.add("");
                output.add("[GoogleV2]");
                output.add("ServiceUrl=");
                output.add("RPCID=MkEWBc");
                output.add("VERSION=boq_translate-webserver_20210323.10_p0");
                output.add("UseSimplest=False");
            }

            Files.write(configFile.toPath(), output, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            System.err.println("[AutoTranslatorConfigWriter] Failed to update AutoTranslatorConfig.ini endpoint: "
                    + ex.getMessage());
        }
    }
}
