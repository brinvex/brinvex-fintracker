package com.brinvex.ptfactivity.connector.ibkr.internal.service;


import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.BuySell;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransactionType;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CorporateAction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CorporateActionType;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.EquitySummary;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatement;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.FlexStatementType;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.SecurityIDType;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.Trade;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.TradeConfirm;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.TradeType;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrStatementParser;
import com.brinvex.ptfactivity.connector.ibkr.internal.builder.ActivityStatementBuilder;
import com.brinvex.ptfactivity.connector.ibkr.internal.builder.CashTransactionBuilder;
import com.brinvex.ptfactivity.connector.ibkr.internal.builder.CorporateActionBuilder;
import com.brinvex.ptfactivity.connector.ibkr.internal.builder.EquitySummaryBuilder;
import com.brinvex.ptfactivity.connector.ibkr.internal.builder.TradeBuilder;
import com.brinvex.ptfactivity.connector.ibkr.internal.builder.TradeConfirmBuilder;
import com.brinvex.ptfactivity.connector.ibkr.internal.builder.TradeConfirmStatementBuilder;
import com.brinvex.java.validation.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({"DuplicatedCode", "unused"})
public class IbkrStatementParserImpl implements IbkrStatementParser {

    @Override
    public LocalDateTime parseStatementCreatedOn(List<String> statementLines) {
        String line3 = statementLines.get(2);
        Matcher m = Lazy.WHEN_GENERATED_PATTERN.matcher(line3);
        if (m.find()) {
            ZonedDateTime zonedDateTime = parseZonedDateTime(m.group(1));
            return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        }
        throw new IllegalArgumentException("Could not parse whenGenerated from line: %s".formatted(line3));
    }

