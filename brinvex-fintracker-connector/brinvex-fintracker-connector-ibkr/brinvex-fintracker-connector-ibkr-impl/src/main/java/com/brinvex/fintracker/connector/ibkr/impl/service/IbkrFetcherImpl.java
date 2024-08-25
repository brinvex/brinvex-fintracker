package com.brinvex.fintracker.connector.ibkr.impl.service;

import com.brinvex.fintracker.api.exception.FetchException;
import com.brinvex.fintracker.api.facade.HttpClientFacade;
import com.brinvex.fintracker.api.facade.HttpClientFacade.HttpResponse;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFetcher;
import com.brinvex.util.java.validation.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofSeconds;

@SuppressWarnings("unused")
public class IbkrFetcherImpl implements IbkrFetcher {

    private static final Logger LOG = LoggerFactory.getLogger(IbkrFetcherImpl.class);

    private final HttpClientFacade httpClientFacade;

    private static class Lazy {
        private static final Pattern HTTP_RESP1_STATUS_PATTERN = Pattern.compile("<Status>(.*)</Status>");
        private static final Pattern HTTP_RESP1_REFERENCE_CODE_PATTERN = Pattern.compile("<ReferenceCode>(.*)</ReferenceCode>");
        private static final Pattern HTTP_RESP1_URL_PATTERN = Pattern.compile("<Url>(.*)</Url>");
        private static final Pattern HTTP_RESP2_ERROR_CODE = Pattern.compile("<ErrorCode>(.+)</ErrorCode>");
    }

    public IbkrFetcherImpl(HttpClientFacade httpClientFacade) {
        this.httpClientFacade = httpClientFacade;
    }

    protected enum HttpRespStatus {
        SUCCESS,
        REPEATABLE_ERROR,
        OTHER_ERROR
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public String fetchFlexStatement(String token, String flexQueryId, int maxRepeatCount, Duration estimatedRemoteInProgressDuration) {
        Validate.notNull(token, () -> "token cannot be null");
        Validate.notNull(flexQueryId, () -> "flexQueryId cannot be null");

        String referenceCode = null;
        String baseUrl2 = null;

        {
            String urlTmpl1 = "https://www.interactivebrokers.com/Universal/servlet/FlexStatementService.SendRequest?t=%s&q=%s&v=3";
            String maskedUrl1 = urlTmpl1.formatted("TOKEN", flexQueryId);
            URI secretUrl1 = URI.create(urlTmpl1.formatted(token, flexQueryId));

            List<HttpResponse> suppressedResponses = new ArrayList<>();
            for (int i = 1; i <= maxRepeatCount; i++) {
                HttpResponse resp1;
                try {
                    LOG.debug("fetchFlexStatement - i={}/{}, url1={}, estRemoteInProgressDuration={}",
                            i, maxRepeatCount, maskedUrl1, estimatedRemoteInProgressDuration);
                    resp1 = httpClientFacade.doGet(secretUrl1, UTF_8);
                } catch (IOException e) {
                    throw new FetchException(buildFailDetail(flexQueryId, maskedUrl1, i, maxRepeatCount, null, suppressedResponses), e);
                }

                String respBody1 = resp1.body();

                HttpRespStatus httpRespStatus = checkHttpResponse(resp1);
                if (httpRespStatus == HttpRespStatus.REPEATABLE_ERROR && i < maxRepeatCount) {
                    long afterErrorWaitSeconds = Math.max(estimatedRemoteInProgressDuration.getSeconds(), 2) * i;
                    String failDetail = buildFailDetail(flexQueryId, maskedUrl1, i, maxRepeatCount, resp1, suppressedResponses);
                    LOG.debug("Repeating preparation - {}, afterErrorWaitSeconds={}", failDetail, afterErrorWaitSeconds);
                    sleep(ofSeconds(afterErrorWaitSeconds), () -> failDetail);
                    suppressedResponses.add(resp1);
                    continue;
                } else if (httpRespStatus == HttpRespStatus.OTHER_ERROR || httpRespStatus == HttpRespStatus.REPEATABLE_ERROR) {
                    throw new FetchException(buildFailDetail(flexQueryId, maskedUrl1, i, maxRepeatCount, resp1, suppressedResponses));
                }

                {
                    Matcher m1 = Lazy.HTTP_RESP1_STATUS_PATTERN.matcher(respBody1);
                    if (m1.find()) {
                        String bodyStatusCode = m1.group(1);
                        if ("Success".equals(bodyStatusCode)) {
                            Matcher m2 = Lazy.HTTP_RESP1_REFERENCE_CODE_PATTERN.matcher(respBody1);
                            if (m2.find()) {
                                referenceCode = m2.group(1);
                                Matcher m3 = Lazy.HTTP_RESP1_URL_PATTERN.matcher(respBody1);
                                if (m3.find()) {
                                    baseUrl2 = m3.group(1);
                                    break;
                                }
                            }
                        }
                    }
                }
                throw new FetchException(buildFailDetail(flexQueryId, maskedUrl1, i, maxRepeatCount, resp1, suppressedResponses));
            }
        }

        sleep(estimatedRemoteInProgressDuration, () -> buildFailDetail(flexQueryId, null, 0, 0, null, null));

        {
            String urlTmpl2 = baseUrl2 + "?q=%s&t=%s&v=3";
            String maskedUrl2 = urlTmpl2.formatted(referenceCode, "TOKEN");
            URI secretUrl2 = URI.create(urlTmpl2.formatted(referenceCode, token));
            List<HttpResponse> suppressedResponses = new ArrayList<>();
            for (int i = 1; i <= maxRepeatCount; i++) {
                HttpResponse resp2;
                try {
                    LOG.debug("fetchFlexStatement - i={}/{}, url2={}, estRemoteInProgressDuration={}",
                            i, maxRepeatCount, maskedUrl2, estimatedRemoteInProgressDuration);
                    resp2 = httpClientFacade.doGet(secretUrl2, UTF_8);
                } catch (IOException e) {
                    throw new FetchException(buildFailDetail(flexQueryId, maskedUrl2, i, maxRepeatCount, null, suppressedResponses), e);
                }
                HttpRespStatus httpRespStatus = checkHttpResponse(resp2);
                if (httpRespStatus == HttpRespStatus.REPEATABLE_ERROR && i < maxRepeatCount) {
                    long afterErrorWaitSeconds = Math.max(estimatedRemoteInProgressDuration.getSeconds(), 2) * i;
                    String failDetail = buildFailDetail(flexQueryId, maskedUrl2, i, maxRepeatCount, resp2, suppressedResponses);
                    LOG.debug("Repeating download - {}, afterErrorWaitSeconds={}", failDetail, afterErrorWaitSeconds);
                    sleep(ofSeconds(afterErrorWaitSeconds), () -> failDetail);
                    suppressedResponses.add(resp2);
                    continue;
                } else if (httpRespStatus == HttpRespStatus.OTHER_ERROR || httpRespStatus == HttpRespStatus.REPEATABLE_ERROR) {
                    throw new FetchException(buildFailDetail(flexQueryId, maskedUrl2, i, maxRepeatCount, resp2, suppressedResponses));
                }
                return resp2.body();
            }
        }
        throw new AssertionError("Unreachable");
    }

