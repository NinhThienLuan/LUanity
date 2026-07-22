package com.aiwrapper.executor;

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
}