    @Override
    public FlexStatement.ActivityStatement parseActivityStatement(String statementXmlContent) {

        ActivityStatementBuilder statementBldr = null;

        List<CashTransaction> cashTransactions = new LinkedList<>();
        List<CorporateAction> corporateActions = new LinkedList<>();
        List<EquitySummary> equitySummaries = new LinkedList<>();
        List<Trade> trades = new LinkedList<>();

        try {
            XMLEventReader reader = Lazy.xmlInputFactory.createXMLEventReader(new StringReader(statementXmlContent));
            while (reader.hasNext()) {
                XMLEvent xmlEvent = reader.nextEvent();
                if (!xmlEvent.isStartElement()) {
                    continue;
                }
                StartElement e = xmlEvent.asStartElement();
                switch (e.getName().getLocalPart()) {
                    case "FlexQueryResponse" -> {
                        FlexStatementType flexStatementType = parseEnum(FlexStatementType::fromValue, getAttrValue(e, FlexQueryResponseQN.type));
                        Assert.equal(FlexStatementType.ACT, flexStatementType);
                    }
                    case "FlexStatement" -> {
                        Assert.isNull(statementBldr);
                        statementBldr = new ActivityStatementBuilder()
                                .accountId(getAttrValue(e, FlexStatementQN.accountId))
                                .fromDate(parseDate(getAttrValue(e, FlexStatementQN.fromDate)))
                                .toDate(parseDate(getAttrValue(e, FlexStatementQN.toDate)))
                                .whenGenerated(parseZonedDateTime(getAttrValue(e, FlexStatementQN.whenGenerated)))
                                .corporateActions(corporateActions)
                                .cashTransactions(cashTransactions)
                                .trades(trades)
                                .equitySummaries(equitySummaries);
                    }
                    case "Trade" -> trades.add(new TradeBuilder()
                            .currency(parseEnum(Currency::valueOf, getAttrValue(e, TradeQN.currency)))
                            .assetCategory(parseEnum(AssetCategory::fromValue, getAttrValue(e, TradeQN.assetCategory)))
                            .assetSubCategory(parseEnum(AssetSubCategory::fromValue, getAttrValue(e, TradeQN.subCategory)))
                            .symbol(getAttrValue(e, TradeQN.symbol))
                            .description(getAttrValue(e, TradeQN.description))
                            .securityID(getAttrValue(e, TradeQN.securityID))
                            .securityIDType(parseEnum(SecurityIDType::fromValue, getAttrValue(e, TradeQN.securityIDType)))
                            .figi(getAttrValue(e, TradeQN.figi))
                            .isin(getAttrValue(e, TradeQN.isin))
                            .listingExchange(getAttrValue(e, TradeQN.listingExchange))
                            .tradeID(getAttrValue(e, TradeQN.tradeID))
                            .reportDate(parseDate(getAttrValue(e, TradeQN.reportDate)))
                            .tradeDate(parseDate(getAttrValue(e, TradeQN.tradeDate)))
                            .settleDateTarget(parseDate(getAttrValue(e, TradeQN.settleDateTarget)))
                            .transactionType(parseEnum(TradeType::fromValue, getAttrValue(e, TradeQN.transactionType)))
                            .exchange(getAttrValue(e, TradeQN.exchange))
                            .quantity(getAttrBigDecimal(e, TradeQN.quantity))
                            .tradePrice(getAttrBigDecimal(e, TradeQN.tradePrice))
                            .tradeMoney(getAttrBigDecimal(e, TradeQN.tradeMoney))
                            .proceeds(getAttrBigDecimal(e, TradeQN.proceeds))
                            .taxes(getAttrBigDecimal(e, TradeQN.taxes))
                            .ibCommission(getAttrBigDecimal(e, TradeQN.ibCommission))
                            .ibCommissionCurrency(parseEnum(Currency::valueOf, getAttrValue(e, TradeQN.ibCommissionCurrency)))
                            .netCash(getAttrBigDecimal(e, TradeQN.netCash))
                            .cost(getAttrBigDecimal(e, TradeQN.cost))
                            .buySell(parseEnum(BuySell::fromValue, getAttrValue(e, TradeQN.buySell)))
                            .transactionID(getAttrValue(e, TradeQN.transactionID))
                            .ibOrderID(getAttrValue(e, TradeQN.ibOrderID))
                            .extraDateTimeStr(getAttrValue(e, TradeQN.dateTime))
                            .orderTime(parseZonedDateTime(getAttrValue(e, TradeQN.orderTime)))
                            .build());
                    case "CashTransaction" -> cashTransactions.add(new CashTransactionBuilder()
                            .currency(parseEnum(Currency::valueOf, getAttrValue(e, CashTransactionQN.currency)))
                            .fxRateToBase(getAttrBigDecimal(e, CashTransactionQN.fxRateToBase))
                            .symbol(getAttrValue(e, CashTransactionQN.symbol))
                            .listingExchange(getAttrValue(e, CashTransactionQN.listingExchange))
                            .assetCategory(parseEnum(AssetCategory::fromValue, getAttrValue(e, CashTransactionQN.assetCategory)))
                            .assetSubCategory(parseEnum(AssetSubCategory::fromValue, getAttrValue(e, CashTransactionQN.subCategory)))
                            .figi(getAttrValue(e, CashTransactionQN.figi))
                            .isin(getAttrValue(e, CashTransactionQN.isin))
                            .securityID(getAttrValue(e, CashTransactionQN.securityID))
                            .securityIDType(parseEnum(SecurityIDType::fromValue, getAttrValue(e, CashTransactionQN.securityIDType)))
                            .description(getAttrValue(e, CashTransactionQN.description))
                            .settleDate(parseDate(getAttrValue(e, CashTransactionQN.settleDate)))
                            .amount(getAttrBigDecimal(e, CashTransactionQN.amount))
                            .type(parseEnum(CashTransactionType::fromValue, getAttrValue(e, CashTransactionQN.type)))
                            .transactionID(getAttrValue(e, CashTransactionQN.transactionID))
                            .reportDate(parseDate(getAttrValue(e, CashTransactionQN.reportDate)))
                            .actionID(getAttrValue(e, CashTransactionQN.actionID))
                            .extraDateTimeStr(getAttrValue(e, CashTransactionQN.dateTime))
                            .build());
                    case "CorporateAction" -> corporateActions.add(new CorporateActionBuilder()
                            .reportDate(parseDate(getAttrValue(e, CorporateActionQN.reportDate)))
                            .assetCategory(parseEnum(AssetCategory::fromValue, getAttrValue(e, CorporateActionQN.assetCategory)))
                            .assetSubCategory(parseEnum(AssetSubCategory::fromValue, getAttrValue(e, CorporateActionQN.subCategory)))
                            .symbol(getAttrValue(e, CorporateActionQN.symbol))
                            .type(parseEnum(CorporateActionType::fromValue, getAttrValue(e, CorporateActionQN.type)))
                            .currency(parseEnum(Currency::valueOf, getAttrValue(e, CorporateActionQN.currency)))
                            .amount(getAttrBigDecimal(e, CorporateActionQN.amount))
                            .value(getAttrBigDecimal(e, CorporateActionQN.value))
                            .quantity(getAttrBigDecimal(e, CorporateActionQN.quantity))
                            .proceeds(getAttrBigDecimal(e, CorporateActionQN.proceeds))
                            .description(getAttrValue(e, CorporateActionQN.description))
                            .securityID(getAttrValue(e, CorporateActionQN.securityID))
                            .securityIDType(parseEnum(SecurityIDType::fromValue, getAttrValue(e, CorporateActionQN.securityIDType)))
                            .figi(getAttrValue(e, CorporateActionQN.figi))
                            .isin(getAttrValue(e, CorporateActionQN.isin))
                            .listingExchange(getAttrValue(e, CorporateActionQN.listingExchange))
                            .issuerCountryCode(getAttrValue(e, CorporateActionQN.issuerCountryCode))
                            .extraDateTimeStr(getAttrValue(e, CorporateActionQN.dateTime))
                            .transactionId(getAttrValue(e, CorporateActionQN.transactionID))
                            .actionID(getAttrValue(e, CorporateActionQN.actionID))
                            .build());
                    case "EquitySummaryByReportDateInBase" -> equitySummaries.add(new EquitySummaryBuilder()
                            .currency(parseEnum(Currency::valueOf, getAttrValue(e, EquitySummaryQN.currency)))
                            .reportDate(parseDate(getAttrValue(e, EquitySummaryQN.reportDate)))
                            .cash(getAttrBigDecimal(e, EquitySummaryQN.cash))
                            .stock(getAttrBigDecimal(e, EquitySummaryQN.stock))
                            .dividendAccruals(getAttrBigDecimal(e, EquitySummaryQN.dividendAccruals))
                            .interestAccruals(getAttrBigDecimal(e, EquitySummaryQN.interestAccruals))
                            .total(getAttrBigDecimal(e, EquitySummaryQN.total))
                            .build());
                    case "Transfer" -> {
                        AssetCategory assetCat = parseEnum(AssetCategory::fromValue, getAttrValue(e, TransferQN.assetCategory));
                        String type = getAttrValue(e, TransferQN.type);
                        if (assetCat.equals(AssetCategory.CASH) && type.equals("INTERNAL")) {
                            continue;
                        }
                        if (assetCat.equals(AssetCategory.STK) && type.equals("INTERCOMPANY")) {
                            continue;
                        }
                        throw new IllegalStateException("Unsupported transfer: %s, %s".formatted(assetCat, type));
                    }
                }
            }
        } catch (XMLStreamException e) {
            Lazy.LOG.error("XMLStreamException while parsing: %s".formatted(statementXmlContent), e);
            throw new RuntimeException(e);
        }
        if (statementBldr == null) {
            Lazy.LOG.error("Expected node 'FlexStatement' was not found while parsing ActivityStatement: {}", statementXmlContent);
            throw new IllegalArgumentException("Expected node 'FlexStatement' was not found while parsing ActivityStatement");
        }
        return statementBldr.build();
    }