    private void sleep(Duration duration, Supplier<String> exceptionDetail) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(exceptionDetail.get(), e);
        }
    }

    private String buildFailDetail(
            String flexQueryId,
            String maskedUrl,
            int repeatCount,
            int maxRepeatCount,
            HttpResponse newestResponse,
            List<HttpResponse> suppressedErrorResponses
    ) {
        newestResponse = newestResponse == null ? null : new HttpResponse(newestResponse.status(), newestResponse.body().replace("\n", ""));
        StringBuilder sb = new StringBuilder();
        sb.append("Fetch failed - ")
                .append("flexQueryId=").append(flexQueryId)
                .append(", i=").append(repeatCount).append("/").append(maxRepeatCount)
                .append(", url=").append(maskedUrl)
                .append(", newestResponse=").append(newestResponse);
        if (suppressedErrorResponses != null) {
            for (int i = 0; i < suppressedErrorResponses.size(); i++) {
                HttpResponse suppResponse = suppressedErrorResponses.get(i);
                suppResponse = new HttpResponse(suppResponse.status(), suppResponse.body().replace("\n", ""));
                sb.append(", %s.response=".formatted(i + 1)).append(suppResponse);
            }
        }
        return sb.toString();
    }

    /*
    1001	Statement could not be generated at this time. Please try again shortly.
    1003	Statement is not available.
    1004	Statement is incomplete at this time. Please try again shortly.
    1005	Settlement data is not ready at this time. Please try again shortly.
    1006	FIFO P/L data is not ready at this time. Please try again shortly.
    1007	MTM P/L data is not ready at this time. Please try again shortly.
    1008	MTM and FIFO P/L data is not ready at this time. Please try again shortly.
    1009	The server is under heavy load. Statement could not be generated at this time. Please try again shortly.
    1010	Legacy Flex Queries are no longer supported. Please convert over to Activity Flex.
    1011	Service account is inactive.
    1012	Token has expired.
    1013	IP restriction.
    1014	Query is invalid.
    1015	Token is invalid.
    1016	Account in invalid.
    1017	Reference code is invalid.
    1018	Too many requests have been made from this token. Please try again shortly. Limited to one request per second, 10 requests per minute (per token).
    1019	Statement generation in progress. Please try again shortly.
    1020	Invalid request or unable to validate request.
    1021	Statement could not be retrieved at this time. Please try again shortly.
     */
    protected HttpRespStatus checkHttpResponse(HttpResponse resp) {
        int status = resp.status();
        String body = resp.body();
        if (status == 500 && body.contains("We are sorry, our services are temporarily unavailable")) {
            return HttpRespStatus.REPEATABLE_ERROR;
        }

        Set<Integer> repeatableErrCodes = Set.of(
                1001, 1003, 1004, 1005, 1006, 1007, 1008, 1009,
                1018, 1019,
                1021
        );
        Matcher m = Lazy.HTTP_RESP2_ERROR_CODE.matcher(body);
        if (m.find()) {
            int errCode = Integer.parseInt(m.group(1));
            if (repeatableErrCodes.contains(errCode)) {
                return HttpRespStatus.REPEATABLE_ERROR;
            } else {
                return HttpRespStatus.OTHER_ERROR;
            }
        }
        return HttpRespStatus.SUCCESS;
    }

}
