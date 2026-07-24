package com.aiwrapper.javafx.ui;

import com.aiwrapper.executor.TranslateExecutor;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.File;
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
        lblZoneCTitle = new Label("TRANSLATION MONITOR & CACHE");
        lblZoneCTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblZoneCTitle.setTextFill(javafx.scene.paint.Color.web("#94a3b8"));

        // Add Row Form
        HBox addRowBox = new HBox(8);
        addRowBox.setAlignment(Pos.CENTER_LEFT);
        addRowBox.setPadding(new Insets(6, 0, 6, 0));
        addRowBox.setManaged(false);
        addRowBox.setVisible(false);

        TextField addOriginField = UiStyles.createTextField("English (Văn bản gốc)...");
        addOriginField.setPrefWidth(200);
        HBox.setHgrow(addOriginField, Priority.ALWAYS);

        TextField addTransField = UiStyles.createTextField("Vietnamese (Bản dịch)...");
        addTransField.setPrefWidth(200);
        HBox.setHgrow(addTransField, Priority.ALWAYS);

        buildAddRowForm(addOriginField, addTransField, addRowBox);

        // Search and Actions
        TextField searchField = UiStyles.createTextField("Nhập từ khóa cần tìm (Bản gốc hoặc bản dịch)...");
        searchField.setPrefWidth(300);
        Node searchBox = buildSearchAndActionsBox(searchField, addRowBox, addOriginField);

        // detailBox creation (required before TableView initialization since TableView
        // references txtTranslated)
        Node detailContainer = buildDetailEditorBox();

        // Build TableView via builder
        tableView = CacheTableBuilder.build(
                historyList,
                searchField,
                translateExecutor,
                onRowChanged,
                txtTranslated,
                () -> currentItem);

        // Selection Listener
        setupSelectionListener();

        getChildren().addAll(lblZoneCTitle, searchBox, addRowBox, tableView, detailContainer);
    }

    private void buildAddRowForm(TextField addOriginField, TextField addTransField, HBox addRowBox) {
        Button btnCommitAdd = UiStyles.createSecondaryButton("Lưu");
        btnCommitAdd.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: black; -fx-font-weight: bold;");

        Button btnCancelAdd = UiStyles.createSecondaryButton("Hủy");

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
    }

    private Node buildSearchAndActionsBox(TextField searchField, HBox addRowBox, TextField addOriginField) {
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        Label searchLabel = UiStyles.createFormLabel("Tìm kiếm:");

        StackPane searchFieldOverlay = new StackPane();
        HBox.setHgrow(searchFieldOverlay, Priority.ALWAYS);

        Button btnClearSearch = new Button("×");
        btnClearSearch.setCursor(javafx.scene.Cursor.HAND);
        btnClearSearch.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0 10 0 0; -fx-border-width: 0;");
        StackPane.setAlignment(btnClearSearch, Pos.CENTER_RIGHT);
        btnClearSearch.visibleProperty().bind(searchField.textProperty().isNotEmpty());
        btnClearSearch.setOnAction(e -> searchField.clear());

        searchFieldOverlay.getChildren().addAll(searchField, btnClearSearch);

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
        btnSync.setOnAction(e -> CacheImportExportHandler.handleSync(
                stage, translateExecutor, gamePathSupplier, onRowChanged, onCacheFileChange, historyList));

        Button btnCache = new Button("Cache");
        btnCache.setCursor(javafx.scene.Cursor.HAND);
        btnCache.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: #f8fafc; -fx-background-radius: 6; -fx-padding: 6 12; -fx-font-family: 'Segoe UI'; -fx-border-color: #334155; -fx-border-width: 1; -fx-border-radius: 6; -fx-font-weight: bold;");

        ContextMenu cacheMenu = new ContextMenu();
        MenuItem optLoadCache = new MenuItem("Chọn Cache...");
        optLoadCache.setOnAction(e -> CacheImportExportHandler.loadCache(
                stage, translateExecutor, historyList, this::updateActiveCacheLabel, onCacheFileChange));

        MenuItem optClearCache = new MenuItem("Xóa Cache Game");
        optClearCache.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
        optClearCache.setOnAction(e -> CacheImportExportHandler.clearCache(
                stage, gamePathSupplier, translateExecutor, historyList, this::updateActiveCacheLabel,
                onCacheFileWiped));

        cacheMenu.getItems().addAll(optLoadCache, optClearCache);
        btnCache.setOnAction(e -> {
            if (cacheMenu.isShowing()) {
                cacheMenu.hide();
            } else {
                cacheMenu.show(btnCache, javafx.geometry.Side.BOTTOM, btnCache.getWidth() - 170, 0);
            }
        });

        searchBox.getChildren().addAll(searchLabel, searchFieldOverlay, btnAddRow, btnSync, btnCache);
        return searchBox;
    }

    private Node buildDetailEditorBox() {
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
        return detailBox;
    }

    private void setupSelectionListener() {
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
    }

    public void updateActiveCacheLabel() {
        if (lblZoneCTitle == null)
            return;
        File active = translateExecutor.getActiveCacheFile();
        String activeName = (active != null) ? active.getName() : "none";
        if (!isAtBottom) {
            lblZoneCTitle.setText("TRANSLATION MONITOR & CACHE (" + activeName
                    + ") [⏸ Đang xem cache cũ - cuộn xuống cuối để tiếp tục theo dõi]");
        } else {
            lblZoneCTitle.setText("TRANSLATION MONITOR & CACHE (" + activeName + ")");
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
                            lblWarning.setText("Bản dịch gốc vừa được server cập nhật ở nền: " + newStr);
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