    @Override
    public FlexStatement.TradeConfirmStatement parseTradeConfirmStatement(String statementXmlContent) {

        TradeConfirmStatementBuilder statementBldr = null;

        List<TradeConfirm> tradeConfirms = new LinkedList<>();

        try {
            XMLEventReader reader = Lazy.xmlInputFactory.createXMLEventReader(new StringReader(statementXmlContent));
            while (reader.hasNext()) {
                XMLEvent xmlEvent = reader.nextEvent();
                if (!xmlEvent.isStartElement()) {
                    continue;
                }
                StartElement e = xmlEvent.asStartElement();
                switch (e.getName().getLocalPart()) {
                    case "FlexQueryResponse" -> {
                        FlexStatementType flexStatementType = parseEnum(FlexStatementType::fromValue, getAttrValue(e, FlexQueryResponseQN.type));
                        Assert.equal(FlexStatementType.TC, flexStatementType);
                    }
                    case "FlexStatement" -> {
                        Assert.isNull(statementBldr);
                        statementBldr = new TradeConfirmStatementBuilder()
                                .accountId(getAttrValue(e, FlexStatementQN.accountId))
                                .fromDate(parseDate(getAttrValue(e, FlexStatementQN.fromDate)))
                                .toDate(parseDate(getAttrValue(e, FlexStatementQN.toDate)))
                                .whenGenerated(parseZonedDateTime(getAttrValue(e, FlexStatementQN.whenGenerated)))
                                .tradeConfirmations(tradeConfirms);
                    }
                    case "TradeConfirm" -> tradeConfirms.add(new TradeConfirmBuilder()
                            .currency(parseEnum(Currency::valueOf, getAttrValue(e, TradeConfirmQN.currency)))
                            .assetCategory(parseEnum(AssetCategory::fromValue, getAttrValue(e, TradeConfirmQN.assetCategory)))
                            .assetSubCategory(parseEnum(AssetSubCategory::fromValue, getAttrValue(e, TradeConfirmQN.subCategory)))
                            .symbol(getAttrValue(e, TradeConfirmQN.symbol))
                            .description(getAttrValue(e, TradeConfirmQN.description))
                            .securityID(getAttrValue(e, TradeConfirmQN.securityID))
                            .securityIDType(parseEnum(SecurityIDType::fromValue, getAttrValue(e, TradeConfirmQN.securityIDType)))
                            .figi(getAttrValue(e, TradeConfirmQN.figi))
                            .isin(getAttrValue(e, TradeConfirmQN.isin))
                            .listingExchange(getAttrValue(e, TradeConfirmQN.listingExchange))
                            .tradeID(getAttrValue(e, TradeConfirmQN.tradeID))
                            .reportDate(parseDate(getAttrValue(e, TradeConfirmQN.reportDate)))
                            .tradeDate(parseDate(getAttrValue(e, TradeConfirmQN.tradeDate)))
                            .settleDate(parseDate(getAttrValue(e, TradeConfirmQN.settleDate)))
                            .transactionType(parseEnum(TradeType::fromValue, getAttrValue(e, TradeConfirmQN.transactionType)))
                            .exchange(getAttrValue(e, TradeConfirmQN.exchange))
                            .quantity(getAttrBigDecimal(e, TradeConfirmQN.quantity))
                            .price(getAttrBigDecimal(e, TradeConfirmQN.price))
                            .amount(getAttrBigDecimal(e, TradeConfirmQN.amount))
                            .proceeds(getAttrBigDecimal(e, TradeConfirmQN.proceeds))
                            .netCash(getAttrBigDecimal(e, TradeConfirmQN.netCash))
                            .commission(getAttrBigDecimal(e, TradeConfirmQN.commission))
                            .commissionCurrency(parseEnum(Currency::valueOf, getAttrValue(e, TradeConfirmQN.commissionCurrency)))
                            .tax(getAttrBigDecimal(e, TradeConfirmQN.tax))
                            .buySell(parseEnum(BuySell::fromValue, getAttrValue(e, TradeConfirmQN.buySell)))
                            .orderID(getAttrValue(e, TradeConfirmQN.orderID))
                            .extraDateTimeStr(getAttrValue(e, TradeConfirmQN.dateTime))
                            .orderTime(parseZonedDateTime(getAttrValue(e, TradeConfirmQN.orderTime)))
                            .build());
                }
            }

        } catch (XMLStreamException e) {
            Lazy.LOG.error("XMLStreamException while parsing: %s".formatted(statementXmlContent), e);
            throw new RuntimeException(e);
        }
        if (statementBldr == null) {
            Lazy.LOG.error("Expected node 'FlexStatement' was not found while parsing TradeConfirmStatement: {}", statementXmlContent);
            throw new IllegalArgumentException("Expected node 'FlexStatement' was not found while parsing TradeConfirmStatement");
        }
        return statementBldr.build();
    }

