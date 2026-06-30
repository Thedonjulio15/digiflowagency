package Model;

import java.math.BigDecimal;

public class OpportunityCostBreakdown {

    private final String symbol;
    private final String buyExchange;
    private final String sellExchange;
    private final long timestamp;
    private final long dataAgeMs;

    private final BigDecimal spotPrice;
    private final BigDecimal futuresPrice;

    private final BigDecimal quantity;
    private final BigDecimal notionalUSDT;

    private final BigDecimal rawSpread;
    private final BigDecimal rawSpreadPercent;

    private final BigDecimal spotTradingFee;
    private final BigDecimal futuresTradingFee;
    private final BigDecimal fundingCost;
    private final BigDecimal slippageCost;
    private final BigDecimal totalCosts;
    private final BigDecimal totalCostsPercent;

    private final BigDecimal netProfit;
    private final BigDecimal netProfitPercent;

    private final BigDecimal fundingRate;
    private final boolean actionable;
    private final BigDecimal transferFee;
    private final long transferTimeMs;
    private final boolean crossExchange;
    private final boolean settlementConfirmed;

    public OpportunityCostBreakdown(
        String symbol, String buyExchange, String sellExchange,
        long timestamp, long dataAgeMs,
        BigDecimal spotPrice, BigDecimal futuresPrice,
        BigDecimal quantity, BigDecimal notionalUSDT,
        BigDecimal rawSpread, BigDecimal rawSpreadPercent,
        BigDecimal spotTradingFee, BigDecimal futuresTradingFee,
        BigDecimal fundingCost, BigDecimal slippageCost,
        BigDecimal totalCosts, BigDecimal totalCostsPercent,
        BigDecimal netProfit, BigDecimal netProfitPercent,
        BigDecimal fundingRate, boolean actionable,
        BigDecimal transferFee, long transferTimeMs,
        boolean crossExchange, boolean settlementConfirmed) {

        this.symbol = symbol;
        this.buyExchange = buyExchange;
        this.sellExchange = sellExchange;
        this.timestamp = timestamp;
        this.dataAgeMs = dataAgeMs;
        this.spotPrice = spotPrice;
        this.futuresPrice = futuresPrice;
        this.quantity = quantity;
        this.notionalUSDT = notionalUSDT;
        this.rawSpread = rawSpread;
        this.rawSpreadPercent = rawSpreadPercent;
        this.spotTradingFee = spotTradingFee;
        this.futuresTradingFee = futuresTradingFee;
        this.fundingCost = fundingCost;
        this.slippageCost = slippageCost;
        this.totalCosts = totalCosts;
        this.totalCostsPercent = totalCostsPercent;
        this.netProfit = netProfit;
        this.netProfitPercent = netProfitPercent;
        this.fundingRate = fundingRate;
        this.actionable = actionable;
        this.transferFee = transferFee;
        this.transferTimeMs = transferTimeMs;
        this.crossExchange = crossExchange;
        this.settlementConfirmed = settlementConfirmed;
    }

    public String getSymbol()                  { return symbol; }
    public String getBuyExchange()             { return buyExchange; }
    public String getSellExchange()            { return sellExchange; }
    public long getTimestamp()                 { return timestamp; }
    public long getDataAgeMs()                 { return dataAgeMs; }
    public BigDecimal getSpotPrice()           { return spotPrice; }
    public BigDecimal getFuturesPrice()        { return futuresPrice; }
    public BigDecimal getQuantity()            { return quantity; }
    public BigDecimal getNotionalUSDT()        { return notionalUSDT; }
    public BigDecimal getRawSpread()           { return rawSpread; }
    public BigDecimal getRawSpreadPercent()    { return rawSpreadPercent; }
    public BigDecimal getSpotTradingFee()      { return spotTradingFee; }
    public BigDecimal getFuturesTradingFee()   { return futuresTradingFee; }
    public BigDecimal getFundingCost()         { return fundingCost; }
    public BigDecimal getSlippageCost()        { return slippageCost; }
    public BigDecimal getTotalCosts()          { return totalCosts; }
    public BigDecimal getTotalCostsPercent()   { return totalCostsPercent; }
    public BigDecimal getNetProfit()           { return netProfit; }
    public BigDecimal getNetProfitPercent()    { return netProfitPercent; }
    public BigDecimal getFundingRate()         { return fundingRate; }
    public boolean isActionable()              { return actionable; }
    public BigDecimal getTransferFee()         { return transferFee; }
    public long getTransferTimeMs()            { return transferTimeMs; }
    public boolean isCrossExchange()           { return crossExchange; }
    public boolean isSettlementConfirmed()     { return settlementConfirmed; }

    public String toCSVRow() {
        return timestamp + "," + symbol + "," + buyExchange + "," + sellExchange + "," +
            spotPrice.toPlainString() + "," + futuresPrice.toPlainString() + "," +
            quantity.toPlainString() + "," + notionalUSDT.toPlainString() + "," +
            rawSpread.toPlainString() + "," + rawSpreadPercent.toPlainString() + "," +
            spotTradingFee.toPlainString() + "," + futuresTradingFee.toPlainString() + "," +
            fundingCost.toPlainString() + "," + slippageCost.toPlainString() + "," +
            totalCosts.toPlainString() + "," + totalCostsPercent.toPlainString() + "," +
            netProfit.toPlainString() + "," + netProfitPercent.toPlainString() + "," +
            fundingRate.toPlainString() + "," + dataAgeMs + "," + actionable;
    }
}
