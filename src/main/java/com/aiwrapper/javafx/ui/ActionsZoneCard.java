package com.aiwrapper.javafx.ui;

import com.aiwrapper.config.AiConfig;
import com.aiwrapper.executor.TranslateExecutor;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ActionsZoneCard extends VBox {

    private final Stage stage;
    private final TranslateExecutor translateExecutor;
    private final AiConfig aiConfig;
    private final ObservableList<TranslationItem> historyList;
    private final Runnable onCacheChange;

    private ComboBox<GameHistoryManager.GameEntry> gamePathCombo;
    private ComboBox<String> presetCombo;
    private Button btnToggleProxy;
    private Label lblStatusTextRef; // Expose status text ref for toolbar sync or callback

    public ActionsZoneCard(Stage stage,
            TranslateExecutor translateExecutor,
            AiConfig aiConfig,
            ObservableList<TranslationItem> historyList,
            Runnable onCacheChange) {
        super(14);
        this.stage = stage;
        this.translateExecutor = translateExecutor;
        this.aiConfig = aiConfig;
        this.historyList = historyList;
        this.onCacheChange = onCacheChange;

        setPadding(new Insets(16));
        setStyle(
                "-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        initLayout();
    }

    // Callback to synchronize the state changes from outside
    private Runnable onProxyStatusChanged;
    private java.util.function.Consumer<String> onConsoleLog;

    public void setOnProxyStatusChanged(Runnable callback) {
        this.onProxyStatusChanged = callback;
    }

    public void setOnConsoleLog(java.util.function.Consumer<String> callback) {
        this.onConsoleLog = callback;
    }

    private void initLayout() {
        Label zoneBTitle = new Label("PROXY ACTIONS & SHORTCUTS");
        zoneBTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        zoneBTitle.setTextFill(javafx.scene.paint.Color.web("#06b6d4"));

        btnToggleProxy = new Button();
        btnToggleProxy.setMaxWidth(Double.MAX_VALUE);
        btnToggleProxy.setPrefHeight(45);
        btnToggleProxy.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btnToggleProxy.setCursor(javafx.scene.Cursor.HAND);

        // Styling helper
        java.util.function.Consumer<Boolean> applyToggleStyle = (Boolean active) -> {
            if (active) {
                btnToggleProxy.setText("STOP PROXY");
                btnToggleProxy.setStyle(
                        "-fx-background-color: linear-gradient(to right, #ef4444, #dc2626); -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(239, 68, 68, 0.4), 10, 0, 0, 2);");
                btnToggleProxy.setOnMouseEntered(e -> btnToggleProxy.setStyle(
                        "-fx-background-color: linear-gradient(to right, #f87171, #ef4444); -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-scale-x: 1.01; -fx-scale-y: 1.01; -fx-effect: dropshadow(three-pass-box, rgba(239, 68, 68, 0.6), 12, 0, 0, 2);"));
                btnToggleProxy.setOnMouseExited(e -> btnToggleProxy.setStyle(
                        "-fx-background-color: linear-gradient(to right, #ef4444, #dc2626); -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-scale-x: 1.0; -fx-scale-y: 1.0; -fx-effect: dropshadow(three-pass-box, rgba(239, 68, 68, 0.4), 10, 0, 0, 2);"));
            } else {
                btnToggleProxy.setText("START PROXY");
                btnToggleProxy.setStyle(
                        "-fx-background-color: linear-gradient(to right, #06b6d4, #0891b2); -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-effect: dropshadow(three-pass-box, rgba(6, 182, 212, 0.4), 10, 0, 0, 2);");
                btnToggleProxy.setOnMouseEntered(e -> btnToggleProxy.setStyle(
                        "-fx-background-color: linear-gradient(to right, #22d3ee, #06b6d4); -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-scale-x: 1.01; -fx-scale-y: 1.01; -fx-effect: dropshadow(three-pass-box, rgba(6, 182, 212, 0.6), 12, 0, 0, 2);"));
                btnToggleProxy.setOnMouseExited(e -> btnToggleProxy.setStyle(
                        "-fx-background-color: linear-gradient(to right, #06b6d4, #0891b2); -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-scale-x: 1.0; -fx-scale-y: 1.0; -fx-effect: dropshadow(three-pass-box, rgba(6, 182, 212, 0.4), 10, 0, 0, 2);"));
            }
        };

        applyToggleStyle.accept(translateExecutor.isProxyActive());

        btnToggleProxy.setOnAction(e -> {
            boolean currentActive = translateExecutor.isProxyActive();
            if (currentActive) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Stop");
                confirm.setHeaderText("Stop Translation Proxy?");
                confirm.setContentText("Are you sure you want to stop the translation proxy?");
                File cssFile = new File("data/ui_style.css");
                if (cssFile.exists()) {
                    confirm.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    translateExecutor.setProxyActive(false);
                    applyToggleStyle.accept(false);
                    if (onProxyStatusChanged != null) {
                        onProxyStatusChanged.run();
                    }
                    System.out.println("Proxy stopped.");
                }
            } else {
                translateExecutor.setProxyActive(true);
                applyToggleStyle.accept(true);
                if (onProxyStatusChanged != null) {
                    onProxyStatusChanged.run();
                }
                System.out.println("Proxy started.");
            }
        });

        // Edit Glossary & Edit Prompt Dialog Resource buttons
        Button editGlossaryBtn = createSecondaryButton("Edit Glossary");
        editGlossaryBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editGlossaryBtn, Priority.ALWAYS);
        editGlossaryBtn.setOnAction(evt -> openGlossaryDialog());

        Button editPromptBtn = createSecondaryButton("Edit Prompt");
        editPromptBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editPromptBtn, Priority.ALWAYS);
        editPromptBtn.setOnAction(evt -> openPromptDialog());

        HBox resBtns = new HBox(8, editGlossaryBtn, editPromptBtn);

        // --- Game History: migrate old single game_path.txt into history if needed ---
        String savedGamePath = GameHistoryManager.loadActivePath();
        if (!savedGamePath.isEmpty()) {
            // Seed into history if not already present (one-time migration)
            List<GameHistoryManager.GameEntry> existing = GameHistoryManager.load();
            boolean alreadyTracked = existing.stream()
                    .anyMatch(e -> e.getExePath().equalsIgnoreCase(savedGamePath));
            if (!alreadyTracked) {
                GameHistoryManager.upsert(savedGamePath);
            }
        }

        // Exe Path — read-only ComboBox backed by game history (add via Browse button
        // only)
        Label gamePathLabel = createFormLabel("Game Exe Path:");
        gamePathCombo = new ComboBox<>();
        gamePathCombo.setEditable(false);
        gamePathCombo.setPromptText("-- Chọn game đã chạy trước --");
        gamePathCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(gamePathCombo, Priority.ALWAYS);
        gamePathCombo.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-background-radius: 6;");

        // Display the game name in the dropdown cell
        gamePathCombo.setConverter(new javafx.util.StringConverter<GameHistoryManager.GameEntry>() {
            @Override
            public String toString(GameHistoryManager.GameEntry entry) {
                return (entry == null) ? "" : entry.getName();
            }

            @Override
            public GameHistoryManager.GameEntry fromString(String text) {
                return null;
            } // not used (non-editable)
        });

        // Populate history items and select active game
        List<GameHistoryManager.GameEntry> histEntries = GameHistoryManager.load();
        gamePathCombo.getItems().addAll(histEntries);
        if (!savedGamePath.isEmpty()) {
            histEntries.stream()
                    .filter(e -> e.getExePath().equalsIgnoreCase(savedGamePath))
                    .findFirst()
                    .ifPresent(e -> gamePathCombo.setValue(e));
        }

        // Re-entrancy guard
        boolean[] updatingCombo = { false };

        // Helper: switch active game by exe path string
        java.util.function.Consumer<String> switchGame = (String path) -> {
            if (updatingCombo[0])
                return;
            if (path == null || path.trim().isEmpty()) {
                translateExecutor.setActiveCacheFile(null);
                if (onCacheChange != null)
                    onCacheChange.run();
                return;
            }
            String trimmed = path.trim();
            GameHistoryManager.upsert(trimmed);

            // Refresh combo items (newest-first) and re-select
            updatingCombo[0] = true;
            try {
                List<GameHistoryManager.GameEntry> updated = GameHistoryManager.load();
                gamePathCombo.getItems().setAll(updated);
                updated.stream()
                        .filter(e -> e.getExePath().equalsIgnoreCase(trimmed))
                        .findFirst()
                        .ifPresent(e -> gamePathCombo.setValue(e));
            } finally {
                updatingCombo[0] = false;
            }

            // Load corresponding cache
            try {
                File exe = new File(trimmed);
                String gameName = exe.getName();
                if (gameName.endsWith(".exe"))
                    gameName = gameName.substring(0, gameName.length() - 4);
                File gameCache = new File("data/cache_" + gameName + ".json");
                if (gameCache.exists()) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, String> cMap = mapper.readValue(gameCache,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                            });
                    historyList.clear();
                    for (Map.Entry<String, String> entry : cMap.entrySet()) {
                        historyList.add(new TranslationItem("Cache", entry.getKey(), entry.getValue()));
                    }
                    translateExecutor.setActiveCacheFile(gameCache);
                } else {
                    translateExecutor.setActiveCacheFile(null);
                    historyList.clear();
                }
            } catch (Exception ex) {
                System.err.println("[ActionsZoneCard] switchGame cache load failed: " + ex.getMessage());
            }

            // Restore active preset from game history
            try {
                String restoredPreset = null;
                List<GameHistoryManager.GameEntry> updated = GameHistoryManager.load();
                for (GameHistoryManager.GameEntry entry : updated) {
                    if (entry.getExePath().equalsIgnoreCase(trimmed)) {
                        restoredPreset = entry.getActivePreset();
                        break;
                    }
                }
                if (restoredPreset != null && presetCombo.getItems().contains(restoredPreset)) {
                    presetCombo.setValue(restoredPreset);
                    translateExecutor.setActivePreset(restoredPreset);
                } else {
                    presetCombo.setValue("None");
                    translateExecutor.setActivePreset(null);
                }
            } catch (Exception ex) {
                System.err.println("[ActionsZoneCard] switchGame restore preset failed: " + ex.getMessage());
            }

            if (onCacheChange != null)
                onCacheChange.run();
        };

        // Selecting from dropdown
        gamePathCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingCombo[0] && newVal != null) {
                switchGame.accept(newVal.getExePath());
            }
        });

        Button gamePathBrowse = createSecondaryButton("Chọn");
        gamePathBrowse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Game Executable");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Executable (*.exe)", "*.exe"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                switchGame.accept(file.getAbsolutePath());
            }
        });
        HBox gamePathRow = new HBox(8, gamePathCombo, gamePathBrowse);
        VBox gamePathContainer = new VBox(6, gamePathLabel, gamePathRow);

        // Active Theme Preset Selection
        Label presetLabel = createFormLabel("Active Theme Preset:");
        presetCombo = new ComboBox<>();
        presetCombo.setEditable(false);
        presetCombo.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-background-radius: 6;");
        presetCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(presetCombo, Priority.ALWAYS);

        // Initialize preset list
        presetCombo.getItems().add("None");
        presetCombo.getItems().addAll(translateExecutor.listPresets());
        presetCombo.setValue("None");

        presetCombo.setOnShowing(evt -> {
            String currentSelected = presetCombo.getValue();
            List<String> presets = translateExecutor.listPresets();
            presetCombo.getItems().clear();
            presetCombo.getItems().add("None");
            presetCombo.getItems().addAll(presets);
            if (currentSelected != null && presetCombo.getItems().contains(currentSelected)) {
                presetCombo.setValue(currentSelected);
            } else {
                presetCombo.setValue("None");
            }
        });

        presetCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            String p = (newVal == null || newVal.equals("None")) ? null : newVal;
            translateExecutor.setActivePreset(p);
            String activeGamePath = getGamePath();
            if (!activeGamePath.isEmpty()) {
                GameHistoryManager.updateActivePreset(activeGamePath, p);
            }
        });

        VBox presetContainer = new VBox(6, presetLabel, presetCombo);

        // BepInEx Shortcut Buttons Box
        Label shortcutTitle = createFormLabel("BepInEx Utilities:");
        shortcutTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        shortcutTitle.setTextFill(javafx.scene.paint.Color.web("#06b6d4"));

        Button btnConfig = createSecondaryButton("File Config");
        Button btnLog = createSecondaryButton("Mở File Log");
        Button btnImport = createSecondaryButton("Nhập từ Game");

        Runnable showPathWarning = () -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng chọn đường dẫn game Game Exe trước!");
            alert.showAndWait();
        };

        btnConfig.setOnAction(e -> {
            String path = getGamePath();
            if (path.isEmpty()) {
                showPathWarning.run();
                return;
            }
            File exe = new File(path);
            File cfg = new File(exe.getParentFile(), "BepInEx/config/AutoTranslatorConfig.ini");
            openFile(cfg);
        });

        btnLog.setOnAction(e -> {
            String path = getGamePath();
            if (path.isEmpty()) {
                showPathWarning.run();
                return;
            }
            File exe = new File(path);
            File log = new File(exe.getParentFile(), "BepInEx/LogOutput.log");
            openFile(log);
        });

        btnImport.setOnAction(e -> {
            String path = getGamePath();
            if (path.isEmpty()) {
                showPathWarning.run();
                return;
            }
            File exe = new File(path);
            File transFile = new File(exe.getParentFile(),
                    "BepInEx/Translation/vi/Text/_AutoGeneratedTranslations.txt");
            if (!transFile.exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("Translation File Not Found");
                alert.setContentText("Cannot find BepInEx translation file at:\n" + transFile.getAbsolutePath());
                File cssFile = new File("data/ui_style.css");
                if (cssFile.exists()) {
                    alert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                alert.showAndWait();
                return;
            }

            try {
                List<String> lines = Files.readAllLines(transFile.toPath(), StandardCharsets.UTF_8);
                Map<String, String> newTranslations = new java.util.LinkedHashMap<>();
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = trimmed.indexOf('=');
                    if (eqIdx > 0) {
                        String orig = trimmed.substring(0, eqIdx).trim();
                        String trans = trimmed.substring(eqIdx + 1).trim();
                        if (!orig.isEmpty() && !trans.isEmpty()) {
                            newTranslations.put(orig, trans);
                        }
                    }
                }

                if (newTranslations.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Information");
                    alert.setHeaderText("No Translations Found");
                    alert.setContentText(
                            "The _AutoGeneratedTranslations.txt file does not contain any valid translation rows.");
                    File cssFile = new File("data/ui_style.css");
                    if (cssFile.exists()) {
                        alert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                    }
                    alert.showAndWait();
                    return;
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Sync");
                confirm.setHeaderText("Sync from Game File");
                confirm.setContentText("Found " + newTranslations.size() + " translations in game file.\n" +
                        "Do you want to import and merge them into the active proxy cache file?");
                File cssFile = new File("data/ui_style.css");
                if (cssFile.exists()) {
                    confirm.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    translateExecutor.importTranslations(newTranslations);

                    File targetCacheFile = translateExecutor.getActiveCacheFile();
                    if (targetCacheFile == null) {
                        String gameName = exe.getName();
                        if (gameName.endsWith(".exe")) {
                            gameName = gameName.substring(0, gameName.length() - 4);
                        }
                        targetCacheFile = new File("data/cache_" + gameName + ".json");
                    }

                    if (targetCacheFile.exists()) {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        try {
                            Map<String, String> cMap = mapper.readValue(targetCacheFile,
                                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                                    });
                            historyList.clear();
                            for (Map.Entry<String, String> entry : cMap.entrySet()) {
                                historyList.add(new TranslationItem("Cache", entry.getKey(), entry.getValue()));
                            }
                        } catch (Exception ex) {
                            // ignore
                        }
                    }

                    if (onCacheChange != null) {
                        onCacheChange.run();
                    }

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText("Sync Complete");
                    successAlert.setContentText(
                            "Successfully imported and merged " + newTranslations.size() + " mappings!");
                    if (cssFile.exists()) {
                        successAlert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                    }
                    successAlert.showAndWait();
                }
            } catch (Exception ex) {
                Alert errAlert = new Alert(Alert.AlertType.ERROR);
                errAlert.setTitle("Error");
                errAlert.setHeaderText("Sync Failed");
                errAlert.setContentText("Failed to read game translations:\n" + ex.getMessage());
                File cssFile = new File("data/ui_style.css");
                if (cssFile.exists()) {
                    errAlert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                errAlert.showAndWait();
            }
        });

        Button btnFont = createSecondaryButton("Cấu hình Font");
        btnFont.setOnAction(e -> openFontDialog());

        btnConfig.setMaxWidth(Double.MAX_VALUE);
        btnLog.setMaxWidth(Double.MAX_VALUE);
        btnImport.setMaxWidth(Double.MAX_VALUE);
        btnFont.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(btnConfig, Priority.ALWAYS);
        HBox.setHgrow(btnLog, Priority.ALWAYS);
        HBox.setHgrow(btnImport, Priority.ALWAYS);
        HBox.setHgrow(btnFont, Priority.ALWAYS);

        HBox row1 = new HBox(8, btnConfig, btnLog);
        HBox row2 = new HBox(8, btnFont, btnImport);
        row1.setMaxWidth(Double.MAX_VALUE);
        row2.setMaxWidth(Double.MAX_VALUE);

        // Auto-Setup button (full width)
        Button btnSetup = createSecondaryButton("⬇ Cài BepInEx + AutoTranslator");
        btnSetup.setMaxWidth(Double.MAX_VALUE);
        btnSetup.setStyle(
                "-fx-background-color: #0f172a; -fx-text-fill: #38bdf8; -fx-border-color: #38bdf8; " +
                        "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: bold;");
        btnSetup.setOnAction(e -> {
            String path = getGamePath();
            if (path.isEmpty()) {
                showPathWarning.run();
                return;
            }
            File gameExe = new File(path);
            if (!gameExe.exists()) {
                Alert err = new Alert(Alert.AlertType.ERROR,
                        "Không tìm thấy file exe:\n" + path);
                err.showAndWait();
                return;
            }

            com.aiwrapper.service.BepInExSetupService svc = new com.aiwrapper.service.BepInExSetupService();
            String alreadyNote = svc.isInstalled(gameExe) ? "\n⚠ BepInEx đã được cài — sẽ cập nhật config và plugin."
                    : "";
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Game: " + gameExe.getName() +
                            "\nThư mục: " + gameExe.getParentFile().getAbsolutePath() +
                            alreadyNote +
                            "\n\nSẽ tải và cài:\n  • BepInEx (latest)\n  • XUnity.AutoTranslator (latest)\n  • Ghi AutoTranslatorConfig.ini → proxy 8080"
                            +
                            "\n\nBắt đầu cài đặt?");
            confirm.setTitle("Xác nhận cài đặt BepInEx");
            confirm.setHeaderText(null);
            File cssFile = new File("data/ui_style.css");
            if (cssFile.exists())
                confirm.getDialogPane().getStylesheets().add(cssFile.toURI().toString());

            if (confirm.showAndWait().filter(r -> r == javafx.scene.control.ButtonType.OK).isEmpty())
                return;

            btnSetup.setDisable(true);
            btnSetup.setText("⏳ Đang cài đặt...");

            Thread setupThread = new Thread(() -> {
                try {
                    svc.setup(gameExe, 8080, "vi", msg -> javafx.application.Platform.runLater(() -> {
                        if (onConsoleLog != null)
                            onConsoleLog.accept(msg);
                    }));
                    javafx.application.Platform.runLater(() -> {
                        btnSetup.setDisable(false);
                        btnSetup.setText("⬇ Cài BepInEx + AutoTranslator");
                        Alert done = new Alert(Alert.AlertType.INFORMATION,
                                "✔ Cài đặt hoàn tất!\n\n" +
                                        "Hãy khởi động game một lần để BepInEx hoàn tất khởi tạo.\n" +
                                        "Sau đó bật proxy và chạy game để dịch.");
                        done.setTitle("Hoàn tất");
                        if (cssFile.exists())
                            done.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                        done.showAndWait();
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        btnSetup.setDisable(false);
                        btnSetup.setText("⬇ Cài BepInEx + AutoTranslator");
                        Alert err = new Alert(Alert.AlertType.ERROR,
                                "Cài đặt thất bại:\n" + ex.getMessage());
                        err.setTitle("Lỗi cài đặt");
                        if (cssFile.exists())
                            err.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                        err.showAndWait();
                    });
                }
            });
            setupThread.setDaemon(true);
            setupThread.start();
        });

        VBox shortcutBox = new VBox(8, row1, row2, btnSetup);
        shortcutBox.setPadding(new Insets(4, 0, 10, 0));
        VBox shortcutContainer = new VBox(6, shortcutTitle, shortcutBox);

        getChildren().addAll(zoneBTitle, btnToggleProxy, resBtns, gamePathContainer, presetContainer,
                shortcutContainer);
    }

    public static class GlossaryRow {
        private final SimpleStringProperty original;
        private final SimpleStringProperty translated;

        public GlossaryRow(String original, String translated) {
            this.original = new SimpleStringProperty(original);
            this.translated = new SimpleStringProperty(translated);
        }

        public String getOriginal() {
            return original.get();
        }

        public void setOriginal(String val) {
            this.original.set(val);
        }

        public SimpleStringProperty originalProperty() {
            return original;
        }

        public String getTranslated() {
            return translated.get();
        }

        public void setTranslated(String val) {
            this.translated.set(val);
        }

        public SimpleStringProperty translatedProperty() {
            return translated;
        }
    }

    private void openGlossaryDialog() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Edit Glossary");
        File cssFile = new File("data/ui_style.css");
        if (cssFile.exists()) {
            dialog.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
        }

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TableView<GlossaryRow> tableView = new TableView<>();
        tableView.setEditable(true);
        tableView.setPrefWidth(450);
        tableView.setPrefHeight(300);

        TableColumn<GlossaryRow, String> colOriginal = new TableColumn<>("Từ gốc (English)");
        colOriginal.setCellValueFactory(cellData -> cellData.getValue().originalProperty());
        colOriginal.setCellFactory(TextFieldTableCell.forTableColumn());
        colOriginal.setOnEditCommit(evt -> evt.getRowValue().setOriginal(evt.getNewValue()));
        colOriginal.setPrefWidth(210);

        TableColumn<GlossaryRow, String> colTranslated = new TableColumn<>("Nghĩa dịch (Vietnamese)");
        colTranslated.setCellValueFactory(cellData -> cellData.getValue().translatedProperty());
        colTranslated.setCellFactory(TextFieldTableCell.forTableColumn());
        colTranslated.setOnEditCommit(evt -> evt.getRowValue().setTranslated(evt.getNewValue()));
        colTranslated.setPrefWidth(210);

        tableView.getColumns().add(colOriginal);
        tableView.getColumns().add(colTranslated);

        javafx.collections.ObservableList<GlossaryRow> data = javafx.collections.FXCollections.observableArrayList();
        Map<String, String> glossaryMap = translateExecutor.loadGlossaryMap();
        for (Map.Entry<String, String> entry : glossaryMap.entrySet()) {
            data.add(new GlossaryRow(entry.getKey(), entry.getValue()));
        }
        tableView.setItems(data);

        Button btnAdd = createSecondaryButton("+ Thêm dòng");
        btnAdd.setOnAction(evt -> {
            GlossaryRow newRow = new GlossaryRow("NewWord", "Nghĩa");
            data.add(newRow);
            tableView.getSelectionModel().select(newRow);
            tableView.scrollTo(newRow);
        });

        Button btnDelete = createSecondaryButton("Xóa dòng");
        btnDelete.setOnAction(evt -> {
            GlossaryRow selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                data.remove(selected);
            }
        });

        HBox editControlBar = new HBox(8, btnAdd, btnDelete);
        VBox vbox = new VBox(10, editControlBar, tableView);
        vbox.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Map<String, String> newMap = new java.util.LinkedHashMap<>();
                for (GlossaryRow row : data) {
                    if (row.getOriginal() != null && row.getTranslated() != null) {
                        String orig = row.getOriginal().trim();
                        String trans = row.getTranslated().trim();
                        if (!orig.isEmpty() && !trans.isEmpty()) {
                            newMap.put(orig, trans);
                        }
                    }
                }
                return newMap;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newMap -> {
            translateExecutor.saveGlossaryMap(newMap);
            System.out.println("Glossary saved successfully.");
        });
    }

    private void openPromptDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Prompt Template");
        File cssFile = new File("data/ui_style.css");
        if (cssFile.exists()) {
            dialog.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
        }

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        Label desc = new Label("Modify the prompt template for translation.\nUse {text} placeholder for source text.");
        desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        TextArea textArea = new TextArea(translateExecutor.getPromptTemplate());
        textArea.setWrapText(true);
        textArea.setPrefWidth(450);
        textArea.setPrefHeight(250);

        VBox vbox = new VBox(10, desc, textArea);
        vbox.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return textArea.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newPrompt -> {
            translateExecutor.setPromptTemplate(newPrompt);
            System.out.println("Prompt template updated.");
        });
    }

    private Map<String, String> readFontConfig(File iniFile) {
        Map<String, String> settings = new java.util.HashMap<>();
        settings.put("OverrideFont", "");
        settings.put("OverrideFontTextMeshPro", "");
        settings.put("FallbackFontTextMeshPro", "");
        if (!iniFile.exists()) {
            return settings;
        }
        try {
            List<String> lines = Files.readAllLines(iniFile.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("OverrideFont=")) {
                    settings.put("OverrideFont", trimmed.substring("OverrideFont=".length()).trim());
                } else if (trimmed.startsWith("OverrideFontTextMeshPro=")) {
                    settings.put("OverrideFontTextMeshPro",
                            trimmed.substring("OverrideFontTextMeshPro=".length()).trim());
                } else if (trimmed.startsWith("FallbackFontTextMeshPro=")) {
                    settings.put("FallbackFontTextMeshPro",
                            trimmed.substring("FallbackFontTextMeshPro=".length()).trim());
                }
            }
        } catch (Exception ex) {
            System.err.println("Failed to read INI file: " + ex.getMessage());
        }
        return settings;
    }

    private void writeFontConfig(File iniFile, Map<String, String> fontSettings) {
        if (!iniFile.exists()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(iniFile.toPath(), StandardCharsets.UTF_8);
            List<String> output = new java.util.ArrayList<>();
            boolean behaviorBlock = false;
            boolean hasOverrideFont = false;
            boolean hasOverrideFontTMP = false;
            boolean hasFallbackFontTMP = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.equals("[Behaviour]")) {
                    behaviorBlock = true;
                    output.add(line);
                    continue;
                } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    behaviorBlock = false;
                }

                if (trimmed.startsWith("OverrideFont=")) {
                    output.add("OverrideFont=" + fontSettings.getOrDefault("OverrideFont", ""));
                    hasOverrideFont = true;
                } else if (trimmed.startsWith("OverrideFontTextMeshPro=")) {
                    output.add("OverrideFontTextMeshPro=" + fontSettings.getOrDefault("OverrideFontTextMeshPro", ""));
                    hasOverrideFontTMP = true;
                } else if (trimmed.startsWith("FallbackFontTextMeshPro=")) {
                    output.add("FallbackFontTextMeshPro=" + fontSettings.getOrDefault("FallbackFontTextMeshPro", ""));
                    hasFallbackFontTMP = true;
                } else {
                    output.add(line);
                }
            }

            if (!hasOverrideFont || !hasOverrideFontTMP || !hasFallbackFontTMP) {
                for (int i = 0; i < output.size(); i++) {
                    if (output.get(i).trim().equals("[Behaviour]")) {
                        if (!hasOverrideFont) {
                            output.add(i + 1, "OverrideFont=" + fontSettings.getOrDefault("OverrideFont", ""));
                        }
                        if (!hasOverrideFontTMP) {
                            output.add(i + 1, "OverrideFontTextMeshPro="
                                    + fontSettings.getOrDefault("OverrideFontTextMeshPro", ""));
                        }
                        if (!hasFallbackFontTMP) {
                            output.add(i + 1, "FallbackFontTextMeshPro="
                                    + fontSettings.getOrDefault("FallbackFontTextMeshPro", ""));
                        }
                        break;
                    }
                }
            }

            Files.write(iniFile.toPath(), output, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            System.err.println("Failed to write INI file: " + ex.getMessage());
        }
    }

    private void downloadFontAssets(File gameDir, Button downloadBtn, File cssFile) {
        downloadBtn.setDisable(true);
        downloadBtn.setText("Đang tải...");

        Thread thread = new Thread(() -> {
            String zipUrl = "https://github.com/bbepis/XUnity.AutoTranslator/releases/download/v5.3.0/TMP_Font_AssetBundles.zip";
            try {
                java.net.URL url = new java.net.URL(zipUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(120000);

                if (conn.getResponseCode() == 200) {
                    int count = 0;
                    byte[] buffer = new byte[4096];
                    try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(conn.getInputStream())) {
                        java.util.zip.ZipEntry entry;
                        while ((entry = zis.getNextEntry()) != null) {
                            String name = entry.getName();

                            File destFile = new File(gameDir, name);
                            String canonicalDest = destFile.getCanonicalPath();
                            String canonicalGameDir = gameDir.getCanonicalPath();
                            if (!canonicalDest.startsWith(canonicalGameDir + File.separator)
                                    && !canonicalDest.equals(canonicalGameDir)) {
                                throw new SecurityException("Zip Slip detected! Entry: " + name);
                            }

                            if (name.equals("arialuni_sdf_u2018") || name.equals("arialuni_sdf_u2019")) {
                                try (FileOutputStream fos = new FileOutputStream(destFile)) {
                                    int read;
                                    while ((read = zis.read(buffer)) != -1) {
                                        fos.write(buffer, 0, read);
                                    }
                                }
                                count++;
                            }
                            zis.closeEntry();
                        }
                    }

                    final int extractedCount = count;
                    javafx.application.Platform.runLater(() -> {
                        downloadBtn.setDisable(false);
                        downloadBtn.setText("Tải thêm từ mạng");
                        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                                "Đã tải thành công " + extractedCount + " font (arialuni_sdf) vào thư mục game!");
                        if (cssFile.exists()) {
                            alert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                        }
                        alert.showAndWait();
                    });
                } else {
                    throw new Exception("HTTP error code: " + conn.getResponseCode());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    downloadBtn.setDisable(false);
                    downloadBtn.setText("Tải thêm từ mạng");
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Lỗi tải font từ network: " + ex.getMessage());
                    if (cssFile.exists()) {
                        alert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                    }
                    alert.showAndWait();
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void openFontDialog() {
        String path = getGamePath();
        if (path.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng chọn đường dẫn game Game Exe trước!");
            alert.showAndWait();
            return;
        }
        File exe = new File(path);
        File iniFile = new File(exe.getParentFile(), "BepInEx/config/AutoTranslatorConfig.ini");
        if (!iniFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Không tìm thấy file cấu hình AutoTranslatorConfig.ini tại:\n" + iniFile.getAbsolutePath());
            File cssFile = new File("data/ui_style.css");
            if (cssFile.exists()) {
                alert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
            }
            alert.showAndWait();
            return;
        }

        Map<String, String> currentFonts = readFontConfig(iniFile);

        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Cấu hình Font tiếng Việt");
        File cssFile = new File("data/ui_style.css");
        if (cssFile.exists()) {
            dialog.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
        }

        ButtonType saveButtonType = new ButtonType("Lưu thiết lập", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox vbox = new VBox(14);
        vbox.setPadding(new Insets(14));
        vbox.setMinWidth(480);

        Label headerLbl = new Label("Cấu hình font chữ BepInEx để hiển thị tiếng Việt");
        headerLbl.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px; -fx-font-weight: bold;");

        // Section 1: System Font Selector
        Label localTitle = new Label("1. CHỌN FONT HỆ THỐNG (SYSTEM FONTS)");
        localTitle.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 11px; -fx-font-weight: bold;");

        ComboBox<String> fontSelector = new ComboBox<>();
        fontSelector.setPromptText("-- Chọn phông chữ đang có trên máy --");
        fontSelector.setMaxWidth(Double.MAX_VALUE);
        fontSelector.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 6;");

        List<String> systemFonts = Font.getFontNames();
        java.util.Collections.sort(systemFonts);
        fontSelector.getItems().addAll(systemFonts);

        // Font Preview
        Label previewTitle = new Label("XEM TRƯỚC KIỂU CHỮ (PREVIEW)");
        previewTitle.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 11px; -fx-font-weight: bold;");

        TextField previewInput = new TextField();
        previewInput.setPromptText("Nhập chữ để chạy thử font (ví dụ: Chào buổi sáng)...");
        previewInput.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-background-radius: 6; -fx-padding: 6 10;");

        Label previewLabel = new Label("Cài đặt game - Tiếng Việt mẫu (Â Ê Ô Ư Đ)");
        previewLabel.setStyle(
                "-fx-text-fill: #10b981; -fx-background-color: #0f172a; -fx-padding: 8 12; -fx-background-radius: 6; -fx-font-size: 15px;");
        previewLabel.setMaxWidth(Double.MAX_VALUE);

        // Restart Warning label
        Label restartWarningLabel = new Label("⚠ Cần khởi động lại game để áp dụng cấu hình font mới!");
        restartWarningLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-weight: bold; -fx-font-size: 11px;");
        restartWarningLabel.setVisible(false);

        // Section 2: Properties
        Label configTitle = new Label("2. CHI TIẾT CẤU HÌNH TRONG INI FILE");
        configTitle.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 11px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 0, 10, 0));

        TextField overrideFontTf = new TextField(currentFonts.get("OverrideFont"));
        overrideFontTf.setPromptText("Ví dụ: Arial, Tahoma, Segoe UI...");
        overrideFontTf.setPrefWidth(240);
        overrideFontTf.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-background-radius: 6; -fx-padding: 6 10;");

        TextField overrideTMPTf = new TextField(currentFonts.get("OverrideFontTextMeshPro"));
        overrideTMPTf.setPromptText("Tên font gốc trong game...");
        overrideTMPTf.setPrefWidth(240);
        overrideTMPTf.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-background-radius: 6; -fx-padding: 6 10;");

        TextField fallbackTMPTf = new TextField(currentFonts.get("FallbackFontTextMeshPro"));
        fallbackTMPTf.setPromptText("Ví dụ: arialuni_sdf_u2018");
        fallbackTMPTf.setPrefWidth(240);
        fallbackTMPTf.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-background-radius: 6; -fx-padding: 6 10;");

        grid.add(new Label("OverrideFont (UI thường):"), 0, 0);
        grid.add(overrideFontTf, 1, 0);
        grid.add(new Label("OverrideFontTextMeshPro:"), 0, 1);
        grid.add(overrideTMPTf, 1, 1);
        grid.add(new Label("FallbackFontTextMeshPro:"), 0, 2);
        grid.add(fallbackTMPTf, 1, 2);

        grid.getChildren().stream()
                .filter(node -> node instanceof Label)
                .forEach(node -> ((Label) node).setTextFill(javafx.scene.paint.Color.web("#94a3b8")));

        // ComboBox action
        fontSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                overrideFontTf.setText(newVal);

                // Apply selected system font to the preview label
                Font previewFont = Font.font(newVal, 15);
                if (previewFont != null && (previewFont.getName().equalsIgnoreCase(newVal)
                        || previewFont.getFamily().equalsIgnoreCase(newVal))) {
                    previewLabel.setFont(previewFont);
                    if (previewInput.getText() == null || previewInput.getText().trim().isEmpty()) {
                        previewLabel.setText("Cài đặt game - Tiếng Việt mẫu (Â Ê Ô Ư Đ)");
                    }
                } else {
                    previewLabel.setFont(Font.font("System", 15));
                    previewLabel.setText("Lưu ý: Font hệ thống dùng fallback (" + newVal + ")");
                }
            }
        });

        // Input text binding action
        previewInput.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                previewLabel.setText("Cài đặt game - Tiếng Việt mẫu (Â Ê Ô Ư Đ)");
            } else {
                previewLabel.setText(newVal);
            }
        });

        // Section 3: Fallbacks & MeshPro Fallback setup
        Label fallbackTitle = new Label("3. CÀI ĐẶT FALLBACK TEXTMESHPRO");
        fallbackTitle.setStyle("-fx-text-fill: #06b6d4; -fx-font-size: 11px; -fx-font-weight: bold;");

        Button presetArialUniBtn = createSecondaryButton("Preset Arial Unicode MS");
        presetArialUniBtn.setTooltip(new Tooltip("Điền cấu hình Arial Unicode hệ thống (Không chép file)"));
        presetArialUniBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(presetArialUniBtn, Priority.ALWAYS);
        presetArialUniBtn.setOnAction(e -> {
            overrideFontTf.setText("Arial");
            fallbackTMPTf.setText("arialuni_sdf_u2018");
            overrideTMPTf.setText("");
            restartWarningLabel.setVisible(true);
        });

        Button btnDownloadFromNet = createSecondaryButton("Tải thêm từ mạng (Internet)");
        btnDownloadFromNet.setTooltip(new Tooltip("Tải gói arialuni_sdf_u2018/u2019 từ GitHub"));
        btnDownloadFromNet.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnDownloadFromNet, Priority.ALWAYS);
        btnDownloadFromNet.setOnAction(e -> {
            restartWarningLabel.setVisible(true);
            downloadFontAssets(exe.getParentFile(), btnDownloadFromNet, cssFile);
        });

        HBox fallbacksRow = new HBox(8, presetArialUniBtn, btnDownloadFromNet);
        fallbacksRow.setMaxWidth(Double.MAX_VALUE);

        Label noteLbl = new Label(
                "(*) Lưu ý: Cấu hình TextMeshPro yêu cầu phải tải file fallback .asset bằng nút tải từ mạng.");
        noteLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-style: italic;");

        vbox.getChildren().addAll(
                headerLbl,
                new Separator(),
                localTitle,
                fontSelector,
                previewTitle,
                previewInput,
                previewLabel,
                new Separator(),
                configTitle,
                grid,
                restartWarningLabel,
                new Separator(),
                fallbackTitle,
                fallbacksRow,
                noteLbl);

        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Map<String, String> res = new java.util.HashMap<>();
                res.put("OverrideFont", overrideFontTf.getText().trim());
                res.put("OverrideFontTextMeshPro", overrideTMPTf.getText().trim());
                res.put("FallbackFontTextMeshPro", fallbackTMPTf.getText().trim());
                return res;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            writeFontConfig(iniFile, res);
            Alert success = new Alert(Alert.AlertType.INFORMATION,
                    "Đã lưu cấu hình font vào AutoTranslatorConfig.ini!\nBạn hãy khởi động lại game để cập nhật font.");
            if (cssFile.exists()) {
                success.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
            }
            success.showAndWait();
        });
    }

    public void autoExportToGame() {
        if (gamePathCombo == null)
            return;
        String path = getGamePath();
        if (path.isEmpty())
            return;
        File exe = new File(path);
        File transFile = new File(exe.getParentFile(), "BepInEx/Translation/vi/Text/_AutoGeneratedTranslations.txt");
        File activeCache = translateExecutor.getActiveCacheFile();
        if (activeCache == null || !activeCache.exists())
            return;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, String> cacheMap = mapper.readValue(activeCache,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                    });
            if (cacheMap.isEmpty())
                return;
            if (transFile.getParentFile() != null && !transFile.getParentFile().exists()) {
                transFile.getParentFile().mkdirs();
            }
            try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                    new java.io.OutputStreamWriter(new FileOutputStream(transFile), StandardCharsets.UTF_8))) {
                writer.write("# Generated by AI Translation Tool\n");
                for (Map.Entry<String, String> entry : cacheMap.entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
                }
            }
            System.out.println("Auto-exported " + cacheMap.size() + " translations to game file.");
        } catch (Exception ex) {
            System.err.println("Auto-export failed: " + ex.getMessage());
        }
    }

    public String getGamePath() {
        if (gamePathCombo == null)
            return "";
        GameHistoryManager.GameEntry selected = gamePathCombo.getValue();
        if (selected != null)
            return selected.getExePath();
        String typed = gamePathCombo.getEditor().getText();
        return (typed != null) ? typed.trim() : "";
    }

    private void openFile(File file) {
        if (!file.exists()) {
            System.err.println("File not found: " + file.getAbsolutePath());
            return;
        }
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec("notepad.exe " + file.getAbsolutePath());
            } else {
                java.awt.Desktop.getDesktop().open(file);
            }
        } catch (Exception ex) {
            System.err.println("Failed to open file: " + ex.getMessage());
        }
    }

    private Label createFormLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(javafx.scene.paint.Color.web("#e2e8f0"));
        label.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        return label;
    }

    private TextField createTextField(String promptText) {
        TextField tf = new TextField();
        tf.setPromptText(promptText);
        tf.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-background-radius: 6; -fx-padding: 8 12;");
        return tf;
    }

    private Button createSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #475569; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"));
        return btn;
    }
}
