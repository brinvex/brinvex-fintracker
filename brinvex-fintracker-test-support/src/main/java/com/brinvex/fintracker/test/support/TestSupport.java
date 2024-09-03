package com.brinvex.fintracker.test.support;

import com.brinvex.fintracker.api.FinTracker;
import com.brinvex.fintracker.api.FinTrackerConfig;
import com.brinvex.fintracker.api.FinTrackerConfigBuilder;
import com.brinvex.util.dms.api.DmsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import static java.util.stream.Collectors.toMap;


public class TestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(TestSupport.class);

    public static final Path home = Optional.ofNullable(System.getenv("BRINVEX_FINTRACKER_HOME"))
            .map(Paths::get)
            .orElse(null);

    private final String module;

    private Path moduleTestDataFolder;

    private DmsFactory moduleDmsFactory;

    private Map<String, String> properties;

    public TestSupport(String module) {
        this.module = module;
    }

    public FinTracker finTracker() {
        return finTracker(Collections.emptyMap());
    }

    public FinTracker finTracker(Map<String, String> properties) {
        return finTracker(configBuilder -> {
            for (Map.Entry<String, String> e : properties.entrySet()) {
                configBuilder.property(e.getKey(), e.getValue());
            }
        });
    }

    public FinTracker finTracker(Consumer<FinTrackerConfigBuilder> configAdjuster) {
        FinTrackerConfigBuilder configBuilder = config();
        configAdjuster.accept(configBuilder);
        return FinTracker.get(configBuilder.build());
    }

    public FinTrackerConfigBuilder config() {
        return FinTrackerConfig.builder()
                .dmsFactory(this::dmsFactory);
    }

    public Path testDataFolder() {
        if (moduleTestDataFolder == null) {
            moduleTestDataFolder = home.resolve("test-data/%s".formatted(module));
            if (!Files.exists(moduleTestDataFolder)) {
                LOG.debug("Creating test data folder: {}", moduleTestDataFolder);
                try {
                    Files.createDirectories(moduleTestDataFolder);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                LOG.debug("Going to use existing test data folder: {}", moduleTestDataFolder);
            }
        }
        return moduleTestDataFolder;
    }

    public DmsFactory dmsFactory() {
        if (moduleDmsFactory == null) {
            Path dmsBasePath = testDataFolder().resolve("dms");
            if (!Files.exists(dmsBasePath)) {
                LOG.debug("Creating test DMS: {}", dmsBasePath);
                try {
                    Files.createDirectories(dmsBasePath);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else {
                LOG.debug("Going to use existing test DMS: {}", dmsBasePath);
            }
            moduleDmsFactory = DmsFactory.createFilesystemDmsFactory(dmsBasePath);
        }
        return moduleDmsFactory;
    }

    public String property(String key) {
        return property(key, null);
    }

    public Map<String, String> subProperties(String keyPrefix) {
        initProperties();
        String moduleKeyPrefix = "test.%s.%s.".formatted(module, keyPrefix);
        int moduleKeyPrefixLength = moduleKeyPrefix.length();
        return properties.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(moduleKeyPrefix))
                .collect(toMap(stringStringEntry -> stringStringEntry.getKey().substring(moduleKeyPrefixLength), Map.Entry::getValue));
    }

    public String property(String key, String defaultValue) {
        initProperties();
        String moduleKey = "test.%s.%s".formatted(module, key);
        return properties.getOrDefault(moduleKey, defaultValue);
    }

    private void initProperties() {
        if (properties == null) {
            Path testPropPath = home.resolve("brinvex-fintracker-test.properties");
            if (!Files.exists(testPropPath)) {
                throw new IllegalStateException("Test property file does not exist: " + testPropPath);
            } else {
                LOG.debug("Going to use test property file: {}", testPropPath);
            }
            try (InputStream is = new FileInputStream(testPropPath.toFile())) {
                //todo 5 - Find a better way to work with properties
                Properties properties = new Properties();
                properties.load(is);
                this.properties = properties.entrySet()
                        .stream()
                        .collect(toMap(e -> e.getKey().toString(), t -> t.getValue().toString()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

}
