package com.aiwrapper.javafx.ui;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Shared UI factory methods for the dark-theme dashboard.
 * Eliminates duplicated helper methods across card components.
 */
public final class UiStyles {

    private UiStyles() {
    }

    public static Label createFormLabel(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#e2e8f0"));
        label.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        return label;
    }

    public static TextField createTextField(String promptText) {
        TextField tf = new TextField();
        tf.setPromptText(promptText);
        tf.setStyle(
                "-fx-background-color: #1e293b; -fx-text-fill: white; -fx-prompt-text-fill: #64748b; -fx-background-radius: 6; -fx-padding: 8 12;");
        return tf;
    }

    public static Button createSecondaryButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
                "-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #475569; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;"));
        return btn;
    }

    public static void styleDropdown(ComboBox<?> cb) {
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-background-color: #1e293b; -fx-text-fill: white; -fx-background-radius: 6; -fx-padding: 4 8;");
    }
}
