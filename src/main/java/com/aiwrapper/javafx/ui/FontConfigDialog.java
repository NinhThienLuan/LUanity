package com.aiwrapper.javafx.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Standalone dialog and helper functions for configuring Vietnamese fonts.
 * Extracted from ActionsZoneCard to reduce target file size.
 */
public class FontConfigDialog {

    private FontConfigDialog() {
    }

    public static void show(Supplier<String> gamePathSupplier) {
        String path = gamePathSupplier.get();
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
        Collections.sort(systemFonts);
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

        Button presetArialUniBtn = UiStyles.createSecondaryButton("Preset Arial Unicode MS");
        presetArialUniBtn.setTooltip(new Tooltip("Điền cấu hình Arial Unicode hệ thống (Không chép file)"));
        presetArialUniBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(presetArialUniBtn, Priority.ALWAYS);
        presetArialUniBtn.setOnAction(e -> {
            overrideFontTf.setText("Arial");
            fallbackTMPTf.setText("arialuni_sdf_u2018");
            overrideTMPTf.setText("");
            restartWarningLabel.setVisible(true);
        });

        Button btnDownloadFromNet = UiStyles.createSecondaryButton("Tải thêm từ mạng (Internet)");
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
                Map<String, String> res = new HashMap<>();
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

    private static Map<String, String> readFontConfig(File iniFile) {
        Map<String, String> settings = new HashMap<>();
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
                    settings.put("OverrideFont", trimmed.substring("OverrideFont=".length()));
                } else if (trimmed.startsWith("OverrideFontTextMeshPro=")) {
                    settings.put("OverrideFontTextMeshPro", trimmed.substring("OverrideFontTextMeshPro=".length()));
                } else if (trimmed.startsWith("FallbackFontTextMeshPro=")) {
                    settings.put("FallbackFontTextMeshPro", trimmed.substring("FallbackFontTextMeshPro=".length()));
                }
            }
        } catch (Exception ex) {
            System.err.println("Failed to read INI file: " + ex.getMessage());
        }
        return settings;
    }

    private static void writeFontConfig(File iniFile, Map<String, String> fontSettings) {
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

    private static void downloadFontAssets(File gameDir, Button downloadBtn, File cssFile) {
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
                    Platform.runLater(() -> {
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
                Platform.runLater(() -> {
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
}
