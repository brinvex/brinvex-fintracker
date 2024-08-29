package com.brinvex.fintracker.connector.ibkr.impl.service;

import com.brinvex.fintracker.api.exception.NotYetImplementedException;
import com.brinvex.fintracker.api.model.domain.Asset;
import com.brinvex.fintracker.api.model.domain.AssetType;
import com.brinvex.fintracker.api.model.domain.FinTransaction;
import com.brinvex.fintracker.api.model.domain.FinTransactionType;
import com.brinvex.fintracker.api.util.Regex;
import com.brinvex.fintracker.api.model.builder.AssetBuilder;
import com.brinvex.fintracker.api.model.builder.FinTransactionBuilder;
import com.brinvex.util.java.validation.Assert;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.BuySell;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CashTransactionType;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CorporateAction;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CorporateActionType;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.Trade;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.TradeConfirm;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrFinTransactionMapper;
import com.brinvex.fintracker.connector.ibkr.impl.builder.TradeBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.brinvex.fintracker.api.model.domain.FinTransactionType.BUY;
import static com.brinvex.fintracker.api.model.domain.FinTransactionType.DEPOSIT;
import static com.brinvex.fintracker.api.model.domain.FinTransactionType.SELL;
import static com.brinvex.fintracker.api.model.domain.FinTransactionType.TRANSFORMATION;
import static com.brinvex.fintracker.api.model.domain.FinTransactionType.WITHDRAWAL;
import static com.brinvex.fintracker.connector.ibkr.api.model.statement.CashTransactionType.DEPOSITS_WITHDRAWALS;
import static com.brinvex.fintracker.connector.ibkr.api.model.statement.CashTransactionType.WITHHOLDING_TAX;
import static com.brinvex.util.java.StringUtil.stripToNull;
import static java.math.BigDecimal.ZERO;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.groupingBy;

@SuppressWarnings({"DuplicatedCode", "unused"})
public class IbkrFinTransactionMapperImpl implements IbkrFinTransactionMapper {

