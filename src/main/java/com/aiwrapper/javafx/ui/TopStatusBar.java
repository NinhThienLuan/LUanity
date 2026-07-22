package com.aiwrapper.javafx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;

public class TopStatusBar extends HBox {

    private final Label lblStatusText;
    private final Label lblReqCount;
    private final Label lblCacheHits;
    private final Label lblAiHits;

    public TopStatusBar(boolean isRunningInitial) {
        super(16);
        setPadding(new Insets(8, 16, 8, 16));
        setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");
        setPrefHeight(40);
        setMinHeight(40);
        setAlignment(Pos.CENTER_LEFT);

        lblStatusText = new Label(isRunningInitial ? "STATUS: RUNNING" : "STATUS: STOPPED");
        lblStatusText.setStyle("-fx-font-weight: bold; -fx-text-fill: "
                + (isRunningInitial ? "#06b6d4;" : "#ef4444;") + " -fx-font-size: 12px;");

        lblReqCount = new Label("Yêu cầu: 0 (Ký tự: 0)");
        lblReqCount.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        lblCacheHits = new Label("Bộ đệm: 0");
        lblCacheHits.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        lblAiHits = new Label("LLM Dịch: 0");
        lblAiHits.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        getChildren().addAll(
                lblStatusText,
                createVerticalSeparator(),
                lblReqCount,
                createVerticalSeparator(),
                lblCacheHits,
                createVerticalSeparator(),
                lblAiHits);
    }

    public void updateStats(int requests, int characters, int cacheHits, int aiHits) {
        lblReqCount.setText("Yêu cầu: " + requests + " (Ký tự: " + characters + ")");
        lblCacheHits.setText("Bộ đệm: " + cacheHits);
        lblAiHits.setText("LLM Dịch: " + aiHits);
    }

    public void updateStatus(boolean active) {
        lblStatusText.setText(active ? "STATUS: RUNNING" : "STATUS: STOPPED");
        lblStatusText.setStyle("-fx-font-weight: bold; -fx-text-fill: "
                + (active ? "#06b6d4;" : "#ef4444;") + " -fx-font-size: 12px;");
    }

    private Separator createVerticalSeparator() {
        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        sep.setStyle("-fx-background-color: #334155;");
        sep.setPrefHeight(16);
        return sep;
    }
}
