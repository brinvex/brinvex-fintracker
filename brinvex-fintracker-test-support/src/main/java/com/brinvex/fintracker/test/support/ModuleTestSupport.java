package com.brinvex.fintracker.test.support;

import com.brinvex.fintracker.core.api.FinTracker;
import com.brinvex.fintracker.core.api.FinTrackerConfig;
import com.brinvex.fintracker.core.api.internal.FinTrackerModule;
import com.brinvex.util.dms.api.DmsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;

public class ModuleTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleTestSupport.class);

    public static final Path home = Optional.ofNullable(System.getenv("BRINVEX_FINTRACKER_HOME"))
            .map(Paths::get)
            .orElse(null);

    private final Class<? extends FinTrackerModule> moduleType;

    private Path moduleTestDataFolder;

    private DmsFactory moduleDmsFactory;

    private Map<String, String> properties;

    public ModuleTestSupport(Class<? extends FinTrackerModule> moduleType) {
        this.moduleType = moduleType;
    }

    public FinTracker finTracker() {
        return finTracker(Collections.emptyMap());
    }

    public FinTracker finTracker(Map<String, String> moduleProperties) {
        FinTrackerConfig config = new FinTrackerConfig();
        config.setProperties(getProperties()
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(FinTrackerConfig.PROP_KEY_PREFIX))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
        config.setModuleProperties(moduleType, moduleProperties);
        config.dmsFactory(this::dmsFactory);
        return FinTracker.newInstance(config);
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

    private Path testDataFolder() {
        if (moduleTestDataFolder == null) {
            moduleTestDataFolder = home.resolve("test-data");
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

    public Map<String, String> subProperties(String keyPrefix) {
        String moduleKeyPrefix = "%s.".formatted(keyPrefix);
        int moduleKeyPrefixLength = moduleKeyPrefix.length();
        return getProperties().entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(moduleKeyPrefix))
                .collect(toMap(e -> e.getKey().substring(moduleKeyPrefixLength), Map.Entry::getValue));
    }

    private Map<String, String> getProperties() {
        if (properties == null) {
            Path testPropPath = home.resolve("brinvex-fintracker-test.properties");
            if (!Files.exists(testPropPath)) {
                throw new IllegalStateException("Test property file does not exist: " + testPropPath);
            } else {
                LOG.debug("Going to use test property file: {}", testPropPath);
            }
            try {
                properties = parseProperties(testPropPath, "test.%s.".formatted(moduleType.getSimpleName()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return properties;
    }

    private Map<String, String> parseProperties(Path path, String keyPrefix) throws IOException {
        int prefixLength = keyPrefix.length();
        return Files.readAllLines(path, UTF_8)
                .stream()
                .filter(not(String::isBlank))
                .filter(line -> line.startsWith(keyPrefix) && !line.startsWith("#"))
                .map(line -> {
                    String[] parts = line.substring(prefixLength).split("=", 2);
                    if (parts.length != 2) {
                        throw new IllegalStateException("Invalid line: " + line);
                    }
                    return parts;
                })
                .collect(toMap(parts -> parts[0], parts -> parts[1]));
    }

}
