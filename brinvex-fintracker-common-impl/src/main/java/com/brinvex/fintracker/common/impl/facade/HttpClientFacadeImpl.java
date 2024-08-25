package com.brinvex.fintracker.common.impl.facade;


import com.brinvex.fintracker.api.exception.NotYetImplementedException;
import com.brinvex.fintracker.api.facade.HttpClientFacade;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.Charset;
import java.util.Map;

import static java.util.Collections.emptyMap;

public class HttpClientFacadeImpl implements HttpClientFacade {

    private static final class Lazy {
        private static final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public HttpResponse doGet(URI uri, Charset respCharset) throws IOException {
        return doGet(uri, emptyMap(), respCharset);
    }

    public HttpResponse doGet(URI uri, Map<String, Object> headers, Charset respCharset) throws IOException {
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(uri);
        if (headers != null && !headers.isEmpty()) {
            //Don't forget to properly handle headers having collection values
            throw new NotYetImplementedException("Request with headers not yet implemented");
        }
        java.net.http.HttpResponse<String> resp;
        try {
            resp = Lazy.httpClient.send(reqBuilder.build(), java.net.http.HttpResponse.BodyHandlers.ofString(respCharset));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Thread was interrupted while fetching %s".formatted(uri), e);
        }
        return new HttpResponse(resp.statusCode(), resp.body());
    }
}
