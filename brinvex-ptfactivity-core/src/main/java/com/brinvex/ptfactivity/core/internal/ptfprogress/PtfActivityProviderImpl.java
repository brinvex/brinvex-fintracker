package com.brinvex.ptfactivity.core.internal.ptfprogress;

import com.brinvex.finance.types.enu.Currency;
import com.brinvex.ptfactivity.core.api.domain.Account;
import com.brinvex.ptfactivity.core.api.domain.Asset;
import com.brinvex.ptfactivity.core.api.domain.PtfActivity;
import com.brinvex.ptfactivity.core.api.domain.PtfActivityReq;
import com.brinvex.ptfactivity.core.api.domain.enu.AssetType;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction;
import com.brinvex.ptfactivity.core.api.domain.FinTransaction.FinTransactionBuilder;
import com.brinvex.ptfactivity.core.api.domain.enu.FinTransactionType;
import com.brinvex.ptfactivity.core.api.provider.PtfActivityProvider;
import com.brinvex.dms.api.Dms;
import com.brinvex.java.StringUtil;
import com.brinvex.csv.CsvConfig;
import com.brinvex.csv.CsvTable;
import com.brinvex.java.validation.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.brinvex.java.NullUtil.nullSafe;
import static com.brinvex.java.StringUtil.stripToNull;
import static java.util.Objects.requireNonNullElse;

public class PtfActivityProviderImpl implements PtfActivityProvider {

    private final Dms dms;

    public PtfActivityProviderImpl(Dms dms) {
        this.dms = dms;
    }

    @Override
    public boolean supports(PtfActivityReq ptfActivityReq) {
        String reqPtfProgressProvider = ptfActivityReq.providerName();
        return reqPtfProgressProvider == null || reqPtfProgressProvider.equals("core");
    }

    @Override
    public PtfActivity process(PtfActivityReq ptfActivityReq) {
        Assert.isTrue(supports(ptfActivityReq));
        Account account = ptfActivityReq.account();
        String docDir = requireNonNullElse(account.name(), account.externalId());
        LocalDate fromDateIncl = ptfActivityReq.fromDateIncl();
        LocalDate toDateIncl = ptfActivityReq.toDateIncl();
        List<String> transCsvLines = dms.getTextLines(docDir, docDir + "_transactions.csv");
        List<FinTransaction> finTrans = parseFinTransactions(transCsvLines, fromDateIncl, toDateIncl);
        return new PtfActivity(finTrans, null);
    }

    private static List<FinTransaction> parseFinTransactions(List<String> transCsvLines, LocalDate fromDateIncl, LocalDate toDateIncl) {
        CsvTable csvTable = CsvTable.of(transCsvLines, CsvConfig.builder()
                .separator(',')
                .quoteCharacter('"')
                .build());

        List<FinTransaction> finTrans = new ArrayList<>();
        for (int br = 0; br < csvTable.bodySize(); br++) {
            Map<String, String> row = csvTable.bodyRow(br);
            LocalDate date = LocalDate.parse(row.get("date"));
            if (date.isBefore(fromDateIncl) || date.isAfter(toDateIncl)) {
                continue;
            }
            FinTransactionBuilder tranBuilder = FinTransaction.builder();
            tranBuilder.date(date);
            tranBuilder.type(FinTransactionType.valueOf(row.get("type")));
            tranBuilder.ccy(nullSafe(row.get("ccy"), StringUtil::stripToNull, Currency::valueOf));
            tranBuilder.netValue(nullSafe(row.get("netValue"), StringUtil::stripToNull, BigDecimal::new));
            tranBuilder.qty(nullSafe(row.get("qty"), StringUtil::stripToNull, BigDecimal::new));
            tranBuilder.price(nullSafe(row.get("price"), StringUtil::stripToNull, BigDecimal::new));

            AssetType assetType = nullSafe(row.get("asset_type"), StringUtil::stripToNull, AssetType::valueOf);
            if (assetType != null) {
                Asset.AssetBuilder assetBuilder = Asset.builder();
                assetBuilder.type(assetType);
                assetBuilder.country(stripToNull(row.get("asset_country")));
                assetBuilder.symbol(stripToNull(row.get("asset_symbol")));
                assetBuilder.name(stripToNull(row.get("asset_name")));
                assetBuilder.countryFigi(stripToNull(row.get("asset_countryFigi")));
                assetBuilder.isin(stripToNull(row.get("asset_isin")));
                assetBuilder.extraType(stripToNull(row.get("asset_extraType")));
                assetBuilder.extraDetail(stripToNull(row.get("asset_extraDetail")));
                tranBuilder.asset(assetBuilder.build());
            }

            tranBuilder.grossValue(nullSafe(row.get("grossValue"), StringUtil::stripToNull, BigDecimal::new));
            tranBuilder.tax(nullSafe(row.get("tax"), StringUtil::stripToNull, BigDecimal::new));
            tranBuilder.fee(nullSafe(row.get("fee"), StringUtil::stripToNull, BigDecimal::new));
            tranBuilder.settleDate(nullSafe(row.get("settleDate"), LocalDate::parse));
            tranBuilder.groupId(stripToNull(row.get("groupId")));
            tranBuilder.externalId(stripToNull(row.get("externalId")));
            tranBuilder.externalType(stripToNull(row.get("externalType")));
            tranBuilder.externalDetail(stripToNull(row.get("externalDetail")));

            finTrans.add(tranBuilder.build());
        }
        return finTrans;
    }
}
