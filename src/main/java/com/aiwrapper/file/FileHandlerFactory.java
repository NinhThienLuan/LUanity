package com.aiwrapper.file;

public class FileHandlerFactory {
    private static final FileHandler jsonHandler = new JsonFileHandler();
    private static final FileHandler csvHandler = new CsvFileHandler();
    private static final FileHandler txtHandler = new TxtFileHandler();

    public static FileHandler get(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null");
        }
        String lowercasePath = path.toLowerCase();
        if (lowercasePath.endsWith(".json")) {
            return jsonHandler;
        } else if (lowercasePath.endsWith(".csv")) {
            return csvHandler;
        } else if (lowercasePath.endsWith(".txt")) {
            return txtHandler;
        } else {
            throw new IllegalArgumentException("Unsupported file extension for path: " + path);
        }
    }
}
