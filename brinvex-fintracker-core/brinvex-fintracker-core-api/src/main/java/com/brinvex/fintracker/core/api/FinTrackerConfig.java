package com.brinvex.fintracker.core.api;

import com.brinvex.fintracker.core.api.internal.FinTrackerModule;
import com.brinvex.util.dms.api.DmsFactory;
import jakarta.validation.ValidatorFactory;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

@SuppressWarnings("UnusedReturnValue")
@Getter
@Setter
@Accessors(fluent = true, chain = true)
public class FinTrackerConfig {

    public static final String PROP_KEY_PREFIX = "brinvex.fintracker.";

    private static class LazyPropKeyPatterns {
        private static final Pattern CORE_PROP_KEY_PATTERN = Pattern.compile("brinvex\\.fintracker\\.core\\.(?<KEY>[\\w.]+)");
        private static final Pattern MODULE_PROP_KEY_PATTERN = Pattern.compile("brinvex\\.fintracker\\.(?<MODULE>[A-Z]\\w+Module)\\.(?<KEY>[\\w.]+)");
    }

    @Getter(AccessLevel.NONE)
    private final Map<String, String> coreProperties = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final Map<String, Map<String, String>> moduleProperties = new ConcurrentHashMap<>();

    private Supplier<DmsFactory> dmsFactory;

    private Supplier<ValidatorFactory> validatorFactory;

    public void setProperties(Map<String, String> rawProperties) {
        for (Map.Entry<String, String> e : rawProperties.entrySet()) {
            String rawKey = e.getKey();
            String value = e.getValue();
            setProperty(rawKey, value);
        }
    }

    public void setProperty(String rawKey, String value) {
        Matcher m = LazyPropKeyPatterns.CORE_PROP_KEY_PATTERN.matcher(rawKey);
        if (m.matches()) {
            setCoreProperty(m.group("KEY"), value);
        } else {
            m = LazyPropKeyPatterns.MODULE_PROP_KEY_PATTERN.matcher(rawKey);
            if (m.matches()) {
                setModuleProperty(m.group("MODULE"), m.group("KEY"), value);
            } else {
                throw new IllegalArgumentException("Invalid property key: '%s'".formatted(rawKey));
            }
        }
    }

    public void setCoreProperties(Map<String, String> coreProperties) {
        for (Map.Entry<String, String> e : coreProperties.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            setCoreProperty(key, value);
        }
    }

    public String setCoreProperty(String key, String value) {
        if (propKeyIsInvalid(key) || key.startsWith(PROP_KEY_PREFIX)) {
            throw new IllegalArgumentException("Invalid core property key: '%s'".formatted(key));
        }
        return coreProperties.put(key, value);
    }

    public String getCoreProperty(String key) {
        return getCoreProperty(key, null);
    }

    public String getCoreProperty(String key, String defaultValue) {
        return coreProperties.getOrDefault(key, defaultValue);
    }

    public void setModuleProperties(Class<? extends FinTrackerModule> moduleType, Map<String, String> moduleProperties) {
        for (Map.Entry<String, String> e : moduleProperties.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();
            setModuleProperty(moduleType, key, value);
        }
    }

    public String setModuleProperty(Class<? extends FinTrackerModule> moduleType, String key, String value) {
        String moduleName = moduleType.getSimpleName();
        return setModuleProperty(moduleName, key, value);
    }

    private String setModuleProperty(String moduleName, String key, String value) {
        if (propKeyIsInvalid(key) || key.startsWith(PROP_KEY_PREFIX)) {
            throw new IllegalArgumentException("Invalid module property key: '%s'".formatted(key));
        }
        return moduleProperties.computeIfAbsent(moduleName, _ -> new ConcurrentHashMap<>()).put(key, value);
    }

    public String getModuleProperty(Class<? extends FinTrackerModule> moduleType, String key) {
        return getModuleProperty(moduleType, key, null);
    }

    public String getModuleProperty(Class<? extends FinTrackerModule> moduleType, String key, String defaultValue) {
        return moduleProperties.getOrDefault(moduleType.getSimpleName(), emptyMap()).getOrDefault(key, defaultValue);
    }

    public Map<String, String> getModuleSubProperties(Class<? extends FinTrackerModule> moduleType, String keyPrefix) {
        String keyPrefixEndingWithDot = keyPrefix.charAt(keyPrefix.length() - 1) == '.' ? keyPrefix : (keyPrefix + '.');
        int prefixLength = keyPrefixEndingWithDot.length();
        return Map.copyOf(moduleProperties.getOrDefault(moduleType.getSimpleName(), emptyMap())
                .entrySet()
                .stream()
                .filter(e -> e.getKey().startsWith(keyPrefixEndingWithDot))
                .collect(toMap(e -> e.getKey().substring(prefixLength), Map.Entry::getValue)));
    }

    private boolean propKeyIsInvalid(String key) {
        return key == null
               || key.isBlank()
               || key.charAt(0) == '.'
               || key.charAt(key.length() - 1) == '.'
               || !key.trim().equals(key);
    }
}
