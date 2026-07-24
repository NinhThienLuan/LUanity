package com.aiwrapper.javafx.ui;

import com.aiwrapper.executor.TranslateExecutor;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ActionsZoneCard extends VBox {

    private final Stage stage;
    private final TranslateExecutor translateExecutor;
    private final com.aiwrapper.config.AiConfig aiConfig;
    private final ObservableList<TranslationItem> historyList;
    private final Runnable onCacheChange;

    private ComboBox<GameHistoryManager.GameEntry> gamePathCombo;
    private MenuButton presetMenuButton;
    private PauseTransition saveDebounce = null;
    private boolean isRestoringPresets = false;
    private boolean updatingCombo = false;
    private Button btnToggleProxy;

    public ActionsZoneCard(Stage stage,
            TranslateExecutor translateExecutor,
            com.aiwrapper.config.AiConfig aiConfig,
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
    private java.util.function.Consumer<String> onGameChanged;

    public void setOnProxyStatusChanged(Runnable callback) {
        this.onProxyStatusChanged = callback;
    }

    public void setOnConsoleLog(java.util.function.Consumer<String> callback) {
        this.onConsoleLog = callback;
    }

    public void setOnGameChanged(java.util.function.Consumer<String> callback) {
        this.onGameChanged = callback;
    }

    private void initLayout() {
        Label zoneBTitle = new Label("PROXY ACTIONS & SHORTCUTS");
        zoneBTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        zoneBTitle.setTextFill(javafx.scene.paint.Color.web("#06b6d4"));

        // 1. Proxy Toggle
        btnToggleProxy = new Button();
        btnToggleProxy.setMaxWidth(Double.MAX_VALUE);
        btnToggleProxy.setPrefHeight(45);
        btnToggleProxy.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btnToggleProxy.setCursor(javafx.scene.Cursor.HAND);
        setupProxyToggle();

        // 2. Glossary & Prompt Toolbar
        Node resBtns = buildResourceButtons();

        // 3. Game Selector Section
        String savedGamePath = GameHistoryManager.loadActivePath();
        if (!savedGamePath.isEmpty()) {
            List<GameHistoryManager.GameEntry> existing = GameHistoryManager.load();
            boolean alreadyTracked = existing.stream()
                    .anyMatch(e -> e.getExePath().equalsIgnoreCase(savedGamePath));
            if (!alreadyTracked) {
                GameHistoryManager.upsert(savedGamePath);
            }
        }
        Node gamePathContainer = buildGameSelector(savedGamePath);

        // 4. Preset Selector Section
        List<GameHistoryManager.GameEntry> histEntries = GameHistoryManager.load();
        Node presetContainer = buildPresetSelector(savedGamePath, histEntries);

        // 5. BepInEx Utilities Box
        Node shortcutContainer = buildUtilitiesSection();

        getChildren().addAll(zoneBTitle, btnToggleProxy, resBtns, gamePathContainer, presetContainer,
                shortcutContainer);
    }

    private void setupProxyToggle() {
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
    }

    private Node buildResourceButtons() {
        Button editGlossaryBtn = UiStyles.createSecondaryButton("Edit Glossary");
        editGlossaryBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editGlossaryBtn, Priority.ALWAYS);
        editGlossaryBtn.setOnAction(evt -> openGlossaryDialog());

        Button editPromptBtn = UiStyles.createSecondaryButton("Edit Prompt");
        editPromptBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editPromptBtn, Priority.ALWAYS);
        editPromptBtn.setOnAction(evt -> openPromptDialog());

        return new HBox(8, editGlossaryBtn, editPromptBtn);
    }

    private void switchGame(String path) {
        if (updatingCombo)
            return;
        flushPendingPresetSave();
        if (path == null || path.trim().isEmpty()) {
            translateExecutor.setActiveCacheFile(null);
            if (onCacheChange != null)
                onCacheChange.run();
            return;
        }
        String trimmed = path.trim();
        GameHistoryManager.upsert(trimmed);

        // Refresh combo items (newest-first) and re-select
        updatingCombo = true;
        try {
            List<GameHistoryManager.GameEntry> updated = GameHistoryManager.load();
            gamePathCombo.getItems().setAll(updated);
            updated.stream()
                    .filter(e -> e.getExePath().equalsIgnoreCase(trimmed))
                    .findFirst()
                    .ifPresent(e -> gamePathCombo.setValue(e));
        } finally {
            updatingCombo = false;
        }

        // Load corresponding cache
        try {
            File exe = new File(trimmed);
            String gameName = exe.getName();
            if (gameName.endsWith(".exe"))
                gameName = gameName.substring(0, gameName.length() - 4);
            File gameCache = new File("data/cache_" + gameName + ".json");
            if (gameCache.exists()) {
                com.aiwrapper.service.CacheService cacheSvc = new com.aiwrapper.service.CacheService();
                Map<String, String> cMap = cacheSvc.loadCacheMap(gameCache);
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
            String restoredLangPair = "EN/VI";
            List<GameHistoryManager.GameEntry> updated = GameHistoryManager.load();
            for (GameHistoryManager.GameEntry entry : updated) {
                if (entry.getExePath().equalsIgnoreCase(trimmed)) {
                    restoredPreset = entry.getActivePreset();
                    restoredLangPair = entry.getLanguagePair();
                    break;
                }
            }
            populatePresetCheckboxes(restoredPreset);
            translateExecutor.setActivePreset(restoredPreset);
            translateExecutor.setLanguagePair(restoredLangPair);
            if (onGameChanged != null) {
                onGameChanged.accept(restoredLangPair);
            }
        } catch (Exception ex) {
            System.err.println("[ActionsZoneCard] switchGame restore preset failed: " + ex.getMessage());
        }

        if (onCacheChange != null)
            onCacheChange.run();
    }

    private Node buildGameSelector(String savedGamePath) {
        Label gamePathLabel = UiStyles.createFormLabel("Game Exe Path:");
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

        // Selecting from dropdown
        gamePathCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingCombo && newVal != null) {
                switchGame(newVal.getExePath());
            }
        });

        Button gamePathBrowse = UiStyles.createSecondaryButton("Chọn");
        gamePathBrowse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Game Executable");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Executable (*.exe)", "*.exe"));
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                switchGame(file.getAbsolutePath());
            }
        });
        HBox gamePathRow = new HBox(8, gamePathCombo, gamePathBrowse);
        return new VBox(6, gamePathLabel, gamePathRow);
    }

    private Node buildPresetSelector(String savedGamePath, List<GameHistoryManager.GameEntry> histEntries) {
        Label presetLabel = UiStyles.createFormLabel("Active Theme Presets:");
        presetMenuButton = new MenuButton("None");
        presetMenuButton.setMaxWidth(Double.MAX_VALUE);
        presetMenuButton.getStyleClass().add("preset-menu-button");

        // Initial populate of presets
        String currentRestoredState = null;
        if (!savedGamePath.isEmpty()) {
            for (GameHistoryManager.GameEntry entry : histEntries) {
                if (entry.getExePath().equalsIgnoreCase(savedGamePath)) {
                    currentRestoredState = entry.getActivePreset();
                    break;
                }
            }
        }
        populatePresetCheckboxes(currentRestoredState);
        translateExecutor.setActivePreset(currentRestoredState);

        return new VBox(6, presetLabel, presetMenuButton);
    }

    private void showPathWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng chọn đường dẫn game Game Exe trước!");
        alert.showAndWait();
    }

    private Node buildUtilitiesSection() {
        Label shortcutTitle = UiStyles.createFormLabel("BepInEx Utilities:");
        shortcutTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        shortcutTitle.setTextFill(javafx.scene.paint.Color.web("#06b6d4"));

        Button btnConfig = UiStyles.createSecondaryButton("File Config");
        Button btnLog = UiStyles.createSecondaryButton("Mở File Log");
        Button btnImport = UiStyles.createSecondaryButton("Nhập từ Game");

        btnConfig.setOnAction(e -> {
            String path = getGamePath();
            if (path.isEmpty()) {
                showPathWarning();
                return;
            }
            File exe = new File(path);
            File cfg = new File(exe.getParentFile(), "BepInEx/config/AutoTranslatorConfig.ini");
            openFile(cfg);
        });

        btnLog.setOnAction(e -> {
            String path = getGamePath();
            if (path.isEmpty()) {
                showPathWarning();
                return;
            }
            File exe = new File(path);
            File log = new File(exe.getParentFile(), "BepInEx/LogOutput.log");
            openFile(log);
        });

        btnImport.setOnAction(e -> {
            String path = getGamePath();
            if (path.isEmpty()) {
                showPathWarning();
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

        Button btnFont = UiStyles.createSecondaryButton("Cấu hình Font");
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
        Button btnSetup = UiStyles.createSecondaryButton("⬇ Cài BepInEx + AutoTranslator");
        btnSetup.setMaxWidth(Double.MAX_VALUE);
        btnSetup.setStyle(
                "-fx-background-color: #0f172a; -fx-text-fill: #38bdf8; -fx-border-color: #38bdf8; " +
                        "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-font-weight: bold;");
        btnSetup.setOnAction(e -> {
            String path = getGamePath();
            if (path.isEmpty()) {
                showPathWarning();
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
                    svc.setup(gameExe, 8080, translateExecutor.getToLang(),
                            msg -> javafx.application.Platform.runLater(() -> {
                                if (onConsoleLog != null)
                                    onConsoleLog.accept(msg);
                            }));
                    try {
                        AutoTranslatorConfigWriter.updateAutoTranslatorEndpoint(gameExe.getParentFile(),
                                aiConfig.getProvider());
                    } catch (Exception ex) {
                        System.err.println("Failed to update config endpoint: " + ex.getMessage());
                    }
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
        return new VBox(6, shortcutTitle, shortcutBox);
    }

    private void updateMenuButtonText(List<String> checked) {
        if (checked.isEmpty()) {
            presetMenuButton.setText("None");
        } else {
            presetMenuButton.setText(String.join(", ", checked));
        }
    }

    private void populatePresetCheckboxes(String activePresetsSpec) {
        presetMenuButton.getItems().clear();
        List<String> available = translateExecutor.listPresets();

        // Split restored preset spec (e.g. "ban_sung,sinh_ton")
        Set<String> activeSet = new java.util.HashSet<>();
        if (activePresetsSpec != null && !activePresetsSpec.isEmpty()) {
            for (String s : activePresetsSpec.split(",")) {
                activeSet.add(s.trim());
            }
        }

        isRestoringPresets = true;
        try {
            List<String> checked = new java.util.ArrayList<>();
            for (String name : available) {
                CheckBox cb = new CheckBox(name);
                cb.setStyle("-fx-text-fill: #e2e8f0; -fx-padding: 4 8;");
                if (activeSet.contains(name)) {
                    cb.setSelected(true);
                    checked.add(name);
                }

                cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!isRestoringPresets) {
                        triggerDebouncedPresetSave();
                    }
                });

                CustomMenuItem item = new CustomMenuItem(cb);
                item.setHideOnClick(false);
                presetMenuButton.getItems().add(item);
            }
            updateMenuButtonText(checked);
        } finally {
            isRestoringPresets = false;
        }
    }

    private void triggerDebouncedPresetSave() {
        if (saveDebounce != null) {
            saveDebounce.stop();
        }
        saveDebounce = new PauseTransition(Duration.millis(400));
        saveDebounce.setOnFinished(evt -> {
            flushPendingPresetSave();
        });
        saveDebounce.play();
    }

    public void flushPendingPresetSave() {
        if (saveDebounce != null) {
            saveDebounce.stop();
        }

        List<String> checked = new java.util.ArrayList<>();
        for (MenuItem node : presetMenuButton.getItems()) {
            if (node instanceof CustomMenuItem) {
                CustomMenuItem customItem = (CustomMenuItem) node;
                if (customItem.getContent() instanceof CheckBox) {
                    CheckBox cb = (CheckBox) customItem.getContent();
                    if (cb.isSelected()) {
                        checked.add(cb.getText());
                    }
                }
            }
        }

        updateMenuButtonText(checked);

        String spec = checked.isEmpty() ? null : String.join(",", checked);
        translateExecutor.setActivePreset(spec);
        String activeGamePath = getGamePath();
        if (!activeGamePath.isEmpty()) {
            GameHistoryManager.updateActivePreset(activeGamePath, spec);
            System.out.println("[ActionsZoneCard] Persisted active presets: " + spec);
        }
    }

    private void openGlossaryDialog() {
        GlossaryDialog.show(translateExecutor);
    }

    private void openPromptDialog() {
        PromptDialog.show(translateExecutor);
    }

    private void openFontDialog() {
        FontConfigDialog.show(this::getGamePath);
    }

    public void autoExportToGame() {
        if (gamePathCombo == null)
            return;
        String path = getGamePath();
        if (path.isEmpty())
            return;
        File activeCache = translateExecutor.getActiveCacheFile();
        if (activeCache == null || !activeCache.exists())
            return;
        try {
            com.aiwrapper.service.TranslationFileSyncService syncService = new com.aiwrapper.service.TranslationFileSyncService();
            syncService.autoExportToGame(path, activeCache);
            System.out.println("Auto-exported translations to game file successfully.");
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
}
