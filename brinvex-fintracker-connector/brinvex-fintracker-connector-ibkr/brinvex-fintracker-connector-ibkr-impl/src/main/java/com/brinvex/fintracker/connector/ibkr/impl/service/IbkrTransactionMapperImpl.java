/*
package com.brinvex.fintracker.connector.ibkr.impl.service;

import com.brinvex.fintracker.api.domain.FinTransaction;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CashTransaction;
import com.brinvex.fintracker.connector.ibkr.api.model.statement.CashTransactionType;
import com.brinvex.fintracker.connector.ibkr.api.service.IbkrTransactionMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNullElse;
import static java.util.stream.Collectors.groupingBy;

@SuppressWarnings({"DuplicatedCode"})
public class IbkrTransactionMapperImpl implements IbkrTransactionMapper {

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

            FinTransactionBuilder tranBldr = switch (cashTranType) {
                case DEPOSITS_WITHDRAWALS -> new FinTransactionBuilder()
                        .type(amount.compareTo(ZERO) > 0 ? DEPOSIT : WITHDRAWAL)
                        .qty(ZERO)
                        .grossValue(amount)
                        .netValue(amount)
                        .fees(ZERO);
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
                            assertIsNegative(tax);
                            cashTransToSkip.add(dividTaxTran);
                        }
                        default -> throw new IbkrException(
                                "Found more than one dividendTaxTransaction related to the same dividendTransaction: " +
                                "reportDateAndActionId=%s, dividTran=%s, dividTaxTran=%s"
                                        .formatted(reportDateAndActionId, cashTran, dividTaxTrans));
                    }

                    FinTransactionType tranType = switch (cashTranType) {
                        case DIVIDENDS -> {
                            Util.assertTrue(cashTran.description().contains("CASH DIVIDEND"));
                            yield FinTransactionType.CASH_DIVIDEND;
                        }
                        case PAYMENT_IN_LIEU_OF_DIVIDENDS -> {
                            Util.assertTrue(cashTran.description().contains("PAYMENT IN LIEU OF DIVIDEND (Ordinary Dividend)"));
                            yield FinTransactionType.PAYMENT_IN_LIEU_OF_DIVIDENDS;
                        }
                        default -> throw new AssertionError("Unreachable");
                    };

                    yield new FinTransactionBuilder()
                            .country(detectCountryByExchange(cashTran.listingExchange()))
                            .symbol(stripToNull(cashTran.symbol()))
                            .instrumentType(InstrumentType.fromAssetCategory(cashTran.assetCategory(), cashTran.assetSubCategory()))
                            .figi(stripToNull(cashTran.figi()))
                            .isin(stripToNull(cashTran.isin()))
                            .type(tranType)
                            .grossValue(amount)
                            .qty(ZERO)
                            .fees(ZERO)
                            .netValue(netValue)
                            .tax(tax);
                }
                case WITHHOLDING_TAX -> {
                    Util.assertTrue(cashTran.settleDate().isBefore(cashTran.reportDate()));
                    yield new FinTransactionBuilder()
                            .country(detectCountryByExchange(cashTran.listingExchange()))
                            .symbol(stripToNull(cashTran.symbol()))
                            .instrumentType(InstrumentType.fromAssetCategory(cashTran.assetCategory(), cashTran.assetSubCategory()))
                            .figi(stripToNull(cashTran.figi()))
                            .isin(stripToNull(cashTran.isin()))
                            .type(FinTransactionType.TAX)
                            .grossValue(ZERO)
                            .qty(ZERO)
                            .fees(ZERO)
                            .netValue(amount)
                            .tax(amount);
                }
                case OTHER_FEES, BROKER_INTEREST_PAID, BROKER_FEES -> new FinTransactionBuilder()
                        .country(detectCountryByExchange(cashTran.listingExchange()))
                        .symbol(stripToNull(cashTran.symbol()))
                        .instrumentType(InstrumentType.fromAssetCategory(cashTran.assetCategory(), cashTran.assetSubCategory()))
                        .figi(stripToNull(cashTran.figi()))
                        .isin(stripToNull(cashTran.isin()))
                        .type(FinTransactionType.FEE)
                        .grossValue(ZERO)
                        .qty(ZERO)
                        .fees(amount)
                        .netValue(amount)
                        .tax(ZERO);
            };

            String tranId = getId(cashTran);
            FinTransaction newTran = tranBldr
                    .id(tranId)
                    .reportDate(cashTran.reportDate())
                    .currency(cashTran.currency())
                    .description(cashTran.description())
                    .settleDate(cashTran.settleDate())
                    .extraDateTimeStr(cashTran.extraDateTimeStr())
                    .build();
            FinTransaction oldTran = resultTrans.put(tranId, newTran);
            if (oldTran != null) {
                throw new IbkrException("ID collision: %s, oldTran=%s, newTran=%s, cashTran=%s".formatted(tranId, oldTran, newTran, cashTran));
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
                            Util.assertTrue(buyCcy.equals(ccy));
                            Util.assertTrue(sellCcy.equals(ibCommissionCcy));
                            if (ccy != ibCommissionCcy) {
                                yield new FinTransactionBuilder()
                                        .symbol(buyCcy.name())
                                        .instrumentType(InstrumentType.CASH)
                                        .currency(sellCcy)
                                        .type(FinTransactionType.FX_BUY)
                                        .qty(trade.proceeds())
                                        .price(trade.tradePrice())
                                        .grossValue(trade.quantity())
                                        .netValue(trade.quantity().add(trade.ibCommission()))
                                        .fees(trade.ibCommission());
                            } else {
                                throw new IbkrException("Not yet implemented ccy=%s, ibCommissionCcy=%s, trade=%s"
                                        .formatted(ccy, ibCommissionCcy, trade));
                            }
                        }
                        case BUY -> {
                            Currency buyCcy = Currency.valueOf(trade.symbol().substring(0, 3));
                            Currency sellCcy = Currency.valueOf(trade.symbol().substring(4));
                            Util.assertTrue(sellCcy.equals(ccy));
                            Util.assertTrue(buyCcy.equals(ibCommissionCcy));
                            if (ccy != ibCommissionCcy) {
                                yield new FinTransactionBuilder()
                                        .symbol(buyCcy.name())
                                        .instrumentType(InstrumentType.CASH)
                                        .currency(ccy)
                                        .type(FinTransactionType.FX_BUY)
                                        .qty(trade.quantity())
                                        .price(trade.tradePrice())
                                        .grossValue(trade.proceeds())
                                        .netValue(trade.proceeds().add(trade.ibCommission()))
                                        .fees(trade.ibCommission());
                            } else {
                                throw new IbkrException("Not yet implemented ccy=%s, ibCommissionCcy=%s, trade=%s"
                                        .formatted(ccy, ibCommissionCcy, trade));
                            }
                        }
                    };
                    case STK -> new FinTransactionBuilder()
                            .country(detectCountryByExchange(trade.listingExchange()))
                            .symbol(stripToNull(trade.symbol()))
                            .instrumentType(InstrumentType.fromAssetCategory(trade.assetCategory(), trade.assetSubCategory()))
                            .figi(stripToNull(trade.figi()))
                            .isin(stripToNull(trade.isin()))
                            .currency(ccy)
                            .type(switch (trade.buySell()) {
                                case BUY -> BUY;
                                case SELL -> SELL;
                            })
                            .qty(trade.quantity())
                            .price(trade.tradePrice())
                            .grossValue(trade.proceeds())
                            .netValue(trade.netCash())
                            .fees(fees);
                };
                case FRAC_SHARE -> {
                    Util.assertTrue(trade.ibCommission().compareTo(ZERO) == 0);
                    Util.assertTrue(trade.assetCategory() == AssetCategory.STK);
                    Util.assertTrue(trade.buySell() == BuySell.SELL);
                    BigDecimal grossValue = trade.proceeds();
                    Util.assertTrue(grossValue.compareTo(ZERO) > 0);
                    Util.assertTrue(grossValue.compareTo(trade.tradeMoney().negate()) == 0);
                    Util.assertTrue(grossValue.compareTo(trade.netCash()) == 0);
                    Util.assertTrue(trade.taxes().compareTo(ZERO) == 0);
                    yield new FinTransactionBuilder()
                            .symbol(trade.symbol())
                            .country(detectCountryByExchange(trade.listingExchange()))
                            .isin(trade.isin())
                            .figi(trade.figi())
                            .instrumentType(InstrumentType.STK_COMMON)
                            .currency(trade.currency())
                            .type(FinTransactionType.SELL)
                            .qty(trade.quantity())
                            .price(trade.tradePrice())
                            .grossValue(grossValue)
                            .netValue(grossValue)
                            .fees(trade.ibCommission());
                }
            };

            String tranId = getId(trade);

            FinTransaction newTran = tranBldr
                    .id(tranId)
                    .reportDate(trade.reportDate())
                    .description(trade.description())
                    .settleDate(trade.settleDateTarget())
                    .extraDateTimeStr(trade.extraDateTimeStr())
                    .build();
            FinTransaction oldTran = resultTrans.put(tranId, newTran);
            if (oldTran != null) {
                throw new IbkrException("ID collision: %s, oldTran=%s, newTran=%s, cashTran=%s".formatted(tranId, oldTran, newTran, trade));
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
            FinTransactionBuilder tranBldr = switch (corpAction.type()) {
                case MERGED_ACQUISITION -> {
                    Util.assertTrue(corpAction.description().contains("MERGED(Acquisition)"));
                    yield new FinTransactionBuilder()
                            .country(Country.valueOf(corpAction.issuerCountryCode()))
                            .symbol(stripToNull(corpAction.symbol()))
                            .isin(stripToNull(corpAction.isin()))
                            .figi(stripToNull(corpAction.figi()))
                            .instrumentType(InstrumentType.fromAssetCategory(corpAction.assetCategory(), corpAction.assetSubCategory()))
                            .qty(corpAction.quantity())
                            .price(corpAction.proceeds().divide(corpAction.quantity().abs(), 2, RoundingMode.HALF_UP))
                            .grossValue(corpAction.proceeds())
                            .netValue(corpAction.proceeds())
                            .tax(ZERO)
                            .fees(ZERO);
                }
                case SPIN_OFF -> {
                    Util.assertTrue(corpAction.description().contains("SPINOFF"));
                    Util.assertTrue(corpAction.amount().compareTo(ZERO) == 0);
                    Util.assertTrue(corpAction.proceeds().compareTo(ZERO) == 0);
                    Util.assertTrue(corpAction.value().compareTo(ZERO) == 0);
                    yield new FinTransactionBuilder()
                            .country(Country.valueOf(corpAction.issuerCountryCode()))
                            .symbol(stripToNull(corpAction.symbol()))
                            .isin(stripToNull(corpAction.isin()))
                            .figi(stripToNull(corpAction.figi()))
                            .instrumentType(InstrumentType.fromAssetCategory(corpAction.assetCategory(), corpAction.assetSubCategory()))
                            .qty(corpAction.quantity())
                            .price(ZERO)
                            .grossValue(ZERO)
                            .netValue(ZERO)
                            .tax(ZERO)
                            .fees(ZERO);
                }
                case SPLIT -> {
                    Util.assertTrue(corpAction.description().contains("SPLIT"));
                    Util.assertTrue(corpAction.amount().compareTo(ZERO) == 0);
                    Util.assertTrue(corpAction.proceeds().compareTo(ZERO) == 0);
                    Util.assertTrue(corpAction.value().compareTo(ZERO) == 0);
                    yield new FinTransactionBuilder()
                            .country(Country.valueOf(corpAction.issuerCountryCode()))
                            .symbol(stripToNull(corpAction.symbol()))
                            .isin(stripToNull(corpAction.isin()))
                            .figi(stripToNull(corpAction.figi()))
                            .instrumentType(InstrumentType.fromAssetCategory(corpAction.assetCategory(), corpAction.assetSubCategory()))
                            .qty(corpAction.quantity())
                            .price(ZERO)
                            .grossValue(ZERO)
                            .netValue(ZERO)
                            .tax(ZERO)
                            .fees(ZERO);
                }
            };

            String tranId = getId(corpAction);
            FinTransaction newTran = tranBldr
                    .id(tranId)
                    .reportDate(corpAction.reportDate())
                    .type(TRANSFORMATION)
                    .currency(corpAction.currency())
                    .description(corpAction.description())
                    .extraDateTimeStr(corpAction.extraDateTimeStr())
                    .build();
            FinTransaction oldTran = resultTrans.put(tranId, newTran);
            if (oldTran != null) {
                throw new IbkrException("ID collision: %s, oldTran=%s, newTran=%s, corpAction=%s".formatted(tranId, oldTran, newTran, corpAction));
            }
        }
        return new ArrayList<>(resultTrans.values());
    }

    private Country detectCountryByExchange(String listingExchange) {
        return listingExchange == null || listingExchange.isBlank() ? null : switch (listingExchange) {
            case "NYSE", "NASDAQ" -> Country.US;
            case "IBIS", "IBIS2" -> Country.DE;
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

}*/
