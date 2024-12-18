package com.brinvex.ptfactivity.testsupport;

import com.brinvex.ptfactivity.core.api.CoreModule;
import com.brinvex.ptfactivity.core.api.PtfActivityRuntime;
import com.brinvex.ptfactivity.core.api.PtfActivityRuntimeConfig;
import com.brinvex.ptfactivity.core.api.Module;
import com.brinvex.ptfactivity.core.api.Toolbox;
import com.brinvex.ptfactivity.core.api.facade.ValidatorFacade;
import com.brinvex.dms.api.DmsFactory;
import com.brinvex.java.LazyConstant;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.brinvex.java.collection.Collectors.toLinkedMap;
import static com.brinvex.java.IOCallUtil.uncheckedIO;
import static com.brinvex.java.LazyConstant.nonThreadSafe;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;

public class TestContext {

    private static class Lazy {

        private static final Path HOME = Optional.of("c:/prj/bx/bx-ptfactivity")
                .map(Paths::get)
                .orElseThrow();

        private static Map<String, String> allTestProperties;

        private static final Path TEST_DATA_FOLDER = HOME.resolve("test-data");

        private static Map<String, String> subProperties(String keyPrefix) {
            if (allTestProperties == null) {
                allTestProperties = loadProperties(HOME.resolve("brinvex-ptfactivity-test.properties"), "test.");
            }
            if (keyPrefix == null || keyPrefix.isEmpty()) {
                return Collections.unmodifiableMap(allTestProperties);
            } else {
                String sanitizedKeyPrefix = keyPrefix.charAt(keyPrefix.length() - 1) == '.' ? keyPrefix : keyPrefix + '.';
                int prefixLength = sanitizedKeyPrefix.length();
                return allTestProperties.entrySet()
                        .stream()
                        .filter(e -> e.getKey().startsWith(sanitizedKeyPrefix))
                        .collect(toLinkedMap(e -> e.getKey().substring(prefixLength), Map.Entry::getValue));
            }
        }

        @SuppressWarnings("SameParameterValue")
        private static Map<String, String> loadProperties(Path path, String keyPrefix) {
            int prefixLength = keyPrefix.length();
            return uncheckedIO(() -> {
                try (Stream<String> lines = Files.lines(path, UTF_8)) {
                    return lines
                            .filter(not(String::isBlank))
                            .filter(line -> line.startsWith(keyPrefix) && !line.startsWith("#"))
                            .map(line -> {
                                String[] parts = line.substring(prefixLength).split("=", 2);
                                if (parts.length != 2) {
                                    throw new IllegalStateException("Invalid line: " + line);
                                }
                                return parts;
                            })
                            .filter(parts -> !parts[1].isBlank())
                            .collect(toLinkedMap(parts -> parts[0], parts -> parts[1]));
                }
            });
        }

    }

    private static final LazyConstant<DmsFactory> DMS_FACTORY = nonThreadSafe(() -> DmsFactory.newFilesystemDmsFactory(Lazy.TEST_DATA_FOLDER));

    private final Class<? extends Module> moduleType;

    private final PtfActivityRuntime ptfActivityRuntime;

    private CoreModule coreModule;

    private Toolbox toolbox;

    private final Map<String, String> properties;

    public TestContext(Class<? extends Module> moduleType) {
        this.moduleType = moduleType;
        this.properties = Lazy.subProperties(Module.compactName(moduleType));
        this.ptfActivityRuntime = PtfActivityRuntime.newPtfActivityRuntime(PtfActivityRuntimeConfig.builder()
                .setProperties(this.properties
                        .entrySet()
                        .stream()
                        .filter(e -> e.getKey().startsWith(PtfActivityRuntimeConfig.FULL_PROP_KEY_PREFIX))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .dmsFactory(DMS_FACTORY)
                .build());
    }

    private TestContext(TestContext sourceCtx, Map<String, String> properties) {
        this.moduleType = sourceCtx.moduleType;
        this.properties = new LinkedHashMap<>(sourceCtx.properties);
        if (properties != null) {
            for (Map.Entry<String, String> e : properties.entrySet()) {
                String key = e.getKey();
                String value = e.getValue();
                this.properties.put(PtfActivityRuntimeConfig.FULL_PROP_KEY_FORMATTER.apply(moduleType, key), value);
            }
        }
        this.ptfActivityRuntime = PtfActivityRuntime.newPtfActivityRuntime(PtfActivityRuntimeConfig.builder()
                .setProperties(this.properties
                        .entrySet()
                        .stream()
                        .filter(e -> e.getKey().startsWith(PtfActivityRuntimeConfig.FULL_PROP_KEY_PREFIX))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .dmsFactory(DMS_FACTORY)
                .build());
    }

    public Map<String, String> subProperties(String keyPrefix) {
        String sanitizedKeyPrefix = keyPrefix.charAt(keyPrefix.length() - 1) != '.' ? keyPrefix + '.' : keyPrefix;
        int prefixLength = sanitizedKeyPrefix.length();
        return properties.entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(sanitizedKeyPrefix))
                .collect(toMap(e -> e.getKey().substring(prefixLength), Map.Entry::getValue));
    }

    public String property(String key) {
        return properties.get(key);
    }

    public PtfActivityRuntime runtime() {
        return ptfActivityRuntime;
    }

    public Toolbox toolbox() {
        if (toolbox == null) {
            toolbox = coreModule().toolbox();
        }
        return toolbox;
    }

    public CoreModule coreModule() {
        if (coreModule == null) {
            coreModule = get(CoreModule.class);
        }
        return coreModule;
    }

    public ValidatorFacade validator() {
        return toolbox().validator();
    }

    public DmsFactory dmsFactory() {
        return DMS_FACTORY.get();
    }

    public String dmsWorkspace() {
        return properties.get(PtfActivityRuntimeConfig.FULL_PROP_KEY_FORMATTER.apply(moduleType, Module.PropKey.dmsWorkspace));
    }

    public <MODULE extends Module> MODULE get(Class<MODULE> moduleType) {
        return ptfActivityRuntime.getModule(moduleType);
    }

    public TestContext withProperties(Map<String, String> properties) {
        return new TestContext(this, properties);
    }

    public TestContext withDmsWorkspace(String dmsWorkspace) {
        return withProperties(Map.of(Module.PropKey.dmsWorkspace, dmsWorkspace));
    }
}
