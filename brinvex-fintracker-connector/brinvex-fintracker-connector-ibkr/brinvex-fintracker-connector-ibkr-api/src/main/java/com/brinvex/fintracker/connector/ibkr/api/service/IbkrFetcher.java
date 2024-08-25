package com.brinvex.fintracker.connector.ibkr.api.service;

import java.time.Duration;

public interface IbkrFetcher {

    /**
     * See also
     * <a href="https://www.interactivebrokers.co.in/en/?f=asr_statements_tradeconfirmations&p=flexqueries4">
     * https://www.interactivebrokers.co.in/en/?f=asr_statements_tradeconfirmations&p=flexqueries4</a>
     *
     * @param estimatedRemoteInProgressTime as of 2024-08-11 it seems that the minimal time period needed
     *                                      to prevent the error '1019 Statement generation in progress. Please try again shortly.' is:
     *                                      4s for an ActivityFlexStatement
     *                                      0s for a TradeConfirmation
     */
    String fetchFlexStatement(String token, String flexQueryId, int maxRepeatCount, Duration estimatedRemoteInProgressTime);
}
