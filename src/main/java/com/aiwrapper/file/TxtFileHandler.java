package com.aiwrapper.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TxtFileHandler implements FileHandler {

    @Override
    public List<Map<String, Object>> read(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            List<Map<String, Object>> list = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("text", line);
                    list.add(row);
                }
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Error reading TXT file: " + filePath, e);
        }
    }

    @Override
    public void write(String filePath, List<Map<String, Object>> data) {
        try {
            File file = new File(filePath);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (Map<String, Object> row : data) {
                    Object val = row.get("translated");
                    if (val == null) {
                        val = row.get("text");
                    }
                    if (val == null && !row.isEmpty()) {
                        val = row.values().iterator().next();
                    }
                    writer.write(val != null ? String.valueOf(val) : "");
                    writer.newLine();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing TXT file: " + filePath, e);
        }
    }
}
