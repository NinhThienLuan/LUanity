package com.aiwrapper.ui;

import com.aiwrapper.config.AiConfig;
import com.aiwrapper.config.AppProperties;
import com.aiwrapper.config.IoProperties;
import com.aiwrapper.executor.BaseExecutor;
import com.aiwrapper.executor.ExecutorFactory;
import com.aiwrapper.executor.TranslateExecutor;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

@Component
public class JavaFxUi {

    private final ExecutorFactory executorFactory;
    private final AiConfig aiConfig;
    private final AppProperties appProperties;
    private final IoProperties ioProperties;
    private final TranslateExecutor translateExecutor;

    private int totalRequests = 0;
    private int totalChars = 0;
    private int cacheHits = 0;
    private int aiHits = 0;

    private final ObservableList<TranslationItem> historyList = FXCollections.observableArrayList();

    public static class TranslationItem {
        private final StringProperty type;
        private final StringProperty original;
        private final StringProperty translated;

        public TranslationItem(String type, String original, String translated) {
            this.type = new SimpleStringProperty(type);
            this.original = new SimpleStringProperty(original);
            this.translated = new SimpleStringProperty(translated);
        }

        public StringProperty typeProperty() {
            return type;
        }

        public StringProperty originalProperty() {
            return original;
        }

        public StringProperty translatedProperty() {
            return translated;
        }

        public String getType() {
            return type.get();
        }

        public String getOriginal() {
            return original.get();
        }

        public String getTranslated() {
            return translated.get();
        }

        public void setTranslated(String translated) {
            this.translated.set(translated);
        }
    }

    @Value("${local.server.port:8080}")
    private int localServerPort;

    private String customPromptTemplate = "Translate the following English game text into Vietnamese according to these rules:\n"
            +
            "1. For single words and phrases: translate literally and precisely (dịch sát nghĩa).\n" +
            "2. For full sentences: translate naturally to suit the game context (dịch phù hợp với ngữ cảnh).\n" +
            "3. Do not explain, do not write anything else, only return the Vietnamese translation.\n" +
            "4. Preserve placeholders like [[TAG_N]] exactly.\n" +
            "5. Absolutely do not output any conversational filler or unrelated details.\n\n" +
            "English: Continue\n" +
            "Vietnamese: Tiếp tục\n\n" +
            "English: Settings\n" +
            "Vietnamese: Cài đặt\n\n" +
            "English: {text}\n" +
            "Vietnamese:";

    public JavaFxUi(ExecutorFactory executorFactory,
            AiConfig aiConfig,
            AppProperties appProperties,
            IoProperties ioProperties,
            TranslateExecutor translateExecutor) {
        this.executorFactory = executorFactory;
        this.aiConfig = aiConfig;
        this.appProperties = appProperties;
        this.ioProperties = ioProperties;
        this.translateExecutor = translateExecutor;
    }

