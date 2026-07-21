package com.aiwrapper.runner;

import com.aiwrapper.config.AppProperties;
import com.aiwrapper.config.IoProperties;
import com.aiwrapper.executor.BaseExecutor;
import com.aiwrapper.executor.ExecutorFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Map;

@Component
public class AppRunner implements CommandLineRunner {

    private final ExecutorFactory executorFactory;
    private final AppProperties appProperties;
    private final IoProperties ioProperties;

    public AppRunner(ExecutorFactory executorFactory, AppProperties appProperties, IoProperties ioProperties) {
        this.executorFactory = executorFactory;
        this.appProperties = appProperties;
        this.ioProperties = ioProperties;
    }

    @Override
    public void run(String... args) throws Exception {
        if (appProperties.isGui()) {
            System.out.println("GUI mode is enabled. Skipping automatic command-line execution.");
            return;
        }
        String module = appProperties.getModule();
        if (module == null || module.isEmpty()) {
            module = "translate";
        }

        String inputDir = ioProperties.getInputDir();
        if (inputDir == null || inputDir.isEmpty()) {
            inputDir = "./data/input";
        }

        String outputDir = ioProperties.getOutputDir();
        if (outputDir == null || outputDir.isEmpty()) {
            outputDir = "./data/output";
        }

        // Ensure directories exist
        new File(inputDir).mkdirs();
        new File(outputDir).mkdirs();

        String inputPath = inputDir + "/sample.txt";
        String outputPath = outputDir + "/result.json";

        System.out.println("Starting executor for module: " + module);
        System.out.println("Reading from: " + inputPath);
        System.out.println("Writing to: " + outputPath);

        BaseExecutor executor = executorFactory.get(module);
        executor.execute(inputPath, outputPath, Map.of());
        System.out.println("Done. Check output file.");
    }
}
