package com.aiwrapper.file;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;

public class CsvFileHandler implements FileHandler {

    @Override
    public List<Map<String, Object>> read(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                return new ArrayList<>();
            }
            List<Map<String, Object>> list = new ArrayList<>();
            try (CSVReaderHeaderAware reader = new CSVReaderHeaderAware(new FileReader(file))) {
                Map<String, String> line;
                while ((line = reader.readMap()) != null) {
                    Map<String, Object> row = new LinkedHashMap<>(line);
                    list.add(row);
                }
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException("Error reading CSV file: " + filePath, e);
        }
    }

    @Override
    public void write(String filePath, List<Map<String, Object>> data) {
        try {
            if (data == null || data.isEmpty()) {
                return;
            }
            File file = new File(filePath);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            Set<String> headerSet = data.get(0).keySet();
            String[] headers = headerSet.toArray(new String[0]);

            try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
                writer.writeNext(headers);
                for (Map<String, Object> row : data) {
                    String[] line = new String[headers.length];
                    for (int i = 0; i < headers.length; i++) {
                        Object val = row.get(headers[i]);
                        line[i] = val != null ? String.valueOf(val) : "";
                    }
                    writer.writeNext(line);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing CSV file: " + filePath, e);
        }
    }
}
