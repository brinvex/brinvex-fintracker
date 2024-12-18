package com.brinvex.ptfactivity.connector.fiob.internal.service.mapper;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.fiob.api.model.FiobTradingTransactionType;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.Lang;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.TradingTransaction;
import com.brinvex.ptfactivity.connector.fiob.api.model.statement.TradingTransactionDirection;
import com.brinvex.ptfactivity.connector.fiob.internal.service.parser.FiobParsingUtil;
import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction.FinTransactionBuilder;
import com.brinvex.ptfactivity.core.api.domain.enu.AssetType;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.java.Num;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.brinvex.java.NullUtil.coalesce;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ZERO;
import static java.util.Objects.requireNonNullElse;

@SuppressWarnings({"DuplicatedCode", "SpellCheckingInspection"})
public class FiobTradingTransactionMapper {

    public List<FinTransaction> mapTransactions(List<TradingTransaction> rawTrans, Lang lang) {
        return new StatefullTransactionMapper(rawTrans, lang).getResultTrans();
    }

    private static class StatefullTransactionMapper {

        private List<FinTransaction> resultTrans;

        private final List<TradingTransaction> rawTransToProcess;

        private final Lang lang;

        private final Map<String, String> symbolCountryMap = new LinkedHashMap<>();

        private StatefullTransactionMapper(List<TradingTransaction> rawTrans, Lang lang) {
            this.resultTrans = null;
            this.rawTransToProcess = new LinkedList<>(rawTrans);
            this.lang = lang;
        }

        public List<FinTransaction> getResultTrans() {
            if (resultTrans == null) {
                processAll();
            }
            return resultTrans;
        }

        private void processAll() {
            resultTrans = new LinkedList<>();
            while (!rawTransToProcess.isEmpty()) {
                int sizeBefore = rawTransToProcess.size();
                processOneGroup();
                int sizeAfter = rawTransToProcess.size();
                Assert.isTrue(sizeBefore > sizeAfter);
            }
        }