    private static final DateTimeFormatter idDf = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public List<FinTransaction> mapCashTransactions(List<CashTransaction> cashTrans) {

        cashTrans = cashTrans.stream()
                .sorted(comparing(CashTransaction::reportDate).thenComparing(CashTransaction::transactionID))
                .toList();

        Function<CashTransaction, String> reportDateAndActionIdConcat = ct -> "%s/%s".formatted(ct.reportDate(), ct.actionID());
        Map<String, List<CashTransaction>> cashTransByReportDateAndActionId = cashTrans
                .stream()
                .collect(groupingBy(reportDateAndActionIdConcat));

        SequencedMap<String, FinTransaction> resultTrans = new LinkedHashMap<>();
        Set<CashTransaction> cashTransToSkip = new HashSet<>();
        for (CashTransaction cashTran : cashTrans) {
            if (cashTransToSkip.contains(cashTran)) {
                continue;
            }
            CashTransactionType cashTranType = cashTran.type();
            BigDecimal amount = cashTran.amount();

            FinTransactionBuilder finTranBldr = switch (cashTranType) {
                case DEPOSITS_WITHDRAWALS -> new FinTransactionBuilder()
                        .type(amount.compareTo(ZERO) > 0 ? DEPOSIT : WITHDRAWAL)
                        .extraType(DEPOSITS_WITHDRAWALS.name())
                        .qty(ZERO)
                        .grossValue(amount)
                        .netValue(amount)
                        .fee(ZERO);
                case DIVIDENDS, PAYMENT_IN_LIEU_OF_DIVIDENDS -> {
                    String reportDateAndActionId = reportDateAndActionIdConcat.apply(cashTran);
                    List<CashTransaction> dividTaxTrans = cashTransByReportDateAndActionId.get(reportDateAndActionId)
                            .stream()
                            .filter(t -> t != cashTran)
                            .filter(t -> t.type().equals(WITHHOLDING_TAX))
                            .toList();

                    BigDecimal netValue;
                    BigDecimal tax;
                    switch (dividTaxTrans.size()) {
                        case 0 -> {
                            tax = ZERO;
                            netValue = amount;
                        }
                        case 1 -> {
                            CashTransaction dividTaxTran = dividTaxTrans.getFirst();
                            tax = dividTaxTran.amount();
                            netValue = amount.add(tax);
                            Assert.negative(tax);
                            cashTransToSkip.add(dividTaxTran);
                        }
                        default -> throw new IllegalStateException(
                                "Found more than one dividendTaxTransaction related to the same dividendTransaction: " +
                                "reportDateAndActionId=%s, dividTran=%s, dividTaxTran=%s"
                                        .formatted(reportDateAndActionId, cashTran, dividTaxTrans));
                    }

                    String extraTranType = switch (cashTranType) {
                        case DIVIDENDS -> {
                            Assert.contains(cashTran.description(), "CASH DIVIDEND");
                            yield "CASH DIVIDEND";
                        }
                        case PAYMENT_IN_LIEU_OF_DIVIDENDS -> {
                            Assert.contains(cashTran.description(), "PAYMENT IN LIEU OF DIVIDEND (Ordinary Dividend)");
                            yield "PAYMENT_IN_LIEU_OF_DIVIDENDS";
                        }
                        default -> throw new AssertionError("Unreachable");
                    };

                    yield new FinTransactionBuilder()
                            .type(FinTransactionType.DIVIDEND)
                            .extraType(extraTranType)
                            .asset(new AssetBuilder()
                                    .type(toAssetType(cashTran.assetCategory(), cashTran.assetSubCategory()))
                                    .extraType("%s/%s".formatted(cashTran.assetCategory(), cashTran.assetSubCategory()))
                                    .country(detectCountryByExchange(cashTran.listingExchange()))
                                    .symbol(stripToNull(cashTran.symbol()))
                                    .countryFigi(stripToNull(cashTran.figi()))
                                    .isin(stripToNull(cashTran.isin()))
                                    .build())
                            .grossValue(amount)
                            .qty(ZERO)
                            .fee(ZERO)
                            .netValue(netValue)
                            .tax(tax);
                }
                case WITHHOLDING_TAX -> {
                    Assert.before(cashTran.settleDate(), cashTran.reportDate());
                    yield new FinTransactionBuilder()
                            .type(FinTransactionType.TAX)
                            .extraType(WITHHOLDING_TAX.name())
                            .asset(new AssetBuilder()
                                    .type(toAssetType(cashTran.assetCategory(), cashTran.assetSubCategory()))
                                    .extraType("%s/%s".formatted(cashTran.assetCategory(), cashTran.assetSubCategory()))
                                    .country(detectCountryByExchange(cashTran.listingExchange()))
                                    .symbol(stripToNull(cashTran.symbol()))
                                    .countryFigi(stripToNull(cashTran.figi()))
                                    .isin(stripToNull(cashTran.isin()))
                                    .build())
                            .grossValue(ZERO)
                            .qty(ZERO)
                            .fee(ZERO)
                            .netValue(amount)
                            .tax(amount);
                }
                case OTHER_FEES, BROKER_INTEREST_PAID, BROKER_FEES -> {
                    Assert.isNull(cashTran.assetCategory());
                    Assert.isNull(cashTran.assetSubCategory());
                    Assert.blank(cashTran.listingExchange());
                    Assert.blank(cashTran.symbol());
                    Assert.blank(cashTran.figi());
                    Assert.blank(cashTran.isin());
                    yield new FinTransactionBuilder()
                            .type(FinTransactionType.FEE)
                            .extraType(cashTranType.name())
                            .grossValue(ZERO)
                            .qty(ZERO)
                            .fee(amount)
                            .netValue(amount)
                            .tax(ZERO);
                }
            };

            String tranId = getId(cashTran);
            FinTransaction newTran = finTranBldr
                    .extraId(tranId)
                    .date(cashTran.reportDate())
                    .ccy(cashTran.currency())
                    .extraDetail(cashTran.extraDateTimeStr() + "/" + cashTran.description())
                    .settleDate(cashTran.settleDate())
                    .build();
            FinTransaction oldTran = resultTrans.put(tranId, newTran);
            if (oldTran != null) {
                throw new IllegalStateException("ID collision: %s, oldTran=%s, newTran=%s, cashTran=%s".formatted(tranId, oldTran, newTran, cashTran));
            }
        }
        return new ArrayList<>(resultTrans.values());
    }

