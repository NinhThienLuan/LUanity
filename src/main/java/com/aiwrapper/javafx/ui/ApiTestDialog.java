package com.aiwrapper.javafx.ui;

import com.aiwrapper.config.AiConfig;
import com.aiwrapper.executor.TranslateExecutor;
import com.aiwrapper.provider.AiProviderFactory;
import com.aiwrapper.provider.AiProvider;
import com.aiwrapper.template.PromptTemplate;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ApiTestDialog {

    public static void show(Stage owner, AiConfig aiConfig, AiProviderFactory aiFactory,
            TranslateExecutor translateExecutor) {
        Stage testStage = new Stage();
        testStage.setTitle("Test API Translation Provider");
        testStage.initOwner(owner);

        VBox testRoot = new VBox();
        testRoot.setStyle("-fx-background-color: #0f172a;");
        testRoot.getChildren().add(createApiTestLayout(aiConfig, aiFactory, translateExecutor));

        Scene testScene = new Scene(testRoot, 1000, 520);
        testStage.setScene(testScene);
        testStage.show();
    }

    private static javafx.scene.Node createApiTestLayout(AiConfig aiConfig, AiProviderFactory aiFactory,
            TranslateExecutor translateExecutor) {
        HBox body = new HBox(20);
        body.setPadding(new Insets(16));
        body.setStyle("-fx-background-color: #0f172a;");

        VBox leftPane = new VBox(14);
        leftPane.setMinWidth(460);
        leftPane.setMaxWidth(460);

        Label inputsTitle = new Label("CẤU HÌNH & DỮ LIỆU ĐẦU VÀO");
        inputsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        inputsTitle.setTextFill(javafx.scene.paint.Color.web("#06b6d4"));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER_LEFT);

        ColumnConstraints col1 = new ColumnConstraints(120);
        ColumnConstraints col2 = new ColumnConstraints(280);
        grid.getColumnConstraints().addAll(col1, col2);

        Label provLabel = createFormLabel("AI Provider:");
        ComboBox<String> provSelect = new ComboBox<>(
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
        provSelect.setValue(matchedVal);
        styleDropdown(provSelect);
        grid.add(provLabel, 0, 0);
        grid.add(provSelect, 1, 0);

        Label modelLabel = createFormLabel("Model:");
        ComboBox<String> modelSelect = new ComboBox<>();
        modelSelect.setEditable(true);
        modelSelect.setMaxWidth(Double.MAX_VALUE);
        modelSelect.setStyle(
                "-fx-background-color: #0b0f19; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 8; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 6;");
        modelSelect.setItems(FXCollections.observableArrayList("gemma2:2b", "gemini-1.5-flash", "gpt-4o-mini"));
        modelSelect.setValue("gemma2:2b");
        grid.add(modelLabel, 0, 1);
        grid.add(modelSelect, 1, 1);

        provSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                if ("gemini".equalsIgnoreCase(newVal)) {
                    modelSelect.setItems(FXCollections.observableArrayList("gemini-1.5-flash", "gemini-1.5-pro"));
                    modelSelect.setValue("gemini-1.5-flash");
                } else if ("openapi".equalsIgnoreCase(newVal) || "openai".equalsIgnoreCase(newVal)) {
                    modelSelect.setItems(FXCollections.observableArrayList("gpt-4o-mini", "gpt-4o", "gpt-3.5-turbo"));
                    modelSelect.setValue("gpt-4o-mini");
                } else if ("googletranslate".equalsIgnoreCase(newVal)) {
                    modelSelect.setItems(FXCollections.observableArrayList("default"));
                    modelSelect.setValue("default");
                } else {
                    modelSelect.setItems(FXCollections.observableArrayList("gemma2:2b", "qwen2.5:3b"));
                    modelSelect.setValue("gemma2:2b");
                }
            }
        });

        CheckBox useCacheBox = new CheckBox("Sử dụng Cache & Glossary");
        useCacheBox.setSelected(true);
        useCacheBox.setStyle("-fx-text-fill: #e2e8f0; -fx-font-family: 'Segoe UI';");

        CheckBox useTemplateBox = new CheckBox("Sử dụng Prompt Template");
        useTemplateBox.setSelected(true);
        useTemplateBox.setStyle("-fx-text-fill: #e2e8f0; -fx-font-family: 'Segoe UI';");

        VBox optionsBox = new VBox(8, useCacheBox, useTemplateBox);

        grid.add(createFormLabel("Logic Options:"), 0, 2);
        grid.add(optionsBox, 1, 2);

        Label srcLabel = createFormLabel("Văn bản cần dịch:");
        TextArea srcArea = new TextArea();
        srcArea.setPromptText("Nhập văn bản tiếng Anh cần dịch thử tại đây...");
        srcArea.setStyle("-fx-control-inner-background: #0b0f19; -fx-text-fill: white; -fx-prompt-text-fill: #64748b;");
        srcArea.setPrefHeight(150);
        srcArea.setWrapText(true);

        VBox srcContainer = new VBox(6, srcLabel, srcArea);

        Button btnTest = new Button("Dịch Thử (Test Translate)");
        btnTest.setStyle(
                "-fx-background-color: #06b6d4; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 10 20; -fx-cursor: hand;");
        btnTest.setMaxWidth(Double.MAX_VALUE);

        btnTest.setOnMouseEntered(e -> btnTest.setStyle(
                "-fx-background-color: #0891b2; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 10 20; -fx-cursor: hand;"));
        btnTest.setOnMouseExited(e -> btnTest.setStyle(
                "-fx-background-color: #06b6d4; -fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-background-radius: 6; -fx-padding: 10 20; -fx-cursor: hand;"));

        leftPane.getChildren().addAll(inputsTitle, grid, srcContainer, btnTest);

        VBox rightPane = new VBox(14);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        Label logsTitle = new Label("PHẢN HỒI & QUÁ TRÌNH THỰC THI (LOGS TRACE)");
        logsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        logsTitle.setTextFill(javafx.scene.paint.Color.web("#06b6d4"));

        Label outLabel = createFormLabel("Kết quả dịch:");
        TextArea outArea = new TextArea();
        outArea.setEditable(false);
        outArea.setPromptText("Kết quả dịch sẽ xuất hiện ở đây...");
        outArea.setPrefHeight(150);
        outArea.setWrapText(true);
        outArea.setStyle("-fx-control-inner-background: #0b0f19; -fx-text-fill: #10b981; -fx-font-weight: bold;");

        Label traceLabel = createFormLabel("Trace Execution Log:");
        TextArea traceArea = new TextArea();
        traceArea.setEditable(false);
        traceArea.setPromptText("Chưa có logs. Nhấn 'Dịch Thử' để xem chi tiết...");
        traceArea.setPrefHeight(250);
        traceArea.setStyle(
                "-fx-control-inner-background: #090d16; -fx-text-fill: #94a3b8; -fx-font-family: 'Consolas', monospace;");

        rightPane.getChildren().addAll(logsTitle, outLabel, outArea, traceLabel, traceArea);

        btnTest.setOnAction(e -> {
            String text = srcArea.getText();
            if (text == null || text.trim().isEmpty()) {
                outArea.setText("[LỖI] Chưa nhập văn bản.");
                return;
            }

            traceArea.setText("");
            outArea.setText("Đang xử lý...");
            btnTest.setDisable(true);

            Thread thread = new Thread(() -> {
                long startTime = System.currentTimeMillis();
                updateTrace(traceArea, "Bắt đầu dịch thử: \"" + text + "\"");

                try {
                    Map<String, Object> testOptions = new HashMap<>();
                    testOptions.put("temperature", 0.2);
                    testOptions.put("model", modelSelect.getValue());
                    testOptions.put("promptTemplate", useTemplateBox.isSelected() ? "" : "{text}");
                    testOptions.put("skipCacheWrite", true);

                    String originalProvider = aiConfig.getProvider();
                    String selectedProv = provSelect.getValue().toLowerCase();
                    if ("openai".equalsIgnoreCase(selectedProv)) {
                        selectedProv = "openapi";
                    }
                    aiConfig.setProvider(selectedProv);
                    updateTrace(traceArea,
                            "Đã chọn provider: " + selectedProv + " (Model: " + modelSelect.getValue() + ")");

                    String resultText;
                    if (useCacheBox.isSelected()) {
                        updateTrace(traceArea, "Đang gửi yêu cầu thông qua TranslateExecutor (Có cache & glossary)...");
                        resultText = translateExecutor.translateSingle(text, testOptions);
                    } else {
                        updateTrace(traceArea,
                                "Đang gửi yêu cầu trực tiếp qua Provider complete (Bỏ qua cache & glossary)...");
                        AiProvider provider = aiFactory.get();
                        String prompt = text;
                        if (useTemplateBox.isSelected()) {
                            prompt = new PromptTemplate(translateExecutor.getPromptTemplate())
                                    .render(Map.of("text", text));
                        }
                        resultText = provider.complete(prompt, testOptions);
                    }

                    long elapsed = System.currentTimeMillis() - startTime;
                    aiConfig.setProvider(originalProvider);

                    Platform.runLater(() -> {
                        updateTrace(traceArea, "Hoàn tất trong " + elapsed + "ms.");
                        outArea.setText(resultText);
                        btnTest.setDisable(false);
                    });

                } catch (Exception ex) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    Platform.runLater(() -> {
                        updateTrace(traceArea, "Thất bại sau " + elapsed + "ms. Lỗi: " + ex.getMessage());
                        outArea.setText("[ERROR] " + ex.getMessage());
                        btnTest.setDisable(false);
                    });
                }
            });
            thread.setDaemon(true);
            thread.start();
        });

        body.getChildren().addAll(leftPane, rightPane);
        return body;
    }

    private static Label createFormLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #94a3b8; -fx-font-family: 'Segoe UI';");
        return label;
    }

    private static void styleDropdown(ComboBox<String> cb) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-background-radius: 6; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 6;");
    }

    private static void updateTrace(TextArea traceArea, String line) {
        Platform.runLater(() -> {
            String time = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
            traceArea.appendText("[" + time + "] " + line + "\n");
        });
    }
}
