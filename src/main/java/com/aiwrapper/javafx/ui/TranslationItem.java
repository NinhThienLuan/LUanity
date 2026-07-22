package com.aiwrapper.javafx.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TranslationItem {
    private final StringProperty type;
    private final StringProperty original;
    private final StringProperty translated;

    public TranslationItem(String type, String original, String translated) {
        this.type = new SimpleStringProperty(type);
        this.original = new SimpleStringProperty(original);
        this.translated = new SimpleStringProperty(translated);
    }

    public StringProperty typeProperty() {
        return type;
    }

    public StringProperty originalProperty() {
        return original;
    }

    public StringProperty translatedProperty() {
        return translated;
    }

    public String getType() {
        return type.get();
    }

    public String getOriginal() {
        return original.get();
    }

    public String getTranslated() {
        return translated.get();
    }

    public void setTranslated(String translated) {
        this.translated.set(translated);
    }

    public void setType(String type) {
        this.type.set(type);
    }
}
