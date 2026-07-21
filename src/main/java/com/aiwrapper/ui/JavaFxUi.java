package com.aiwrapper.ui;

import com.aiwrapper.config.AiConfig;
import com.aiwrapper.config.AppProperties;
import com.aiwrapper.config.IoProperties;
import com.aiwrapper.executor.BaseExecutor;
import com.aiwrapper.executor.ExecutorFactory;
import com.aiwrapper.provider.AiProviderFactory;
import javafx.collections.FXCollections;
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
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

@Component
public class JavaFxUi {

    private final ExecutorFactory executorFactory;
    private final AiProviderFactory aiProviderFactory;
    private final AiConfig aiConfig;
    private final AppProperties appProperties;
    private final IoProperties ioProperties;

    public JavaFxUi(ExecutorFactory executorFactory,
                    AiProviderFactory aiProviderFactory,
                    AiConfig aiConfig,
                    AppProperties appProperties,
                    IoProperties ioProperties) {
        this.executorFactory = executorFactory;
        this.aiProviderFactory = aiProviderFactory;
        this.aiConfig = aiConfig;
        this.appProperties = appProperties;
        this.ioProperties = ioProperties;
    }

    public void start(Stage stage) {
        stage.setTitle("Modular AI Executor");

        VBox root = new VBox(20);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color: #0f172a;"); // Slate 900
        root.setPrefWidth(680);
        root.setPrefHeight(620);

        // Header
        Label titleLabel = new Label("AI Wrapper - Modular Executor");
        titleLabel.setTextFill(javafx.scene.paint.Color.web("#e2e8f0"));
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        Label subtitleLabel = new Label("Execute batch workflows using localized and cloud LLMs via Strategy Pattern");
        subtitleLabel.setTextFill(javafx.scene.paint.Color.web("#94a3b8"));
        subtitleLabel.setFont(Font.font("Segoe UI", 12));

        VBox header = new VBox(4, titleLabel, subtitleLabel);
        root.getChildren().add(header);

        // Form Container
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER_LEFT);

        ColumnConstraints col1 = new ColumnConstraints(140);
        ColumnConstraints col2 = new ColumnConstraints(370);
        ColumnConstraints col3 = new ColumnConstraints(100);
        grid.getColumnConstraints().addAll(col1, col2, col3);

