package com.brinvex.ptfactivity.core.internal.facade;

import com.brinvex.ptfactivity.core.api.facade.JsonMapperFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

public class JsonMapperFacadeImpl implements JsonMapperFacade {

    private static class Lazy {
        private static final ObjectMapper jsonObjectMapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
    }

    @Override
    public <T> T readFromJson(Path jsonFilePath, Class<T> type) {
        try {
            return Lazy.jsonObjectMapper.readValue(jsonFilePath.toFile(), type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <T> T readFromJson(String jsonContent, Class<T> type) {
        try {
            return Lazy.jsonObjectMapper.readValue(jsonContent, type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
