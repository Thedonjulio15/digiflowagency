package Model;

import java.math.BigDecimal;

public class TradeRecords {

    public static class TradeLeg {
        public final String orderId;
        public final String exchange;
        public final String symbol;
        public final String side;
        public final String market;
        public final BigDecimal quantity;
        public final BigDecimal executedPrice;
        public final BigDecimal fee;
        public final long executedTimestamp;
        public final String status;
        public final String statusMessage;

        public TradeLeg(
            String orderId,
            String exchange,
            String symbol,
            String side,
            String market,
            BigDecimal quantity,
            BigDecimal executedPrice,
            BigDecimal fee,
            long executedTimestamp,
            String status,
            String statusMessage
        ) {
            this.orderId = orderId;
            this.exchange = exchange;
            this.symbol = symbol;
            this.side = side;
            this.market = market;
            this.quantity = quantity;
            this.executedPrice = executedPrice;
            this.fee = fee;
            this.executedTimestamp = executedTimestamp;
            this.status = status;
            this.statusMessage = statusMessage;
        }

        public BigDecimal getGrossProceeds() {
            if (executedPrice == null || quantity == null) return BigDecimal.ZERO;
            return executedPrice.multiply(quantity);
        }

        public BigDecimal getNetProceeds() {
            BigDecimal gross = getGrossProceeds();
            if (fee == null) return gross;
            if (side.equalsIgnoreCase("SELL") || side.equalsIgnoreCase("SHORT")) {
                return gross.subtract(fee);
            } else {
                return gross.add(fee);
            }
        }

        @Override
        public String toString() {
            return String.format(
                "TradeLeg{id=%s, %s:%s %s %.8f @ $%.8f, fee=$%.2f, status=%s}",
                orderId, exchange, symbol, side, quantity, executedPrice, fee, status
            );
        }
    }

    public static class RoundTripTrade {
        public final TradeLeg spotBuyLeg;
        public final TradeLeg futuresShortLeg;
        public final long transferStartTime;
        public final long transferEndTime;
        public final BigDecimal transferredQuantity;
        public final BigDecimal transferFeeApplied;
        public final TradeLeg spotSellLeg;
        public final TradeLeg futuresCoverLeg;
        public final BigDecimal scannerPredictedProfitPercent;
        public final BigDecimal actualRealisedProfitPercent;
        public final long timeToCompletionMs;
        public final String status;

        public RoundTripTrade(
            TradeLeg spotBuyLeg,
            TradeLeg futuresShortLeg,
            long transferStartTime,
            long transferEndTime,
            BigDecimal transferredQuantity,
            BigDecimal transferFeeApplied,
            TradeLeg spotSellLeg,
            TradeLeg futuresCoverLeg,
            BigDecimal scannerPredictedProfitPercent,
            BigDecimal actualRealisedProfitPercent,
            long timeToCompletionMs,
            String status
        ) {
            this.spotBuyLeg = spotBuyLeg;
            this.futuresShortLeg = futuresShortLeg;
            this.transferStartTime = transferStartTime;
            this.transferEndTime = transferEndTime;
            this.transferredQuantity = transferredQuantity;
            this.transferFeeApplied = transferFeeApplied;
            this.spotSellLeg = spotSellLeg;
            this.futuresCoverLeg = futuresCoverLeg;
            this.scannerPredictedProfitPercent = scannerPredictedProfitPercent;
            this.actualRealisedProfitPercent = actualRealisedProfitPercent;
            this.timeToCompletionMs = timeToCompletionMs;
            this.status = status;
        }

        public String getSymbol() {
            return spotBuyLeg != null ? spotBuyLeg.symbol : "UNKNOWN";
        }

        public boolean isComplete() {
            return spotBuyLeg != null && spotBuyLeg.status.equals("FILLED")
                && futuresShortLeg != null && futuresShortLeg.status.equals("FILLED")
                && spotSellLeg != null && spotSellLeg.status.equals("FILLED")
                && futuresCoverLeg != null && futuresCoverLeg.status.equals("FILLED");
        }

        public long getTransferDurationMs() {
            if (transferStartTime == 0 || transferEndTime == 0) return 0;
            return transferEndTime - transferStartTime;
        }

        public BigDecimal getProfitVariance() {
            if (scannerPredictedProfitPercent == null || actualRealisedProfitPercent == null) return BigDecimal.ZERO;
            return actualRealisedProfitPercent.subtract(scannerPredictedProfitPercent);
        }

        public BigDecimal getTotalFeesPaid() {
            BigDecimal total = BigDecimal.ZERO;
            if (spotBuyLeg != null && spotBuyLeg.fee != null) total = total.add(spotBuyLeg.fee);
            if (futuresShortLeg != null && futuresShortLeg.fee != null) total = total.add(futuresShortLeg.fee);
            if (spotSellLeg != null && spotSellLeg.fee != null) total = total.add(spotSellLeg.fee);
            if (futuresCoverLeg != null && futuresCoverLeg.fee != null) total = total.add(futuresCoverLeg.fee);
            if (transferFeeApplied != null) total = total.add(transferFeeApplied);
            return total;
        }

        @Override
        public String toString() {
            return String.format(
                "RoundTripTrade{symbol=%s, predicted=%.4f%%, actual=%.4f%%, status=%s, duration=%dms}",
                getSymbol(),
                scannerPredictedProfitPercent != null ? scannerPredictedProfitPercent.doubleValue() : 0,
                actualRealisedProfitPercent != null ? actualRealisedProfitPercent.doubleValue() : 0,
                status,
                timeToCompletionMs
            );
        }
    }
}