    public void start(Stage stage) {
        stage.setTitle("Modular AI Executor - Game translation Bridge");

        // Load saved game path
        String savedGamePath = "";
        java.io.File gamePathFile = new java.io.File("data/game_path.txt");
        if (gamePathFile.exists()) {
            try {
                savedGamePath = new String(java.nio.file.Files.readAllBytes(gamePathFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8).trim();
            } catch (Exception ex) {
                // Ignore
            }
        }

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #0f172a;"); // Slate 900
        root.setPrefWidth(1200);
        root.setPrefHeight(750);

        // Header
        Label titleLabel = new Label("AI Wrapper - Modular Executor");
        titleLabel.setTextFill(javafx.scene.paint.Color.web("#e2e8f0"));
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        Label subtitleLabel = new Label("Execute batch workflows using localized and cloud LLMs via Strategy Pattern");
        subtitleLabel.setTextFill(javafx.scene.paint.Color.web("#94a3b8"));
        subtitleLabel.setFont(Font.font("Segoe UI", 12));

        VBox header = new VBox(4, titleLabel, subtitleLabel);
        root.getChildren().add(header);

        // Form Container - Grid constraints adjusting for new elements
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER_LEFT);

        ColumnConstraints col1 = new ColumnConstraints(130);
        ColumnConstraints col2 = new ColumnConstraints(220);
        ColumnConstraints col3 = new ColumnConstraints(90);
        grid.getColumnConstraints().addAll(col1, col2, col3);

        // 1. Provider Selection
        Label providerLabel = createFormLabel("AI Provider:");
        ComboBox<String> providerSelect = new ComboBox<>(
                FXCollections.observableArrayList("ollama", "gemini", "googletranslate"));
        providerSelect.setValue(aiConfig.getProvider() != null ? aiConfig.getProvider() : "ollama");
        styleDropdown(providerSelect);
        providerSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                aiConfig.setProvider(newVal.toLowerCase());
            }
        });
        grid.add(providerLabel, 0, 0);
        grid.add(providerSelect, 1, 0);

        Button editGlossaryBtn = createSecondaryButton("Edit Glossary");
        editGlossaryBtn.setOnAction(evt -> {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Edit Glossary");
            dialog.setHeaderText("Add terminology mapping dictionary.\nFormat: Key=Value (one pair per line).");

            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            StringBuilder glossaryBuilder = new StringBuilder();
            java.io.File glossaryFile = new java.io.File("data/glossary.json");
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

            TextArea textArea = new TextArea(glossaryBuilder.toString());
            textArea.setWrapText(true);
            textArea.setPrefWidth(450);
            textArea.setPrefHeight(250);

            dialog.getDialogPane().setContent(textArea);
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
                    java.io.File parent = glossaryFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    mapper.writerWithDefaultPrettyPrinter().writeValue(glossaryFile, newMap);
                    System.out.println("Glossary saved to data/glossary.json successfully.");
                } catch (Exception ex) {
                    System.err.println("Failed to write glossary.json: " + ex.getMessage());
                }
            });
        });
        grid.add(editGlossaryBtn, 2, 0);

        // 2. Edit Prompt Button
        Button editPromptBtn = createSecondaryButton("Edit Prompt");
        editPromptBtn.setOnAction(evt -> {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Edit Prompt Template");
            dialog.setHeaderText(
                    "Modify the prompt template for translation.\nUse {text} placeholder for source text.");

            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            TextArea textArea = new TextArea(customPromptTemplate);
            textArea.setWrapText(true);
            textArea.setPrefWidth(450);
            textArea.setPrefHeight(150);

            dialog.getDialogPane().setContent(textArea);
            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    return textArea.getText();
                }
                return null;
            });

            dialog.showAndWait().ifPresent(newPrompt -> {
                customPromptTemplate = newPrompt;
                System.out.println("Prompt template updated.");
            });
        });

        // 3. Model Selection ComboBox (Editable)
        Label modelLabel = createFormLabel("Model:");
        ComboBox<String> modelSelect = new ComboBox<>();
        modelSelect.setEditable(true);
        modelSelect.setMaxWidth(Double.MAX_VALUE);
        modelSelect.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 8;");

        String defaultModel = "gemma2:2b";
        if ("gemini".equals(aiConfig.getProvider())) {
            defaultModel = aiConfig.getGemini().getModel();
            modelSelect.setItems(FXCollections.observableArrayList("gemini-1.5-flash", "gemini-1.5-pro"));
        } else {
            fetchOllamaModels(modelSelect);
        }
        modelSelect.setValue(defaultModel);

        providerSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if ("gemini".equalsIgnoreCase(newVal)) {
                    modelSelect.setItems(FXCollections.observableArrayList("gemini-1.5-flash", "gemini-1.5-pro"));
                    String def = aiConfig.getGemini().getModel();
                    modelSelect.setValue(def != null && !def.isEmpty() ? def : "gemini-1.5-flash");
                } else if ("ollama".equalsIgnoreCase(newVal)) {
                    fetchOllamaModels(modelSelect);
                } else if ("googletranslate".equalsIgnoreCase(newVal)) {
                    modelSelect.setItems(FXCollections.observableArrayList("default"));
                    modelSelect.setValue("default");
                }
            }
        });

        modelSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                String prov = providerSelect.getValue();
                if ("gemini".equalsIgnoreCase(prov)) {
                    aiConfig.getGemini().setModel(newVal.trim());
                } else if ("ollama".equalsIgnoreCase(prov)) {
                    aiConfig.getOllama().setModel(newVal.trim());
                }
            }
        });

        grid.add(modelLabel, 0, 1);
        grid.add(modelSelect, 1, 1);
        grid.add(editPromptBtn, 2, 1);

        // 4. Temperature Option
        Label tempLabel = createFormLabel("Temperature (0.1-1):");
        Slider tempSlider = new Slider(0.1, 1.0, 0.2);
        tempSlider.setShowTickLabels(true);
        tempSlider.setShowTickMarks(true);
        tempSlider.setMajorTickUnit(0.3);
        tempSlider.setBlockIncrement(0.1);
        tempSlider.setStyle("-fx-control-inner-background: #1e293b;");
        Label tempValueLabel = new Label("0.2");
        tempValueLabel.setTextFill(javafx.scene.paint.Color.web("#e2e8f0"));
        tempValueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        tempSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                double rounded = Math.round(newVal.doubleValue() * 10.0) / 10.0;
                tempValueLabel.setText(String.format("%.1f", rounded));
                String prov = providerSelect.getValue();
                if ("ollama".equalsIgnoreCase(prov)) {
                    aiConfig.getOllama().setTemperature(rounded);
                }
            }
        });
        HBox tempBox = new HBox(12, tempSlider, tempValueLabel);
        tempBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(tempLabel, 0, 2);
        grid.add(tempBox, 1, 2);

        // 5. Port Option
        Label portLabel = createFormLabel("Server Port:");
        TextField portField = createTextField(String.valueOf(localServerPort));
        portField.setMaxWidth(80);
        portField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.matches("\\d+")) {
                savePortToConfig(newVal);
            }
        });
        grid.add(portLabel, 0, 3);
        grid.add(portField, 1, 3);

        // 6. Game Exe Path Option
        Label gamePathLabel = createFormLabel("Game Exe Path:");
        TextField gamePathField = createTextField("Path to game exe...");
        gamePathField.setText(savedGamePath);
        gamePathField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                try {
                    java.io.File file = new java.io.File("data/game_path.txt");
                    java.io.File parent = file.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    java.nio.file.Files.write(file.toPath(),
                            newVal.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
        grid.add(gamePathLabel, 0, 4);
        grid.add(gamePathField, 1, 4);
        grid.add(gamePathBrowse, 2, 4);

        // BepInEx Shortcut Buttons Box
        Label shortcutTitle = createFormLabel("BepInEx Utilities:");
        shortcutTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        shortcutTitle.setTextFill(javafx.scene.paint.Color.web("#38bdf8"));

        Button btnConfig = createSecondaryButton("File Config");
        Button btnLog = createSecondaryButton("Console Log");
        Button btnFolder = createSecondaryButton("Folder Autogen");
        Button btnClear = createSecondaryButton("Xóa Cache");

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

        btnFolder.setOnAction(e -> {
            String path = gamePathField.getText().trim();
            if (path.isEmpty()) {
                showPathWarning.run();
                return;
            }
            File exe = new File(path);
            File folder = new File(exe.getParentFile(), "BepInEx/Translation/vi/Text");
            openFolder(folder);
        });

        btnClear.setOnAction(e -> {
            String path = gamePathField.getText().trim();
            if (path.isEmpty()) {
                showPathWarning.run();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Bạn có chắc chắn muốn xóa cache của game hiện tại và proxy không?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    File exe = new File(path);
                    String gameName = exe.getName();
                    if (gameName.endsWith(".exe")) {
                        gameName = gameName.substring(0, gameName.length() - 4);
                    }
                    java.io.File gameCache = new java.io.File("data/cache_" + gameName + ".json");
                    if (gameCache.exists()) {
                        gameCache.delete();
                    }
                    File targetCache = new File(exe.getParentFile(),
                            "BepInEx/Translation/vi/Text/_AutoGeneratedTranslations.txt");
                    if (targetCache.exists()) {
                        targetCache.delete();
                    }
                    historyList.clear();
                    totalRequests = 0;
                    totalChars = 0;
                    cacheHits = 0;
                    aiHits = 0;
                    System.out.println("Caches wiped successfully.");
                }
            });
        });

        HBox shortcutBox = new HBox(8, btnConfig, btnLog, btnFolder, btnClear);
        shortcutBox.setPadding(new Insets(4, 0, 12, 0));
        VBox shortcutContainer = new VBox(6, shortcutTitle, shortcutBox);

        // Separators
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #334155;");

        Label statusLabel = new Label("Status: Ready (Proxy listening...)");
        statusLabel.setTextFill(javafx.scene.paint.Color.web("#10b981"));
        statusLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        // Console log area
        TextArea console = new TextArea();
        console.setEditable(false);
        console.setWrapText(true);
        console.setPrefHeight(120);
        console.setMinHeight(120);
        console.setStyle(
                "-fx-control-inner-background: #0b0f19; -fx-text-fill: #10b981; -fx-font-family: Consolas, 'Courier New', monospace; -fx-font-size: 11;");
        console.appendText("System ready. Live proxy active.\n");

        // Intercept System.out to show in UI console
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                oldOut.write(b);
                javafx.application.Platform.runLater(() -> console.appendText(String.valueOf((char) b)));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                oldOut.write(b, off, len);
                String str = new String(b, off, len);
                javafx.application.Platform.runLater(() -> console.appendText(str));
            }
        }));

        // Left Container (Form parameters)
        VBox leftPane = new VBox(12);
        leftPane.setMinWidth(460);
        leftPane.setMaxWidth(460);
        leftPane.getChildren().addAll(grid, sep, shortcutContainer, statusLabel);

        // Right Container Setup (Stats, Search, TableView, Compact Console)
        VBox rightPane = new VBox(10);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        // Stats Box
        Label lblReqCount = new Label("Yêu cầu: 0 (Ký tự: 0)");
        lblReqCount.setTextFill(javafx.scene.paint.Color.web("#94a3b8"));
        lblReqCount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        Label lblCacheHits = new Label("Bộ đệm: 0");
        lblCacheHits.setTextFill(javafx.scene.paint.Color.web("#10b981"));
        lblCacheHits.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        Label lblAiHits = new Label("LLM Dịch: 0");
        lblAiHits.setTextFill(javafx.scene.paint.Color.web("#8b5cf6"));
        lblAiHits.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        HBox statsBox = new HBox(24, lblReqCount, lblCacheHits, lblAiHits);
        statsBox.setStyle("-fx-background-color: #1e293b; -fx-padding: 8 16; -fx-background-radius: 6;");
        statsBox.setAlignment(Pos.CENTER_LEFT);

        // Search Box
        Label searchLabel = createFormLabel("Tìm kiếm:");
        TextField searchField = createTextField("Nhập từ khóa cần tìm (Bản gốc hoặc bản dịch)...");
        searchField.setPrefWidth(300);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        Button clearSearch = createSecondaryButton("Xóa");
        clearSearch.setOnAction(e -> searchField.clear());
        HBox searchBox = new HBox(10, searchLabel, searchField, clearSearch);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        // History TableView
        TableView<TranslationItem> tableView = new TableView<>();
        tableView.setEditable(true);
        tableView.setStyle(
                "-fx-background-color: #0b0f19; -fx-control-inner-background: #0b0f19; -fx-table-cell-border-color: #1e293b;");

        TableColumn<TranslationItem, String> typeCol = new TableColumn<>("Loại");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(80);
        typeCol.setMaxWidth(100);
        typeCol.setStyle("-fx-alignment: CENTER; -fx-text-fill: #38bdf8;");

        TableColumn<TranslationItem, String> originCol = new TableColumn<>("Văn bản gốc");
        originCol.setCellValueFactory(new PropertyValueFactory<>("original"));
        originCol.setPrefWidth(280);
        originCol.setStyle("-fx-text-fill: #e2e8f0;");

        TableColumn<TranslationItem, String> transCol = new TableColumn<>("Bản dịch Việt");
        transCol.setCellValueFactory(new PropertyValueFactory<>("translated"));
        transCol.setPrefWidth(280);
        transCol.setStyle("-fx-text-fill: #22d3ee;");
        transCol.setCellFactory(TextFieldTableCell.forTableColumn());
        transCol.setOnEditCommit(event -> {
            TranslationItem item = event.getRowValue();
            String newTranslation = event.getNewValue();
            if (newTranslation != null && !newTranslation.equals(event.getOldValue())) {
                item.setTranslated(newTranslation);
                translateExecutor.updateCacheValue(item.getOriginal(), newTranslation);
            }
        });

        tableView.getColumns().addAll(typeCol, originCol, transCol);

        ContextMenu rowMenu = new ContextMenu();
        MenuItem mntAddToGlobal = new MenuItem("Thêm vào cache tổng (Shared Global)");
        mntAddToGlobal.setOnAction(evt -> {
            TranslationItem selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                translateExecutor.updateGlobalCacheValue(selected.getOriginal(), selected.getTranslated());
                System.out.println("Đã lưu bản dịch '" + selected.getOriginal() + "' vào cache tổng.");
            }
        });
        rowMenu.getItems().add(mntAddToGlobal);

        tableView.setRowFactory(tv -> {
            TableRow<TranslationItem> row = new TableRow<>();
            row.contextMenuProperty().bind(
                    javafx.beans.binding.Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(rowMenu));
            return row;
        });

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        // Filter and bind TableView
        FilteredList<TranslationItem> filteredData = new FilteredList<>(historyList, p -> true);
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }
                String lowerCaseFilter = newValue.toLowerCase();
                if (item.getOriginal().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                } else if (item.getTranslated().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                return false;
            });
        });
        tableView.setItems(filteredData);

        // Connect listener to real-time events from TranslateExecutor
        translateExecutor.setTranslationListener((original, translated, type, characterCount) -> {
            javafx.application.Platform.runLater(() -> {
                totalRequests++;
                totalChars += characterCount;
                if ("Cache".equals(type)) {
                    cacheHits++;
                } else {
                    aiHits++;
                }
                lblReqCount.setText("Yêu cầu: " + totalRequests + " (Ký tự: " + totalChars + ")");
                lblCacheHits.setText("Bộ đệm: " + cacheHits);
                lblAiHits.setText("LLM Dịch: " + aiHits);

                // Add to start of history list
                historyList.add(0, new TranslationItem(type, original, translated));
                if (historyList.size() > 500) {
                    historyList.remove(historyList.size() - 1);
                }
            });
        });

        // Live execution monitor label
        Label monitorLabel = createFormLabel("Live Execution Monitor / Console Log:");
        monitorLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        monitorLabel.setTextFill(javafx.scene.paint.Color.web("#38bdf8"));

        rightPane.getChildren().addAll(statsBox, searchBox, tableView, monitorLabel, console);

        // Main Layout (Left and Right Split Pane clone)
        HBox mainLayout = new HBox(20);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);
        mainLayout.getChildren().addAll(leftPane, rightPane);

        root.getChildren().addAll(mainLayout);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
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

    private void openFolder(File file) {
        if (!file.exists()) {
            file.mkdirs();
        }
        try {
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec("explorer.exe " + file.getAbsolutePath());
            } else {
                java.awt.Desktop.getDesktop().open(file);
            }
        } catch (Exception ex) {
            System.err.println("Failed to open folder: " + ex.getMessage());
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

    private void styleDropdown(ComboBox<String> cb) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 8;");
    }

    private void fetchOllamaModels(ComboBox<String> modelSelect) {
        String ollamaUrl = aiConfig.getOllama().getUrl();
        if (ollamaUrl == null || ollamaUrl.isEmpty()) {
            ollamaUrl = "http://localhost:11434";
        }
        if (ollamaUrl.endsWith("/")) {
            ollamaUrl = ollamaUrl.substring(0, ollamaUrl.length() - 1);
        }
        String finalUrl = ollamaUrl + "/api/tags";

        Task<List<String>> fetchTask = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
                Map<?, ?> response = rt.getForObject(finalUrl, Map.class);
                List<String> modelNames = new java.util.ArrayList<>();
                if (response != null && response.containsKey("models")) {
                    List<?> modelsList = (List<?>) response.get("models");
                    for (Object item : modelsList) {
                        if (item instanceof Map) {
                            Map<?, ?> modelMap = (Map<?, ?>) item;
                            if (modelMap.containsKey("name")) {
                                modelNames.add(String.valueOf(modelMap.get("name")));
                            }
                        }
                    }
                }
                return modelNames;
            }
        };

        fetchTask.setOnSucceeded(e -> {
            List<String> models = fetchTask.getValue();
            if (models != null && !models.isEmpty()) {
                modelSelect.setItems(FXCollections.observableArrayList(models));
                String current = modelSelect.getValue();
                if (current == null || current.isEmpty() || !models.contains(current)) {
                    if (models.contains("gemma2:2b")) {
                        modelSelect.setValue("gemma2:2b");
                    } else {
                        modelSelect.setValue(models.get(0));
                    }
                }
            }
        });

        fetchTask.setOnFailed(e -> {
            modelSelect.setItems(FXCollections.observableArrayList("gemma2:2b", "qwen2.5:3b"));
            if (modelSelect.getValue() == null || modelSelect.getValue().isEmpty()) {
                modelSelect.setValue("gemma2:2b");
            }
        });

        new Thread(fetchTask).start();
    }

    private void savePortToConfig(String newPort) {
        try {
            java.io.File file = new java.io.File("src/main/resources/application.yml");
            if (file.exists()) {
                String content = new String(java.nio.file.Files.readAllBytes(file.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8);
                content = content.replaceAll("(?m)^(\\s*port:\\s*)\\d+", "$1" + newPort);
                java.nio.file.Files.write(file.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                System.out.println(
                        "Port configuration updated in application.yml to " + newPort + ". Restart app to apply.");
            }
        } catch (Exception ex) {
            System.err.println("Failed to update application.yml port: " + ex.getMessage());
        }
    }
}
