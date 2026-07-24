package com.aiwrapper.javafx.ui;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class AutoTranslatorConfigWriterTest {

    @Test
    public void testUpdateAutoTranslatorEndpoint() throws Exception {
        File tempDir = Files.createTempDirectory("bepinex-test").toFile();
        tempDir.deleteOnExit();

        File configDir = new File(tempDir, "BepInEx/config");
        configDir.mkdirs();

        File configFile = new File(configDir, "AutoTranslatorConfig.ini");

        // Write initial config with CustomTranslate
        String initialContent = "[Service]\nEndpoint=CustomTranslate\nFallbackEndpoint=\n\n[General]\nLanguage=vi\n";
        Files.writeString(configFile.toPath(), initialContent, StandardCharsets.UTF_8);

        // Update to googletranslate
        AutoTranslatorConfigWriter.updateAutoTranslatorEndpoint(tempDir, "googletranslate");

        // Verify config changed to GoogleTranslateV2 and GoogleV2 section appended
        List<String> lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(lines.stream().anyMatch(l -> l.equals("Endpoint=GoogleTranslateV2")),
                "Endpoint should be GoogleTranslateV2");
        assertTrue(lines.stream().anyMatch(l -> l.equals("[GoogleV2]")), "GoogleV2 block should be present");

        // Update back to ollama
        AutoTranslatorConfigWriter.updateAutoTranslatorEndpoint(tempDir, "ollama");

        lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
        assertTrue(lines.stream().anyMatch(l -> l.equals("Endpoint=CustomTranslate")),
                "Endpoint should be CustomTranslate");
    }
}