    @Override
    public List<FinTransaction> mapTrades(List<Trade> trades) {
        trades = trades.stream()
                .sorted(comparing(Trade::reportDate).thenComparing(Trade::tradeID))
                .toList();

        SequencedMap<String, FinTransaction> resultTrans = new LinkedHashMap<>();
        for (Trade trade : trades) {
            String ccy = trade.currency();
            String ibCommissionCcy = trade.ibCommissionCurrency();
            BigDecimal fees = requireNonNullElse(trade.ibCommission(), ZERO);

            FinTransactionBuilder tranBldr = switch (trade.transactionType()) {
                case EXCH_TRADE -> switch (trade.assetCategory()) {
                    case CASH -> switch (trade.buySell()) {
                        case SELL -> {
                            String sellCcy = trade.symbol().substring(0, 3);
                            String buyCcy = trade.symbol().substring(4);
                            Assert.matches(sellCcy, Regex.CCY.pattern());
                            Assert.matches(buyCcy, Regex.CCY.pattern());
                            Assert.equal(buyCcy, ccy);
                            Assert.equal(sellCcy, ibCommissionCcy);
                            if (!ccy.equals(ibCommissionCcy)) {
                                yield new FinTransactionBuilder()
                                        .type(FinTransactionType.FX_BUY)
                                        .asset(new AssetBuilder()
                                                .type(AssetType.CASH)
                                                .symbol(buyCcy)
                                                .build())
                                        .ccy(sellCcy)
                                        .qty(trade.proceeds())
                                        .price(trade.tradePrice())
                                        .grossValue(trade.quantity())
                                        .netValue(trade.quantity().add(trade.ibCommission()))
                                        .fee(trade.ibCommission());
                            } else {
                                throw new NotYetImplementedException("ccy=%s, ibCommissionCcy=%s, trade=%s"
                                        .formatted(ccy, ibCommissionCcy, trade));
                            }
                        }
                        case BUY -> {
                            String buyCcy = trade.symbol().substring(0, 3);
                            String sellCcy = trade.symbol().substring(4);
                            Assert.equal(sellCcy, ccy);
                            Assert.equal(buyCcy, ibCommissionCcy);
                            if (!Objects.equals(ccy, ibCommissionCcy)) {
                                yield new FinTransactionBuilder()
                                        .type(FinTransactionType.FX_BUY)
                                        .asset(new AssetBuilder()
                                                .type(AssetType.CASH)
                                                .symbol(buyCcy)
                                                .build())
                                        .ccy(ccy)
                                        .qty(trade.quantity())
                                        .price(trade.tradePrice())
                                        .grossValue(trade.proceeds())
                                        .netValue(trade.proceeds().add(trade.ibCommission()))
                                        .fee(trade.ibCommission());
                            } else {
                                throw new NotYetImplementedException("ccy=%s, ibCommissionCcy=%s, trade=%s"
                                        .formatted(ccy, ibCommissionCcy, trade));
                            }
                        }
                    };
                    case STK -> new FinTransactionBuilder()
                            .type(switch (trade.buySell()) {
                                case BUY -> BUY;
                                case SELL -> SELL;
                            })
                            .extraType(trade.transactionType().name())
                            .asset(new AssetBuilder()
                                    .type(toAssetType(trade.assetCategory(), trade.assetSubCategory()))
                                    .extraType("%s/%s".formatted(trade.assetCategory(), trade.assetSubCategory()))
                                    .country(detectCountryByExchange(trade.listingExchange()))
                                    .symbol(stripToNull(trade.symbol()))
                                    .countryFigi(stripToNull(trade.figi()))
                                    .isin(stripToNull(trade.isin()))
                                    .build())
                            .ccy(ccy)
                            .qty(trade.quantity())
                            .price(trade.tradePrice())
                            .grossValue(trade.proceeds())
                            .netValue(trade.netCash())
                            .fee(fees);
                };
                case FRAC_SHARE -> {
                    Assert.zero(trade.ibCommission());
                    Assert.equal(trade.assetCategory(), AssetCategory.STK);
                    Assert.equal(trade.buySell(), BuySell.SELL);
                    BigDecimal grossValue = trade.proceeds();
                    Assert.positive(grossValue);
                    Assert.opposite(grossValue, trade.tradeMoney());
                    Assert.equal(grossValue, trade.netCash());
                    Assert.zero(trade.taxes());
                    yield new FinTransactionBuilder()
                            .type(FinTransactionType.SELL)
                            .asset(new AssetBuilder()
                                    .type(AssetType.STOCK)
                                    .country(detectCountryByExchange(trade.listingExchange()))
                                    .symbol(stripToNull(trade.symbol()))
                                    .countryFigi(stripToNull(trade.figi()))
                                    .isin(stripToNull(trade.isin()))
                                    .build())
                            .ccy(trade.currency())
                            .qty(trade.quantity())
                            .price(trade.tradePrice())
                            .grossValue(grossValue)
                            .netValue(grossValue)
                            .fee(trade.ibCommission());
                }
            };

            String tranId = getId(trade);

            FinTransaction newTran = tranBldr
                    .extraId(tranId)
                    .date(trade.reportDate())
                    .extraDetail(trade.extraDateTimeStr() + "/" + trade.description())
                    .settleDate(trade.settleDateTarget())
                    .build();
            FinTransaction oldTran = resultTrans.put(tranId, newTran);
            if (oldTran != null) {
                throw new IllegalStateException("ID collision: %s, oldTran=%s, newTran=%s, trade=%s".formatted(tranId, oldTran, newTran, trade));
            }
        }
        return new ArrayList<>(resultTrans.values());
    }