    private String getAttrValue(StartElement e, QName attributeName) {
        return e.getAttributeByName(attributeName).getValue();
    }

    private BigDecimal getAttrBigDecimal(StartElement e, QName attributeName) {
        return new BigDecimal(getAttrValue(e, attributeName));
    }

    private <E extends Enum<E>> E parseEnum(Function<String, E> strToEnumFnc, String str) {
        return str == null || str.isBlank() ? null : strToEnumFnc.apply(str);
    }

    private ZonedDateTime parseZonedDateTime(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        if (str.indexOf(';') > -1) {
            return ZonedDateTime.parse(str, Lazy.ibkrDtf);
        } else {
            throw new IllegalArgumentException("Unexpected format: " + str);
        }
    }

    private LocalDate parseDate(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        return LocalDate.parse(str, Lazy.ibkrDf);
    }

    private static class FlexQueryResponseQN {
        static final QName type = new QName("type");
    }

    private static class FlexStatementQN {
        static final QName accountId = new QName("accountId");
        static final QName fromDate = new QName("fromDate");
        static final QName toDate = new QName("toDate");
        static final QName whenGenerated = new QName("whenGenerated");
    }

    private static class CashTransactionQN {
        static final QName currency = new QName("currency");
        static final QName fxRateToBase = new QName("fxRateToBase");
        static final QName description = new QName("description");
        static final QName symbol = new QName("symbol");
        static final QName listingExchange = new QName("listingExchange");
        static final QName assetCategory = new QName("assetCategory");
        static final QName subCategory = new QName("subCategory");
        static final QName figi = new QName("figi");
        static final QName isin = new QName("isin");
        static final QName securityID = new QName("securityID");
        static final QName securityIDType = new QName("securityIDType");
        static final QName dateTime = new QName("dateTime");
        static final QName settleDate = new QName("settleDate");
        static final QName amount = new QName("amount");
        static final QName type = new QName("type");
        static final QName transactionID = new QName("transactionID");
        static final QName reportDate = new QName("reportDate");
        static final QName actionID = new QName("actionID");
    }

