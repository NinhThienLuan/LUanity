package com.aiwrapper.javafx.ui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.OutputStream;
import java.io.PrintStream;

public class TerminalConsoleArea extends VBox {

    private final TextArea console;

    public TerminalConsoleArea() {
        super(10);

        Label monitorLabel = new Label("MONITOR LOGS (CONSOLE SYSTEM.OUT / SERVER LOGS)");
        monitorLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        monitorLabel.setTextFill(javafx.scene.paint.Color.web("#06b6d4"));

        console = new TextArea();
        console.setEditable(false);
        console.setWrapText(true);
        console.setPrefHeight(150);
        console.setMinHeight(120);
        console.setStyle(
                "-fx-control-inner-background: #0b0f19; -fx-text-fill: #06b6d4; -fx-font-family: Consolas, 'Courier New', monospace; -fx-font-size: 11;");
        console.appendText("System ready. Live proxy active.\n");

        getChildren().addAll(monitorLabel, console);

        // Intercept System.out
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                oldOut.write(b);
                Platform.runLater(() -> console.appendText(String.valueOf((char) b)));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                oldOut.write(b, off, len);
                String str = new String(b, off, len);
                Platform.runLater(() -> console.appendText(str));
            }
        }));
    }

    public TextArea getConsoleTextArea() {
        return console;
    }
}