    @Override
    public List<FinTransaction> mapTradeConfirms(List<TradeConfirm> tradeConfirms) {
        List<Trade> tradeConfirmTrades = tradeConfirms
                .stream()
                .map(tradeConfirm -> new TradeBuilder()
                        .currency(tradeConfirm.currency())
                        .assetCategory(tradeConfirm.assetCategory())
                        .assetSubCategory(tradeConfirm.assetSubCategory())
                        .symbol(tradeConfirm.symbol())
                        .description(tradeConfirm.description())
                        .securityID(tradeConfirm.securityID())
                        .securityIDType(tradeConfirm.securityIDType())
                        .isin(tradeConfirm.isin())
                        .figi(tradeConfirm.figi())
                        .listingExchange(tradeConfirm.listingExchange())
                        .tradeID(tradeConfirm.tradeID())
                        .reportDate(tradeConfirm.reportDate())
                        .extraDateTimeStr(tradeConfirm.extraDateTimeStr())
                        .settleDateTarget(tradeConfirm.settleDate())
                        .transactionType(tradeConfirm.transactionType())
                        .exchange(tradeConfirm.exchange())
                        .quantity(tradeConfirm.quantity())
                        .tradePrice(tradeConfirm.price())
                        .tradeMoney(tradeConfirm.amount())
                        .proceeds(tradeConfirm.proceeds())
                        .netCash(tradeConfirm.netCash())
                        .ibCommission(tradeConfirm.commission())
                        .ibCommissionCurrency(tradeConfirm.commissionCurrency())
                        .taxes(tradeConfirm.tax())
                        .buySell(tradeConfirm.buySell())
                        .ibOrderID(tradeConfirm.orderID())
                        .orderTime(tradeConfirm.orderTime())
                        .tradeDate(tradeConfirm.tradeDate())
                        .build())
                .collect(Collectors.toList());
        return mapTrades(tradeConfirmTrades);
    }