    private static class TradeQN {
        static final QName currency = new QName("currency");
        static final QName assetCategory = new QName("assetCategory");
        static final QName subCategory = new QName("subCategory");
        static final QName symbol = new QName("symbol");
        static final QName description = new QName("description");
        static final QName securityID = new QName("securityID");
        static final QName securityIDType = new QName("securityIDType");
        static final QName figi = new QName("figi");
        static final QName isin = new QName("isin");
        static final QName listingExchange = new QName("listingExchange");
        static final QName tradeID = new QName("tradeID");
        static final QName reportDate = new QName("reportDate");
        static final QName dateTime = new QName("dateTime");
        static final QName tradeDate = new QName("tradeDate");
        static final QName settleDateTarget = new QName("settleDateTarget");
        static final QName transactionType = new QName("transactionType");
        static final QName exchange = new QName("exchange");
        static final QName quantity = new QName("quantity");
        static final QName tradePrice = new QName("tradePrice");
        static final QName tradeMoney = new QName("tradeMoney");
        static final QName proceeds = new QName("proceeds");
        static final QName taxes = new QName("taxes");
        static final QName ibCommission = new QName("ibCommission");
        static final QName ibCommissionCurrency = new QName("ibCommissionCurrency");
        static final QName netCash = new QName("netCash");
        static final QName cost = new QName("cost");
        static final QName buySell = new QName("buySell");
        static final QName transactionID = new QName("transactionID");
        static final QName ibOrderID = new QName("ibOrderID");
        static final QName orderTime = new QName("orderTime");
    }