        @SuppressWarnings({"DataFlowIssue"})
        private void processOneGroup() {

            LinkedList<FinTransaction.FinTransactionBuilder> groupTrans = new LinkedList<>();

            TradingTransaction rawTran = rawTransToProcess.removeFirst();
            FiobTradingTransactionType extraType = detectTranType(rawTran, lang);
            FinTransactionBuilder tranBuilder = initTranBuilder(extraType, rawTran);
            String country = detectCountry(tranBuilder.ccy());
            String symbol = rawTran.symbol();

            TradingTransaction nextRawTran;
            FiobTradingTransactionType nextExtraType;
            FinTransactionBuilder nextTranBuilder;
            String nextCountry;
            String nextSymbol;
            {
                TradingTransaction nextRawTranCandidate = rawTransToProcess.isEmpty() ? null : rawTransToProcess.getFirst();
                if (nextRawTranCandidate != null
                    && rawTran.tradeDate().isEqual(nextRawTranCandidate.tradeDate())
                    && rawTran.settleDate().isEqual(nextRawTranCandidate.settleDate())
                ) {
                    nextRawTran = nextRawTranCandidate;
                    nextExtraType = detectTranType(nextRawTran, lang);
                    nextTranBuilder = initTranBuilder(nextExtraType, nextRawTran);
                    nextCountry = detectCountry(nextTranBuilder.ccy());
                    nextSymbol = nextRawTran.symbol();
                } else {
                    nextRawTran = null;
                    nextExtraType = null;
                    nextTranBuilder = null;
                    nextCountry = null;
                    nextSymbol = null;
                }
            }

            groupTrans.add(tranBuilder);

            if (extraType == FiobTradingTransactionType.DEPOSIT) {
                Assert.isNull(symbol);
                Assert.isNull(tranBuilder.price());
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) == 0);
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) > 0);
                Assert.notNull(tranBuilder.fee());
                Assert.zero(tranBuilder.fee());
            } else if (extraType == FiobTradingTransactionType.WITHDRAWAL) {
                Assert.isNull(symbol);
                Assert.isNull(tranBuilder.price());
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) == 0);
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) < 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);
                if (FiobTradingTransactionType.FEE.equals(nextExtraType) && "Poplatek za převod peněz".equals(nextTranBuilder.externalDetail())) {
                    Assert.notNull(nextTranBuilder.fee());
                    Assert.notNull(nextTranBuilder.grossValue());
                    Assert.equal(nextTranBuilder.fee(), nextTranBuilder.grossValue());
                    Assert.isTrue(nextTranBuilder.fee().compareTo(ZERO) < 0);
                    tranBuilder.fee(nextTranBuilder.fee());
                    tranBuilder.netValue(tranBuilder.grossValue().add(tranBuilder.fee()).add(requireNonNullElse(tranBuilder.tax(), ZERO)));
                    rawTransToProcess.removeFirst();
                }
            } else if (extraType == FiobTradingTransactionType.BUY) {
                Assert.notNull(country);
                Assert.notNull(symbol);
                Assert.isTrue(tranBuilder.price().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.notNull(tranBuilder.ccy());
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) < 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) <= 0);
                Assert.isTrue(tranBuilder.tax() == null || tranBuilder.tax().compareTo(ZERO) == 0);
                tranBuilder.netValue(tranBuilder.grossValue());
                tranBuilder.grossValue(tranBuilder.netValue().subtract(tranBuilder.fee()));
            } else if (extraType == FiobTradingTransactionType.SELL) {
                Assert.notNull(country);
                Assert.notNull(symbol);
                Assert.isTrue(tranBuilder.price().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.notNull(tranBuilder.ccy());
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) <= 0);
                tranBuilder.qty(tranBuilder.qty().negate());
                tranBuilder.netValue(tranBuilder.grossValue());
                tranBuilder.grossValue(tranBuilder.netValue().subtract(tranBuilder.fee()));
            } else if (extraType == FiobTradingTransactionType.FX_BUY) {
                Assert.notNull(symbol);
                Assert.isTrue(tranBuilder.price().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.notNull(tranBuilder.ccy());
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) < 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);
                tranBuilder.asset(Asset.builder().type(AssetType.CASH).symbol(symbol).build());
            } else if (extraType == FiobTradingTransactionType.FX_SELL) {
                Assert.notNull(symbol);
                Assert.isTrue(tranBuilder.price().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.notNull(tranBuilder.ccy());
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);
                tranBuilder.asset(Asset.builder().type(AssetType.CASH).symbol(symbol).build());
                tranBuilder.qty(tranBuilder.qty().negate());
            } else if (extraType == FiobTradingTransactionType.CASH_DIVIDEND) {
                Assert.notNull(symbol);
                Assert.notNull(tranBuilder.ccy());
                Assert.equal(ONE, tranBuilder.price());
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);

                tranBuilder.price(null);
                if (FiobTradingTransactionType.TAX.equals(nextExtraType) && symbol.equals(nextSymbol)) {
                    Assert.equal(tranBuilder.ccy(), nextTranBuilder.ccy());
                    Assert.equal(ONE, nextTranBuilder.price());
                    Assert.isTrue(nextTranBuilder.fee().compareTo(ZERO) == 0);
                    Assert.isTrue(nextTranBuilder.grossValue().compareTo(ZERO) < 0);
                    Assert.equal(nextTranBuilder.grossValue(), nextTranBuilder.qty());
                    tranBuilder.tax(nextTranBuilder.grossValue());
                    tranBuilder.netValue(tranBuilder.grossValue().add(tranBuilder.tax()));
                    rawTransToProcess.removeFirst();
                } else {
                    Matcher m = Lazy.DIVIDEND_TAX_RATE_PATTERN.matcher(rawTran.text());
                    if (m.find()) {
                        BigDecimal taxRateInPct = FiobParsingUtil.toDecimal(m.group("taxRate"));
                        BigDecimal taxRate = taxRateInPct.divide(Num._100$00, 6, RoundingMode.HALF_UP);
                        Assert.isTrue(taxRate.compareTo(ZERO) > 0);
                        Assert.equal(tranBuilder.grossValue(), tranBuilder.qty());
                        tranBuilder.netValue(tranBuilder.grossValue());
                        tranBuilder.tax(tranBuilder.grossValue()
                                .divide(ONE.subtract(taxRate), 6, RoundingMode.HALF_UP)
                                .multiply(taxRate)
                                .negate()
                                .setScale(2, RoundingMode.HALF_UP));
                        tranBuilder.grossValue(tranBuilder.netValue().subtract(tranBuilder.tax()));
                    }
                    if (FiobTradingTransactionType.FEE.equals(nextExtraType) && "Poplatek za připsání dividend".equals(nextTranBuilder.externalDetail())) {
                        Assert.equal(nextTranBuilder.ccy(), tranBuilder.ccy());
                        Assert.notNull(nextTranBuilder.grossValue());
                        Assert.notNull(nextTranBuilder.fee());
                        Assert.equal(nextTranBuilder.grossValue(), nextTranBuilder.fee());
                        Assert.isTrue(nextTranBuilder.grossValue().compareTo(ZERO) < 0);
                        tranBuilder.fee(nextTranBuilder.fee());
                        tranBuilder.netValue(tranBuilder.grossValue().add(tranBuilder.fee()).add(requireNonNullElse(tranBuilder.tax(), ZERO)));
                        rawTransToProcess.removeFirst();
                    }
                }
                tranBuilder.qty(ZERO);
            } else if (extraType == FiobTradingTransactionType.CAPITAL_DIVIDEND) {
                Assert.notNull(symbol);
                Assert.notNull(tranBuilder.ccy());
                Assert.isTrue(tranBuilder.price().compareTo(ONE) == 0);
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);
                tranBuilder.qty(ZERO);
                tranBuilder.price(null);
            } else if (extraType == FiobTradingTransactionType.STOCK_DIVIDEND) {
                Assert.notNull(symbol);
                Assert.equal(ONE, tranBuilder.price());
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);

                if (("%s - Stock Dividend".formatted(symbol)).equals(rawTran.text())) {
                    Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) == 0);
                    Assert.isTrue(tranBuilder.netValue().compareTo(ZERO) == 0);
                    Assert.isTrue(tranBuilder.price().compareTo(ONE) == 0);
                    Assert.isNull(country);
                    country = symbolCountryMap.get(symbol);
                    Assert.notNull(country, () -> "Missing country for symbol: %s, %s, %s".formatted(symbol, rawTran, symbolCountryMap));

                    tranBuilder.price(null);
                    tranBuilder.ccy(detectCurrency(country));
                } else if (("%s - Finanční kompenzace - Stock Dividend".formatted(symbol)).equals(rawTran.text())) {
                    Assert.equal(tranBuilder.grossValue(), tranBuilder.qty());
                    Assert.notNull(country);
                    tranBuilder.price(null);
                    tranBuilder.qty(ZERO);
                } else {
                    throw new IllegalStateException("Unexpected: " + rawTran.text());
                }

            } else if (extraType == FiobTradingTransactionType.DIVIDEND_REVERSAL) {
                Assert.notNull(country);
                Assert.notNull(symbol);
                Assert.notNull(tranBuilder.ccy());
                Assert.equal(ONE, tranBuilder.price());
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) < 0);
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) < 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);

                Assert.isTrue(FiobTradingTransactionType.TAX_REFUND.equals(nextExtraType));
                Assert.isTrue(symbol.equals(nextSymbol));
                Assert.isTrue(tranBuilder.ccy().equals(nextTranBuilder.ccy()));
                Assert.isTrue(nextTranBuilder.price().compareTo(ONE) == 0);
                Assert.isTrue(nextTranBuilder.fee().compareTo(ZERO) == 0);
                Assert.isTrue(nextTranBuilder.grossValue().compareTo(ZERO) > 0);
                Assert.equal(nextTranBuilder.grossValue(), nextTranBuilder.qty());

                tranBuilder.tax(nextTranBuilder.grossValue());
                tranBuilder.qty(ZERO);
                tranBuilder.netValue(tranBuilder.grossValue().add(tranBuilder.tax()));
                tranBuilder.price(null);

                rawTransToProcess.removeFirst();
            } else if (extraType == FiobTradingTransactionType.FEE) {
                Assert.isTrue(symbol != null || tranBuilder.qty().compareTo(ZERO) == 0);
                Assert.notNull(tranBuilder.ccy());
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) <= 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) <= 0);
                Assert.isTrue(tranBuilder.price() == null || tranBuilder.price().compareTo(ONE) == 0);

                BigDecimal netValue;
                if (tranBuilder.fee().compareTo(ZERO) == 0) {
                    netValue = tranBuilder.grossValue();
                } else if (tranBuilder.grossValue().compareTo(ZERO) == 0) {
                    netValue = tranBuilder.fee();
                } else if (tranBuilder.grossValue().equals(tranBuilder.fee())) {
                    netValue = tranBuilder.grossValue();
                } else {
                    throw new IllegalStateException("fee=%s, grossValue=%s".formatted(tranBuilder.fee(), tranBuilder.grossValue()));
                }
                if (netValue.compareTo(ZERO) == 0) {
                    return;
                }
                tranBuilder.qty(ZERO);
                tranBuilder.grossValue(ZERO);
                tranBuilder.netValue(netValue);
                tranBuilder.fee(netValue);
                tranBuilder.price(null);
            } else if (extraType == FiobTradingTransactionType.RECLAMATION) {
                Assert.isNull(symbol);
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) == 0);
                Assert.notNull(tranBuilder.ccy());
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) >= 0);
                Assert.isTrue(tranBuilder.fee().compareTo(tranBuilder.grossValue()) == 0);
                tranBuilder.fee(ZERO);
                tranBuilder.netValue(tranBuilder.grossValue());
            } else if (extraType == FiobTradingTransactionType.INSTRUMENT_CHANGE_CHILD) {
                Assert.isTrue(nextExtraType == FiobTradingTransactionType.INSTRUMENT_CHANGE_PARENT);
                rawTransToProcess.add(1, rawTran);
                processOneGroup();
                return;
            } else if (extraType == FiobTradingTransactionType.INSTRUMENT_CHANGE_PARENT) {
                Assert.isTrue(nextExtraType == FiobTradingTransactionType.INSTRUMENT_CHANGE_CHILD);
                Assert.isTrue(rawTran.text().equals(nextTranBuilder.externalDetail()));
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.equal(tranBuilder.qty(), nextTranBuilder.qty());
                Assert.isTrue(tranBuilder.price().compareTo(ZERO) == 0);
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) == 0);
                Assert.isTrue(tranBuilder.netValue().compareTo(ZERO) == 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);
                Assert.isTrue(nextTranBuilder.price() != null && nextTranBuilder.price().compareTo(ZERO) == 0);
                Assert.isTrue(nextTranBuilder.grossValue() != null && nextTranBuilder.grossValue().compareTo(ZERO) == 0);
                Assert.isTrue(nextTranBuilder.fee() != null && nextTranBuilder.fee().compareTo(ZERO) == 0);

                Assert.isNull(tranBuilder.ccy());
                Assert.isNull(nextTranBuilder.ccy());
                Assert.isNull(nextCountry);

                Assert.isNull(country);
                country = symbolCountryMap.get(symbol);
                Assert.notNull(country, () -> "Missing country for symbol: %s, %s, %s".formatted(symbol, rawTran, symbolCountryMap));

                Currency countryCcy = detectCurrency(country);

                {
                    Assert.isTrue(TradingTransactionDirection.SELL.equals(rawTran.direction()));
                    tranBuilder.ccy(countryCcy);
                    tranBuilder.qty(tranBuilder.qty().negate());
                    tranBuilder.price(null);
                }
                {
                    Assert.isTrue(TradingTransactionDirection.BUY.equals(nextRawTran.direction()));
                    FinTransactionBuilder tranBuilder2 = FinTransaction.builder();
                    tranBuilder2.type(mapTranType(FiobTradingTransactionType.INSTRUMENT_CHANGE_CHILD));
                    tranBuilder2.date(tranBuilder.date());
                    tranBuilder2.settleDate(tranBuilder.settleDate());
                    tranBuilder2.ccy(tranBuilder.ccy());
                    tranBuilder2.qty(ZERO);
                    tranBuilder2.fee(ZERO);
                    tranBuilder2.tax(ZERO);
                    tranBuilder2.netValue(ZERO);
                    tranBuilder2.grossValue(ZERO);
                    tranBuilder2.ccy(countryCcy);
                    tranBuilder2.asset(Asset.builder().country(country).symbol(nextSymbol).type(detectAssetType(country, nextSymbol)).build());
                    tranBuilder2.qty(tranBuilder.qty().negate());
                    tranBuilder2.externalDetail(rawTran.text());
                    tranBuilder2.externalType(FiobTradingTransactionType.INSTRUMENT_CHANGE_CHILD.name());
                    groupTrans.add(tranBuilder2);
                }
                rawTransToProcess.removeFirst();
            } else if (extraType == FiobTradingTransactionType.TAX_REFUND) {
                Assert.notNull(symbol);
                Assert.notNull(tranBuilder.ccy());
                Assert.equal(tranBuilder.price(), ONE);
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.grossValue().compareTo(tranBuilder.qty()) == 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);
                tranBuilder.qty(ZERO);
                tranBuilder.tax(tranBuilder.grossValue());
                tranBuilder.grossValue(ZERO);
                tranBuilder.price(null);
            } else if (extraType == FiobTradingTransactionType.TAX) {
                Assert.notNull(symbol);
                Assert.notNull(tranBuilder.ccy());
                Assert.notNull(tranBuilder.price());
                Assert.equal(tranBuilder.price(), ONE);
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) <= 0);
                Assert.isTrue(tranBuilder.grossValue().compareTo(tranBuilder.qty()) == 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);
                tranBuilder.qty(ZERO);
                tranBuilder.tax(tranBuilder.grossValue());
                tranBuilder.grossValue(ZERO);
                tranBuilder.price(null);
            } else if (extraType == FiobTradingTransactionType.LIQUIDATION) {
                Assert.notNull(symbol);
                Assert.isTrue(tranBuilder.price().compareTo(ONE) == 0 || tranBuilder.price().compareTo(ZERO) == 0);
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) == 0 || tranBuilder.grossValue().compareTo(tranBuilder.qty()) == 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);

                if (FiobTradingTransactionType.LIQUIDATION.equals(nextExtraType)) {
                    Assert.notNull(country);
                    Assert.isTrue(nextRawTran.direction().equals(TradingTransactionDirection.SELL));
                    Assert.isTrue(nextSymbol.equals(symbol));
                    Assert.isTrue(nextTranBuilder.ccy().equals(tranBuilder.ccy()));
                    Assert.isTrue(nextTranBuilder.price().compareTo(ZERO) == 0);
                    Assert.isTrue(nextTranBuilder.qty().compareTo(ZERO) > 0);
                    Assert.isTrue(nextTranBuilder.grossValue().compareTo(ZERO) == 0);
                    Assert.isTrue(nextTranBuilder.fee().compareTo(ZERO) == 0);
                    rawTransToProcess.removeFirst();

                    tranBuilder.grossValue(tranBuilder.qty());
                    tranBuilder.netValue(tranBuilder.grossValue());
                    tranBuilder.qty(nextTranBuilder.qty().negate());
                    tranBuilder.price(null);
                } else {
                    Assert.isNull(tranBuilder.ccy());
                    Assert.isNull(country);
                    country = symbolCountryMap.get(symbol);
                    Assert.notNull(country, () -> "Missing country for symbol: %s, %s, %s".formatted(symbol, rawTran, symbolCountryMap));

                    tranBuilder.ccy(detectCurrency(country));
                    tranBuilder.grossValue(ZERO);
                    tranBuilder.netValue(ZERO);
                    tranBuilder.qty(tranBuilder.qty().negate());
                    tranBuilder.price(null);
                }
            } else if (extraType == FiobTradingTransactionType.SPINOFF_PARENT) {
                Assert.notNull(symbol);
                Assert.isNull(tranBuilder.ccy());
                Assert.notNull(rawTran.rawCcy());
                Assert.equal(ONE, tranBuilder.price());
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) == 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);

                Assert.isNull(country);
                country = symbolCountryMap.get(symbol);
                Assert.notNull(country, () -> "Missing country for symbol: %s, %s, %s".formatted(symbol, rawTran, symbolCountryMap));
                Currency countryCcy = detectCurrency(country);

                BigDecimal childQty = tranBuilder.qty();
                {
                    tranBuilder.ccy(countryCcy);
                    tranBuilder.qty(ZERO);
                }
                {
                    FinTransactionBuilder tranBuilder2 = FinTransaction.builder();
                    tranBuilder2.type(mapTranType(FiobTradingTransactionType.SPINOFF_CHILD));
                    tranBuilder2.date(tranBuilder.date());
                    tranBuilder2.settleDate(tranBuilder.settleDate());
                    tranBuilder2.ccy(tranBuilder.ccy());
                    tranBuilder2.qty(ZERO);
                    tranBuilder2.grossValue(ZERO);
                    tranBuilder2.tax(ZERO);
                    tranBuilder2.netValue(ZERO);
                    tranBuilder2.fee(ZERO);
                    tranBuilder2.externalDetail(rawTran.text());
                    tranBuilder2.externalType(FiobTradingTransactionType.SPINOFF_CHILD.name());
                    tranBuilder2.ccy(countryCcy);
                    tranBuilder2.asset(Asset.builder().country(country).symbol(rawTran.rawCcy()).type(detectAssetType(country, rawTran.rawCcy())).build());
                    tranBuilder2.qty(childQty);
                    groupTrans.add(tranBuilder2);
                }
            } else if (extraType == FiobTradingTransactionType.MERGER_CHILD) {
                Assert.notNull(symbol);
                Assert.isNull(tranBuilder.ccy());
                Assert.isTrue(tranBuilder.price().compareTo(ZERO) == 0);
                Assert.isTrue(tranBuilder.qty().compareTo(ZERO) > 0);
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) == 0);
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);

                Assert.isTrue(FiobTradingTransactionType.MERGER_PARENT.equals(nextExtraType));
                Assert.isTrue(nextRawTran.direction() == TradingTransactionDirection.SELL);
                Assert.isTrue(nextTranBuilder.grossValue() != null && nextTranBuilder.grossValue().compareTo(ZERO) == 0);
                Assert.isTrue(nextTranBuilder.fee() != null && nextTranBuilder.fee().compareTo(ZERO) == 0);
                Assert.isTrue(nextTranBuilder.price() != null && nextTranBuilder.price().compareTo(ZERO) == 0);
                Assert.isTrue(nextTranBuilder.qty() != null && nextTranBuilder.qty().compareTo(ZERO) > 0);

                Assert.isNull(country);
                country = symbolCountryMap.get(nextSymbol);
                Assert.notNull(country, () -> "Missing country for symbol: %s, %s, %s".formatted(nextSymbol, nextRawTran, symbolCountryMap));
                Currency countryCcy = detectCurrency(country);

                BigDecimal childQty = tranBuilder.qty();
                {
                    tranBuilder.ccy(countryCcy);
                    tranBuilder.asset(Asset.builder().country(country).symbol(nextSymbol).type(detectAssetType(country, nextSymbol)).build());
                    tranBuilder.qty(Objects.requireNonNull(nextTranBuilder.qty()).negate());
                    tranBuilder.price(null);
                }
                {
                    FinTransactionBuilder tranBuilder2 = FinTransaction.builder();
                    tranBuilder2.type(mapTranType(FiobTradingTransactionType.MERGER_PARENT));
                    tranBuilder2.date(tranBuilder.date());
                    tranBuilder2.ccy(tranBuilder.ccy());
                    tranBuilder2.fee(ZERO);
                    tranBuilder2.price(null);
                    tranBuilder2.ccy(countryCcy);
                    tranBuilder2.asset(Asset.builder().country(country).symbol(symbol).type(detectAssetType(country, symbol)).build());
                    tranBuilder2.qty(childQty);
                    tranBuilder2.settleDate(tranBuilder.settleDate());
                    tranBuilder2.externalType(FiobTradingTransactionType.MERGER_PARENT.name());
                    tranBuilder2.externalDetail(rawTran.text());
                    tranBuilder2.netValue(ZERO);
                    tranBuilder2.grossValue(ZERO);
                    tranBuilder2.tax(ZERO);
                    groupTrans.add(tranBuilder2);
                }
                rawTransToProcess.removeFirst();
            } else if (extraType == FiobTradingTransactionType.SPINOFF_VALUE) {
                Assert.notNull(symbol);
                Assert.notNull(tranBuilder.ccy());
                Assert.equal(ONE, tranBuilder.price());
                Assert.isTrue(tranBuilder.grossValue().compareTo(ZERO) > 0);
                Assert.equal(tranBuilder.grossValue(), tranBuilder.qty());
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);

                Assert.isTrue(FiobTradingTransactionType.SPINOFF_VALUE.equals(nextExtraType));
                Assert.isTrue(nextRawTran.direction() == null);
                Assert.isTrue(nextSymbol != null && nextSymbol.equals(symbol));
                Assert.isTrue(nextTranBuilder.grossValue() != null && nextTranBuilder.grossValue().negate().equals(tranBuilder.grossValue()));
                Assert.isTrue(nextTranBuilder.qty() != null && nextTranBuilder.qty().negate().equals(tranBuilder.qty()));
                Assert.isTrue(nextTranBuilder.fee() != null && nextTranBuilder.fee().compareTo(ZERO) == 0);
                Assert.isTrue(tranBuilder.price().compareTo(ONE) == 0);

                {
                    tranBuilder.qty(ZERO);
                }
                {
                    FinTransactionBuilder tranBuilder2 = FinTransaction.builder();
                    tranBuilder2.type(mapTranType(FiobTradingTransactionType.SPINOFF_VALUE));
                    tranBuilder2.date(tranBuilder.date());
                    tranBuilder2.settleDate(tranBuilder.settleDate());
                    tranBuilder2.ccy(tranBuilder.ccy());
                    tranBuilder2.qty(ZERO);
                    tranBuilder2.fee(ZERO);
                    tranBuilder2.externalType(FiobTradingTransactionType.SPINOFF_VALUE.name());
                    tranBuilder2.externalDetail(rawTran.text());
                    tranBuilder2.asset(Asset.builder().country(country).symbol(symbol).type(detectAssetType(country, symbol)).build());
                    tranBuilder2.grossValue(tranBuilder.grossValue().negate());
                    tranBuilder2.netValue(tranBuilder.grossValue().negate());
                    tranBuilder2.tax(ZERO);
                    groupTrans.add(tranBuilder2);
                }
                rawTransToProcess.removeFirst();
            } else if (extraType == FiobTradingTransactionType.SPLIT) {
                Assert.isNull(country);
                Assert.isNull(tranBuilder.ccy());
                Assert.isTrue(tranBuilder.fee().compareTo(ZERO) == 0);
                Assert.isTrue(tranBuilder.price().compareTo(ZERO) == 0);
                country = symbolCountryMap.get(symbol);
                Assert.notNull(country, () -> "Missing country for symbol: %s, %s, %s".formatted(symbol, nextRawTran, symbolCountryMap));

                if (FiobTradingTransactionType.SPLIT.equals(nextExtraType) && nextSymbol.equals(symbol)) {
                    Assert.isTrue(rawTran.direction() == TradingTransactionDirection.SELL);
                    Assert.isTrue(nextRawTran.direction() == TradingTransactionDirection.BUY);
                    Assert.isNull(nextTranBuilder.ccy());
                    Assert.isTrue(nextTranBuilder.price().compareTo(ZERO) == 0);
                    Assert.isTrue(nextTranBuilder.qty().compareTo(ZERO) > 0);
                    Assert.isTrue(nextTranBuilder.grossValue().compareTo(ZERO) == 0);
                    Assert.isTrue(nextTranBuilder.fee().compareTo(ZERO) == 0);
                    tranBuilder.qty(tranBuilder.qty().negate().add(nextTranBuilder.qty()));
                    rawTransToProcess.removeFirst();
                }
                tranBuilder.ccy(detectCurrency(country));
                tranBuilder.price(null);
                tranBuilder.grossValue(ZERO);
                tranBuilder.netValue(ZERO);
            } else {
                throw new IllegalStateException("Unexpected: %s".formatted(rawTran));
            }

            String groupId = null;
            for (FinTransactionBuilder memberTranBuilder : groupTrans) {
                assert memberTranBuilder.externalId() == null;
                assert memberTranBuilder.groupId() == null;

                String extraId = generateTranId(memberTranBuilder, rawTran.rowNumberOverStatementLine(), rawTran.statementLineHash());
                if (groupId == null) {
                    groupId = extraId;
                }

                Asset asset = coalesce(memberTranBuilder.asset(), symbol == null ? null :
                        Asset.builder().country(country).symbol(symbol).type(detectAssetType(country, symbol)).build());

                memberTranBuilder.externalId(extraId);
                memberTranBuilder.groupId(groupId);
                memberTranBuilder.asset(asset);

                resultTrans.add(memberTranBuilder.build());

                if (asset != null && asset.country() != null && asset.symbol() != null) {
                    symbolCountryMap.put(asset.symbol(), asset.country());
                }
            }
        }

        private static FinTransactionBuilder initTranBuilder(FiobTradingTransactionType extraType, TradingTransaction rawTran) {
            FinTransactionBuilder tranBuilder = FinTransaction.builder();
            tranBuilder.type(mapTranType(extraType));
            tranBuilder.date(rawTran.tradeDate().toLocalDate());
            tranBuilder.ccy(detectCcy(rawTran.rawCcy()));
            tranBuilder.price(rawTran.price());
            tranBuilder.qty(rawTran.shares());
            tranBuilder.grossValue(mapGrossValue(rawTran, tranBuilder.ccy()));
            tranBuilder.fee(mapFee(rawTran, tranBuilder.ccy()));
            tranBuilder.tax(ZERO);
            tranBuilder.reconcileNetValue();
            tranBuilder.settleDate(rawTran.settleDate());
            tranBuilder.externalDetail(rawTran.text());
            tranBuilder.externalType(extraType.name());
            return tranBuilder;
        }

        private static BigDecimal mapGrossValue(TradingTransaction rawTran, Currency ccy) {
            BigDecimal rawValue = null;
            if (ccy != null) {
                BigDecimal volUsd = rawTran.volumeUsd();
                BigDecimal volEur = rawTran.volumeEur();
                BigDecimal volCzk = rawTran.volumeCzk();
                switch (ccy) {
                    case USD -> {
                        Assert.isTrue(volEur == null);
                        Assert.isTrue(volCzk == null);
                        rawValue = volUsd;
                    }
                    case EUR -> {
                        Assert.isTrue(volUsd == null);
                        Assert.isTrue(volCzk == null);
                        rawValue = volEur;
                    }
                    case CZK -> {
                        Assert.isTrue(volUsd == null);
                        Assert.isTrue(volEur == null);
                        rawValue = volCzk;
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + ccy);
                }
            }
            return requireNonNullElse(rawValue, ZERO);
        }

        private static BigDecimal mapFee(TradingTransaction rawTran, Currency ccy) {
            BigDecimal rawFees = null;
            if (ccy != null) {
                BigDecimal feesUsd = rawTran.feesUsd();
                BigDecimal feesEur = rawTran.feesEur();
                BigDecimal feesCzk = rawTran.feesCzk();
                switch (ccy) {
                    case USD -> {
                        Assert.isTrue(feesEur == null);
                        Assert.isTrue(feesCzk == null);
                        rawFees = feesUsd;
                    }
                    case EUR -> {
                        Assert.isTrue(feesUsd == null);
                        Assert.isTrue(feesCzk == null);
                        rawFees = feesEur;
                    }
                    case CZK -> {
                        Assert.isTrue(feesUsd == null);
                        Assert.isTrue(feesEur == null);
                        rawFees = feesCzk;
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + ccy);
                }
            }
            return rawFees == null ? ZERO : rawFees.negate();
        }

        private static String detectCountry(Currency ccy) {
            return switch (ccy) {
                case USD -> "US";
                case EUR -> "DE";
                case CZK -> "CZ";
                case null -> null;
            };
        }

        @SuppressWarnings({"SpellCheckingInspection", "DuplicatedCode", "RedundantIfStatement"})
        private static FiobTradingTransactionType detectTranType(TradingTransaction tran, Lang lang) {
            String symbol = tran.symbol();
            TradingTransactionDirection direction = tran.direction();
            String text = tran.text();
            String market = tran.market();
            if (direction == null && text.startsWith("Vloženo na účet z") && text.endsWith("Bezhotovostní vklad")) {
                return FiobTradingTransactionType.DEPOSIT;
            }
            if (direction == null && text.equals("Vklad Bezhotovostní vklad")) {
                return FiobTradingTransactionType.DEPOSIT;
            }
            if (direction == null && text.equals("v Bezhotovostní vklad")) {
                return FiobTradingTransactionType.DEPOSIT;
            }
            if (direction == null && text.startsWith("Převod z účtu")) {
                return FiobTradingTransactionType.DEPOSIT;
            }
            if (TradingTransactionDirection.BANK_TRANSFER.equals(direction) && text.startsWith("Převod na účet")) {
                return FiobTradingTransactionType.WITHDRAWAL;
            }

            if (TradingTransactionDirection.CURRENCY_CONVERSION.equals(direction) && text.equals("Nákup")) {
                return FiobTradingTransactionType.FX_BUY;
            }
            if (TradingTransactionDirection.CURRENCY_CONVERSION.equals(direction) && text.equals("Prodej")) {
                return FiobTradingTransactionType.FX_SELL;
            }

            if (TradingTransactionDirection.BUY.equals(direction) && text.equals("Nákup")) {
                return FiobTradingTransactionType.BUY;
            }
            if (TradingTransactionDirection.SELL.equals(direction) && text.equals("Prodej")) {
                return FiobTradingTransactionType.SELL;
            }
            if (direction == null && text.startsWith("%s - Dividenda".formatted(symbol))) {
                return FiobTradingTransactionType.CASH_DIVIDEND;
            }
            if (direction == null && text.startsWith("%s - Daň z divid. zaplacená".formatted(symbol))) {
                return FiobTradingTransactionType.TAX;
            }
            if (direction == null && text.startsWith("%s - Daň z dividend zaplacená".formatted(symbol))) {
                return FiobTradingTransactionType.TAX;
            }
            if (direction == null && text.startsWith("%s - Divi.".formatted(symbol))) {
                return FiobTradingTransactionType.CASH_DIVIDEND;
            }
            if (direction == null && text.startsWith("%s - Return of Principal".formatted(symbol))) {
                return FiobTradingTransactionType.CAPITAL_DIVIDEND;
            }
            if (direction == null && text.startsWith("%s - Stock Dividend".formatted(symbol))) {
                return FiobTradingTransactionType.STOCK_DIVIDEND;
            }
            if (direction == null && text.startsWith("%s - Finanční kompenzace - Stock Dividend".formatted(symbol))) {
                return FiobTradingTransactionType.STOCK_DIVIDEND;
            }
            if (direction == null && text.startsWith("%s - Oprava dividendy z".formatted(symbol))) {
                return FiobTradingTransactionType.DIVIDEND_REVERSAL;
            }
            if (direction == null && text.startsWith("%s - Tax Refund".formatted(symbol))) {
                return FiobTradingTransactionType.TAX_REFUND;
            }
            if (direction == null && text.startsWith("%s - Oprava daně z dividendy".formatted(symbol))) {
                return FiobTradingTransactionType.TAX_REFUND;
            }
            if (direction == null && text.startsWith("%s - Refundable U.S. Fed Tax Reclassified By Issuer".formatted(symbol))) {
                return FiobTradingTransactionType.TAX_REFUND;
            }

            if (direction == null && text.startsWith("%s - ADR Fee".formatted(symbol))) {
                return FiobTradingTransactionType.FEE;
            }
            if (direction == null && text.startsWith("%s - Spin-off Fair Market Value".formatted(symbol))) {
                return FiobTradingTransactionType.SPINOFF_VALUE;
            }
            if (direction == null && text.startsWith("%s - Spin-off - daň zaplacená".formatted(symbol))) {
                return FiobTradingTransactionType.TAX;
            }
            if (direction == null &&
                text.startsWith("%s - Spin-off".formatted(symbol)) &&
                !text.contains("Fair Market Value") &&
                !text.contains("daň zaplacená")
            ) {
                return FiobTradingTransactionType.SPINOFF_PARENT;
            }
            if (direction == null && text.startsWith("%s - Security Liquidated".formatted(symbol))) {
                return FiobTradingTransactionType.LIQUIDATION;
            }
            if (direction == TradingTransactionDirection.SELL && text.startsWith("Security Liquidated")) {
                return FiobTradingTransactionType.LIQUIDATION;
            }
            if (direction == null && text.startsWith("Poplatek za on-line data")) {
                return FiobTradingTransactionType.FEE;
            }
            if (direction == null && text.startsWith("Reklamace ")) {
                return FiobTradingTransactionType.RECLAMATION;
            }

            if (lang.equals(Lang.CZ) && market.equals("Poplatek")) {
                return FiobTradingTransactionType.FEE;
            }
            if (lang.equals(Lang.EN) && market.equals("Fee")) {
                return FiobTradingTransactionType.FEE;
            }
            if (lang.equals(Lang.SK) && market.equals("Poplatok")) {
                return FiobTradingTransactionType.FEE;
            }

            boolean isTransformation = false;
            if (lang.equals(Lang.CZ) && market.equals("Transformace")) {
                isTransformation = true;
            }
            if (lang.equals(Lang.EN) && market.equals("Transformation")) {
                isTransformation = true;
            }
            if (lang.equals(Lang.SK) && market.equals("Transformácia")) {
                isTransformation = true;
            }
            if (isTransformation) {
                if (text.contains("Ticker Change: ")
                    || text.contains("Change of Listing: ")
                    || text.contains("Change in Security ID (ISIN Change)")
                ) {
                    if (TradingTransactionDirection.SELL.equals(direction)) {
                        return FiobTradingTransactionType.INSTRUMENT_CHANGE_PARENT;
                    } else if (TradingTransactionDirection.BUY.equals(direction)) {
                        return FiobTradingTransactionType.INSTRUMENT_CHANGE_CHILD;
                    }
                }
                if (text.contains("Split ")) {
                    return FiobTradingTransactionType.SPLIT;
                }
                if (text.contains("Stock Merger ")) {
                    if (TradingTransactionDirection.SELL.equals(direction)) {
                        return FiobTradingTransactionType.MERGER_PARENT;
                    } else if (TradingTransactionDirection.BUY.equals(direction)) {
                        return FiobTradingTransactionType.MERGER_CHILD;
                    }
                }
                if (text.contains("Security Deleted As Worthless")) {
                    return FiobTradingTransactionType.LIQUIDATION;
                }
                if (text.equals("%s - Reorganization".formatted(symbol))) {
                    String rawSymbol = tran.rawSymbol();
                    if (TradingTransactionDirection.SELL.equals(direction) && (symbol + "*").equals(rawSymbol)) {
                        return FiobTradingTransactionType.INSTRUMENT_CHANGE_PARENT;
                    } else if (TradingTransactionDirection.BUY.equals(direction) && symbol.equals(rawSymbol)) {
                        return FiobTradingTransactionType.INSTRUMENT_CHANGE_CHILD;
                    }
                }
            }
            throw new IllegalStateException("Could not detect transaction type: %s".formatted(tran));
        }

        private static String generateTranId(FinTransactionBuilder tranBuilder, int rowNumberOverStatementLine, int statementLineHash) {
            LocalDate date = tranBuilder.date();
            BigDecimal qty = tranBuilder.qty();
            BigDecimal price = tranBuilder.price();
            BigDecimal grossValue = tranBuilder.grossValue();
            String type = tranBuilder.externalType();
            Currency ccy = tranBuilder.ccy();
            Asset asset = tranBuilder.asset();
            String country = asset == null ? null : asset.country();
            String symbol = asset == null ? null : asset.symbol();
            return "%s/%s/%s/%s/%s/%s/%s/%s/%d/%d".formatted(
                    Lazy.ID_DATE_FORMAT.format(date),
                    type,
                    ccy,
                    country,
                    symbol,
                    grossValue == null ? "null" : grossValue.toPlainString(),
                    qty == null ? "null" : qty.toPlainString(),
                    price == null ? "null" : price.toPlainString(),
                    rowNumberOverStatementLine,
                    statementLineHash);
        }

        @SuppressWarnings("SpellCheckingInspection")
        private static AssetType detectAssetType(String country, String symbol) {
            if (symbol == null || country == null) {
                return null;
            }
            if ("US".equals(country)) {
                if (symbol.equals("OILU")) {
                    return AssetType.ETF;
                }
            } else if ("DE".equals(country)) {
                if (symbol.equals("SXR8")
                    || symbol.equals("SXRV")
                    || symbol.equals("L8I7")
                ) {
                    return AssetType.ETF;
                }
            }
            if (symbol.endsWith(".W")) {
                return AssetType.DERIVATIVE;
            }

            return AssetType.STOCK;
        }

        private static Currency detectCurrency(String country) {
            return switch (country) {
                case "DE" -> Currency.EUR;
                case "US" -> Currency.USD;
                case "CZ" -> Currency.CZK;
                default -> null;
            };
        }

        @SuppressWarnings("DuplicateBranchesInSwitch")
        private static FinTransactionType mapTranType(FiobTradingTransactionType fiobType) {
            return switch (fiobType) {
                case BUY -> FinTransactionType.BUY;
                case SELL -> FinTransactionType.SELL;
                case DEPOSIT -> FinTransactionType.DEPOSIT;
                case WITHDRAWAL -> FinTransactionType.WITHDRAWAL;
                case CASH_DIVIDEND -> FinTransactionType.DIVIDEND;
                case CAPITAL_DIVIDEND -> FinTransactionType.DIVIDEND;
                case DIVIDEND_REVERSAL -> FinTransactionType.DIVIDEND;
                case STOCK_DIVIDEND -> FinTransactionType.DIVIDEND;
                case INTEREST -> FinTransactionType.INTEREST;
                case FX_BUY -> FinTransactionType.FX_BUY;
                case FX_SELL -> FinTransactionType.FX_SELL;
                case FEE -> FinTransactionType.FEE;
                case TAX -> FinTransactionType.TAX;
                case TAX_REFUND -> FinTransactionType.TAX;
                case RECLAMATION -> FinTransactionType.OTHER_INTERNAL_FLOW;
                case MERGER_CHILD -> FinTransactionType.TRANSFORMATION;
                case MERGER_PARENT -> FinTransactionType.TRANSFORMATION;
                case LIQUIDATION -> FinTransactionType.TRANSFORMATION;
                case SPLIT -> FinTransactionType.TRANSFORMATION;
                case SPINOFF_VALUE -> FinTransactionType.TRANSFORMATION;
                case SPINOFF_CHILD -> FinTransactionType.TRANSFORMATION;
                case SPINOFF_PARENT -> FinTransactionType.TRANSFORMATION;
                case INSTRUMENT_CHANGE_PARENT -> FinTransactionType.TRANSFORMATION;
                case INSTRUMENT_CHANGE_CHILD -> FinTransactionType.TRANSFORMATION;
            };
        }

        private static Currency detectCcy(String s) {
            if (s == null || s.isBlank()) {
                return null;
            }
            s = s.trim().toUpperCase();
            return switch (s) {
                case "EUR", "USD", "CZK" -> Currency.valueOf(s);
                default -> null;
            };
        }


        private static class Lazy {
            private static final Pattern DIVIDEND_TAX_RATE_PATTERN = Pattern.compile("\\(((čistá)|(po\\s+zdanění)),\\s+daň\\s(?<taxRate>\\d+(,\\d+)?)\\s*%\\)");
            private static final DateTimeFormatter ID_DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");
        }
    }


}