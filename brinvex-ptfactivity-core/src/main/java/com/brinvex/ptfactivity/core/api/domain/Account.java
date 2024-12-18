package com.brinvex.ptfactivity.core.api.domain;

import com.brinvex.finance.types.enu.Currency;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNullElse;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;

public record Account(
        String name,
        String type,
        Currency ccy,
        LocalDate openDate,
        LocalDate closeDate,
        String externalId,
        String credentials,
        Map<String, String> extraProps
) {
    public Account {
        if (externalId != null && externalId.isBlank()) {
            throw new IllegalArgumentException("externalId must not be blank");
        }
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (externalId == null && name == null) {
            throw new IllegalArgumentException("at least one of [externalId, name] must not be null");
        }
        if (type != null && type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        if (ccy == null) {
            throw new IllegalArgumentException("ccy must not be null");
        }
        if (openDate == null) {
            throw new IllegalArgumentException("openDate must not be null");
        }
        if (closeDate != null && !openDate.isBefore(closeDate)) {
            throw new IllegalArgumentException("openDate must be before closeDate, given: %s, %s".formatted(openDate, closeDate));
        }
        if (credentials != null && credentials.isBlank()) {
            throw new IllegalArgumentException("credentials must not be blank");
        }
        extraProps = extraProps == null ? Map.of() : Map.copyOf(extraProps);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Account.class.getSimpleName() + "[", "]")
                .add("name=" + name)
                .add("type=" + type)
                .add("ccy=" + ccy)
                .add("openDate=" + openDate)
                .add("closeDate=" + closeDate)
                .add("externalId=" + externalId)
                .add("credentials=***")
                .add("extraProps=" + extraProps)
                .toString();
    }

    public static Account of(Map<String, String> props) {
        props = new LinkedHashMap<>(props);

        Long id = ofNullable(props.remove("id")).filter(not(String::isEmpty)).map(Long::parseLong).orElse(null);
        String name = ofNullable(props.remove("name")).filter(not(String::isEmpty)).orElse(null);
        String externalId = ofNullable(props.remove("externalId")).filter(not(String::isEmpty)).orElse(null);
        if (id == null && name == null && externalId == null) {
            return null;
        }
        String type = ofNullable(props.remove("type")).filter(not(String::isBlank)).orElse(null);
        Currency ccy = ofNullable(props.remove("ccy")).filter(not(String::isBlank)).map(Currency::valueOf).orElse(null);
        LocalDate openDate = ofNullable(props.remove("openDate")).filter(not(String::isBlank)).map(LocalDate::parse).orElse(null);
        LocalDate closeDate = ofNullable(props.remove("closeDate")).filter(not(String::isBlank)).map(LocalDate::parse).orElse(null);
        String credentials = ofNullable(props.remove("credentials")).filter(not(String::isBlank)).orElse(null);

        return new Account(
                name,
                type,
                ccy,
                openDate,
                closeDate,
                externalId,
                credentials,
                props
        );
    }

    public Map<String, String> toProps() {
        LinkedHashMap<String, String> props = new LinkedHashMap<>(extraProps);
        props.put("name", requireNonNullElse(name, ""));
        props.put("type", requireNonNullElse(type, ""));
        props.put("ccy", ccy.name());
        props.put("openDate", requireNonNullElse(openDate, "").toString());
        props.put("closeDate", requireNonNullElse(closeDate, "").toString());
        props.put("externalId", requireNonNullElse(externalId, ""));
        props.put("credentials", requireNonNullElse(credentials, ""));
        extraProps.forEach(props::putIfAbsent);
        return unmodifiableMap(props);
    }

    public Account withExtraProp(String key, String value) {
        LinkedHashMap<String, String> modifiedExtraProps = new LinkedHashMap<>(extraProps);
        if (value == null) {
            modifiedExtraProps.remove(key);
        } else {
            modifiedExtraProps.put(key, value);
        }
        return new Account(name, type, ccy, openDate, closeDate, externalId, credentials, modifiedExtraProps);
    }
}
