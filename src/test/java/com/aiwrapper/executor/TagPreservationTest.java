package com.aiwrapper.executor;

import com.aiwrapper.template.PromptTemplate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TagPreservationTest {

    @Test
    public void testPreserveAndRestore() {
        TranslateExecutor.TagPreserver preserver = new TranslateExecutor.TagPreserver();
        String original = "Hello {0}, <color=red>welcome</color>! <b>Enjoy</b> your stay.";

        String preserved = preserver.preserve(original);
        assertEquals("Hello [[TAG_0]], [[TAG_1]]welcome[[TAG_2]]! [[TAG_3]]Enjoy[[TAG_4]] your stay.", preserved);

        // Simulating translation result that keeps the placeholders
        String translated = "Xin chào [[TAG_0]], [[TAG_1]]chào mừng[[TAG_2]]! [[TAG_3]]Tận hưởng[[TAG_4]] kỳ nghỉ của bạn.";
        String restored = preserver.restore(translated);

        assertEquals("Xin chào {0}, <color=red>chào mừng</color>! <b>Tận hưởng</b> kỳ nghỉ của bạn.", restored);
    }

    @Test
    public void testPreserveAndRestoreMeasurementUnits() {
        TranslateExecutor.TagPreserver preserver = new TranslateExecutor.TagPreserver();
        String original = "Speed is 120km/h, weight is 50 kg, height is 1.8m, frequency is 60Hz.";
        String preserved = preserver.preserve(original);
        assertEquals("Speed is [[TAG_0]], weight is [[TAG_1]], height is [[TAG_2]], frequency is [[TAG_3]].",
                preserved);

        String translated = "Tốc độ là [[TAG_0]], cân nặng là [[TAG_1]], chiều cao là [[TAG_2]], tần số là [[TAG_3]].";
        String restored = preserver.restore(translated);
        assertEquals("Tốc độ là 120km/h, cân nặng là 50 kg, chiều cao là 1.8m, tần số là 60Hz.", restored);
    }

    @Test
    public void testPreserveAndRestoreWithMismatchedSpacingAndCase() {
        TranslateExecutor.TagPreserver preserver = new TranslateExecutor.TagPreserver();
        String original = "Hello {player_name}!";
        String preserved = preserver.preserve(original);
        assertEquals("Hello [[TAG_0]]!", preserved);

        // LLM adds space or changes capitalization
        String translated = "Xin chào [[ tag_0 ]]!";
        String restored = preserver.restore(translated);
        assertEquals("Xin chào {player_name}!", restored);
    }

    @Test
    public void testPreserveAndRestoreQuantityPatterns() {
        TranslateExecutor.TagPreserver preserver = new TranslateExecutor.TagPreserver();
        String original = "Standard Bullet (MAG) x8, MP7a x2, Submit: Snowball x 20 Owned: 0";
        String preserved = preserver.preserve(original);
        assertEquals("Standard Bullet (MAG) [[TAG_0]], MP7a [[TAG_1]], Submit: Snowball [[TAG_2]] Owned: 0", preserved);

        String translated = "Đạn tiêu chuẩn (MAG) [[TAG_0]], MP7a [[TAG_1]], Gửi: Bóng tuyết [[TAG_2]] Sở hữu: 0";
        String restored = preserver.restore(translated);
        assertEquals("Đạn tiêu chuẩn (MAG) x8, MP7a x2, Gửi: Bóng tuyết x 20 Sở hữu: 0", restored);
    }

    @Test
    public void testCacheDeserializationAndSerialization() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("cache-test", ".json");
        tempFile.deleteOnExit();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<String, String> cacheMap = new java.util.HashMap<>();
        cacheMap.put("hello", "xin chào");
        cacheMap.put("world", "thế giới");

        mapper.writeValue(tempFile, cacheMap);

        java.util.Map<String, String> loaded = mapper.readValue(tempFile,
                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {
                });
        assertEquals("xin chào", loaded.get("hello"));
        assertEquals("thế giới", loaded.get("world"));
    }

    @Test
    public void testGlossaryMatchingAndReplacement() {
        java.util.Map<String, String> glossaryMap = new java.util.HashMap<>();
        glossaryMap.put("Dragon", "Rồng");
        glossaryMap.put("Sword", "Kiếm");

        String text = "The Dragon Sword of destiny.";
        java.util.Map<String, String> matchedGlossary = new java.util.HashMap<>();
        for (java.util.Map.Entry<String, String> entry : glossaryMap.entrySet()) {
            String key = entry.getKey();
            if (text.toLowerCase().contains(key.toLowerCase())) {
                matchedGlossary.put(key, entry.getValue());
            }
        }

        assertEquals(2, matchedGlossary.size());
        assertEquals("Rồng", matchedGlossary.get("Dragon"));
        assertEquals("Kiếm", matchedGlossary.get("Sword"));

        // Test post-translation replacement
        String translated = "Một chiếc Dragon Sword định mệnh.";
        for (java.util.Map.Entry<String, String> entry : matchedGlossary.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();
            translated = translated.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(key) + "\\b", val);
        }
        assertEquals("Một chiếc Rồng Kiếm định mệnh.", translated);
    }

    @Test
    public void testIsRefusalOrJunk() {
        TranslateExecutor executor = new TranslateExecutor(null);
        assertTrue(executor.isRefusalOrJunk("Please provide the English game text you want me to translate."));
        assertTrue(executor.isRefusalOrJunk("Unable to translate the given term."));
        assertTrue(executor.isRefusalOrJunk("Sorry, but I cannot fulfill the request due to unrelated details."));
        assertFalse(executor.isRefusalOrJunk("Tiếp tục"));
        assertFalse(executor.isRefusalOrJunk("Cài đặt"));
    }

    @Test
    public void testImportTranslations() throws Exception {
        TranslateExecutor executor = new TranslateExecutor(null);
        java.io.File tempFile = java.io.File.createTempFile("cache-import-test", ".json");
        tempFile.deleteOnExit();
        executor.setActiveCacheFile(tempFile);

        java.util.Map<String, String> newTrans = new java.util.HashMap<>();
        newTrans.put("Start Game", "Bắt đầu");
        newTrans.put("Settings", "Cài đặt");

        executor.importTranslations(newTrans);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<String, String> loaded = mapper.readValue(tempFile,
                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {
                });
        assertEquals("Bắt đầu", loaded.get("Start Game"));
        assertEquals("Cài đặt", loaded.get("Settings"));
    }

    @Test
    public void testCleanRawTranslation() {
        TranslateExecutor executor = new TranslateExecutor(null);

        // Test markdown wrapper stripping
        assertEquals("Tôi sẽ kể", executor.cleanRawTranslation("Original", "**Tôi sẽ kể**"));
        assertEquals("Tôi sẽ kể", executor.cleanRawTranslation("Original", "  **  Tôi sẽ kể  **  "));
        assertEquals("Tôi sẽ kể", executor.cleanRawTranslation("Original", "\"Tôi sẽ kể\""));
        assertEquals("Tôi sẽ kể", executor.cleanRawTranslation("Original", "**\"Tôi sẽ kể\"**"));
        assertEquals("Tôi sẽ kể", executor.cleanRawTranslation("Original", "\"**Tôi sẽ kể**\""));

        // Test parenthetical notes and explanations
        assertEquals("Tôi sẽ kể cho quái vật biển", executor.cleanRawTranslation("Original",
                "Tôi sẽ kể cho quái vật biển (This translates directly to \"I will tell the sea monster\")"));
        assertEquals("Tôi sẽ kể", executor.cleanRawTranslation("Original", "Tôi sẽ kể (Note: translation details)"));
        assertEquals("Tôi sẽ kể", executor.cleanRawTranslation("Original", "Tôi sẽ kể [Literally: I will tell]"));

        // Test normal parenthesis preservation
        assertEquals("Mở (Open)", executor.cleanRawTranslation("Original", "Mở (Open)"));
        assertEquals("Sức mạnh (10)", executor.cleanRawTranslation("Original", "Sức mạnh (10)"));

        // Test prefixes integration
        assertEquals("Tôi sẽ kể", executor.cleanRawTranslation("Original", "**Vietnamese: \"Tôi sẽ kể\"**"));
        assertEquals("Tôi sẽ kể", executor.cleanRawTranslation("Original", "bản dịch: Tôi sẽ kể"));
    }

    @Test
    public void testLanguagePairPromptRendering() {
        TranslateExecutor executor = new TranslateExecutor(null);
        executor.setLanguagePair("ZH/VI");
        assertEquals("zh", executor.getFromLang());
        assertEquals("vi", executor.getToLang());

        // Render template with placeholders
        String template = "Translate {from} to {to}: {text}";
        PromptTemplate pt = new PromptTemplate(template);
        String rendered = pt.render(java.util.Map.of(
                "from", "Chinese",
                "to", "Vietnamese",
                "text", "你好"));
        assertEquals("Translate Chinese to Vietnamese: 你好", rendered);
    }

    @Test
    public void testQualityGateOptAndBypasses() throws Exception {
        TranslateExecutor executor = new TranslateExecutor(null);

        // 1. BBCode Tag Bypass Verification
        // Unclosed tag start
        assertTrue(executor.shouldBypassTranslation("Save Slot : 3 <size\\"));
        // Nesting mismatch
        assertTrue(executor.shouldBypassTranslation("<b><i>text</b></i>"));
        // Unopened tag end
        assertTrue(executor.shouldBypassTranslation("text</b>"));
        // Fully balanced
        assertFalse(executor.shouldBypassTranslation("<b><i>text</i></b>"));
        // Mathematical / Standalone operator
        assertFalse(executor.shouldBypassTranslation("HP < 50%"));
        assertFalse(executor.shouldBypassTranslation("Damage > 100"));

        // 2. Parenthetical explanation gate verification
        executor.setLanguagePair("EN/VI");

        // Explanation added
        assertTrue(executor.isInvalidTranslation("BuildingName", "BuildingName (không thay đổi)"));
        assertTrue(executor.isInvalidTranslation("小鸭子adasdasdasdasd",
                "小鸭子adasdasdasdasd (Không thể dịch chính xác do có lỗi)"));
        // Normal parenthesis allowed
        assertFalse(executor.isInvalidTranslation("Frames Per Second", "FPS (khung hình/giây)"));
    }

    @Test
    public void testConcurrentCacheSync() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("concurrent-cache-test", ".json");
        tempFile.deleteOnExit();

        TranslateExecutor executor = new TranslateExecutor(
                new com.aiwrapper.provider.AiProviderFactory(new com.aiwrapper.config.AiConfig()) {
                    @Override
                    public com.aiwrapper.provider.AiProvider get() {
                        return new com.aiwrapper.provider.AiProvider() {
                            @Override
                            public String complete(String prompt, java.util.Map<String, Object> options) {
                                try {
                                    Thread.sleep(50);
                                } catch (InterruptedException e) {
                                }
                                return "Translation"; // Mock translation
                            }
                        };
                    }
                });
        executor.setActiveCacheFile(tempFile);
        executor.setLanguagePair("EN/VI");

        int threadCount = 10;
        java.util.List<java.util.concurrent.CompletableFuture<String>> futures = new java.util.ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final String text = "Key_" + i;
            futures.add(executor.translateSingleAsync(text, java.util.Map.of("bypassCache", true)));
        }

        java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                .join();

        // Sleep to allow debounce flush scheduler to run
        Thread.sleep(800);

        // Force call flushToDiskNow to run immediately in case scheduler is still
        // waiting
        // executor.flushToDiskNow(tempFile.getAbsolutePath(), tempFile);

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<String, String> cachedData = mapper.readValue(tempFile,
                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, String>>() {
                });

        assertEquals(threadCount, cachedData.size());
        for (int i = 0; i < threadCount; i++) {
            assertEquals("Translation", cachedData.get("Key_" + i));
        }
    }
}
