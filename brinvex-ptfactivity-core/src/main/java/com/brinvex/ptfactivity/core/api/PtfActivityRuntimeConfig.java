package com.brinvex.ptfactivity.core.api;

import com.brinvex.dms.api.DmsFactory;
import com.brinvex.ptfactivity.core.api.facade.JsonMapperFacade;
import com.brinvex.ptfactivity.core.api.facade.PdfReaderFacade;
import jakarta.validation.Validator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/// ###### Property Naming Convention
/// ```
/// ptfactivity.core.abc=value1
/// ptfactivity.ibkr.x.y.z1=value2
///```
/// - *FULL_PROP_KEY*
///   The complete property key including the application prefix.
///   Example: `ptfactivity.core.abc`, `ptfactivity.ibkr.x.y.z1`
///
/// - *COMPACT_PROP_KEY*
///   The property key without the application prefix.
///   Example: `core.key1`, `ibkr.x.y.z1`
///
/// - *MODULE_PROP_KEY_PART*
///   The part of the key that identifies the module, represented as its compact name {@link Module#compactName(Class)}.
///   Example: `core`, `ibkr`
///
/// - *MAIN_KEY*
///   The primary name of the property.
///   Example: `key1`, `x.y.z1`
public final class PtfActivityRuntimeConfig {

    public static final String FULL_PROP_KEY_PREFIX = "ptfactivity.";

    private static final Pattern FULL_PROP_KEY_PATTERN = Pattern.compile("ptfactivity\\.(?<Module>[a-z]\\w+)\\.(?<MainKey>[\\w.]+)");

    private static final BiFunction<String, String, String> COMPACT_PROP_KEY_FORMATTER = "%s.%s"::formatted;

    public static final BiFunction<Class<? extends Module>, String, String> FULL_PROP_KEY_FORMATTER =
            (moduleType, key) -> "ptfactivity.%s.%s".formatted(Module.compactName(moduleType), key);

    private final Map<String, String> properties;

    private final Supplier<DmsFactory> dmsFactory;
    private final Supplier<JsonMapperFacade> jsonMapper;
    private final Supplier<PdfReaderFacade> pdfReader;
    private final Supplier<Validator> validator;

    private PtfActivityRuntimeConfig(
            Map<String, String> properties,
            Supplier<DmsFactory> dmsFactory,
            Supplier<JsonMapperFacade> jsonMapper,
            Supplier<PdfReaderFacade> pdfReader,
            Supplier<Validator> validator
    ) {
        this.properties = Map.copyOf(properties);
        this.dmsFactory = dmsFactory;
        this.jsonMapper = jsonMapper;
        this.pdfReader = pdfReader;
        this.validator = validator;
    }

    public static PtfActivityConfigBuilder builder() {
        return new PtfActivityConfigBuilder();
    }

    public String getProperty(String moduleCompactName, String mainKey, String defaultValue) {
        return properties.getOrDefault(COMPACT_PROP_KEY_FORMATTER.apply(moduleCompactName, mainKey), defaultValue);
    }

    public Supplier<DmsFactory> dmsFactory() {
        return dmsFactory;
    }

    public Supplier<JsonMapperFacade> jsonMapper() {
        return jsonMapper;
    }

    public Supplier<PdfReaderFacade> pdfReader() {
        return pdfReader;
    }

    public Supplier<Validator> validator() {
        return validator;
    }

    @SuppressWarnings("UnusedReturnValue")
    public static class PtfActivityConfigBuilder {

        private final Map<String, String> properties = new LinkedHashMap<>();
        private Supplier<DmsFactory> dmsFactory;
        private Supplier<JsonMapperFacade> jsonMapper;
        private Supplier<PdfReaderFacade> pdfReader;
        private Supplier<Validator> validator;

        private PtfActivityConfigBuilder() {
        }

        public PtfActivityRuntimeConfig build() {
            return new PtfActivityRuntimeConfig(
                    properties,
                    dmsFactory,
                    jsonMapper,
                    pdfReader,
                    validator
            );
        }

        public PtfActivityConfigBuilder setProperties(Map<String, String> fullKeyProperties) {
            for (Map.Entry<String, String> e : fullKeyProperties.entrySet()) {
                setProperty(e.getKey(), e.getValue());
            }
            return this;
        }

        public PtfActivityConfigBuilder setProperty(String fullKey, String value) {
            Matcher m = FULL_PROP_KEY_PATTERN.matcher(fullKey);
            if (m.matches()) {
                setProperty(m.group("Module"), m.group("MainKey"), value);
            } else {
                throw new IllegalArgumentException("Invalid property key: '%s'".formatted(fullKey));
            }
            return this;
        }

        public PtfActivityConfigBuilder setProperty(Class<? extends Module> moduleType, String mainKey, String value) {
            return setProperty(Module.compactName(moduleType), mainKey, value);
        }

        private PtfActivityConfigBuilder setProperty(String moduleKeyPart, String mainKey, String value) {
            if (propKeyIsInvalid(mainKey)) {
                throw new IllegalArgumentException("Invalid property mainKey: '%s'".formatted(mainKey));
            }
            properties.put(COMPACT_PROP_KEY_FORMATTER.apply(moduleKeyPart, mainKey), value);
            return this;
        }

        private boolean propKeyIsInvalid(String key) {
            return key == null
                   || key.isBlank()
                   || key.charAt(0) == '.'
                   || key.charAt(key.length() - 1) == '.'
                   || !key.trim().equals(key);
        }

        public PtfActivityConfigBuilder dmsFactory(Supplier<DmsFactory> dmsFactory) {
            this.dmsFactory = dmsFactory;
            return this;
        }

        public PtfActivityConfigBuilder jsonMapper(Supplier<JsonMapperFacade> jsonMapper) {
            this.jsonMapper = jsonMapper;
            return this;
        }

        public PtfActivityConfigBuilder pdfReader(Supplier<PdfReaderFacade> pdfReader) {
            this.pdfReader = pdfReader;
            return this;
        }

        public PtfActivityConfigBuilder validator(Supplier<Validator> validator) {
            this.validator = validator;
            return this;
        }
    }
}
