package com.aiwrapper.javafx.ui;

import com.aiwrapper.executor.TranslateExecutor;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Standalone dialog for editing the glossary key-value pairs.
 * Extracted from ActionsZoneCard to reduce file size.
 */
public class GlossaryDialog {

    private GlossaryDialog() {
    }

    public static void show(TranslateExecutor translateExecutor) {
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

        ObservableList<GlossaryRow> data = FXCollections.observableArrayList();
        Map<String, String> glossaryMap = translateExecutor.loadGlossaryMap();
        for (Map.Entry<String, String> entry : glossaryMap.entrySet()) {
            data.add(new GlossaryRow(entry.getKey(), entry.getValue()));
        }
        tableView.setItems(data);

        Button btnAdd = UiStyles.createSecondaryButton("+ Thêm dòng");
        btnAdd.setOnAction(evt -> {
            GlossaryRow newRow = new GlossaryRow("NewWord", "Nghĩa");
            data.add(newRow);
            tableView.getSelectionModel().select(newRow);
            tableView.scrollTo(newRow);
        });

        Button btnDelete = UiStyles.createSecondaryButton("Xóa dòng");
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
                Map<String, String> newMap = new LinkedHashMap<>();
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

    // ---- Inner POJO ----

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
}
