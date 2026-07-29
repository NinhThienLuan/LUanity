package com.aiwrapper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * One-click BepInEx + XUnity.AutoTranslator setup pipeline.
 *
 * <p>
 * Usage:
 * 
 * <pre>{@code
 * BepInExSetupService svc = new BepInExSetupService();
 * svc.setup(gameExeFile, 8080, "vi", msg -> System.out.println(msg));
 * }</pre>
 */
public class BepInExSetupService {

    private static final String BEPINEX_REPO = "BepInEx/BepInEx";
    private static final String AUTOTL_REPO = "bbepis/XUnity.AutoTranslator";
    private static final String GH_API_BASE = "https://api.github.com/repos/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Returns true if BepInEx core DLL is present next to the given exe.
     */
    public boolean isInstalled(File gameExe) {
        File gameRoot = gameExe.getParentFile();
        File coreMono = new File(gameRoot, "BepInEx/core/BepInEx.dll");
        File coreIl2cpp = new File(gameRoot, "BepInEx/core/BepInEx.Core.dll");
        return coreMono.exists() || coreIl2cpp.exists();
    }

    /**
     * Returns true if the game is Unity IL2CPP (presence of GameAssembly.dll).
     */
    public boolean isIl2Cpp(File gameExe) {
        File gameRoot = gameExe.getParentFile();
        return new File(gameRoot, "GameAssembly.dll").exists();
    }

    /**
     * Full setup pipeline. Runs synchronously — call from a background thread.
     *
     * @param gameExe   path to the game executable
     * @param proxyPort port the Spring Boot proxy is listening on
     * @param lang      target language code (e.g. "vi")
     * @param log       consumer for progress messages
     */
    public void setup(File gameExe, int proxyPort, String lang, Consumer<String> log) throws Exception {
        File gameRoot = gameExe.getParentFile();
        log.accept("[Setup] Game root: " + gameRoot.getAbsolutePath());

        // 1. Detect architecture and scripting backend
        String arch = detectArch(gameRoot);
        log.accept("[Setup] Detected architecture: " + arch);

        boolean il2cpp = isIl2Cpp(gameExe);
        log.accept("[Setup] Scripting backend: " + (il2cpp ? "IL2CPP" : "Mono"));

        // 2. Install BepInEx
        if (isInstalled(gameExe)) {
            log.accept("[Setup] BepInEx already installed — skipping download.");
        } else {
            log.accept("[Setup] Fetching BepInEx release info ...");
            String prefix;
            if (il2cpp) {
                // IL2CPP uses BepInEx UnityIL2CPP
                prefix = "BepInEx_UnityIL2CPP_" + arch;
            } else {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    prefix = "BepInEx_win_" + arch;
                } else if (os.contains("mac")) {
                    prefix = "BepInEx_macos";
                } else {
                    prefix = "BepInEx_linux_" + arch;
                }
            }

            String bepInExUrl = resolveDownloadUrl(BEPINEX_REPO, prefix, il2cpp);
            log.accept("[Setup] Downloading BepInEx: " + bepInExUrl);
            File bepInExZip = downloadToTemp(bepInExUrl, "bepinex", ".zip", log);
            log.accept("[Setup] Extracting BepInEx to game root ...");
            unzip(bepInExZip, gameRoot, log);
            bepInExZip.delete();
            log.accept("[Setup] BepInEx installed.");
        }

        // 3. Install XUnity.AutoTranslator
        File pluginsDir = new File(gameRoot, "BepInEx/plugins");
        pluginsDir.mkdirs();
        boolean autoTlPresent = new File(pluginsDir, "XUnity.AutoTranslator.Plugin.Core.dll").exists()
                || new File(pluginsDir, "XUnity.AutoTranslator/XUnity.AutoTranslator.Plugin.Core.dll").exists();
        if (autoTlPresent) {
            log.accept("[Setup] XUnity.AutoTranslator already installed — skipping download.");
        } else {
            log.accept("[Setup] Fetching XUnity.AutoTranslator release info ...");
            String autoTlPrefix = il2cpp ? "XUnity.AutoTranslator-BepInEx-IL2CPP-" : "XUnity.AutoTranslator-BepInEx-5x";
            String autoTlUrl = resolveDownloadUrl(AUTOTL_REPO, autoTlPrefix, il2cpp);
            log.accept("[Setup] Downloading AutoTranslator: " + autoTlUrl);
            File autoTlZip = downloadToTemp(autoTlUrl, "autotl", ".zip", log);
            log.accept("[Setup] Extracting AutoTranslator to game root ...");
            // Extract AutoTranslator to game root so the zip's BepInEx/... structure merges
            // correctly
            unzip(autoTlZip, gameRoot, log);
            autoTlZip.delete();
            log.accept("[Setup] XUnity.AutoTranslator installed.");
        }

        // 4. Write AutoTranslatorConfig.ini
        log.accept("[Setup] Writing AutoTranslatorConfig.ini ...");
        writeConfig(gameRoot, proxyPort, lang);
        log.accept("[Setup] Config written.");

