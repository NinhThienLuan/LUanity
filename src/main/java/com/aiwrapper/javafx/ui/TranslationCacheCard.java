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

    private TranslationItem currentItem = null;
    private javafx.beans.value.ChangeListener<String> activeTranslationListener = null;
    private boolean isChangingSelection = false;
    private VBox detailBox;
    private TextArea txtOriginal;
    private TextArea txtTranslated;
    private Label lblWarning;

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

        MenuItem mntReTranslate = new MenuItem("Dịch lại");
        mntReTranslate.setOnAction(evt -> {
            TranslationItem selected = tableView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String original = selected.getOriginal();
                selected.setType("Dịch...");
                selected.setTranslated("Đang dịch lại...");
                if (currentItem == selected) {
                    txtTranslated.setText("Đang dịch lại...");
                }
                translateExecutor.translateSingleAsync(original, Map.of("bypassCache", true))
                        .thenAccept(newTranslation -> {
                            javafx.application.Platform.runLater(() -> {
                                selected.setType("AI");
                                selected.setTranslated(newTranslation);
                                if (currentItem == selected) {
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
                                if (currentItem == selected) {
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

        // Detail Box UI construction
        detailBox = new VBox(8);
        detailBox.setPadding(new Insets(10, 0, 10, 0));
        detailBox.setVisible(false);
        detailBox.setManaged(false);

        Label lblDetailTitle = new Label("CHI TIẾT VĂN BẢN ĐÃ CHỌN");
        lblDetailTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblDetailTitle.setTextFill(javafx.scene.paint.Color.web("#94a3b8"));

        HBox textAreasBox = new HBox(15);
        VBox.setVgrow(textAreasBox, Priority.ALWAYS);

        VBox origBox = new VBox(5);
        HBox.setHgrow(origBox, Priority.ALWAYS);
        Label lblOrig = new Label("Bản gốc (Không thể chỉnh sửa):");
        lblOrig.setTextFill(javafx.scene.paint.Color.web("#cbd5e1"));
        lblOrig.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        txtOriginal = new TextArea();
        txtOriginal.setEditable(false);
        txtOriginal.setWrapText(true);
        txtOriginal.setPrefHeight(75);
        txtOriginal.setPromptText("Bản gốc...");
        txtOriginal.setStyle(
                "-fx-control-inner-background: #1e293b; -fx-text-fill: #94a3b8; -fx-background-radius: 6; -fx-padding: 2;");
        origBox.getChildren().addAll(lblOrig, txtOriginal);

        VBox transBox = new VBox(5);
        HBox.setHgrow(transBox, Priority.ALWAYS);
        Label lblTrans = new Label("Bản dịch (Có thể sửa trực tiếp):");
        lblTrans.setTextFill(javafx.scene.paint.Color.web("#cbd5e1"));
        lblTrans.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        txtTranslated = new TextArea();
        txtTranslated.setEditable(true);
        txtTranslated.setWrapText(true);
        txtTranslated.setPrefHeight(75);
        txtTranslated.setPromptText("Bản dịch...");
        txtTranslated.setStyle(
                "-fx-control-inner-background: #1e293b; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 2;");
        txtTranslated.setOnKeyPressed(evt -> {
            if (evt.getCode() == javafx.scene.input.KeyCode.ENTER) {
                if (evt.isShiftDown()) {
                    txtTranslated.appendText("\n");
                } else {
                    evt.consume(); // prevent inserting newline
                    commitDetailTranslation();
                }
            }
        });
        transBox.getChildren().addAll(lblTrans, txtTranslated);

        textAreasBox.getChildren().addAll(origBox, transBox);

        lblWarning = new Label();
        lblWarning.setTextFill(javafx.scene.paint.Color.web("#ef4444"));
        lblWarning.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblWarning.setWrapText(true);
        lblWarning.setVisible(false);
        lblWarning.setManaged(false);

        detailBox.getChildren().addAll(lblDetailTitle, textAreasBox, lblWarning);

        // Selection Property Listener with deselect dirty confirmation guards
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (isChangingSelection)
                return;

            if (newVal != currentItem) {
                if (currentItem != null && isTranslatedDirty()) {
                    isChangingSelection = true;
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Thay đổi chưa lưu");
                    confirm.setHeaderText("Bạn đã chỉnh sửa bản dịch nhưng chưa lưu.");
                    confirm.setContentText("Bạn có muốn lưu các thay đổi lại trước khi chọn dòng mới không?");

                    ButtonType btnYes = new ButtonType("Có", ButtonBar.ButtonData.YES);
                    ButtonType btnNo = new ButtonType("Không", ButtonBar.ButtonData.NO);
                    ButtonType btnCancel = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
                    confirm.getButtonTypes().setAll(btnYes, btnNo, btnCancel);

                    File cssFile = new File("data/ui_style.css");
                    if (cssFile.exists()) {
                        confirm.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
                    }

                    Optional<ButtonType> result = confirm.showAndWait();
                    if (result.isPresent() && result.get() == btnYes) {
                        if (commitDetailTranslation()) {
                            switchToNewRow(newVal);
                        } else {
                            javafx.application.Platform.runLater(() -> {
                                tableView.getSelectionModel().select(currentItem);
                                isChangingSelection = false;
                            });
                            return;
                        }
                    } else if (result.isPresent() && result.get() == btnNo) {
                        switchToNewRow(newVal);
                    } else {
                        javafx.application.Platform.runLater(() -> {
                            tableView.getSelectionModel().select(currentItem);
                            isChangingSelection = false;
                        });
                        return;
                    }
                    isChangingSelection = false;
                } else {
                    switchToNewRow(newVal);
                }
            }
        });

        getChildren().addAll(lblZoneCTitle, searchBox, addRowBox, tableView, detailBox);
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

    private boolean isTranslatedDirty() {
        if (currentItem == null)
            return false;
        String cur = txtTranslated.getText();
        String originalVal = currentItem.getTranslated();
        return cur != null && !cur.equals(originalVal);
    }

    private void switchToNewRow(TranslationItem newItem) {
        // Unbind previous listener
        if (currentItem != null && activeTranslationListener != null) {
            currentItem.translatedProperty().removeListener(activeTranslationListener);
        }

        currentItem = newItem;
        lblWarning.setVisible(false);
        lblWarning.setManaged(false);

        if (newItem != null) {
            txtOriginal.setText(newItem.getOriginal());
            txtTranslated.setText(newItem.getTranslated());
            detailBox.setVisible(true);
            detailBox.setManaged(true);

            // Bind listener to listen for background updates
            activeTranslationListener = (obsStr, oldStr, newStr) -> {
                if (newStr != null) {
                    javafx.application.Platform.runLater(() -> {
                        if (!txtTranslated.isFocused() || !isTranslatedDirty()) {
                            txtTranslated.setText(newStr);
                        } else {
                            lblWarning.setText("⚠️ Bản dịch gốc vừa được server cập nhật ở nền: " + newStr);
                            lblWarning.setVisible(true);
                            lblWarning.setManaged(true);
                        }
                    });
                }
            };
            newItem.translatedProperty().addListener(activeTranslationListener);
        } else {
            txtOriginal.clear();
            txtTranslated.clear();
            detailBox.setVisible(false);
            detailBox.setManaged(false);
        }
    }

    private boolean commitDetailTranslation() {
        if (currentItem == null)
            return false;
        String newTrans = txtTranslated.getText().trim();
        if (newTrans.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Bản dịch không được để trống!");
            File cssFile = new File("data/ui_style.css");
            if (cssFile.exists()) {
                alert.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
            }
            alert.showAndWait();
            return false;
        }
        currentItem.setTranslated(newTrans);
        translateExecutor.updateCacheValue(currentItem.getOriginal(), newTrans);
        lblWarning.setVisible(false);
        lblWarning.setManaged(false);
        if (onRowChanged != null) {
            onRowChanged.run();
        }
        return true;
    }
}
