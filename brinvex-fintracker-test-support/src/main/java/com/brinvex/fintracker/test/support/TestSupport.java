package com.brinvex.fintracker.test.support;

import com.brinvex.fintracker.api.FinTrackerApplication;
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

public class TestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(TestSupport.class);

    public static final Path home = Optional.ofNullable(System.getenv("BRINVEX_FINTRACKER_HOME"))
            .map(Paths::get)
            .orElse(null);

    private final String module;

    private Path moduleTestDataFolder;

    private DmsFactory moduleDmsFactory;

    private Properties properties;

    public TestSupport(String module) {
        this.module = module;
    }

    public FinTrackerApplication app() {
        return app(Collections.emptyMap());
    }

    public FinTrackerApplication app(Map<String, String> properties) {
        return app(configBuilder -> {
            for (Map.Entry<String, String> e : properties.entrySet()) {
                configBuilder.property(e.getKey(), e.getValue());
            }
        });
    }

    public FinTrackerApplication app(Consumer<FinTrackerConfigBuilder> configAdjuster) {
        FinTrackerConfigBuilder configBuilder = config();
        configAdjuster.accept(configBuilder);
        return FinTrackerApplication.get(configBuilder.build());
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

    public String property(String key, String defaultValue) {
        if (properties == null) {
            Path testPropPath = home.resolve("brinvex-fintracker-test.properties");
            if (!Files.exists(testPropPath)) {
                throw new IllegalStateException("Test property file does not exist: " + testPropPath);
            } else {
                LOG.debug("Going to use test property file: {}", testPropPath);
            }
            try (InputStream is = new FileInputStream(testPropPath.toFile())) {
                properties = new Properties();
                properties.load(is);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        String moduleKey = "test.%s.%s".formatted(module, key);
        return properties.getProperty(moduleKey);
    }

}
