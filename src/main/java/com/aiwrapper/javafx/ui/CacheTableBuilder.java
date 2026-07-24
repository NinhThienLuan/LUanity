package com.aiwrapper.javafx.ui;

import com.aiwrapper.executor.TranslateExecutor;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;

public class CacheTableBuilder {

    public static TableView<TranslationItem> build(
            ObservableList<TranslationItem> historyList,
            TextField searchField,
            TranslateExecutor translateExecutor,
            Runnable onRowChanged,
            TextArea txtTranslated,
            java.util.function.Supplier<TranslationItem> currentItemSupplier) {
        TableView<TranslationItem> tableView = new TableView<>();
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
        typeCol.setStyle("-fx-alignment: CENTER; -fx-text-fill: #06b6d4;");

        TableColumn<TranslationItem, String> originCol = new TableColumn<>("Văn bản gốc");
        originCol.setCellValueFactory(new PropertyValueFactory<>("original"));
        originCol.setPrefWidth(280);
        originCol.setStyle("-fx-text-fill: #e2e8f0;");

        TableColumn<TranslationItem, String> transCol = new TableColumn<>("Bản dịch Việt");
        transCol.setCellValueFactory(new PropertyValueFactory<>("translated"));
        transCol.setPrefWidth(280);
        transCol.setStyle("-fx-text-fill: #06b6d4;");
        transCol.setCellFactory(TextFieldTableCell.forTableColumn());
        transCol.setOnEditCommit(event -> {
            TranslationItem item = event.getRowValue();
            String newTranslation = event.getNewValue();
            if (newTranslation != null && !newTranslation.equals(event.getOldValue())) {
                item.setTranslated(newTranslation);
                translateExecutor.updateCacheValue(item.getOriginal(), newTranslation);
                if (onRowChanged != null) {
                    onRowChanged.run();
                }
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

        MenuItem mntReTranslate = new MenuItem("Dịch lại");
        mntReTranslate.setOnAction(evt -> {
            TranslationItem selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String original = selected.getOriginal();
                selected.setType("Dịch...");
                selected.setTranslated("Đang dịch lại...");
                if (currentItemSupplier.get() == selected && txtTranslated != null) {
                    txtTranslated.setText("Đang dịch lại...");
                }
                translateExecutor.translateSingleAsync(original, Map.of("bypassCache", true))
                        .thenAccept(newTranslation -> {
                            javafx.application.Platform.runLater(() -> {
                                selected.setType("AI");
                                selected.setTranslated(newTranslation);
                                if (currentItemSupplier.get() == selected && txtTranslated != null) {
                                    txtTranslated.setText(newTranslation);
                                }
                                if (onRowChanged != null) {
                                    onRowChanged.run();
                                }
                            });
                        }).exceptionally(ex -> {
                            javafx.application.Platform.runLater(() -> {
                                selected.setType("AI_Error");
                                selected.setTranslated("[LỖI] " + ex.getMessage());
                                if (currentItemSupplier.get() == selected && txtTranslated != null) {
                                    txtTranslated.setText("[LỖI] " + ex.getMessage());
                                }
                            });
                            return null;
                        });
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
                if (onRowChanged != null) {
                    onRowChanged.run();
                }
            }
        });
        rowMenu.getItems().addAll(mntPin, mntReTranslate, mntDelete);

        tableView.setRowFactory(tv -> {
            TableRow<TranslationItem> row = new TableRow<>();
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty())
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

        return tableView;
    }
}
