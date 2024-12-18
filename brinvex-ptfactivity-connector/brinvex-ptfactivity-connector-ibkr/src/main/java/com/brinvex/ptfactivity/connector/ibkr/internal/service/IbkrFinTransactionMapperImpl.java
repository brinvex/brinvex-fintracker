package com.brinvex.ptfactivity.connector.ibkr.internal.service;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.AssetSubCategory;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.BuySell;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransactionType;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CorporateAction;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CorporateActionType;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.Trade;
import com.brinvex.ptfactivity.connector.ibkr.api.model.statement.TradeConfirm;
import com.brinvex.ptfactivity.connector.ibkr.api.service.IbkrFinTransactionMapper;
import com.brinvex.ptfactivity.connector.ibkr.internal.builder.TradeBuilder;
import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.enu.AssetType;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction.FinTransactionBuilder;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.java.validation.Assert;

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

import static com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransactionType.DEPOSITS_WITHDRAWALS;
import static com.brinvex.ptfactivity.connector.ibkr.api.model.statement.CashTransactionType.WITHHOLDING_TAX;
import static com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType.BUY;
import static com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType.DEPOSIT;
import static com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType.SELL;
import static com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType.TRANSFORMATION;
import static com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType.WITHDRAWAL;
import static com.brinvex.java.StringUtil.stripToNull;
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
                case DEPOSITS_WITHDRAWALS -> FinTransaction.builder()
                        .type(amount.compareTo(ZERO) > 0 ? DEPOSIT : WITHDRAWAL)
                        .externalType(DEPOSITS_WITHDRAWALS.name())
                        .qty(ZERO)
                        .grossValue(amount)
                        .netValue(amount)
                        .tax(ZERO)
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
                            Assert.isTrue(cashTran.description().contains("CASH DIVIDEND"));
                            yield "CASH DIVIDEND";
                        }
                        case PAYMENT_IN_LIEU_OF_DIVIDENDS -> {
                            Assert.isTrue(cashTran.description().contains("PAYMENT IN LIEU OF DIVIDEND (Ordinary Dividend)"));
                            yield "PAYMENT_IN_LIEU_OF_DIVIDENDS";
                        }
                        default -> throw new AssertionError("Unreachable");
                    };

                    yield FinTransaction.builder()
                            .type(FinTransactionType.DIVIDEND)
                            .externalType(extraTranType)
                            .asset(Asset.builder()
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
                    Assert.isTrue(cashTran.settleDate().isBefore(cashTran.reportDate()));
                    yield FinTransaction.builder()
                            .type(FinTransactionType.TAX)
                            .externalType(WITHHOLDING_TAX.name())
                            .asset(Asset.builder()
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
                    Assert.empty(cashTran.listingExchange());
                    Assert.empty(cashTran.symbol());
                    Assert.empty(cashTran.figi());
                    Assert.empty(cashTran.isin());
                    yield FinTransaction.builder()
                            .type(FinTransactionType.FEE)
                            .externalType(cashTranType.name())
                            .grossValue(ZERO)
                            .qty(ZERO)
                            .fee(amount)
                            .netValue(amount)
                            .tax(ZERO);
                }
            };

            String tranId = getId(cashTran);
            FinTransaction newTran = finTranBldr
                    .externalId(tranId)
                    .date(cashTran.reportDate())
                    .ccy(cashTran.currency())
                    .externalDetail(cashTran.extraDateTimeStr() + "/" + cashTran.description())
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
            Currency ccy = trade.currency();
            Currency ibCommissionCcy = trade.ibCommissionCurrency();
            BigDecimal fees = requireNonNullElse(trade.ibCommission(), ZERO);

            FinTransactionBuilder tranBldr = switch (trade.transactionType()) {
                case EXCH_TRADE -> switch (trade.assetCategory()) {
                    case CASH -> switch (trade.buySell()) {
                        case SELL -> {
                            Currency sellCcy = Currency.valueOf(trade.symbol().substring(0, 3));
                            Currency buyCcy = Currency.valueOf(trade.symbol().substring(4));
                            Assert.equal(buyCcy, ccy);
                            Assert.equal(sellCcy, ibCommissionCcy);
                            Assert.zero(trade.taxes());
                            if (!ccy.equals(ibCommissionCcy)) {
                                yield FinTransaction.builder()
                                        .type(FinTransactionType.FX_BUY)
                                        .asset(Asset.builder()
                                                .type(AssetType.CASH)
                                                .symbol(buyCcy.name())
                                                .build())
                                        .ccy(sellCcy)
                                        .qty(trade.proceeds())
                                        .price(trade.tradePrice())
                                        .grossValue(trade.quantity())
                                        .netValue(trade.quantity().add(trade.ibCommission()))
                                        .fee(trade.ibCommission())
                                        .tax(ZERO);
                            } else {
                                throw new IllegalStateException("ccy=%s, ibCommissionCcy=%s, trade=%s"
                                        .formatted(ccy, ibCommissionCcy, trade));
                            }
                        }
                        case BUY -> {
                            Currency buyCcy = Currency.valueOf(trade.symbol().substring(0, 3));
                            Currency sellCcy = Currency.valueOf(trade.symbol().substring(4));
                            Assert.equal(sellCcy, ccy);
                            Assert.equal(buyCcy, ibCommissionCcy);
                            Assert.zero(trade.taxes());
                            if (!Objects.equals(ccy, ibCommissionCcy)) {
                                yield FinTransaction.builder()
                                        .type(FinTransactionType.FX_BUY)
                                        .asset(Asset.builder()
                                                .type(AssetType.CASH)
                                                .symbol(buyCcy.name())
                                                .build())
                                        .ccy(ccy)
                                        .qty(trade.quantity())
                                        .price(trade.tradePrice())
                                        .grossValue(trade.proceeds())
                                        .netValue(trade.proceeds().add(trade.ibCommission()))
                                        .fee(trade.ibCommission())
                                        .tax(ZERO);
                            } else {
                                throw new IllegalStateException("ccy=%s, ibCommissionCcy=%s, trade=%s"
                                        .formatted(ccy, ibCommissionCcy, trade));
                            }
                        }
                    };
                    case STK -> {
                        Assert.zero(trade.taxes());
                        yield FinTransaction.builder()
                                .type(switch (trade.buySell()) {
                                    case BUY -> BUY;
                                    case SELL -> SELL;
                                })
                                .externalType(trade.transactionType().name())
                                .asset(Asset.builder()
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
                                .fee(fees)
                                .tax(ZERO);
                    }
                };
                case FRAC_SHARE -> {
                    Assert.zero(trade.ibCommission());
                    Assert.equal(trade.assetCategory(), AssetCategory.STK);
                    Assert.equal(trade.buySell(), BuySell.SELL);
                    BigDecimal grossValue = trade.proceeds();
                    Assert.positive(grossValue);
                    Assert.equal(grossValue, trade.tradeMoney().negate());
                    Assert.equal(grossValue, trade.netCash());
                    Assert.zero(trade.taxes());
                    yield FinTransaction.builder()
                            .type(FinTransactionType.SELL)
                            .asset(Asset.builder()
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
                            .fee(trade.ibCommission())
                            .tax(ZERO);
                }
            };

            String tranId = getId(trade);

            FinTransaction newTran = tranBldr
                    .externalId(tranId)
                    .date(trade.reportDate())
                    .externalDetail(trade.extraDateTimeStr() + "/" + trade.description())
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
            Asset asset = Asset.builder()
                    .type(toAssetType(corpAction.assetCategory(), corpAction.assetSubCategory()))
                    .extraType("%s/%s".formatted(corpAction.assetCategory(), corpAction.assetSubCategory()))
                    .country(corpAction.issuerCountryCode())
                    .symbol(stripToNull(corpAction.symbol()))
                    .isin(stripToNull(corpAction.isin()))
                    .countryFigi(stripToNull(corpAction.figi()))
                    .build();

            FinTransactionBuilder tranBldr = switch (corpAction.type()) {
                case MERGED_ACQUISITION -> {
                    Assert.isTrue(corpAction.description().contains("MERGED(Acquisition)"));
                    yield FinTransaction.builder()
                            .externalType(CorporateActionType.MERGED_ACQUISITION.name())
                            .qty(corpAction.quantity())
                            .price(corpAction.proceeds().divide(corpAction.quantity().abs(), 2, RoundingMode.HALF_UP))
                            .grossValue(corpAction.proceeds())
                            .netValue(corpAction.proceeds());
                }
                case SPIN_OFF -> {
                    Assert.isTrue(corpAction.description().contains("SPINOFF"));
                    Assert.zero(corpAction.amount());
                    Assert.zero(corpAction.proceeds());
                    Assert.zero(corpAction.value());
                    yield FinTransaction.builder()
                            .externalType(CorporateActionType.SPIN_OFF.name())
                            .qty(corpAction.quantity())
                            .grossValue(ZERO)
                            .netValue(ZERO);
                }
                case SPLIT -> {
                    Assert.isTrue(corpAction.description().contains("SPLIT"));
                    Assert.zero(corpAction.amount());
                    Assert.zero(corpAction.proceeds());
                    Assert.zero(corpAction.value());
                    yield FinTransaction.builder()
                            .externalType(CorporateActionType.SPLIT.name())
                            .qty(corpAction.quantity())
                            .grossValue(ZERO)
                            .netValue(ZERO);
                }
            };

            String tranId = getId(corpAction);
            FinTransaction newTran = tranBldr
                    .externalId(tranId)
                    .type(TRANSFORMATION)
                    .date(corpAction.reportDate())
                    .asset(asset)
                    .ccy(corpAction.currency())
                    .tax(ZERO)
                    .fee(ZERO)
                    .externalDetail(corpAction.extraDateTimeStr() + "/" + corpAction.description())
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
