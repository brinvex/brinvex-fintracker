package com.brinvex.ptfactivity.connector.fiob.internal.service;

import com.brinvex.ptfactivity.connector.fiob.api.model.FiobAccountType;
import com.brinvex.ptfactivity.connector.fiob.api.service.FiobFetcher;
import com.brinvex.ptfactivity.core.api.domain.Account;
import com.brinvex.ptfactivity.core.api.exception.AssistanceRequiredException;
import com.brinvex.ptfactivity.core.api.exception.FetchException;
import com.brinvex.java.validation.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.LocalDate;

import static java.nio.charset.StandardCharsets.UTF_8;

public class FiobFetcherImpl implements FiobFetcher {

    private static final Logger LOG = LoggerFactory.getLogger(FiobFetcherImpl.class);

    private final HttpClient httpClient;

    public FiobFetcherImpl() {
        httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Override
    public String fetchTransStatement(Account account, LocalDate fromDateIncl, LocalDate toDateIncl) {
        LOG.debug("fetchTransStatement({}/{}, {}-{})", account.externalId(), account.name(), fromDateIncl, toDateIncl);
        return switch (FiobAccountType.valueOf(account.type())) {
            case SAVING -> {
                String url = Lazy.FIOB_BANK_API_URL_FMT.formatted(account.credentials(), fromDateIncl, toDateIncl);
                try {
                    HttpRequest req = HttpRequest.newBuilder(URI.create(url)).build();
                    HttpResponse<String> resp = httpClient.send(req, BodyHandlers.ofString(UTF_8));
                    String content = resp.body();
                    boolean isFail = content.isBlank();
                    if (!isFail) {
                        if (content.startsWith("Data není možné poskytnout bez silné autorizace.")) {
                            //Use https://www.fio.cz/ib_api/rest/periods/!!!API_KEY!!!/2024-10-01/2024-11-15/transactions.xml
                            throw new AssistanceRequiredException((
                                    "Option 1: Internet Banking -> Settings -> API -> Tokens -> Temporary disclosure of complete data accessible by API token; " +
                                    "Option 2: Manually retrieve the missed statements and upload them to DMS; " +
                                    "externalId=%s, name=%s, fromDateIncl=%s, toDateIncl=%s; " +
                                    "%s")
                                    .formatted(account.externalId(), account.name(), fromDateIncl, toDateIncl, content));
                        }
                    }
                    if (!isFail) {
                        isFail = content.contains("<status>error</status>");
                        if (isFail) {
                            Assert.isTrue(content.contains("<errorCode>15</errorCode>"));
                            Assert.isTrue(content.contains("<message>Služba je momentálně nedostupná</message>"));
                        }
                    }
                    if (isFail) {
                        throw new FetchException("%s/%s %s-%s, content=%s".formatted(account.externalId(), account.name(), fromDateIncl, toDateIncl, content));
                    }
                    yield content;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            case TRADING -> null;
        };
    }

    @Override
    public String fetchSnapshotStatement(Account account, LocalDate date) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FiobFetcher newSessionReusingFetcher() {
        throw new UnsupportedOperationException();
    }

    private static class Lazy {
        private static final String FIOB_BANK_API_URL_FMT = "https://www.fio.cz/ib_api/rest/periods/%s/%s/%s/transactions.xml";
    }

}
