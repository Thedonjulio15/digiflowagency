package Scanner_Core;

import Config.JarvisConfig;
import Exchanges.Exchange_Client;
import Logger.ArcReactorCSV;
import Model.OpportunityCostBreakdown;
import Model.TradeRecords;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class OpportunityScanner {

    // ── PRICE STORAGE ────────────────────────────────────────────────

    public static class PriceData {
        public final BigDecimal price;
        public final long timestamp;
        public PriceData(BigDecimal price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }

    private final ConcurrentHashMap<String, PriceData> binanceSpotPrices    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PriceData> binanceFuturesPrices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PriceData> bybitSpotPrices      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, PriceData> bybitFuturesPrices   = new ConcurrentHashMap<>();

    private final long dataFreshnessThresholdMs;

    // ── CONFIG ───────────────────────────────────────────────────────

    private final JarvisConfig config;
    private final Map<String, BigDecimal> fundingRates;
    private final ArcReactorCSV csvLogger;
    private final Set<String> symbols;

    // ── EXECUTION ────────────────────────────────────────────────────

    private final ExecutorService tradeExecutor;
    private Exchange_Client binanceConnector;
    private Exchange_Client bybitConnector;
    private final AtomicInteger activeTrades = new AtomicInteger(0);
    private final boolean executionEnabled;
    private final boolean simulationMode;

    // ── CONSTRUCTOR ──────────────────────────────────────────────────

    public OpportunityScanner(JarvisConfig config,
                              Map<String, BigDecimal> fundingRates,
                              ArcReactorCSV csvLogger,
                              Set<String> symbols) {
        this.config = config;
        this.fundingRates = fundingRates;
        this.csvLogger = csvLogger;
        this.symbols = symbols;
        this.dataFreshnessThresholdMs = config.getDataFreshnessThresholdMs();
        this.executionEnabled = config.isExecutionEnabled();
        this.simulationMode = config.isSimulationMode();
        this.tradeExecutor = Executors.newFixedThreadPool(config.getTradeHandlerThreadPoolSize());
        logConfiguration();
    }

    public void setConnectors(Exchange_Client binance, Exchange_Client bybit) {
        this.binanceConnector = binance;
        this.bybitConnector = bybit;
    }

    // ── PRICE UPDATES (called by connectors via callbacks) ───────────

    public synchronized void updateBinanceSpotPrice(String symbol, BigDecimal price) {
        binanceSpotPrices.put(symbol, new PriceData(price, System.currentTimeMillis()));
    }

    public synchronized void updateBinanceFuturesPrice(String symbol, BigDecimal price) {
        binanceFuturesPrices.put(symbol, new PriceData(price, System.currentTimeMillis()));
    }

    public synchronized void updateBybitSpotPrice(String symbol, BigDecimal price) {
        bybitSpotPrices.put(symbol, new PriceData(price, System.currentTimeMillis()));
    }

    public synchronized void updateBybitFuturesPrice(String symbol, BigDecimal price) {
        bybitFuturesPrices.put(symbol, new PriceData(price, System.currentTimeMillis()));
    }

    // ── FRESHNESS CHECKS ─────────────────────────────────────────────

    private boolean isRouteFresh(String buyExch, String sellExch, String symbol) {
        PriceData spot = getSpotPrice(buyExch, symbol);
        PriceData fut = getFuturesPrice(sellExch, symbol);
        if (spot == null || fut == null) return false;
        long delta = Math.abs(spot.timestamp - fut.timestamp);
        return delta <= dataFreshnessThresholdMs;
    }

    private long getRouteDataAgeMs(String buyExch, String sellExch, String symbol) {
        PriceData spot = getSpotPrice(buyExch, symbol);
        PriceData fut = getFuturesPrice(sellExch, symbol);
        if (spot == null || fut == null) return Long.MAX_VALUE;
        return Math.abs(spot.timestamp - fut.timestamp);
    }

    // ── MAIN SCAN ENTRY POINT ────────────────────────────────────────

    public void checkOpportunity(String symbol) {
        try {
            if (isRouteFresh("BYBIT", "BINANCE", symbol)) {
                scanRoute(symbol, "BYBIT", "BINANCE", getRouteDataAgeMs("BYBIT", "BINANCE", symbol));
            }
            if (isRouteFresh("BINANCE", "BYBIT", symbol)) {
                scanRoute(symbol, "BINANCE", "BYBIT", getRouteDataAgeMs("BINANCE", "BYBIT", symbol));
            }
        } catch (Exception e) {
            System.err.println("Error scanning " + symbol + ": " + e.getMessage());
        }
    }

    // ── ROUTE SCAN ───────────────────────────────────────────────────

    private void scanRoute(String symbol, String buyExch, String sellExch, long dataAgeMs) {
        try {
            PriceData spotData    = getSpotPrice(buyExch, symbol);
            PriceData futuresData = getFuturesPrice(sellExch, symbol);

            if (spotData == null || futuresData == null) return;

            BigDecimal spotPrice    = spotData.price;
            BigDecimal futuresPrice = futuresData.price;

            if (spotPrice.compareTo(BigDecimal.ZERO) <= 0 ||
                futuresPrice.compareTo(BigDecimal.ZERO) <= 0) return;

            BigDecimal notionalUSDT = config.getNotionalUSDT();
            BigDecimal quantity = notionalUSDT.divide(spotPrice, 8, RoundingMode.HALF_UP);

            BigDecimal rawSpread = futuresPrice.subtract(spotPrice);
            BigDecimal rawSpreadPercent = rawSpread.divide(spotPrice, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

            BigDecimal spotTradingFee    = spotPrice.multiply(quantity).multiply(config.getSpotTakerFee(buyExch));
            BigDecimal futuresTradingFee = futuresPrice.multiply(quantity).multiply(config.getFuturesTakerFee(sellExch));

            BigDecimal fundingRate = fundingRates.getOrDefault(symbol, BigDecimal.ZERO);
            BigDecimal fundingPeriods = BigDecimal.valueOf(config.getExpectedHoldHours())
                .divide(BigDecimal.valueOf(8), 4, RoundingMode.HALF_UP);
            BigDecimal fundingCost = futuresPrice.multiply(quantity).multiply(fundingRate).multiply(fundingPeriods);

            BigDecimal slippageRate = config.getSlippageEstimate();
            BigDecimal slippageCost = spotPrice.multiply(quantity).multiply(slippageRate)
                .add(futuresPrice.multiply(quantity).multiply(slippageRate));

            String asset = symbol.replace("USDT", "").replace("USDC", "");
            BigDecimal transferFee = config.getTransferFee(buyExch, asset);

            BigDecimal totalCosts = spotTradingFee.add(futuresTradingFee).add(fundingCost).add(slippageCost).add(transferFee);
            BigDecimal totalCostsPercent = totalCosts.divide(notionalUSDT, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

            BigDecimal grossProfit  = rawSpread.multiply(quantity);
            BigDecimal netProfit    = grossProfit.subtract(totalCosts);
            BigDecimal netProfitPercent = netProfit.divide(notionalUSDT, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

            BigDecimal threshold = config.getThresholdForSymbol(symbol);
            boolean actionable = netProfitPercent.compareTo(threshold) >= 0;

            OpportunityCostBreakdown breakdown = new OpportunityCostBreakdown(
                symbol, buyExch, sellExch,
                Instant.now().toEpochMilli(), dataAgeMs,
                spotPrice, futuresPrice,
                quantity, notionalUSDT,
                rawSpread, rawSpreadPercent,
                spotTradingFee, futuresTradingFee, fundingCost, slippageCost,
                totalCosts, totalCostsPercent,
                netProfit, netProfitPercent,
                fundingRate, actionable,
                transferFee, 0L, true, false
            );

            if (actionable) {
                logOpportunity(breakdown);
                csvLogger.logOpportunity(breakdown);

                if (executionEnabled && binanceConnector != null && bybitConnector != null) {
                    int current = activeTrades.get();
                    if (current < config.getMaxConcurrentTrades()) {
                        activeTrades.incrementAndGet();
                        final OpportunityCostBreakdown finalBreakdown = breakdown;
                        tradeExecutor.submit(() -> {
                            try {
                                executeRoundTrip(finalBreakdown);
                            } catch (Exception e) {
                                System.err.println("Trade executor error [" + finalBreakdown.getSymbol() + "]: " + e.getMessage());
                            }
                        });
                    } else {
                        System.out.println("Trade queue full (" + current + "/" + config.getMaxConcurrentTrades() + "), skipping " + symbol);
                    }
                }
            } else if (config.shouldShowNonProfitable()) {
                logNonProfitable(breakdown);
            }

        } catch (Exception e) {
            System.err.println("Error scanning route " + symbol + " (" + buyExch + "→" + sellExch + "): " + e.getMessage());
        }
    }

    // ── PRICE GETTERS ────────────────────────────────────────────────

    private PriceData getSpotPrice(String exchange, String symbol) {
        return switch (exchange.toUpperCase()) {
            case "BINANCE" -> binanceSpotPrices.get(symbol);
            case "BYBIT"   -> bybitSpotPrices.get(symbol);
            default        -> null;
        };
    }

    private PriceData getFuturesPrice(String exchange, String symbol) {
        return switch (exchange.toUpperCase()) {
            case "BINANCE" -> binanceFuturesPrices.get(symbol);
            case "BYBIT"   -> bybitFuturesPrices.get(symbol);
            default        -> null;
        };
    }

    // ── LOGGING ──────────────────────────────────────────────────────

    private void logOpportunity(OpportunityCostBreakdown b) {
        BigDecimal grossProfit = b.getRawSpread().multiply(b.getQuantity());
        System.out.println("════════════════════════════════════════════════════════════════");
        System.out.println("OPPORTUNITY: " + b.getSymbol());
        System.out.println("Route  : BUY " + b.getBuyExchange() + " Spot → SHORT " + b.getSellExchange() + " Futures");
        System.out.println("Spot   : $" + fmt(b.getSpotPrice()) + "  Futures: $" + fmt(b.getFuturesPrice()));
        System.out.println("Spread : $" + fmt(b.getRawSpread()) + " (" + pct(b.getRawSpreadPercent()) + ")");
        System.out.println("Qty    : " + b.getQuantity().toPlainString() + " " + getAsset(b.getSymbol()) + "  Notional: $" + fmt(b.getNotionalUSDT()));
        System.out.println("Costs  : spot=$" + fmt(b.getSpotTradingFee()) + " fut=$" + fmt(b.getFuturesTradingFee())
            + " fund=$" + fmt(b.getFundingCost()) + " slip=$" + fmt(b.getSlippageCost())
            + " xfer=$" + fmt(b.getTransferFee()) + " | Total: $" + fmt(b.getTotalCosts()) + " (" + pct(b.getTotalCostsPercent()) + ")");
        System.out.println("Profit : gross=$" + fmt(grossProfit) + "  net=$" + fmt(b.getNetProfit()) + " (" + pct(b.getNetProfitPercent()) + ")");
        System.out.println("Data age: " + b.getDataAgeMs() + "ms | Funding rate: " + pct(b.getFundingRate().multiply(BigDecimal.valueOf(100))));
        System.out.println("════════════════════════════════════════════════════════════════");
    }

    private void logNonProfitable(OpportunityCostBreakdown b) {
        System.out.println("NON-PROFITABLE: " + b.getSymbol() + " " + b.getBuyExchange() + "→" + b.getSellExchange()
            + " raw=" + pct(b.getRawSpreadPercent()) + " costs=" + pct(b.getTotalCostsPercent()) + " net=" + pct(b.getNetProfitPercent()));
    }

    private void logConfiguration() {
        System.out.println("══════════════════════════════════════");
        System.out.println("Scanner config:");
        System.out.println("  Threshold : " + pct(config.getScannerThreshold()));
        System.out.println("  Notional  : $" + fmt(config.getNotionalUSDT()));
        System.out.println("  Hold hours: " + config.getExpectedHoldHours());
        System.out.println("  Symbols   : " + symbols.size());
        System.out.println("  Execution : " + (executionEnabled ? (simulationMode ? "SIMULATION" : "LIVE") : "SCAN_ONLY"));
        System.out.println("══════════════════════════════════════");
    }

    private String fmt(BigDecimal val)  { return String.format("%,.2f", val.doubleValue()); }
    private String pct(BigDecimal val)  { return String.format("%.4f%%", val.doubleValue()); }
    private String getAsset(String sym) { return sym.replace("USDT", "").replace("USDC", ""); }

    // ── EXECUTION ENGINE ─────────────────────────────────────────────

    public void executeRoundTrip(OpportunityCostBreakdown breakdown) throws Exception {
        if (breakdown == null) return;

        long start     = System.currentTimeMillis();
        String symbol  = breakdown.getSymbol();
        String buyExch = breakdown.getBuyExchange();
        String sellExch = breakdown.getSellExchange();

        if (binanceConnector == null || bybitConnector == null) {
            System.out.println("Connectors not initialized");
            return;
        }

        System.out.println("════════════════════════════════════════════════════════════════");
        System.out.println("EXECUTION: " + symbol + " | " + buyExch + " spot → " + sellExch + " futures");
        System.out.println("Qty: " + breakdown.getQuantity() + "  P&L predicted: " + String.format("%.4f%%", breakdown.getNetProfitPercent()));
        System.out.println("Mode: " + (simulationMode ? "SIMULATION" : "LIVE"));
        System.out.println("════════════════════════════════════════════════════════════════");

        try {
            performPreFlightChecks(breakdown);

            Exchange_Client buyConnector  = buyExch.equals("BINANCE")  ? binanceConnector : bybitConnector;
            Exchange_Client sellConnector = sellExch.equals("BINANCE") ? binanceConnector : bybitConnector;

            // Leg 1: Buy Spot
            System.out.println("[Leg 1/4] BUY SPOT on " + buyExch);
            TradeRecords.TradeLeg spotBuyLeg = placeOrderWithRetry(buyConnector, symbol, "BUY", breakdown.getQuantity(), false, "Leg1-BuySpot");

            // Leg 2: Short Futures
            System.out.println("[Leg 2/4] SHORT FUTURES on " + sellExch);
            TradeRecords.TradeLeg futuresShortLeg;
            try {
                futuresShortLeg = placeOrderWithRetry(sellConnector, symbol, "SELL", breakdown.getQuantity(), true, "Leg2-ShortFutures");
            } catch (Exception e) {
                System.out.println("Futures short failed — closing spot position");
                try {
                    placeOrderWithRetry(buyConnector, symbol, "SELL", spotBuyLeg.quantity, false, "EmergencyClose");
                    System.out.println("Spot position closed");
                } catch (Exception closeErr) {
                    System.out.println("MANUAL INTERVENTION REQUIRED: could not close spot. " + closeErr.getMessage());
                }
                throw new Exception("Atomic entry failed: " + e.getMessage());
            }

            validateAtomicEntry(spotBuyLeg, futuresShortLeg);

            // Phase 2: Transfer
            long transferDuration = executeTransfer(spotBuyLeg, breakdown);

            // Leg 3: Sell Spot
            System.out.println("[Leg 3/4] SELL SPOT on " + sellExch);
            TradeRecords.TradeLeg spotSellLeg = placeOrderWithRetry(sellConnector, symbol, "SELL", spotBuyLeg.quantity, false, "Leg3-SellSpot");

            // Leg 4: Cover Short
            System.out.println("[Leg 4/4] COVER SHORT on " + sellExch);
            TradeRecords.TradeLeg futuresCoverLeg = placeOrderWithRetry(sellConnector, symbol, "BUY", futuresShortLeg.quantity, true, "Leg4-CoverShort");

            // Reconciliation
            long totalTimeMs = System.currentTimeMillis() - start;
            BigDecimal spotProfit    = spotSellLeg.executedPrice.subtract(spotBuyLeg.executedPrice).multiply(spotSellLeg.quantity);
            BigDecimal futuresProfit = futuresShortLeg.executedPrice.subtract(futuresCoverLeg.executedPrice).multiply(futuresCoverLeg.quantity);
            BigDecimal totalFees     = spotBuyLeg.fee.add(futuresShortLeg.fee).add(spotSellLeg.fee).add(futuresCoverLeg.fee).add(breakdown.getTransferFee());
            BigDecimal netProfit     = spotProfit.add(futuresProfit).subtract(totalFees);
            BigDecimal actualPct     = netProfit.divide(breakdown.getNotionalUSDT(), 8, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

            System.out.println("════════════════════════════════════════════════════════════════");
            System.out.println("COMPLETE: " + symbol + " | net=$" + String.format("%.4f", netProfit) + " (" + String.format("%.4f%%", actualPct) + ")");
            System.out.println("Predicted: " + String.format("%.4f%%", breakdown.getNetProfitPercent()) + "  Actual: " + String.format("%.4f%%", actualPct));
            System.out.println("Transfer: " + transferDuration + "ms  Total: " + totalTimeMs + "ms");
            System.out.println("════════════════════════════════════════════════════════════════");

        } catch (Exception e) {
            System.err.println("EXECUTION FAILED [" + symbol + "]: " + e.getMessage());
        } finally {
            activeTrades.decrementAndGet();
        }
    }

    // ── ORDER WITH RETRY ─────────────────────────────────────────────

    private TradeRecords.TradeLeg placeOrderWithRetry(
        Exchange_Client connector, String symbol, String side,
        BigDecimal quantity, boolean isFutures, String legName) throws Exception {

        final int MAX_RETRIES   = 3;
        final long RETRY_DELAY  = 2000;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                if (attempt > 1) {
                    System.out.println("Retry #" + attempt + " for " + legName);
                    Thread.sleep(RETRY_DELAY);
                }

                TradeRecords.TradeLeg leg = connector.placeOrder(symbol, side, quantity, isFutures);

                if (leg == null) {
                    if (attempt < MAX_RETRIES) continue;
                    throw new Exception("Null response from " + connector.name());
                }

                if (!leg.status.equals("FILLED")) {
                    String msg = leg.statusMessage != null ? leg.statusMessage : "";
                    if (msg.contains("Insufficient") || msg.contains("Invalid")) throw new Exception(msg);
                    if (attempt < MAX_RETRIES) continue;
                    throw new Exception("Order not filled: " + msg);
                }

                System.out.println("  FILLED: " + leg.quantity + " @ $" + String.format("%.4f", leg.executedPrice));
                return leg;

            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("Insufficient") || msg.contains("Invalid Signature") || msg.contains("Unauthorized")) throw e;
                if (attempt >= MAX_RETRIES) throw e;
            }
        }
        throw new Exception("All retries failed for " + legName);
    }

    // ── PRE-FLIGHT CHECKS ────────────────────────────────────────────

    private void performPreFlightChecks(OpportunityCostBreakdown breakdown) throws Exception {
        String buyExch  = breakdown.getBuyExchange();
        String sellExch = breakdown.getSellExchange();

        Exchange_Client buyConnector  = buyExch.equals("BINANCE")  ? binanceConnector : bybitConnector;
        Exchange_Client sellConnector = sellExch.equals("BINANCE") ? binanceConnector : bybitConnector;

        BigDecimal buyBalance  = buyConnector.getBalance("USDT", false);
        BigDecimal requiredBuy = breakdown.getSpotPrice().multiply(breakdown.getQuantity());
        System.out.println("[Pre-flight] " + buyExch + " USDT: $" + String.format("%.2f", buyBalance) + " (need $" + String.format("%.2f", requiredBuy) + ")");
        if (buyBalance.compareTo(requiredBuy) < 0) {
            throw new Exception("Insufficient USDT on " + buyExch + ": have $" + String.format("%.2f", buyBalance) + " need $" + String.format("%.2f", requiredBuy));
        }

        BigDecimal sellBalance    = sellConnector.getBalance("USDT", true);
        BigDecimal requiredMargin = breakdown.getFuturesPrice().multiply(breakdown.getQuantity()).multiply(new BigDecimal("0.20"));
        System.out.println("[Pre-flight] " + sellExch + " margin: $" + String.format("%.2f", sellBalance) + " (need $" + String.format("%.2f", requiredMargin) + ")");
        if (sellBalance.compareTo(requiredMargin) < 0) {
            throw new Exception("Insufficient margin on " + sellExch + ": have $" + String.format("%.2f", sellBalance) + " need $" + String.format("%.2f", requiredMargin));
        }

        System.out.println("[Pre-flight] PASSED");
    }

    // ── ATOMIC ENTRY VALIDATION ──────────────────────────────────────

    private void validateAtomicEntry(TradeRecords.TradeLeg spotBuy, TradeRecords.TradeLeg futuresShort) throws Exception {
        if (spotBuy == null || !spotBuy.status.equals("FILLED"))
            throw new Exception("Spot buy not filled");
        if (futuresShort == null || !futuresShort.status.equals("FILLED"))
            throw new Exception("Futures short not filled");
        System.out.println("[Atomic] Both entry legs confirmed");
    }

    // ── TRANSFER ─────────────────────────────────────────────────────

    private static final long TRANSFER_MAX_WAIT_MS   = 600_000; // 10 minutes max
    private static final long TRANSFER_POLL_INTERVAL = 5_000;   // check every 5 seconds

    private long executeTransfer(TradeRecords.TradeLeg spotBuyLeg, OpportunityCostBreakdown breakdown) throws Exception {
        System.out.println("[Transfer] " + breakdown.getBuyExchange() + " → " + breakdown.getSellExchange());

        String buyExch  = breakdown.getBuyExchange();
        String sellExch = breakdown.getSellExchange();
        String symbol   = breakdown.getSymbol();
        String asset    = symbol.replace("USDT", "").replace("USDC", "");

        String depositAddress = config.getDepositAddress(sellExch, asset);
        if (depositAddress == null || depositAddress.isEmpty()) {
            throw new Exception("No deposit address for " + sellExch + " " + asset
                + " — set deposit.address." + sellExch + "." + asset + " in Application.properties");
        }

        // Memo required for XRP, XLM, etc. — null for BTC, SOL, LTC, DOGE, AVAX
        String depositMemo = config.getDepositMemo(sellExch, asset);

        Exchange_Client buyConnector  = buyExch.equals("BINANCE")  ? binanceConnector : bybitConnector;
        Exchange_Client sellConnector = sellExch.equals("BINANCE") ? binanceConnector : bybitConnector;

        BigDecimal balanceBefore = BigDecimal.ZERO;
        try {
            balanceBefore = sellConnector.getBalance(asset, false);
            System.out.println("[Transfer] " + sellExch + " " + asset + " balance before: " + balanceBefore.toPlainString());
        } catch (Exception e) {
            System.err.println("[Transfer] Could not snapshot pre-transfer balance: " + e.getMessage());
        }

        boolean withdrawn = buyConnector.transferAsset(asset, spotBuyLeg.quantity, depositAddress, depositMemo);
        if (!withdrawn) throw new Exception("Withdrawal failed from " + buyExch);

        System.out.println("[Transfer] Withdrawal initiated — polling for arrival (max "
            + (TRANSFER_MAX_WAIT_MS / 1000) + "s, every " + (TRANSFER_POLL_INTERVAL / 1000) + "s)");

        long startTime  = System.currentTimeMillis();
        int pollAttempt = 0;

        while (System.currentTimeMillis() - startTime < TRANSFER_MAX_WAIT_MS) {
            Thread.sleep(TRANSFER_POLL_INTERVAL);
            pollAttempt++;

            try {
                BigDecimal currentBalance = sellConnector.getBalance(asset, false);
                BigDecimal received       = currentBalance.subtract(balanceBefore);

                System.out.println("[Transfer] Poll #" + pollAttempt
                    + " — " + sellExch + " " + asset
                    + " balance: " + currentBalance.toPlainString()
                    + " (received: " + received.toPlainString()
                    + " / expected: " + spotBuyLeg.quantity.toPlainString() + ")");

                BigDecimal threshold = spotBuyLeg.quantity.multiply(new BigDecimal("0.99"));
                if (received.compareTo(threshold) >= 0) {
                    long duration = System.currentTimeMillis() - startTime;
                    System.out.println("[Transfer] CONFIRMED in " + duration + "ms after " + pollAttempt + " polls");
                    return duration;
                }

            } catch (Exception e) {
                System.err.println("[Transfer] Poll #" + pollAttempt + " balance check failed: " + e.getMessage());
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        throw new Exception("[Transfer] TIMEOUT after " + elapsed + "ms — "
            + asset + " did not arrive on " + sellExch + ". Manual intervention required.");
    }

    // ── SHUTDOWN ─────────────────────────────────────────────────────

    public void shutdown() {
        if (tradeExecutor != null && !tradeExecutor.isShutdown()) {
            tradeExecutor.shutdownNow();
            System.out.println("Trade executor shut down");
        }
    }
}
