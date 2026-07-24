package com.aiwrapper.service;

import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AssetDownloadService {

    public interface ProgressListener {
        void onProgress(String message);
    }

    public void downloadFontAssets(File gameDir, ProgressListener progressListener) throws Exception {
        String zipUrl = "https://github.com/bbepis/XUnity.AutoTranslator/releases/download/v5.3.0/TMP_Font_AssetBundles.zip";
        URL url = new URL(zipUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(120000);

        if (conn.getResponseCode() != 200) {
            throw new Exception("HTTP error code: " + conn.getResponseCode());
        }

        int count = 0;
        byte[] buffer = new byte[4096];
        try (ZipInputStream zis = new ZipInputStream(conn.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                File destFile = new File(gameDir, name);
                String canonicalDest = destFile.getCanonicalPath();
                String canonicalGameDir = gameDir.getCanonicalPath();
                if (!canonicalDest.startsWith(canonicalGameDir + File.separator)
                        && !canonicalDest.equals(canonicalGameDir)) {
                    throw new SecurityException("Zip Slip detected! Entry: " + name);
                }

                if (name.equals("arialuni_sdf_u2018") || name.equals("arialuni_sdf_u2019")) {
                    try (FileOutputStream fos = new FileOutputStream(destFile)) {
                        int read;
                        while ((read = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, read);
                        }
                    }
                    count++;
                }
                zis.closeEntry();
            }
        }
        if (progressListener != null) {
            progressListener.onProgress("Đã tải thành công " + count + " font (arialuni_sdf) vào thư mục game!");
        }
    }
}
