package com.aiwrapper.executor;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class ExecutorFactory {

    private final Map<String, BaseExecutor> executors;

    public ExecutorFactory(TranslateExecutor translateExecutor) {
        this.executors = Map.of(
            "translate", translateExecutor
        );
    }

    public BaseExecutor get(String moduleName) {
        BaseExecutor executor = executors.get(moduleName);
        if (executor == null) {
            throw new IllegalArgumentException("Module unsupported: " + moduleName);
        }
        return executor;
    }
}