        // 1. Input File
        Label inputLabel = createFormLabel("Input File:");
        TextField inputField = createTextField("Path to input file...");
        String defaultInput = ioProperties.getInputDir() != null ? ioProperties.getInputDir() + "/sample.txt" : "./data/input/sample.txt";
        inputField.setText(defaultInput);
        Button inputBrowse = createSecondaryButton("Browse");
        inputBrowse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Open Input File");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("TXT Files (*.txt)", "*.txt"),
                    new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"),
                    new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv")
            );
            File file = chooser.showOpenDialog(stage);
            if (file != null) {
                inputField.setText(file.getAbsolutePath());
            }
        });
        grid.add(inputLabel, 0, 0);
        grid.add(inputField, 1, 0);
        grid.add(inputBrowse, 2, 0);

        // 2. Output File
        Label outputLabel = createFormLabel("Output File:");
        TextField outputField = createTextField("Path to output file...");
        String defaultOutput = ioProperties.getOutputDir() != null ? ioProperties.getOutputDir() + "/result.json" : "./data/output/result.json";
        outputField.setText(defaultOutput);
        Button outputBrowse = createSecondaryButton("Browse");
        outputBrowse.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Output File");
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"),
                    new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"),
                    new FileChooser.ExtensionFilter("TXT Files (*.txt)", "*.txt")
            );
            File file = chooser.showSaveDialog(stage);
            if (file != null) {
                outputField.setText(file.getAbsolutePath());
            }
        });
        grid.add(outputLabel, 0, 1);
        grid.add(outputField, 1, 1);
        grid.add(outputBrowse, 2, 1);

        // 3. Module Selection
        Label moduleLabel = createFormLabel("Executor Module:");
        ComboBox<String> moduleSelect = new ComboBox<>(FXCollections.observableArrayList("translate"));
        moduleSelect.setValue(appProperties.getModule() != null ? appProperties.getModule() : "translate");
        styleDropdown(moduleSelect);
        grid.add(moduleLabel, 0, 2);
        grid.add(moduleSelect, 1, 2);

        // 4. Provider Selection
        Label providerLabel = createFormLabel("AI Provider:");
        ComboBox<String> providerSelect = new ComboBox<>(FXCollections.observableArrayList("ollama", "openai", "gemini", "googletranslate"));
        providerSelect.setValue(aiConfig.getProvider() != null ? aiConfig.getProvider() : "ollama");
        styleDropdown(providerSelect);
        grid.add(providerLabel, 0, 3);
        grid.add(providerSelect, 1, 3);

        // 5. Model Selection ComboBox (Editable)
        Label modelLabel = createFormLabel("Model:");
        ComboBox<String> modelSelect = new ComboBox<>();
        modelSelect.setEditable(true);
        modelSelect.setMaxWidth(Double.MAX_VALUE);
        modelSelect.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 8;");

        String defaultModel = "gemma2:2b";
        if ("openai".equals(aiConfig.getProvider())) {
            defaultModel = aiConfig.getOpenai().getModel();
            modelSelect.setItems(FXCollections.observableArrayList("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo"));
        } else if ("gemini".equals(aiConfig.getProvider())) {
            defaultModel = aiConfig.getGemini().getModel();
            modelSelect.setItems(FXCollections.observableArrayList("gemini-1.5-flash", "gemini-1.5-pro"));
        } else {
            fetchOllamaModels(modelSelect);
        }
        modelSelect.setValue(defaultModel);

        providerSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if ("openai".equalsIgnoreCase(newVal)) {
                    modelSelect.setItems(FXCollections.observableArrayList("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo"));
                    String def = aiConfig.getOpenai().getModel();
                    modelSelect.setValue(def != null && !def.isEmpty() ? def : "gpt-4o-mini");
                } else if ("gemini".equalsIgnoreCase(newVal)) {
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

        grid.add(modelLabel, 0, 4);
        grid.add(modelSelect, 1, 4);

        // 6. Temperature Option
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
            tempValueLabel.setText(String.format("%.1f", newVal));
        });
        HBox tempBox = new HBox(12, tempSlider, tempValueLabel);
        tempBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(tempLabel, 0, 5);
        grid.add(tempBox, 1, 5);

        root.getChildren().add(grid);

        // Divider
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #334155;");
        root.getChildren().add(sep);

        // Console log area
        TextArea console = new TextArea();
        console.setEditable(false);
        console.setWrapText(true);
        console.setPrefHeight(180);
        console.setStyle("-fx-control-inner-background: #0b0f19; -fx-text-fill: #10b981; -fx-font-family: Consolas, 'Courier New', monospace; -fx-font-size: 12;");
        console.appendText("System ready. Click Run to process.\n");

        // Action Button
        Button runButton = new Button("Run Pipeline");
        runButton.setStyle("-fx-background-color: linear-gradient(to right, #6366f1, #8b5cf6); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 32; -fx-cursor: hand; -fx-font-size: 14;");
        runButton.setMaxWidth(Double.MAX_VALUE);

        Label statusLabel = new Label("Status: Ready");
        statusLabel.setTextFill(javafx.scene.paint.Color.web("#94a3b8"));
        statusLabel.setFont(Font.font("Segoe UI", 12));

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

        runButton.setOnAction(e -> {
            runButton.setDisable(true);
            statusLabel.setText("Status: Executing...");
            console.clear();

            String module = moduleSelect.getValue();
            String provider = providerSelect.getValue();
            String model = modelSelect.getValue() != null ? modelSelect.getValue().trim() : "";
            double temperature = Math.round(tempSlider.getValue() * 10.0) / 10.0;
            String inputPath = inputField.getText().trim();
            String outputPath = outputField.getText().trim();

            console.appendText("Executing strategy: " + module + "\n");
            console.appendText("Provider: " + provider + "\n");
            console.appendText("Model: " + (model.isEmpty() ? "(default)" : model) + "\n");
            console.appendText("Temperature: " + temperature + "\n");

            // Update Spring configs dynamically
            appProperties.setModule(module);
            aiConfig.setProvider(provider);

            // Build dynamic run options
            java.util.Map<String, Object> runOptions = new java.util.HashMap<>();
            runOptions.put("provider", provider);
            if (!model.isEmpty()) {
                runOptions.put("model", model);
            }
            runOptions.put("temperature", temperature);

            Task<Void> executionTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    BaseExecutor executor = executorFactory.get(module);
                    executor.execute(inputPath, outputPath, runOptions);
                    return null;
                }
            };

            executionTask.setOnSucceeded(event -> {
                runButton.setDisable(false);
                statusLabel.setText("Status: Completed successfully!");
                console.appendText("\nExecution Completed. Output file saved.\n");
            });

            executionTask.setOnFailed(event -> {
                runButton.setDisable(false);
                statusLabel.setText("Status: Failed!");
                Throwable ex = executionTask.getException();
                console.appendText("\n[ERROR] " + (ex != null ? ex.getMessage() : "Unknown execution error") + "\n");
                if (ex != null) {
                    ex.printStackTrace(oldOut);
                }
            });

            new Thread(executionTask).start();
        });

        HBox footer = new HBox(20, statusLabel, runButton);
        footer.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(statusLabel, Priority.ALWAYS);
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        root.getChildren().addAll(console, footer);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
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
        tf.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-background-radius: 6; -fx-padding: 8 12;");
        return tf;
    }

    private Button createSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #475569; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"));
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
                @SuppressWarnings("unchecked")
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
}