    private static class TradeConfirmQN {
        static final QName currency = new QName("currency");
        static final QName assetCategory = new QName("assetCategory");
        static final QName subCategory = new QName("subCategory");
        static final QName symbol = new QName("symbol");
        static final QName description = new QName("description");
        static final QName securityID = new QName("securityID");
        static final QName securityIDType = new QName("securityIDType");
        static final QName figi = new QName("figi");
        static final QName isin = new QName("isin");
        static final QName listingExchange = new QName("listingExchange");
        static final QName tradeID = new QName("tradeID");
        static final QName reportDate = new QName("reportDate");
        static final QName dateTime = new QName("dateTime");
        static final QName tradeDate = new QName("tradeDate");
        static final QName settleDate = new QName("settleDate");
        static final QName transactionType = new QName("transactionType");
        static final QName exchange = new QName("exchange");
        static final QName quantity = new QName("quantity");
        static final QName price = new QName("price");
        static final QName amount = new QName("amount");
        static final QName proceeds = new QName("proceeds");
        static final QName netCash = new QName("netCash");
        static final QName tax = new QName("tax");
        static final QName commission = new QName("commission");
        static final QName commissionCurrency = new QName("commissionCurrency");
        static final QName buySell = new QName("buySell");
        static final QName orderID = new QName("orderID");
        static final QName orderTime = new QName("orderTime");
    }

    private static class CorporateActionQN {
        static final QName currency = new QName("currency");
        static final QName assetCategory = new QName("assetCategory");
        static final QName subCategory = new QName("subCategory");
        static final QName symbol = new QName("symbol");
        static final QName description = new QName("description");
        static final QName securityID = new QName("securityID");
        static final QName securityIDType = new QName("securityIDType");
        static final QName figi = new QName("figi");
        static final QName isin = new QName("isin");
        static final QName listingExchange = new QName("listingExchange");
        static final QName issuerCountryCode = new QName("issuerCountryCode");
        static final QName reportDate = new QName("reportDate");
        static final QName dateTime = new QName("dateTime");
        static final QName type = new QName("type");
        static final QName quantity = new QName("quantity");
        static final QName amount = new QName("amount");
        static final QName proceeds = new QName("proceeds");
        static final QName value = new QName("value");
        static final QName transactionID = new QName("transactionID");
        static final QName actionID = new QName("actionID");
    }

    private static class EquitySummaryQN {
        static final QName currency = new QName("currency");
        static final QName reportDate = new QName("reportDate");
        static final QName cash = new QName("cash");
        static final QName stock = new QName("stock");
        static final QName dividendAccruals = new QName("dividendAccruals");
        static final QName interestAccruals = new QName("interestAccruals");
        static final QName total = new QName("total");
    }

    private static class TransferQN {
        static final QName assetCategory = new QName("assetCategory");
        static final QName type = new QName("type");
    }

    private static class Lazy {
        private static final Logger LOG = LoggerFactory.getLogger(IbkrStatementParserImpl.class);

        private static final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

        private static final DateTimeFormatter ibkrDf = DateTimeFormatter.ofPattern("yyyyMMdd");
        // 20230727;052240 EDT == 2023-07-27T05:22:40-04:00[America/New_York]
        private static final DateTimeFormatter ibkrDtf = DateTimeFormatter.ofPattern("yyyyMMdd;HHmmss z");

        private static final Pattern WHEN_GENERATED_PATTERN = Pattern.compile("whenGenerated=\"(.*?)\"");
    }


}
