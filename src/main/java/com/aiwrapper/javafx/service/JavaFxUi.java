package com.aiwrapper.javafx.service;

import com.aiwrapper.config.AiConfig;
import com.aiwrapper.executor.TranslateExecutor;
import com.aiwrapper.javafx.ui.*;
import com.aiwrapper.provider.AiProviderFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JavaFxUi {

    private final AiConfig aiConfig;
    private final TranslateExecutor translateExecutor;
    private final AiProviderFactory aiFactory;

    private int totalRequests = 0;
    private int totalChars = 0;
    private int cacheHits = 0;
    private int aiHits = 0;

    private final ObservableList<TranslationItem> historyList = FXCollections.observableArrayList();

    @Value("${local.server.port:8080}")
    private int localServerPort;

    public JavaFxUi(AiConfig aiConfig, TranslateExecutor translateExecutor, AiProviderFactory aiFactory) {
        this.aiConfig = aiConfig;
        this.translateExecutor = translateExecutor;
        this.aiFactory = aiFactory;
    }

    public void start(Stage stage) {
        stage.setTitle("LUanity Translator - Game translation Bridge");

        // Root Container
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #0f172a;"); // Slate 900
        root.setPrefWidth(1200);
        root.setPrefHeight(750);

        // TOP STATUS BAR (Horizontal fixed status bar)
        TopStatusBar topStatusBar = new TopStatusBar(translateExecutor.isProxyActive());
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

        ConfigZoneCard configZoneCard = new ConfigZoneCard(stage, aiConfig, aiFactory, translateExecutor);

        // ActionsCard needs callback to update cache labels in translationCacheCard
        TranslationCacheCard translationCacheCard = new TranslationCacheCard(stage, translateExecutor, historyList);

        ActionsZoneCard actionsZoneCard = new ActionsZoneCard(
                stage,
                translateExecutor,
                aiConfig,
                historyList,
                translationCacheCard::updateActiveCacheLabel);

        leftPane.getChildren().addAll(configZoneCard, actionsZoneCard);

        // Connect cross-card custom properties & triggers
        actionsZoneCard.setOnProxyStatusChanged(() -> topStatusBar.updateStatus(translateExecutor.isProxyActive()));
        translationCacheCard.setGamePathSupplier(actionsZoneCard::getGamePath);
        translationCacheCard.setOnRowChanged(actionsZoneCard::autoExportToGame);
        translationCacheCard.setOnCacheFileChange(translationCacheCard::updateActiveCacheLabel);
        translationCacheCard.setOnCacheFileWiped(() -> {
            totalRequests = 0;
            totalChars = 0;
            cacheHits = 0;
            aiHits = 0;
            topStatusBar.updateStats(0, 0, 0, 0);
        });

        // RIGHT PANEL: ZONE C (TABLE & MONITOR)
        VBox rightPane = new VBox(10);
        HBox.setHgrow(rightPane, Priority.ALWAYS);

        // Console Log Area
        TerminalConsoleArea consoleArea = new TerminalConsoleArea();

        rightPane.getChildren().addAll(translationCacheCard, consoleArea);
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
                topStatusBar.updateStats(totalRequests, totalChars, cacheHits, aiHits);

                TranslationItem selected = translationCacheCard.getTableView().getSelectionModel().getSelectedItem();
                TranslationItem newItem = new TranslationItem(type, original, translated);
                historyList.add(newItem);
                if (historyList.size() > 500) {
                    historyList.remove(0);
                }
                if (selected != null) {
                    translationCacheCard.getTableView().getSelectionModel().select(selected);
                }
                translationCacheCard.handleNewTranslationAdded();
            });
        });

        // Load initially active cache
        translationCacheCard.updateActiveCacheLabel();

        Scene scene = new Scene(root);
        java.io.File cssFile = new java.io.File("data/ui_style.css");
        if (cssFile.exists()) {
            scene.getStylesheets().add(cssFile.toURI().toString());
        }
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
        javafx.application.Platform.runLater(translationCacheCard::initScrollTracking);
    }
}
