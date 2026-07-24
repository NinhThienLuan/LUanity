package com.aiwrapper.javafx.ui;

import com.aiwrapper.config.AppConfig;
import com.aiwrapper.config.AppConfigManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages a persistent history of game executable paths.
 * Stores up to MAX_ENTRIES entries in data/game_history.json,
 * sorted by last_used descending.
 */
public class GameHistoryManager {

    private static final File HISTORY_FILE = new File("data/game_history.json");
    private static final int MAX_ENTRIES = 10;
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Load the history list, sorted newest-first. Never returns null. */
    public static List<GameEntry> load() {
        if (!HISTORY_FILE.exists() || HISTORY_FILE.length() == 0)
            return new ArrayList<>();
        try {
            return MAPPER.readValue(HISTORY_FILE, new TypeReference<List<GameEntry>>() {
            });
        } catch (Exception e) {
            System.err.println("[GameHistoryManager] Failed to read history: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Upsert the given exe path into the history list and save.
     * Also persists the path to AppConfigManager as the active game.
     */
    public static void upsert(String exePath) {
        if (exePath == null || exePath.trim().isEmpty())
            return;
        String path = exePath.trim();

        List<GameEntry> list = load();

        // Preserve existing activePreset and languagePair if present
        String existingPreset = null;
        String existingLangPair = "EN/VI";
        for (GameEntry e : list) {
            if (e.getExePath().equalsIgnoreCase(path)) {
                existingPreset = e.getActivePreset();
                existingLangPair = e.getLanguagePair();
                break;
            }
        }

        // Remove existing entry for this path (case-insensitive)
        list.removeIf(e -> e.getExePath().equalsIgnoreCase(path));

        // Build new/updated entry
        File exe = new File(path);
        String rawName = exe.getName();
        if (rawName.toLowerCase().endsWith(".exe")) {
            rawName = rawName.substring(0, rawName.length() - 4);
        }
        GameEntry entry = new GameEntry(rawName, path, LocalDateTime.now().format(FMT));
        entry.setActivePreset(existingPreset);
        entry.setLanguagePair(existingLangPair);

        // Prepend (most recent first)
        list.add(0, entry);

        // Enforce cap
        if (list.size() > MAX_ENTRIES) {
            list = list.subList(0, MAX_ENTRIES);
        }

        save(list);
        writeActivePath(path);
    }

    /** Update active preset for a game path directly in history. */
    public static void updateActivePreset(String exePath, String preset) {
        if (exePath == null || exePath.trim().isEmpty())
            return;
        String path = exePath.trim();
        List<GameEntry> list = load();
        boolean updated = false;
        for (GameEntry entry : list) {
            if (entry.getExePath().equalsIgnoreCase(path)) {
                entry.setActivePreset(preset);
                updated = true;
                break;
            }
        }
        if (updated) {
            save(list);
        }
    }

    /** Update language pair for a game path directly in history. */
    public static void updateLanguagePair(String exePath, String languagePair) {
        if (exePath == null || exePath.trim().isEmpty())
            return;
        String path = exePath.trim();
        List<GameEntry> list = load();
        boolean updated = false;
        for (GameEntry entry : list) {
            if (entry.getExePath().equalsIgnoreCase(path)) {
                entry.setLanguagePair(languagePair);
                updated = true;
                break;
            }
        }
        if (updated) {
            save(list);
        }
    }

    /** Persist a path as the active game without altering history order. */
    public static void setActive(String exePath) {
        if (exePath == null || exePath.trim().isEmpty())
            return;
        writeActivePath(exePath.trim());
    }

    /** Return the currently active game path, or empty string. */
    public static String loadActivePath() {
        try {
            return AppConfigManager.load().getActiveGamePath();
        } catch (Exception e) {
            return "";
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private static void save(List<GameEntry> list) {
        try {
            File parent = HISTORY_FILE.getParentFile();
            if (parent != null && !parent.exists())
                parent.mkdirs();
            MAPPER.writeValue(HISTORY_FILE, list);
        } catch (Exception e) {
            System.err.println("[GameHistoryManager] Failed to save history: " + e.getMessage());
        }
    }

    private static void writeActivePath(String path) {
        try {
            AppConfig config = AppConfigManager.load();
            config.setActiveGamePath(path);
            AppConfigManager.save(config);
        } catch (Exception e) {
            System.err.println("[GameHistoryManager] Failed to write active path: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Inner POJO
    // ------------------------------------------------------------------

    public static class GameEntry {
        private String name;
        private String exePath;
        private String lastUsed;
        private String activePreset;
        private String languagePair = "EN/VI";

        /** Jackson default constructor */
        public GameEntry() {
        }

        public GameEntry(String name, String exePath, String lastUsed) {
            this.name = name;
            this.exePath = exePath;
            this.lastUsed = lastUsed;
        }

        public String getName() {
            return name;
        }

        public String getExePath() {
            return exePath;
        }

        public String getLastUsed() {
            return lastUsed;
        }

        public String getActivePreset() {
            return activePreset;
        }

        public String getLanguagePair() {
            return languagePair;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setExePath(String exePath) {
            this.exePath = exePath;
        }

        public void setLastUsed(String lastUsed) {
            this.lastUsed = lastUsed;
        }

        public void setActivePreset(String activePreset) {
            this.activePreset = activePreset;
        }

        public void setLanguagePair(String languagePair) {
            this.languagePair = languagePair;
        }

        @Override
        public String toString() {
            return name;
        } // used by ComboBox cell renderer
    }
}