        log.accept("[Setup] ✔ Setup complete! Khởi động game một lần để BepInEx hoàn tất khởi tạo.");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Detect x64 vs x86 from game Data/Plugins folder. */
    private String detectArch(File gameRoot) {
        // Look for x86_64 subfolder in any *_Data/Plugins directory
        File[] dataDirs = gameRoot.listFiles(f -> f.isDirectory() && f.getName().endsWith("_Data"));
        if (dataDirs != null) {
            for (File dataDir : dataDirs) {
                File plugins = new File(dataDir, "Plugins");
                if (plugins.exists()) {
                    if (new File(plugins, "x86_64").exists())
                        return "x64";
                    if (new File(plugins, "x86").exists())
                        return "x86";
                }
            }
        }
        return "x64"; // safe default
    }

    /**
     * Calls GitHub API to find the latest release download URL matching an asset
     * name prefix.
     */
    private String resolveDownloadUrl(String repo, String assetPrefix, boolean il2cpp) throws Exception {
        String apiUrl;
        if (repo.contains("BepInEx") && il2cpp) {
            // Find recent releases to look for UnityIL2CPP pre-releases
            apiUrl = GH_API_BASE + repo + "/releases";
        } else {
            apiUrl = GH_API_BASE + repo + "/releases/latest";
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestProperty("Accept", "application/vnd.github+json");
        conn.setRequestProperty("User-Agent", "LUanity-Translator/1.0");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(30_000);

        try (InputStream in = conn.getInputStream()) {
            JsonNode root = MAPPER.readTree(in);
            if (root.isArray()) {
                // Releases list array
                for (JsonNode release : root) {
                    JsonNode assets = release.path("assets");
                    for (JsonNode asset : assets) {
                        String name = asset.path("name").asText("");
                        if (name.endsWith(".zip") && name.startsWith(assetPrefix)) {
                            return asset.path("browser_download_url").asText();
                        }
                    }
                }
            } else {
                // Single release object
                JsonNode assets = root.path("assets");
                for (JsonNode asset : assets) {
                    String name = asset.path("name").asText("");
                    if (name.endsWith(".zip")) {
                        if (repo.contains("AutoTranslator")) {
                            if (il2cpp) {
                                // Match: XUnity.AutoTranslator-BepInEx-IL2CPP-[version].zip
                                if (name.startsWith("XUnity.AutoTranslator-BepInEx-IL2CPP-")) {
                                    return asset.path("browser_download_url").asText();
                                }
                            } else {
                                // Match: XUnity.AutoTranslator-BepInEx-[version].zip
                                if (name.startsWith("XUnity.AutoTranslator-BepInEx-") && !name.contains("IL2CPP")) {
                                    return asset.path("browser_download_url").asText();
                                }
                            }
                        } else {
                            if (name.startsWith(assetPrefix)) {
                                return asset.path("browser_download_url").asText();
                            }
                        }
                    }
                }
            }
        }
        throw new IOException("No matching asset found in " + repo + " for filter matching prefix: " + assetPrefix);
    }

    /** Download a URL to a temp file, logging progress periodically. */
    private File downloadToTemp(String urlStr, String prefix, String suffix,
            Consumer<String> log) throws Exception {
        File tmp = File.createTempFile(prefix, suffix);
        tmp.deleteOnExit();

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestProperty("User-Agent", "LUanity-Translator/1.0");
        conn.setConnectTimeout(15_000);
        conn.setReadTimeout(120_000);

        int total = conn.getContentLength();
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
                OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {

            byte[] buf = new byte[8192];
            int downloaded = 0, n;
            int lastPct = -1;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                downloaded += n;
                if (total > 0) {
                    int pct = (int) (downloaded * 100L / total);
                    if (pct / 10 != lastPct / 10) {
                        lastPct = pct;
                        log.accept(
                                "[Download] " + pct + "% (" + (downloaded / 1024) + " KB / " + (total / 1024) + " KB)");
                    }
                }
            }
        }
        return tmp;
    }

    /**
     * Unzip {@code zipFile} into {@code targetDir}, preserving directory structure.
     */
    private void unzip(File zipFile, File targetDir, Consumer<String> log) throws IOException {
        targetDir.mkdirs();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File dest = new File(targetDir, entry.getName());
                if (!dest.getCanonicalPath().startsWith(targetDir.getCanonicalPath())) {
                    throw new IOException("Zip slip detected: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    dest.mkdirs();
                } else {
                    dest.getParentFile().mkdirs();
                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))) {
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = zis.read(buf)) != -1)
                            out.write(buf, 0, n);
                    }
                    log.accept("[Unzip] " + entry.getName());
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Read the bundled config template and write a configured
     * {@code AutoTranslatorConfig.ini} into {@code BepInEx/config/}.
     */
    private void writeConfig(File gameRoot, int proxyPort, String lang) throws IOException {
        // Load template from classpath
        String template;
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("bepinex_config_template.ini")) {
            if (in == null)
                throw new IOException("bepinex_config_template.ini not found in classpath");
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        String config = template
                .replace("{PROXY_PORT}", String.valueOf(proxyPort))
                .replace("{LANG}", lang);

        File configDir = new File(gameRoot, "BepInEx/config");
        configDir.mkdirs();
        File configFile = new File(configDir, "AutoTranslatorConfig.ini");

        // Back up existing config if present
        if (configFile.exists()) {
            File backup = new File(configDir, "AutoTranslatorConfig.ini.bak");
            Files.copy(configFile.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        Files.writeString(configFile.toPath(), config, StandardCharsets.UTF_8);
    }
}
