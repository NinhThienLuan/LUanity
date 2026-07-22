package com.aiwrapper.javafx.ui;

import com.aiwrapper.executor.TranslateExecutor;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class TranslationCacheCard extends VBox {

    private final Stage stage;
    private final TranslateExecutor translateExecutor;
    private final ObservableList<TranslationItem> historyList;

    private Label lblZoneCTitle;
    private TableView<TranslationItem> tableView;
    private ScrollBar verticalScrollBar;
    private boolean isAtBottom = true;

    private Runnable onRowChanged;
    private Runnable onCacheFileChange;
    private Runnable onCacheFileWiped;
    private Supplier<String> gamePathSupplier;

    public TranslationCacheCard(Stage stage, TranslateExecutor translateExecutor,
            ObservableList<TranslationItem> historyList) {
        super(10);
        this.stage = stage;
        this.translateExecutor = translateExecutor;
        this.historyList = historyList;

        initLayout();
    }

    public void setOnRowChanged(Runnable onRowChanged) {
        this.onRowChanged = onRowChanged;
    }

    public void setOnCacheFileChange(Runnable onCacheFileChange) {
        this.onCacheFileChange = onCacheFileChange;
    }

    public void setOnCacheFileWiped(Runnable onCacheFileWiped) {
        this.onCacheFileWiped = onCacheFileWiped;
    }

    public void setGamePathSupplier(Supplier<String> gamePathSupplier) {
        this.gamePathSupplier = gamePathSupplier;
    }

    public TableView<TranslationItem> getTableView() {
        return tableView;
    }

    private void initLayout() {
        HBox.setHgrow(this, Priority.ALWAYS);

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
                TranslationItem newItem = new TranslationItem("Cache", orig, tran);
                translateExecutor.updateCacheValue(orig, tran);
                historyList.add(newItem);
                addOriginField.clear();
                addTransField.clear();
                addRowBox.setVisible(false);
                addRowBox.setManaged(false);
                System.out.println("Added cache row: " + orig + " -> " + tran);
                if (onRowChanged != null) {
                    onRowChanged.run();
                }
                javafx.application.Platform.runLater(() -> {
                    if (tableView != null) {
                        tableView.requestFocus();
                        tableView.getSelectionModel().select(newItem);
                        tableView.scrollTo(newItem);
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

        Runnable showPathWarning = () -> {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Vui lòng chọn đường dẫn game Game Exe trước!");
            alert.showAndWait();
        };

        Button btnSync = new Button("↻");
        btnSync.setCursor(javafx.scene.Cursor.HAND);
        btnSync.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: #06b6d4; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-family: 'Segoe UI'; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 6; -fx-font-weight: bold; -fx-font-size: 14px;");
        btnSync.setTooltip(new Tooltip("Sync"));
        btnSync.setOnAction(e -> {
            String path = (gamePathSupplier != null) ? gamePathSupplier.get() : "";
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
                File cssFile = new File("data/ui_style.css");
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
                    File cssFile = new File("data/ui_style.css");
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
                File cssFile = new File("data/ui_style.css");
                if (cssFile.exists()) {
                    confirm.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                }
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    if (transFile.getParentFile() != null && !transFile.getParentFile().exists()) {
                        transFile.getParentFile().mkdirs();
                    }

                    try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                            new java.io.OutputStreamWriter(new FileOutputStream(transFile),
                                    StandardCharsets.UTF_8))) {
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
                File cssFile = new File("data/ui_style.css");
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
            chooser.setInitialDirectory(new File("data"));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON (*.json)", "*.json"));
            File file = chooser.showOpenDialog(stage);
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
                    if (onCacheFileChange != null) {
                        onCacheFileChange.run();
                    }
                    System.out.println("Loaded " + cMap.size() + " translation mappings from cache: " + file.getName());
                } catch (Exception ex) {
                    System.err.println("Failed to read cache file: " + ex.getMessage());
                }
            }
        });

        MenuItem optClearCache = new MenuItem("Xóa Cache Game");
        optClearCache.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        optClearCache.setOnAction(e -> {
            String path = (gamePathSupplier != null) ? gamePathSupplier.get() : "";
            if (path.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Warning");
                alert.setHeaderText("Game Executable Key Missing");
                alert.setContentText("Please select the Game Exe Path in the settings panel first.");
                File cssFile = new File("data/ui_style.css");
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
            File cssFile = new File("data/ui_style.css");
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
                    File gameCache = new File("data/cache_" + gameName + ".json");
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
                    if (onCacheFileWiped != null) {
                        onCacheFileWiped.run();
                    }
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

        getChildren().addAll(lblZoneCTitle, searchBox, addRowBox, tableView);
    }

    public void updateActiveCacheLabel() {
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

    public void initScrollTracking() {
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

    private ScrollBar findVerticalScrollBar(javafx.scene.Node node) {
        if (node instanceof ScrollBar) {
            ScrollBar sb = (ScrollBar) node;
            if (sb.getOrientation() == javafx.geometry.Orientation.VERTICAL) {
                return sb;
            }
        }
        if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                ScrollBar sb = findVerticalScrollBar(child);
                if (sb != null) {
                    return sb;
                }
            }
        }
        return null;
    }

    public void handleNewTranslationAdded() {
        if (isAtBottom && tableView != null) {
            tableView.scrollTo(historyList.size() - 1);
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
