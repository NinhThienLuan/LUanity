package com.aiwrapper.javafx.ui;

import com.aiwrapper.config.AiConfig;
import com.aiwrapper.executor.TranslateExecutor;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
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

    private TextField gamePathField;
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

    public void setOnProxyStatusChanged(Runnable callback) {
        this.onProxyStatusChanged = callback;
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

        // Load saved game path
        String savedGamePath = "";
        File gamePathFile = new File("data/game_path.txt");
        if (gamePathFile.exists()) {
            try {
                savedGamePath = new String(Files.readAllBytes(gamePathFile.toPath()), StandardCharsets.UTF_8).trim();
            } catch (Exception ex) {
                // Ignore
            }
        }

        // Exe Path Option HBox
        Label gamePathLabel = createFormLabel("Game Exe Path:");
        gamePathField = createTextField("Path to game exe...");
        gamePathField.setText(savedGamePath);
        gamePathField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                try {
                    File file = new File("data/game_path.txt");
                    File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    Files.write(file.toPath(), newVal.trim().getBytes(StandardCharsets.UTF_8));

                    String path = newVal.trim();
                    if (!path.isEmpty()) {
                        File exe = new File(path);
                        String gameName = exe.getName();
                        if (gameName.endsWith(".exe")) {
                            gameName = gameName.substring(0, gameName.length() - 4);
                        }
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
                        }
                    } else {
                        translateExecutor.setActiveCacheFile(null);
                    }
                    if (onCacheChange != null) {
                        onCacheChange.run();
                    }
                } catch (Exception ex) {
                    // Ignore
                }
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
                gamePathField.setText(file.getAbsolutePath());
            }
        });
        HBox gamePathRow = new HBox(8, gamePathField, gamePathBrowse);
        VBox gamePathContainer = new VBox(6, gamePathLabel, gamePathRow);

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
            String path = gamePathField.getText().trim();
            if (path.isEmpty()) {
                showPathWarning.run();
                return;
            }
            File exe = new File(path);
            File cfg = new File(exe.getParentFile(), "BepInEx/config/AutoTranslatorConfig.ini");
            openFile(cfg);
        });

        btnLog.setOnAction(e -> {
            String path = gamePathField.getText().trim();
            if (path.isEmpty()) {
                showPathWarning.run();
                return;
            }
            File exe = new File(path);
            File log = new File(exe.getParentFile(), "BepInEx/LogOutput.log");
            openFile(log);
        });

        btnImport.setOnAction(e -> {
            String path = gamePathField.getText().trim();
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

        HBox shortcutBox = new HBox(8, btnConfig, btnLog, btnImport);
        shortcutBox.setPadding(new Insets(4, 0, 10, 0));
        VBox shortcutContainer = new VBox(6, shortcutTitle, shortcutBox);

        getChildren().addAll(zoneBTitle, btnToggleProxy, resBtns, gamePathContainer, shortcutContainer);
    }

    private void openGlossaryDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Glossary");
        File cssFile = new File("data/ui_style.css");
        if (cssFile.exists()) {
            dialog.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
        }

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        StringBuilder glossaryBuilder = new StringBuilder();
        File glossaryFile = new File("data/glossary.json");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        if (glossaryFile.exists()) {
            try {
                Map<String, String> map = mapper.readValue(glossaryFile,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                        });
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    glossaryBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
                }
            } catch (Exception ex) {
                System.err.println("Failed to read glossary.json: " + ex.getMessage());
            }
        }

        Label desc = new Label(
                "Add terminology mapping dictionary (one pair per line):\nFormat: EnglishWord=VietnameseTranslation");
        desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        TextArea textArea = new TextArea(glossaryBuilder.toString());
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

        dialog.showAndWait().ifPresent(content -> {
            Map<String, String> newMap = new java.util.LinkedHashMap<>();
            String[] lines = content.split("\\r?\\n");
            for (String line : lines) {
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();
                    if (!key.isEmpty() && !val.isEmpty()) {
                        newMap.put(key, val);
                    }
                }
            }
            try {
                File parent = glossaryFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                mapper.writerWithDefaultPrettyPrinter().writeValue(glossaryFile, newMap);
                System.out.println("Glossary saved to data/glossary.json successfully.");
            } catch (Exception ex) {
                System.err.println("Failed to write glossary.json: " + ex.getMessage());
            }
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

    public void autoExportToGame() {
        if (gamePathField == null)
            return;
        String path = gamePathField.getText().trim();
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
        return (gamePathField != null) ? gamePathField.getText().trim() : "";
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
