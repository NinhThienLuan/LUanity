package com.aiwrapper.javafx.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
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
    private static final File ACTIVE_PATH_FILE = new File("data/game_path.txt");
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
     * Also persists the path to game_path.txt as the active game.
     */
    public static void upsert(String exePath) {
        if (exePath == null || exePath.trim().isEmpty())
            return;
        String path = exePath.trim();

        List<GameEntry> list = load();

        // Remove existing entry for this path (case-insensitive)
        list.removeIf(e -> e.getExePath().equalsIgnoreCase(path));

        // Build new/updated entry
        File exe = new File(path);
        String rawName = exe.getName();
        if (rawName.toLowerCase().endsWith(".exe")) {
            rawName = rawName.substring(0, rawName.length() - 4);
        }
        GameEntry entry = new GameEntry(rawName, path, LocalDateTime.now().format(FMT));

        // Prepend (most recent first)
        list.add(0, entry);

        // Enforce cap
        if (list.size() > MAX_ENTRIES) {
            list = list.subList(0, MAX_ENTRIES);
        }

        save(list);
        writeActivePath(path);
    }

    /** Persist a path as the active game without altering history order. */
    public static void setActive(String exePath) {
        if (exePath == null || exePath.trim().isEmpty())
            return;
        writeActivePath(exePath.trim());
    }

    /** Return the currently active game path, or empty string. */
    public static String loadActivePath() {
        if (!ACTIVE_PATH_FILE.exists())
            return "";
        try {
            return new String(Files.readAllBytes(ACTIVE_PATH_FILE.toPath()), StandardCharsets.UTF_8).trim();
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
            File parent = ACTIVE_PATH_FILE.getParentFile();
            if (parent != null && !parent.exists())
                parent.mkdirs();
            Files.write(ACTIVE_PATH_FILE.toPath(), path.getBytes(StandardCharsets.UTF_8));
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

        public void setName(String name) {
            this.name = name;
        }

        public void setExePath(String exePath) {
            this.exePath = exePath;
        }

        public void setLastUsed(String lastUsed) {
            this.lastUsed = lastUsed;
        }

        @Override
        public String toString() {
            return name;
        } // used by ComboBox cell renderer
    }
}
