package com.aiwrapper.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class GlossaryPresetTest {

    private static File originalGlossary = new File("data/glossary.json");
    private static File backupGlossary = new File("data/glossary.json.bak");
    private static File originalPresetsDir = new File("data/presets");
    private static File tempPresetsDir = new File("data/presets_test_backup");

    @BeforeAll
    public static void setUp() throws Exception {
        // Back up glossary.json
        if (originalGlossary.exists()) {
            Files.copy(originalGlossary.toPath(), backupGlossary.toPath(), StandardCopyOption.REPLACE_EXISTING);
            originalGlossary.delete();
        }
        // Back up presets dir
        if (originalPresetsDir.exists()) {
            Files.move(originalPresetsDir.toPath(), tempPresetsDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        originalPresetsDir.mkdirs();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        // Cleanup generated test files
        File testPreset = new File("data/presets/test_theme.json");
        if (testPreset.exists()) {
            testPreset.delete();
        }
        if (originalGlossary.exists()) {
            originalGlossary.delete();
        }
        if (originalPresetsDir.exists()) {
            originalPresetsDir.delete();
        }

        // Restore backups
        if (backupGlossary.exists()) {
            Files.copy(backupGlossary.toPath(), originalGlossary.toPath(), StandardCopyOption.REPLACE_EXISTING);
            backupGlossary.delete();
        }
        if (tempPresetsDir.exists()) {
            Files.move(tempPresetsDir.toPath(), originalPresetsDir.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    public void testLoadGlossaryMapWithMtimeCaching() throws Exception {
        TranslateExecutor executor = new TranslateExecutor(null);

        // Scenario 1: File does not exist -> loads empty glossary
        Map<String, String> initial = executor.loadGlossaryMap();
        assertNotNull(initial);
        assertTrue(initial.isEmpty());

        // Scenario 2: Save map and load -> parses correctly
        Map<String, String> data = new HashMap<>();
        data.put("gold", "vàng");
        data.put("sword", "kiếm");
        executor.saveGlossaryMap(data);

        Map<String, String> loaded1 = executor.loadGlossaryMap();
        assertEquals(2, loaded1.size());
        assertEquals("vàng", loaded1.get("gold"));

        // Scenario 3: Load again without changes -> uses cached map (references same
        // object instance)
        Map<String, String> loaded2 = executor.loadGlossaryMap();
        assertSame(loaded1, loaded2);

        // Scenario 4: Touch/Modify file on disk -> causes reload
        ObjectMapper mapper = new ObjectMapper();
        data.put("shield", "khiên");
        // write directly to bypass setter invalidation and test mtime detection
        mapper.writeValue(originalGlossary, data);
        originalGlossary.setLastModified(System.currentTimeMillis() + 5000); // touch mtime forward

        Map<String, String> loaded3 = executor.loadGlossaryMap();
        assertEquals(3, loaded3.size());
        assertEquals("khiên", loaded3.get("shield"));
        assertNotSame(loaded1, loaded3);
    }

    @Test
    public void testResolveActiveGlossaryMerging() throws Exception {
        TranslateExecutor executor = new TranslateExecutor(null);

        // Global Glossary
        Map<String, String> globalVal = new HashMap<>();
        globalVal.put("character", "nhân vật");
        globalVal.put("potion", "thuốc");
        executor.saveGlossaryMap(globalVal);

        // Theme Preset: test_theme.json overrides "potion"
        Map<String, String> presetVal = new HashMap<>();
        presetVal.put("potion", "dược phẩm"); // override
        presetVal.put("elixir", "linh đơn");

        File presetFile = new File("data/presets/test_theme.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(presetFile, presetVal);

        // Resolve active glossary (None)
        Map<String, String> resolvedNone = executor.resolveActiveGlossary(null);
        assertEquals(2, resolvedNone.size());
        assertEquals("thuốc", resolvedNone.get("potion"));

        // Resolve active glossary (test_theme)
        Map<String, String> resolvedTheme = executor.resolveActiveGlossary("test_theme");
        assertEquals(3, resolvedTheme.size());
        assertEquals("nhân vật", resolvedTheme.get("character"));
        assertEquals("dược phẩm", resolvedTheme.get("potion")); // verified override
        assertEquals("linh đơn", resolvedTheme.get("elixir"));

        // Direct shortcut check
        TranslateExecutor.GlossaryMatchResult match1 = executor.findExactGlossaryMatch("potion", "test_theme");
        assertNotNull(match1);
        assertEquals("dược phẩm", match1.value);
        assertEquals("Preset", match1.type);

        TranslateExecutor.GlossaryMatchResult match2 = executor.findExactGlossaryMatch("character", "test_theme");
        assertNotNull(match2);
        assertEquals("nhân vật", match2.value);
        assertEquals("Glossary", match2.type);
    }

    @Test
    public void testConcurrencyAccess() throws Exception {
        TranslateExecutor executor = new TranslateExecutor(null);

        // Set up initial data
        Map<String, String> initial = new HashMap<>();
        initial.put("hp", "máu");
        executor.saveGlossaryMap(initial);

        ExecutorService service = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 50; i++) {
            final int index = i;
            // Reader threads
            service.submit(() -> {
                try {
                    executor.resolveActiveGlossary(null);
                    executor.findExactGlossaryMatch("hp", null);
                } catch (Exception e) {
                    fail("Concurrently reading glossary failed: " + e.getMessage());
                }
            });
            // Writer threads
            service.submit(() -> {
                try {
                    Map<String, String> updated = new HashMap<>();
                    updated.put("hp", "máu " + index);
                    executor.saveGlossaryMap(updated);
                } catch (Exception e) {
                    fail("Concurrently writing glossary failed: " + e.getMessage());
                }
            });
        }

        service.shutdown();
        boolean finished = service.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(finished, "Concurrency task did not finish in time");
    }
}
