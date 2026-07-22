package com.aiwrapper.ui;

import com.aiwrapper.config.AiConfig;
import com.aiwrapper.config.AppProperties;
import com.aiwrapper.config.IoProperties;
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
    private Label lblZoneCTitle;
    private TextField gamePathField;
    private TableView<TranslationItem> tableView;
    private javafx.scene.control.ScrollBar verticalScrollBar;
    private boolean isAtBottom = true;

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
        stage.setTitle("LUanity Translator - Game translation Bridge");

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

        // Load saved Gemini API key
        String savedApiKey = "";
        java.io.File apiKeyFile = new java.io.File("data/gemini_key.txt");
        if (apiKeyFile.exists()) {
            try {
                savedApiKey = new String(java.nio.file.Files.readAllBytes(apiKeyFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8).trim();
                aiConfig.getGemini().setApiKey(savedApiKey);
            } catch (Exception ex) {
                // Ignore
            }
        }

        // Load saved OpenAPI API key
        String savedOpenApiKey = "";
        java.io.File openApiKeyFile = new java.io.File("data/openapi_key.txt");
        if (openApiKeyFile.exists()) {
            try {
                savedOpenApiKey = new String(java.nio.file.Files.readAllBytes(openApiKeyFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8).trim();
                aiConfig.getOpenapi().setApiKey(savedOpenApiKey);
            } catch (Exception ex) {
                // Ignore
            }
        }

        // Root Container
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0f172a;"); // Slate 900
        root.setPrefWidth(1200);
        root.setPrefHeight(750);

        // TOP STATUS BAR (Horizontal fixed status bar)
        HBox topStatusBar = new HBox(16);
        topStatusBar.setPadding(new Insets(8, 16, 8, 16));
        topStatusBar.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        topStatusBar.setPrefHeight(40);
        topStatusBar.setMinHeight(40);
        topStatusBar.setAlignment(Pos.CENTER_LEFT);

        Label lblStatusText = new Label(translateExecutor.isProxyActive() ? "STATUS: RUNNING" : "STATUS: STOPPED");
        lblStatusText.setStyle("-fx-font-weight: bold; -fx-text-fill: "
                + (translateExecutor.isProxyActive() ? "#06b6d4;" : "#ef4444;") + " -fx-font-size: 12px;");

        Label lblReqCount = new Label("Yêu cầu: 0 (Ký tự: 0)");
        lblReqCount.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Label lblCacheHits = new Label("Bộ đệm: 0");
        lblCacheHits.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Label lblAiHits = new Label("LLM Dịch: 0");
        lblAiHits.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        topStatusBar.getChildren().addAll(
                lblStatusText,
                createVerticalSeparator(),
                lblReqCount,
                createVerticalSeparator(),
                lblCacheHits,
                createVerticalSeparator(),
                lblAiHits);
        root.getChildren().add(topStatusBar);

        // Body Container (HBox)
        HBox bodyLayout = new HBox(20);
        bodyLayout.setPadding(new Insets(16));
        VBox.setVgrow(bodyLayout, Priority.ALWAYS);
        root.getChildren().add(bodyLayout);

        // LEFT PANEL (Zone A Config + Separator + Zone B Actions)
        VBox leftPane = new VBox(14);
        leftPane.setMinWidth(460);
        leftPane.setMaxWidth(460);

        // ZONE A: CONFIG
        Label zoneATitle = new Label("SYSTEM CONFIGURATION");
        zoneATitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        zoneATitle.setTextFill(javafx.scene.paint.Color.web("#06b6d4"));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER_LEFT);
        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints(280);
        grid.getColumnConstraints().addAll(col1, col2);

        // 1. Selector AI Provider
        Label providerLabel = createFormLabel("AI Provider:");
        ComboBox<String> providerSelect = new ComboBox<>(
                FXCollections.observableArrayList("Ollama", "Gemini", "Google Translate", "OpenAI"));

        String initialProv = aiConfig.getProvider() != null ? aiConfig.getProvider() : "ollama";
        String matchedVal = "Ollama";
        if ("gemini".equalsIgnoreCase(initialProv)) {
            matchedVal = "Gemini";
        } else if ("googletranslate".equalsIgnoreCase(initialProv) || "google".equalsIgnoreCase(initialProv)) {
            matchedVal = "Google Translate";
        } else if ("openapi".equalsIgnoreCase(initialProv) || "openai".equalsIgnoreCase(initialProv)) {
            matchedVal = "OpenAI";
        }
        providerSelect.setValue(matchedVal);
        styleDropdown(providerSelect);

        javafx.scene.shape.Circle providerStatusDot = new javafx.scene.shape.Circle(5);
        providerStatusDot.setFill(javafx.scene.paint.Color.GRAY);

        HBox providerBox = new HBox(8, providerSelect, providerStatusDot);
        providerBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(providerLabel, 0, 0);
        grid.add(providerBox, 1, 0);

        // 1b. API Key Row
        Label apiKeyLabel = createFormLabel("API Key:");
        PasswordField apiKeyField = new PasswordField();
        apiKeyField.setPromptText("Enter API Key...");

        if ("gemini".equalsIgnoreCase(initialProv)) {
            apiKeyLabel.setText("Gemini API Key:");
            apiKeyField.setPromptText("Enter Gemini API Key...");
            apiKeyField.setText(savedApiKey);
        } else if ("openapi".equalsIgnoreCase(initialProv) || "openai".equalsIgnoreCase(initialProv)) {
            apiKeyLabel.setText("OpenAPI API Key:");
            apiKeyField.setPromptText("Enter OpenAPI API Key...");
            apiKeyField.setText(savedOpenApiKey);
        }
        apiKeyField.setStyle(
                "-fx-background-color: #0b0f19; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 12; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 6;");
        // Health dot checker logic
        Runnable checkOllamaHealth = () -> {
            providerStatusDot.setFill(javafx.scene.paint.Color.GRAY);
            String selectedProv = providerSelect.getValue();
            if ("ollama".equalsIgnoreCase(selectedProv)) {
                Thread thread = new Thread(() -> {
                    boolean ok = false;
                    try {
                        java.net.URL url = new java.net.URL("http://localhost:11434/api/tags");
                        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(2000);
                        conn.setReadTimeout(2000);
                        int code = conn.getResponseCode();
                        if (code == 200) {
                            ok = true;
                        }
                    } catch (Exception ex) {
                        // ignore
                    }
                    final boolean success = ok;
                    javafx.application.Platform.runLater(() -> {
                        if ("ollama".equalsIgnoreCase(providerSelect.getValue())) {
                            providerStatusDot.setFill(success ? javafx.scene.paint.Color.web("#22c55e")
                                    : javafx.scene.paint.Color.web("#ef4444"));
                        }
                    });
                });
                thread.setDaemon(true);
                thread.start();
            } else if ("gemini".equalsIgnoreCase(selectedProv)) {
                String key = apiKeyField.getText().trim();
                providerStatusDot.setFill(key.isEmpty() ? javafx.scene.paint.Color.web("#ef4444")
                        : javafx.scene.paint.Color.web("#22c55e"));
            } else if ("openapi".equalsIgnoreCase(selectedProv) || "openai".equalsIgnoreCase(selectedProv)) {
                String key = apiKeyField.getText().trim();
                providerStatusDot.setFill(key.isEmpty() ? javafx.scene.paint.Color.web("#ef4444")
                        : javafx.scene.paint.Color.web("#22c55e"));
            } else if ("googletranslate".equalsIgnoreCase(selectedProv)) {
                providerStatusDot.setFill(javafx.scene.paint.Color.web("#22c55e"));
            } else {
                providerStatusDot.setFill(javafx.scene.paint.Color.GRAY);
            }
        };

        providerSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                aiConfig.setProvider(newVal.toLowerCase());
                if ("gemini".equalsIgnoreCase(newVal)) {
                    apiKeyField
                            .setText(aiConfig.getGemini().getApiKey() != null ? aiConfig.getGemini().getApiKey() : "");
                    apiKeyLabel.setText("Gemini API Key:");
                    apiKeyField.setPromptText("Enter Gemini API Key...");
                } else if ("openapi".equalsIgnoreCase(newVal) || "openai".equalsIgnoreCase(newVal)) {
                    apiKeyField.setText(
                            aiConfig.getOpenapi().getApiKey() != null ? aiConfig.getOpenapi().getApiKey() : "");
                    apiKeyLabel.setText("OpenAPI API Key:");
                    apiKeyField.setPromptText("Enter OpenAPI API Key...");
                }
                checkOllamaHealth.run();
            }
        });

        apiKeyField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String prov = providerSelect.getValue();
                if ("gemini".equalsIgnoreCase(prov)) {
                    aiConfig.getGemini().setApiKey(newVal.trim());
                    try {
                        java.io.File file = new java.io.File("data/gemini_key.txt");
                        java.io.File parent = file.getParentFile();
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }
                        java.nio.file.Files.write(file.toPath(),
                                newVal.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    } catch (Exception ex) {
                        // Ignore
                    }
                } else if ("openapi".equalsIgnoreCase(prov) || "openai".equalsIgnoreCase(prov)) {
                    aiConfig.getOpenapi().setApiKey(newVal.trim());
                    try {
                        java.io.File file = new java.io.File("data/openapi_key.txt");
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
                checkOllamaHealth.run();
            }
        });

        javafx.application.Platform.runLater(checkOllamaHealth);

        apiKeyLabel.managedProperty().bind(apiKeyLabel.visibleProperty());
        apiKeyField.managedProperty().bind(apiKeyField.visibleProperty());

        javafx.beans.binding.BooleanBinding needsApiKey = javafx.beans.binding.Bindings.createBooleanBinding(
                () -> "gemini".equalsIgnoreCase(providerSelect.getValue())
                        || "openapi".equalsIgnoreCase(providerSelect.getValue())
                        || "openai".equalsIgnoreCase(providerSelect.getValue()),
                providerSelect.valueProperty());
        apiKeyLabel.visibleProperty().bind(needsApiKey);
        apiKeyField.visibleProperty().bind(needsApiKey);

        grid.add(apiKeyLabel, 0, 1);
        grid.add(apiKeyField, 1, 1);

        // 2. Model ComboBox
        Label modelLabel = createFormLabel("Model:");
        ComboBox<String> modelSelect = new ComboBox<>();
        modelSelect.setEditable(true);
        modelSelect.setMaxWidth(Double.MAX_VALUE);
        modelSelect.setStyle(
                "-fx-background-color: #0b0f19; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 8; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 6;");

        String defaultModel = "gemma2:2b";
        if ("gemini".equals(aiConfig.getProvider())) {
            defaultModel = aiConfig.getGemini().getModel();
            modelSelect.setItems(FXCollections.observableArrayList("gemini-1.5-flash", "gemini-1.5-pro"));
        } else if ("openapi".equals(aiConfig.getProvider()) || "openai".equals(aiConfig.getProvider())) {
            defaultModel = aiConfig.getOpenapi().getModel();
            modelSelect.setItems(FXCollections.observableArrayList("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo"));
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
                } else if ("openapi".equalsIgnoreCase(newVal) || "openai".equalsIgnoreCase(newVal)) {
                    modelSelect.setItems(FXCollections.observableArrayList("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo"));
                    String def = aiConfig.getOpenapi().getModel();
                    modelSelect.setValue(def != null && !def.isEmpty() ? def : "gpt-4o-mini");
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
                } else if ("openapi".equalsIgnoreCase(prov) || "openai".equalsIgnoreCase(prov)) {
                    aiConfig.getOpenapi().setModel(newVal.trim());
                } else if ("ollama".equalsIgnoreCase(prov)) {
                    aiConfig.getOllama().setModel(newVal.trim());
                }
            }
        });
        grid.add(modelLabel, 0, 2);
        grid.add(modelSelect, 1, 2);

        // 3. Slider Temperature
        Label tempLabel = createFormLabel("Temperature (0.1-1):");
        Slider tempSlider = new Slider(0.1, 1.0, 0.2);
        tempSlider.setShowTickLabels(true);
        tempSlider.setShowTickMarks(true);
        tempSlider.setMajorTickUnit(0.3);
        tempSlider.setBlockIncrement(0.1);
        tempSlider.setPrefWidth(150);
        tempSlider.setMinWidth(150);
        tempSlider.setStyle("-fx-control-inner-background: #0b0f19;");
        Label tempValueLabel = new Label("0.2");
        tempValueLabel.setTextFill(javafx.scene.paint.Color.web("#e2e8f0"));
        tempValueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        Label tempTipLabel = new Label("(Low = precise, High = creative)");
        tempTipLabel.setTextFill(javafx.scene.paint.Color.web("#64748b"));
        tempTipLabel.setFont(Font.font("Segoe UI", 10));

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

        HBox tempSliderRow = new HBox(8, tempSlider, tempValueLabel);
        tempSliderRow.setAlignment(Pos.CENTER_LEFT);

        VBox tempBox = new VBox(2, tempSliderRow, tempTipLabel);
        tempBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(tempLabel, 0, 3);
        grid.add(tempBox, 1, 3);

        VBox zoneACard = new VBox(12, zoneATitle, grid);
        zoneACard.setPadding(new Insets(16));
        zoneACard.setStyle(
                "-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        // ZONE B: ACTIONS
        Label zoneBTitle = new Label("PROXY ACTIONS & SHORTCUTS");
        zoneBTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        zoneBTitle.setTextFill(javafx.scene.paint.Color.web("#06b6d4"));

        // Big toggle proxy button (with high contrast gradient + shadows)
        Button btnToggleProxy = new Button();
        btnToggleProxy.setMaxWidth(Double.MAX_VALUE);
        btnToggleProxy.setPrefHeight(45);
        btnToggleProxy.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        btnToggleProxy.setCursor(javafx.scene.Cursor.HAND);

        // Styling helpers for toggle button state transitions
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

        // Initial style mapping
        applyToggleStyle.accept(translateExecutor.isProxyActive());

        btnToggleProxy.setOnAction(e -> {
            boolean currentActive = translateExecutor.isProxyActive();
            if (currentActive) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Stop");
                confirm.setHeaderText("Stop Translation Proxy?");
                confirm.setContentText("Are you sure you want to stop the translation proxy?");
                java.io.File cssFile = new java.io.File("data/ui_style.css");
                if (cssFile.exists()) {
                    confirm.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                java.util.Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    translateExecutor.setProxyActive(false);
                    applyToggleStyle.accept(false);
                    lblStatusText.setText("STATUS: STOPPED");
                    lblStatusText.setStyle("-fx-font-weight: bold; -fx-text-fill: #ef4444; -fx-font-size: 12px;");
                    System.out.println("Proxy stopped.");
                }
            } else {
                translateExecutor.setProxyActive(true);
                applyToggleStyle.accept(true);
                lblStatusText.setText("STATUS: RUNNING");
                lblStatusText.setStyle("-fx-font-weight: bold; -fx-text-fill: #06b6d4; -fx-font-size: 12px;");
                System.out.println("Proxy started.");
            }
        });

        // Edit Glossary & Edit Prompt Dialog Resource buttons (Side-by-side)
        Button editGlossaryBtn = createSecondaryButton("Edit Glossary");
        editGlossaryBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editGlossaryBtn, Priority.ALWAYS);
        editGlossaryBtn.setOnAction(evt -> {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Edit Glossary");
            java.io.File cssFile = new java.io.File("data/ui_style.css");
            if (cssFile.exists()) {
                dialog.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
            }

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

        Button editPromptBtn = createSecondaryButton("Edit Prompt");
        editPromptBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editPromptBtn, Priority.ALWAYS);
        editPromptBtn.setOnAction(evt -> {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Edit Prompt Template");
            java.io.File cssFile = new java.io.File("data/ui_style.css");
            if (cssFile.exists()) {
                dialog.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
            }

            ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            Label desc = new Label(
                    "Modify the prompt template for translation.\nUse {text} placeholder for source text.");
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
        });

        HBox resBtns = new HBox(8, editGlossaryBtn, editPromptBtn);

        // BepInEx Shortcut Buttons Box
        Label shortcutTitle = createFormLabel("BepInEx Utilities:");
        shortcutTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        shortcutTitle.setTextFill(javafx.scene.paint.Color.web("#06b6d4")); // cyan accent

        Button btnConfig = createSecondaryButton("File Config");
        Button btnLog = createSecondaryButton("Mở File Log");

        Runnable showPathWarning = () -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng chọn đường dẫn game Game Exe trước!");
            alert.showAndWait();
        };

        // Exe Path Option HBox
        Label gamePathLabel = createFormLabel("Game Exe Path:");
        gamePathField = createTextField("Path to game exe...");
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

                    // Automatically load game cache into TableView if available
                    String path = newVal.trim();
                    if (!path.isEmpty()) {
                        File exe = new File(path);
                        String gameName = exe.getName();
                        if (gameName.endsWith(".exe")) {
                            gameName = gameName.substring(0, gameName.length() - 4);
                        }
                        java.io.File gameCache = new java.io.File("data/cache_" + gameName + ".json");
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
                    updateActiveCacheLabel();
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

        Button btnImport = createSecondaryButton("Nhập từ Game");
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
                java.io.File cssFile = new java.io.File("data/ui_style.css");
                if (cssFile.exists()) {
                    alert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                alert.showAndWait();
                return;
            }

            try {
                List<String> lines = java.nio.file.Files.readAllLines(transFile.toPath(),
                        java.nio.charset.StandardCharsets.UTF_8);
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
                    java.io.File cssFile = new java.io.File("data/ui_style.css");
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
                java.io.File cssFile = new java.io.File("data/ui_style.css");
                if (cssFile.exists()) {
                    confirm.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                java.util.Optional<ButtonType> result = confirm.showAndWait();
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

                    updateActiveCacheLabel();

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
                java.io.File cssFile = new java.io.File("data/ui_style.css");
                if (cssFile.exists()) {
                    errAlert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                errAlert.showAndWait();
            }
        });

        HBox shortcutBox = new HBox(8, btnConfig, btnLog, btnImport);
        shortcutBox.setPadding(new Insets(4, 0, 10, 0));
        VBox shortcutContainer = new VBox(6, shortcutTitle, shortcutBox);

        VBox zoneBCard = new VBox(14, zoneBTitle, btnToggleProxy, resBtns, gamePathContainer, shortcutContainer);
        zoneBCard.setPadding(new Insets(16));
        zoneBCard.setStyle(
                "-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        leftPane.getChildren().addAll(zoneACard, zoneBCard);

        // RIGHT PANEL: ZONE C (TABLE & MONITOR)
        VBox rightPane = new VBox(10);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        // Zone C Title
        lblZoneCTitle = new Label("ZONE C - TRANSLATION MONITOR & CACHE");
        lblZoneCTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblZoneCTitle.setTextFill(javafx.scene.paint.Color.web("#94a3b8"));

        // Search + Cache Actions Box
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = createFormLabel("Tìm kiếm:");

        // Search & Overlay container
        StackPane searchFieldOverlay = new StackPane();
        TextField searchField = createTextField("Nhập từ khóa cần tìm (Bản gốc hoặc bản dịch)...");
        searchField.setPrefWidth(300);
        HBox.setHgrow(searchFieldOverlay, Priority.ALWAYS);

        Button btnClearSearch = new Button("×");
        btnClearSearch.setCursor(javafx.scene.Cursor.HAND);
        btnClearSearch.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 10 0 0; -fx-border-width: 0;");
        StackPane.setAlignment(btnClearSearch, Pos.CENTER_RIGHT);
        btnClearSearch.visibleProperty().bind(searchField.textProperty().isNotEmpty());
        btnClearSearch.setOnAction(e -> searchField.clear());

        searchFieldOverlay.getChildren().addAll(searchField, btnClearSearch);

        // INLINE ADD ROW FORM
        HBox addRowBox = new HBox(8);
        addRowBox.setAlignment(Pos.CENTER_LEFT);
        addRowBox.setPadding(new Insets(6, 0, 6, 0));
        addRowBox.setManaged(false);
        addRowBox.setVisible(false);

        TextField addOriginField = createTextField("English (Văn bản gốc)...");
        addOriginField.setPrefWidth(200);
        HBox.setHgrow(addOriginField, Priority.ALWAYS);

        TextField addTransField = createTextField("Vietnamese (Bản dịch)...");
        addTransField.setPrefWidth(200);
        HBox.setHgrow(addTransField, Priority.ALWAYS);

        Button btnCommitAdd = createSecondaryButton("Lưu");
        btnCommitAdd.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: black; -fx-font-weight: bold;");

        Button btnCancelAdd = createSecondaryButton("Hủy");

        addRowBox.getChildren().addAll(addOriginField, addTransField, btnCommitAdd, btnCancelAdd);

        btnCommitAdd.setOnAction(e -> {
            String orig = addOriginField.getText().trim();
            String tran = addTransField.getText().trim();
            if (!orig.isEmpty() && !tran.isEmpty()) {
                translateExecutor.updateCacheValue(orig, tran);
                historyList.add(new TranslationItem("Cache", orig, tran));
                addOriginField.clear();
                addTransField.clear();
                addRowBox.setVisible(false);
                addRowBox.setManaged(false);
                System.out.println("Added cache row: " + orig + " -> " + tran);
                autoExportToGame();
                javafx.application.Platform.runLater(() -> {
                    if (tableView != null) {
                        tableView.requestFocus();
                        TranslationItem currentlySelected = tableView.getSelectionModel().getSelectedItem();
                        if (currentlySelected != null) {
                            tableView.getSelectionModel().select(currentlySelected);
                            tableView.scrollTo(currentlySelected);
                        }
                    }
                });
            }
        });

        btnCancelAdd.setOnAction(e -> {
            addOriginField.clear();
            addTransField.clear();
            addRowBox.setVisible(false);
            addRowBox.setManaged(false);
        });

        Button btnAddRow = new Button("+");
        btnAddRow.setCursor(javafx.scene.Cursor.HAND);
        btnAddRow.setStyle(
                "-fx-background-color: #0284c7; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-family: 'Segoe UI'; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnAddRow.setOnAction(e -> {
            if (addRowBox.isVisible()) {
                addRowBox.setVisible(false);
                addRowBox.setManaged(false);
            } else {
                addRowBox.setManaged(true);
                addRowBox.setVisible(true);
                addOriginField.requestFocus();
            }
        });

        Button btnSync = new Button("↻");
        btnSync.setCursor(javafx.scene.Cursor.HAND);
        btnSync.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: #06b6d4; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-family: 'Segoe UI'; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 6; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnSync.setTooltip(new Tooltip("Sync"));
        btnSync.setOnAction(e -> {
            String path = gamePathField.getText().trim();
            if (path.isEmpty()) {
                showPathWarning.run();
                return;
            }
            File exe = new File(path);
            File transFile = new File(exe.getParentFile(),
                    "BepInEx/Translation/vi/Text/_AutoGeneratedTranslations.txt");

            File activeCache = translateExecutor.getActiveCacheFile();
            if (activeCache == null || !activeCache.exists()) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setHeaderText("No Active Cache");
                alert.setContentText("There is no active cache file loaded or found for this game.");
                java.io.File cssFile = new java.io.File("data/ui_style.css");
                if (cssFile.exists()) {
                    alert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                alert.showAndWait();
                return;
            }

            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, String> cacheMap = mapper.readValue(activeCache,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                        });

                if (cacheMap.isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Information");
                    alert.setHeaderText("Active Cache Empty");
                    alert.setContentText("The active JSON cache file is empty. Nothing to export.");
                    java.io.File cssFile = new java.io.File("data/ui_style.css");
                    if (cssFile.exists()) {
                        alert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                    }
                    alert.showAndWait();
                    return;
                }

                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Export");
                confirm.setHeaderText("Export Cache to Game");
                confirm.setContentText(
                        "This will write " + cacheMap.size() + " translations to BepInEx's translation file:\n"
                                + transFile.getAbsolutePath() + "\n\nDo you want to proceed?");
                java.io.File cssFile = new java.io.File("data/ui_style.css");
                if (cssFile.exists()) {
                    confirm.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                java.util.Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    if (transFile.getParentFile() != null && !transFile.getParentFile().exists()) {
                        transFile.getParentFile().mkdirs();
                    }

                    try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                            new java.io.OutputStreamWriter(new java.io.FileOutputStream(transFile),
                                    java.nio.charset.StandardCharsets.UTF_8))) {
                        writer.write("# Generated by AI Translation Tool\n");
                        for (Map.Entry<String, String> entry : cacheMap.entrySet()) {
                            writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
                        }
                    }

                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                    successAlert.setTitle("Success");
                    successAlert.setHeaderText("Export Complete");
                    successAlert
                            .setContentText("Successfully exported " + cacheMap.size() + " translations to game file!");
                    if (cssFile.exists()) {
                        successAlert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                    }
                    successAlert.showAndWait();
                }
            } catch (Exception ex) {
                Alert errAlert = new Alert(Alert.AlertType.ERROR);
                errAlert.setTitle("Error");
                errAlert.setHeaderText("Export Failed");
                errAlert.setContentText("Failed to export to game:\n" + ex.getMessage());
                java.io.File cssFile = new java.io.File("data/ui_style.css");
                if (cssFile.exists()) {
                    errAlert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                errAlert.showAndWait();
            }
        });

        Button btnCache = new Button("Cache");
        btnCache.setCursor(javafx.scene.Cursor.HAND);
        btnCache.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-family: 'Segoe UI'; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 6; -fx-font-weight: bold;");

        ContextMenu cacheMenu = new ContextMenu();

        MenuItem optLoadCache = new MenuItem("Chọn Cache...");
        optLoadCache.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Cache JSON File");
            chooser.setInitialDirectory(new java.io.File("data"));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON (*.json)", "*.json"));
            java.io.File file = chooser.showOpenDialog(stage);
            if (file != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    Map<String, String> cMap = mapper.readValue(file,
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {
                            });
                    historyList.clear();
                    for (Map.Entry<String, String> entry : cMap.entrySet()) {
                        historyList.add(new TranslationItem("Cache", entry.getKey(), entry.getValue()));
                    }
                    translateExecutor.setActiveCacheFile(file);
                    updateActiveCacheLabel();
                    System.out.println("Loaded " + cMap.size() + " translation mappings from cache: " + file.getName());
                } catch (Exception ex) {
                    System.err.println("Failed to read cache file: " + ex.getMessage());
                }
            }
        });

        MenuItem optClearCache = new MenuItem("Xóa Cache Game");
        optClearCache.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        optClearCache.setOnAction(e -> {
            String path = gamePathField.getText().trim();
            if (path.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("Game Executable Key Missing");
                alert.setContentText("Please select the Game Exe Path in the settings panel first.");
                java.io.File cssFile = new java.io.File("data/ui_style.css");
                if (cssFile.exists()) {
                    alert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                alert.showAndWait();
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Deletion");
            confirm.setHeaderText("Clear cache files?");
            confirm.setContentText("Are you sure you want to delete the game cache and proxy cached translations?");
            java.io.File cssFile = new java.io.File("data/ui_style.css");
            if (cssFile.exists()) {
                confirm.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
            }
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
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
                    translateExecutor.setActiveCacheFile(null);
                    updateActiveCacheLabel();
                    totalRequests = 0;
                    totalChars = 0;
                    cacheHits = 0;
                    aiHits = 0;
                    lblReqCount.setText("Yêu cầu: 0 (Ký tự: 0)");
                    lblCacheHits.setText("Bộ đệm: 0");
                    lblAiHits.setText("LLM Dịch: 0");
                    System.out.println("Caches wiped successfully.");
                }
            });
        });

        cacheMenu.getItems().addAll(optLoadCache, optClearCache);

        btnCache.setOnAction(e -> {
            if (cacheMenu.isShowing()) {
                cacheMenu.hide();
            } else {
                cacheMenu.show(btnCache, javafx.geometry.Side.BOTTOM, btnCache.getWidth() - 170, 0);
            }
        });

        searchBox.getChildren().addAll(searchLabel, searchFieldOverlay, btnAddRow, btnSync, btnCache);

        // History TableView
        tableView = new TableView<>();
        tableView.setEditable(true);
        tableView.setStyle(
                "-fx-background-color: #0b0f19; -fx-control-inner-background: #0b0f19; -fx-table-cell-border-color: #1e293b;");
        tableView.setFixedCellSize(30.0);
        tableView.setPrefHeight(300);
        tableView.setMinHeight(200);

        TableColumn<TranslationItem, String> typeCol = new TableColumn<>("Loại");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(80);
        typeCol.setMaxWidth(100);
        typeCol.setStyle("-fx-alignment: CENTER; -fx-text-fill: #06b6d4;"); // Cyan accent for type Column

        TableColumn<TranslationItem, String> originCol = new TableColumn<>("Văn bản gốc");
        originCol.setCellValueFactory(new PropertyValueFactory<>("original"));
        originCol.setPrefWidth(280);
        originCol.setStyle("-fx-text-fill: #e2e8f0;");

        TableColumn<TranslationItem, String> transCol = new TableColumn<>("Bản dịch Việt");
        transCol.setCellValueFactory(new PropertyValueFactory<>("translated"));
        transCol.setPrefWidth(280);
        transCol.setStyle("-fx-text-fill: #06b6d4;"); // Cyan accent for translation Column
        transCol.setCellFactory(TextFieldTableCell.forTableColumn());
        transCol.setOnEditCommit(event -> {
            TranslationItem item = event.getRowValue();
            String newTranslation = event.getNewValue();
            if (newTranslation != null && !newTranslation.equals(event.getOldValue())) {
                item.setTranslated(newTranslation);
                translateExecutor.updateCacheValue(item.getOriginal(), newTranslation);
                autoExportToGame();
            }
        });

        tableView.getColumns().addAll(typeCol, originCol, transCol);

        ContextMenu rowMenu = new ContextMenu();
        MenuItem mntPin = new MenuItem("Pin (Thêm vào glossary)");
        mntPin.setOnAction(evt -> {
            TranslationItem selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                translateExecutor.updateGlossaryValue(selected.getOriginal(), selected.getTranslated());
                System.out.println("Pinned translation to glossary: " + selected.getOriginal() + " -> "
                        + selected.getTranslated());
            }
        });

        MenuItem mntDelete = new MenuItem("Xóa dòng");
        mntDelete.setStyle("-fx-text-fill: #ef4444;");
        mntDelete.setOnAction(evt -> {
            TranslationItem selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                translateExecutor.deleteCacheValue(selected.getOriginal());
                historyList.remove(selected);
                System.out.println("Deleted cache row: " + selected.getOriginal());
                autoExportToGame();
            }
        });
        rowMenu.getItems().addAll(mntPin, mntDelete);

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

        // Auto-scroll lockout tracked via scrollbar values

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

        // Console Log text area
        Label monitorLabel = createFormLabel("Live Execution Monitor / Console Log:");
        monitorLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        monitorLabel.setTextFill(javafx.scene.paint.Color.web("#06b6d4")); // cyan accent

        TextArea console = new TextArea();
        console.setEditable(false);
        console.setWrapText(true);
        console.setPrefHeight(150);
        console.setMinHeight(120);
        console.setStyle(
                "-fx-control-inner-background: #0b0f19; -fx-text-fill: #06b6d4; -fx-font-family: Consolas, 'Courier New', monospace; -fx-font-size: 11;");
        console.appendText("System ready. Live proxy active.\n");

        // Intercept System.out
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

        rightPane.getChildren().addAll(lblZoneCTitle, searchBox, addRowBox, tableView, monitorLabel, console);

        bodyLayout.getChildren().addAll(leftPane, rightPane);

        // Connect listener to stats
        translateExecutor.setTranslationListener((original, translated, type, characterCount) -> {
            javafx.application.Platform.runLater(() -> {
                totalRequests++;
                totalChars += characterCount;
                if ("cache".equalsIgnoreCase(type)) {
                    cacheHits++;
                } else {
                    aiHits++;
                }
                lblReqCount.setText("Yêu cầu: " + totalRequests + " (Ký tự: " + totalChars + ")");
                lblCacheHits.setText("Bộ đệm: " + cacheHits);
                lblAiHits.setText("LLM Dịch: " + aiHits);
                TranslationItem selected = (tableView != null) ? tableView.getSelectionModel().getSelectedItem() : null;
                TranslationItem newItem = new TranslationItem(type, original, translated);
                historyList.add(newItem);
                if (historyList.size() > 500) {
                    historyList.remove(0);
                }
                if (tableView != null && selected != null) {
                    tableView.getSelectionModel().select(selected);
                }
                if (isAtBottom && tableView != null) {
                    tableView.scrollTo(historyList.size() - 1);
                }
            });
        });

        if (!savedGamePath.isEmpty()) {
            try {
                File exe = new File(savedGamePath);
                String gameName = exe.getName();
                if (gameName.endsWith(".exe")) {
                    gameName = gameName.substring(0, gameName.length() - 4);
                }
                java.io.File gameCache = new java.io.File("data/cache_" + gameName + ".json");
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
                }
            } catch (Exception ex) {
                // Ignore
            }
        }

        updateActiveCacheLabel();

        Scene scene = new Scene(root);
        java.io.File cssFile = new java.io.File("data/ui_style.css");
        if (cssFile.exists()) {
            scene.getStylesheets().add(cssFile.toURI().toString());
        }
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
        javafx.application.Platform.runLater(this::initScrollTracking);
    }

    private void updateActiveCacheLabel() {
        if (lblZoneCTitle == null)
            return;
        File active = translateExecutor.getActiveCacheFile();
        String activeName = (active != null) ? active.getName() : "none";
        if (!isAtBottom) {
            lblZoneCTitle.setText("ZONE C - TRANSLATION MONITOR & CACHE (" + activeName
                    + ") [⏸ Đang xem cache cũ - cuộn xuống cuối để tiếp tục theo dõi]");
        } else {
            lblZoneCTitle.setText("ZONE C - TRANSLATION MONITOR & CACHE (" + activeName + ")");
        }
    }

    private void initScrollTracking() {
        if (tableView == null)
            return;
        tableView.applyCss();
        tableView.layout();
        verticalScrollBar = findVerticalScrollBar(tableView);
        if (verticalScrollBar != null) {
            verticalScrollBar.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    isAtBottom = (newVal.doubleValue() >= 0.98);
                    updateActiveCacheLabel();
                }
            });
            System.out.println("Vertical scroll tracking initialized successfully.");
        } else {
            System.err.println("Failed to find vertical ScrollBar in TableView.");
        }
    }

    private javafx.scene.control.ScrollBar findVerticalScrollBar(javafx.scene.Node node) {
        if (node instanceof javafx.scene.control.ScrollBar) {
            javafx.scene.control.ScrollBar sb = (javafx.scene.control.ScrollBar) node;
            if (sb.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                return sb;
            }
        }
        if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                javafx.scene.control.ScrollBar sb = findVerticalScrollBar(child);
                if (sb != null) {
                    return sb;
                }
            }
        }
        return null;
    }

    private void autoExportToGame() {
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
                    new java.io.OutputStreamWriter(new java.io.FileOutputStream(transFile),
                            java.nio.charset.StandardCharsets.UTF_8))) {
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

    private Separator createVerticalSeparator() {
        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep.setStyle("-fx-background-color: #334155;");
        sep.setPrefHeight(16);
        return sep;
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
