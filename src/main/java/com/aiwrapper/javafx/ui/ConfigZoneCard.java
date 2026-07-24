package com.aiwrapper.javafx.ui;

import com.aiwrapper.config.AiConfig;
import com.aiwrapper.config.AppConfig;
import com.aiwrapper.config.AppConfigManager;
import com.aiwrapper.executor.TranslateExecutor;
import com.aiwrapper.provider.AiProviderFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigZoneCard extends VBox {

    private final AiConfig aiConfig;
    private final AiProviderFactory aiFactory;
    private final TranslateExecutor translateExecutor;
    private final Stage stage;

    private ComboBox<String> providerSelect;
    private PasswordField apiKeyField;
    private ComboBox<String> modelSelect;
    private Label apiKeyLabel;
    private Circle providerStatusDot;
    private ComboBox<String> langSelect;

    private String savedApiKey = "";
    private String savedOpenApiKey = "";

    public ConfigZoneCard(Stage stage, AiConfig aiConfig, AiProviderFactory aiFactory,
            TranslateExecutor translateExecutor) {
        super(12);
        this.stage = stage;
        this.aiConfig = aiConfig;
        this.aiFactory = aiFactory;
        this.translateExecutor = translateExecutor;

        setPadding(new Insets(16));
        setStyle(
                "-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 8; -fx-background-radius: 8;");

        loadSavedApiKeys();
        initLayout();
    }

    private void loadSavedApiKeys() {
        AppConfig appCfg = AppConfigManager.load();
        savedApiKey = appCfg.getGeminiApiKey();
        aiConfig.getGemini().setApiKey(savedApiKey);

        savedOpenApiKey = appCfg.getOpenaiApiKey();
        aiConfig.getOpenapi().setApiKey(savedOpenApiKey);
    }

    private void initLayout() {
        Label zoneATitle = new Label("SYSTEM CONFIGURATION");
        zoneATitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        zoneATitle.setTextFill(Color.web("#06b6d4"));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER_LEFT);
        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints(280);
        grid.getColumnConstraints().addAll(col1, col2);

        // 1. AI Provider selector
        Label providerLabel = createFormLabel("AI Provider:");
        providerSelect = new ComboBox<>(
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

        providerStatusDot = new Circle(5);
        providerStatusDot.setFill(Color.GRAY);

        Button btnOpenTestApi = new Button("🧪 Test API");
        btnOpenTestApi.setStyle(
                "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;");
        btnOpenTestApi.setOnMouseEntered(e -> btnOpenTestApi.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;"));
        btnOpenTestApi.setOnMouseExited(e -> btnOpenTestApi.setStyle(
                "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 4 10; -fx-cursor: hand;"));

        btnOpenTestApi.setOnAction(e -> ApiTestDialog.show(stage, aiConfig, aiFactory, translateExecutor));

        HBox providerHbox = new HBox(8);
        providerHbox.setAlignment(Pos.CENTER_LEFT);
        providerHbox.getChildren().addAll(providerSelect, providerStatusDot, btnOpenTestApi);

        grid.add(providerLabel, 0, 0);
        grid.add(providerHbox, 1, 0);

        // 1b. API Key label/field
        apiKeyLabel = createFormLabel("API Key:");
        apiKeyField = new PasswordField();
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

        Runnable checkOllamaHealth = () -> {
            providerStatusDot.setFill(Color.GRAY);
            String selectedProv = providerSelect.getValue();
            if ("ollama".equalsIgnoreCase(selectedProv)) {
                Thread thread = new Thread(() -> {
                    boolean ok = false;
                    try {
                        URL url = new URL("http://localhost:11434/api/tags");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
                    Platform.runLater(() -> {
                        if ("ollama".equalsIgnoreCase(providerSelect.getValue())) {
                            providerStatusDot.setFill(success ? Color.web("#22c55e") : Color.web("#ef4444"));
                        }
                    });
                });
                thread.setDaemon(true);
                thread.start();
            } else if ("gemini".equalsIgnoreCase(selectedProv)) {
                String key = apiKeyField.getText().trim();
                providerStatusDot.setFill(key.isEmpty() ? Color.web("#ef4444") : Color.web("#22c55e"));
            } else if ("openapi".equalsIgnoreCase(selectedProv) || "openai".equalsIgnoreCase(selectedProv)) {
                String key = apiKeyField.getText().trim();
                providerStatusDot.setFill(key.isEmpty() ? Color.web("#ef4444") : Color.web("#22c55e"));
            } else if ("googletranslate".equalsIgnoreCase(selectedProv)) {
                providerStatusDot.setFill(Color.web("#22c55e"));
            } else {
                providerStatusDot.setFill(Color.GRAY);
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
                AppConfig appCfg = AppConfigManager.load();
                if ("gemini".equalsIgnoreCase(prov)) {
                    aiConfig.getGemini().setApiKey(newVal.trim());
                    appCfg.setGeminiApiKey(newVal.trim());
                } else if ("openapi".equalsIgnoreCase(prov) || "openai".equalsIgnoreCase(prov)) {
                    aiConfig.getOpenapi().setApiKey(newVal.trim());
                    appCfg.setOpenaiApiKey(newVal.trim());
                }
                AppConfigManager.save(appCfg);
                checkOllamaHealth.run();
            }
        });

        Platform.runLater(checkOllamaHealth);

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
        modelSelect = new ComboBox<>();
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
        tempValueLabel.setTextFill(Color.web("#e2e8f0"));
        tempValueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));

        Label tempTipLabel = new Label("(Low = precise, High = creative)");
        tempTipLabel.setTextFill(Color.web("#64748b"));
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

        // 4. Language Pair
        Label langLabel = createFormLabel("Language Pair:");
        langSelect = new ComboBox<>(FXCollections.observableArrayList("EN/VI", "ZH/VI", "JA/VI", "KO/VI"));
        langSelect.setValue("EN/VI");
        styleDropdown(langSelect);
        langSelect.setMaxWidth(Double.MAX_VALUE);

        // Load saved state (from current app_config.json)
        AppConfig appConfig = AppConfigManager.load();
        String savedPair = appConfig.getLanguagePair();
        if (savedPair != null && !savedPair.trim().isEmpty()) {
            langSelect.setValue(savedPair);
            translateExecutor.setLanguagePair(savedPair);
        } else {
            appConfig.setLanguagePair("EN/VI");
            AppConfigManager.save(appConfig);
            translateExecutor.setLanguagePair("EN/VI");
        }

        langSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                AppConfig appCfg = AppConfigManager.load();
                appCfg.setLanguagePair(newVal);
                AppConfigManager.save(appCfg);

                translateExecutor.setLanguagePair(newVal);

                String activePath = GameHistoryManager.loadActivePath();
                if (activePath != null && !activePath.isEmpty()) {
                    GameHistoryManager.updateLanguagePair(activePath, newVal);
                    updateAutoTranslatorLanguages(new File(activePath).getParentFile(), translateExecutor.getFromLang(),
                            translateExecutor.getToLang());
                }
            }
        });

        grid.add(langLabel, 0, 4);
        grid.add(langSelect, 1, 4);

        getChildren().addAll(zoneATitle, grid);
    }

    public void selectLanguagePair(String languagePair) {
        if (languagePair != null && !languagePair.isEmpty() && langSelect != null) {
            langSelect.setValue(languagePair);
            translateExecutor.setLanguagePair(languagePair);
        }
    }

    private void updateAutoTranslatorLanguages(File gameRoot, String fromLang, String targetLang) {
        File configDir = new File(gameRoot, "BepInEx/config");
        File configFile = new File(configDir, "AutoTranslatorConfig.ini");
        if (!configFile.exists()) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
            List<String> output = new java.util.ArrayList<>();
            boolean generalBlock = false;
            boolean hasLanguage = false;
            boolean hasFromLanguage = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.equals("[General]")) {
                    generalBlock = true;
                    output.add(line);
                    continue;
                } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    generalBlock = false;
                }

                if (generalBlock) {
                    if (trimmed.startsWith("Language=")) {
                        output.add("Language=" + targetLang);
                        hasLanguage = true;
                    } else if (trimmed.startsWith("FromLanguage=")) {
                        output.add("FromLanguage=" + fromLang);
                        hasFromLanguage = true;
                    } else {
                        output.add(line);
                    }
                } else {
                    output.add(line);
                }
            }

            if (!hasLanguage || !hasFromLanguage) {
                for (int i = 0; i < output.size(); i++) {
                    if (output.get(i).trim().equals("[General]")) {
                        if (!hasLanguage) {
                            output.add(i + 1, "Language=" + targetLang);
                        }
                        if (!hasFromLanguage) {
                            output.add(i + 1, "FromLanguage=" + fromLang);
                        }
                        break;
                    }
                }
            }

            Files.write(configFile.toPath(), output, StandardCharsets.UTF_8);
            System.out.println("[ConfigZoneCard] Updated AutoTranslatorConfig.ini language pair: " + fromLang + " -> "
                    + targetLang);
        } catch (Exception ex) {
            System.err.println("[ConfigZoneCard] Failed to update AutoTranslatorConfig.ini: " + ex.getMessage());
        }
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
                List<String> modelNames = new ArrayList<>();
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

    private Label createFormLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#e2e8f0"));
        label.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        return label;
    }

    private void styleDropdown(ComboBox<String> cb) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 8;");
    }
}
