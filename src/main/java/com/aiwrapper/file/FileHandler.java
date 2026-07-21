package com.aiwrapper.file;

import java.util.List;
import java.util.Map;

public interface FileHandler {
    List<Map<String, Object>> read(String filePath);
    void write(String filePath, List<Map<String, Object>> data);
}
