package com.brinvex.fintracker.core.api.facade;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

public interface HttpClientFacade {

    record HttpResponse(int status, String body) {
    }

    default HttpResponse doGet(URI uri, Charset respCharset) throws IOException {
        return doGet(uri, Collections.emptyMap(), respCharset);
    }

    HttpResponse doGet(URI uri, Map<String, Object> headers, Charset respCharset) throws IOException;

}
