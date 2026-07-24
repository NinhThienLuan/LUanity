package com.aiwrapper.javafx.ui;

import com.aiwrapper.executor.TranslateExecutor;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.File;

/**
 * Standalone dialog for editing the prompt template.
 * Extracted from ActionsZoneCard to reduce file size.
 */
public class PromptDialog {

    private PromptDialog() {
    }

    public static void show(TranslateExecutor translateExecutor) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Prompt Template");
        File cssFile = new File("data/ui_style.css");
        if (cssFile.exists()) {
            dialog.getDialogPane().getStylesheets().add(cssFile.toURI().toString());
        }

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        Label desc = new Label("Modify the prompt template for translation.\nUse {text} placeholder for source text.");
        desc.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        TextArea textArea = new TextArea(translateExecutor.getPromptTemplate());
        textArea.setWrapText(true);
        textArea.setPrefWidth(450);
        textArea.setPrefHeight(250);

        VBox vbox = new VBox(10, desc, textArea);
        vbox.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                return textArea.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newPrompt -> {
            translateExecutor.setPromptTemplate(newPrompt);
            System.out.println("Prompt template updated.");
        });
    }
}
