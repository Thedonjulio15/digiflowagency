package Exchanges;

import java.io.Closeable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import Model.TradeRecords;

public interface Exchange_Client extends Closeable {

    String name();

    void startDepthStreams(List<String> symbols);

    void startFuturesDepthStreams(List<String> symbols);

    Map<String, BigDecimal> getAllFundingRates();

    double takerFee();

    double makerFee();

    BigDecimal getBalance(String asset, boolean isFutures) throws Exception;

    TradeRecords.TradeLeg placeOrder(String symbol, String side, BigDecimal quantity, boolean isFutures) throws Exception;

    // memo is null for assets that don't need one (BTC, SOL, LTC, etc.)
    // memo is the destination tag / memo for XRP, XLM, etc.
    boolean transferAsset(String asset, BigDecimal quantity, String toAddress, String memo) throws Exception;

    @Override
    void close();
}
