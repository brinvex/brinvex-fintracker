package com.brinvex.ptfactivity.core.api.facade;

import java.nio.file.Path;

public interface JsonMapperFacade {

    <T> T readFromJson(Path jsonFilePath, Class<T> type);

    <T> T readFromJson(String jsonContent, Class<T> type);

}
