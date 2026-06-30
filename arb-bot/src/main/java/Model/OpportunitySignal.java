package Model;

import java.math.BigDecimal;

public class OpportunitySignal {

    public final String symbol;
    public final String buyExchange;
    public final String sellExchange;
    public final BigDecimal spotPrice;
    public final BigDecimal futuresPrice;
    public final BigDecimal rawSpreadPercent;
    public final BigDecimal netProfitPercent;
    public final BigDecimal fundingRate;
    public final BigDecimal transferFee;
    public final long timestamp;
    public final long dataAgeMs;

    public OpportunitySignal(
        String symbol, String buyExchange, String sellExchange,
        BigDecimal spotPrice, BigDecimal futuresPrice,
        BigDecimal rawSpreadPercent, BigDecimal netProfitPercent,
        BigDecimal fundingRate, BigDecimal transferFee,
        long timestamp, long dataAgeMs) {

        this.symbol = symbol;
        this.buyExchange = buyExchange;
        this.sellExchange = sellExchange;
        this.spotPrice = spotPrice;
        this.futuresPrice = futuresPrice;
        this.rawSpreadPercent = rawSpreadPercent;
        this.netProfitPercent = netProfitPercent;
        this.fundingRate = fundingRate;
        this.transferFee = transferFee;
        this.timestamp = timestamp;
        this.dataAgeMs = dataAgeMs;
    }

    @Override
    public String toString() {
        return "OpportunitySignal{" +
            "symbol='" + symbol + '\'' +
            ", buyExchange='" + buyExchange + '\'' +
            ", sellExchange='" + sellExchange + '\'' +
            ", spotPrice=" + spotPrice.toPlainString() +
            ", futuresPrice=" + futuresPrice.toPlainString() +
            ", rawSpreadPercent=" + rawSpreadPercent.toPlainString() +
            ", netProfitPercent=" + netProfitPercent.toPlainString() +
            ", fundingRate=" + fundingRate.toPlainString() +
            ", transferFee=" + transferFee.toPlainString() +
            ", timestamp=" + timestamp +
            ", dataAgeMs=" + dataAgeMs +
            '}';
    }
}
