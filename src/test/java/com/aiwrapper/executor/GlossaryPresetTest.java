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

    @org.junit.jupiter.api.BeforeEach
    public void cleanBefore() throws Exception {
        if (originalGlossary.exists()) {
            originalGlossary.delete();
        }
        File testPreset = new File("data/presets/test_theme.json");
        if (testPreset.exists()) {
            testPreset.delete();
        }
    }

    @org.junit.jupiter.api.AfterEach
    public void cleanAfter() throws Exception {
        if (originalGlossary.exists()) {
            originalGlossary.delete();
        }
        File testPreset = new File("data/presets/test_theme.json");
        if (testPreset.exists()) {
            testPreset.delete();
        }
        File pAFile = new File("data/presets/presetA.json");
        if (pAFile.exists()) {
            pAFile.delete();
        }
        File pBFile = new File("data/presets/presetB.json");
        if (pBFile.exists()) {
            pBFile.delete();
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

    @Test
    public void testMultiPresetBehavior() throws Exception {
        TranslateExecutor executor = new TranslateExecutor(null);

        // Define a base glossary map
        Map<String, String> base = new HashMap<>();
        base.put("key1", "glo_val1");
        base.put("key2", "glo_val2");
        executor.saveGlossaryMap(base);

        // Create preset A
        Map<String, String> pA = new HashMap<>();
        pA.put("key2", "A_val2"); // overrides glossary
        pA.put("keyA", "A_only");
        File pAFile = new File("data/presets/presetA.json");
        new ObjectMapper().writeValue(pAFile, pA);

        // Create preset B
        Map<String, String> pB = new HashMap<>();
        pB.put("key2", "B_val2"); // Z-priority overrides glossary and A (since B comes alphabetically after A)
        pB.put("keyB", "B_only");
        File pBFile = new File("data/presets/presetB.json");
        new ObjectMapper().writeValue(pBFile, pB);

        try {
            // Test 1: Priority merge & alphabetical order: "presetA,presetB"
            Map<String, String> resolved = executor.resolveActiveGlossary("presetA,presetB");
            assertEquals("glo_val1", resolved.get("key1"));
            assertEquals("B_val2", resolved.get("key2")); // B overrides A
            assertEquals("A_only", resolved.get("keyA"));
            assertEquals("B_only", resolved.get("keyB"));

            // "presetB,presetA" (different input order) should result in same cache key and
            // order internally: B overrides A
            Map<String, String> resolvedAlt = executor.resolveActiveGlossary("presetB,presetA");
            assertSame(resolved, resolvedAlt); // Cache normalization: same Map instance returned!

            // Test 2: Invalidation of composite cache on preset file modify
            pA.put("keyA", "A_new_val");
            new ObjectMapper().writeValue(pAFile, pA);
            pAFile.setLastModified(System.currentTimeMillis() + 6000); // force mtime update

            Map<String, String> resolvedUpdated = executor.resolveActiveGlossary("presetA,presetB");
            assertNotSame(resolved, resolvedUpdated); // cache invalidation detected
            assertEquals("A_new_val", resolvedUpdated.get("keyA"));

            // Test 3: Missing preset safety
            // Should load and skip nonexistent preset gracefully
            Map<String, String> resolvedMissing = executor.resolveActiveGlossary("presetA,ghostPreset,presetB");
            assertNotNull(resolvedMissing);
            assertEquals("A_new_val", resolvedMissing.get("keyA"));
            assertEquals("B_val2", resolvedMissing.get("key2"));
        } finally {
            if (pAFile.exists())
                pAFile.delete();
            if (pBFile.exists())
                pBFile.delete();
        }
    }
}