    @Override
    public List<FinTransaction> mapCorporateAction(List<CorporateAction> corpActions) {
        corpActions = corpActions.stream()
                .sorted(comparing(CorporateAction::reportDate))
                .toList();

        SequencedMap<String, FinTransaction> resultTrans = new LinkedHashMap<>();

        for (CorporateAction corpAction : corpActions) {
            Asset asset = new AssetBuilder()
                    .type(toAssetType(corpAction.assetCategory(), corpAction.assetSubCategory()))
                    .extraType("%s/%s".formatted(corpAction.assetCategory(), corpAction.assetSubCategory()))
                    .country(corpAction.issuerCountryCode())
                    .symbol(stripToNull(corpAction.symbol()))
                    .isin(stripToNull(corpAction.isin()))
                    .countryFigi(stripToNull(corpAction.figi()))
                    .build();

            FinTransactionBuilder tranBldr = switch (corpAction.type()) {
                case MERGED_ACQUISITION -> {
                    Assert.contains(corpAction.description(), "MERGED(Acquisition)");
                    yield new FinTransactionBuilder()
                            .extraType(CorporateActionType.MERGED_ACQUISITION.name())
                            .qty(corpAction.quantity())
                            .price(corpAction.proceeds().divide(corpAction.quantity().abs(), 2, RoundingMode.HALF_UP))
                            .grossValue(corpAction.proceeds())
                            .netValue(corpAction.proceeds());
                }
                case SPIN_OFF -> {
                    Assert.contains(corpAction.description(), "SPINOFF");
                    Assert.zero(corpAction.amount());
                    Assert.zero(corpAction.proceeds());
                    Assert.zero(corpAction.value());
                    yield new FinTransactionBuilder()
                            .extraType(CorporateActionType.SPIN_OFF.name())
                            .qty(corpAction.quantity())
                            .price(ZERO)
                            .grossValue(ZERO)
                            .netValue(ZERO);
                }
                case SPLIT -> {
                    Assert.contains(corpAction.description(), "SPLIT");
                    Assert.zero(corpAction.amount());
                    Assert.zero(corpAction.proceeds());
                    Assert.zero(corpAction.value());
                    yield new FinTransactionBuilder()
                            .extraType(CorporateActionType.SPLIT.name())
                            .qty(corpAction.quantity())
                            .price(ZERO)
                            .grossValue(ZERO)
                            .netValue(ZERO);
                }
            };

            String tranId = getId(corpAction);
            FinTransaction newTran = tranBldr
                    .extraId(tranId)
                    .type(TRANSFORMATION)
                    .date(corpAction.reportDate())
                    .asset(asset)
                    .ccy(corpAction.currency())
                    .tax(ZERO)
                    .fee(ZERO)
                    .extraDetail(corpAction.extraDateTimeStr() + "/" + corpAction.description())
                    .build();
            FinTransaction oldTran = resultTrans.put(tranId, newTran);
            if (oldTran != null) {
                throw new IllegalStateException("ID collision: %s, oldTran=%s, newTran=%s, corpAction=%s".formatted(tranId, oldTran, newTran, corpAction));
            }
        }
        return new ArrayList<>(resultTrans.values());
    }

    private String detectCountryByExchange(String listingExchange) {
        return listingExchange == null || listingExchange.isBlank() ? null : switch (listingExchange) {
            case "NYSE", "NASDAQ" -> "US";
            case "IBIS", "IBIS2" -> "DE";
            default -> throw new IllegalStateException("Unexpected value: " + listingExchange);
        };
    }

    private String getId(CashTransaction cashTran) {
        return "CT/%s/%s".formatted(
                cashTran.reportDate().format(idDf),
                cashTran.transactionID()
        );
    }

    private String getId(CorporateAction corpAction) {
        return "CA/%s/%s".formatted(
                corpAction.reportDate().format(idDf),
                corpAction.actionID()
        );
    }

    private String getId(Trade trade) {
        return "T/%s/%s/%s".formatted(
                trade.reportDate().format(idDf), trade.tradeID(), trade.ibOrderID()
        );
    }

    public AssetType toAssetType(AssetCategory assetCategory, AssetSubCategory assetSubCategory) {
        return switch (assetCategory) {
            case null -> null;
            case STK -> switch (assetSubCategory) {
                case COMMON, ADR, REIT -> AssetType.STOCK;
                case ETF -> AssetType.ETF;
            };
            case CASH -> AssetType.CASH;
        };
    }

}
