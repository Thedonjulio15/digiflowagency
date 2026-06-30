package Config;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class JarvisConfig {

    private static JarvisConfig instance;
    private final Properties appProps;
    private final Properties apiProps;

    private JarvisConfig() {
        this.appProps = loadProperties("Application.properties");
        this.apiProps = loadProperties("config.properties");
    }

    public static synchronized JarvisConfig getInstance() {
        if (instance == null) {
            instance = new JarvisConfig();
        }
        return instance;
    }

    private Properties loadProperties(String resourcePath) {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                props.load(is);
                System.out.println("Loaded: " + resourcePath);
            } else {
                System.err.println("Config file not found: " + resourcePath);
            }
        } catch (IOException e) {
            System.err.println("Failed to load " + resourcePath + ": " + e.getMessage());
        }
        return props;
    }

    // ── SYMBOLS ──────────────────────────────────────────────────────

    public List<String> getSymbols() {
        String symbols = appProps.getProperty("scanner.symbols", "BTCUSDT,SOLUSDT,XRPUSDT");
        return Arrays.stream(symbols.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toUpperCase)
            .collect(Collectors.toList());
    }

    // ── THRESHOLDS ───────────────────────────────────────────────────

    public BigDecimal getScannerThreshold() {
        String val = appProps.getProperty("scanner.threshold.minSpread", "0.005");
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            System.err.println("Invalid scanner threshold, using 0.005");
            return new BigDecimal("0.005");
        }
    }

    public BigDecimal getThresholdForSymbol(String symbol) {
        if (symbol == null) return getScannerThreshold();
        String base = symbol.trim().toLowerCase();
        if (base.endsWith("usdt")) base = base.substring(0, base.length() - 4);
        String val = appProps.getProperty("scanner.threshold." + base);
        if (val == null || val.isEmpty()) return getScannerThreshold();
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return getScannerThreshold();
        }
    }

    // ── SCANNER SETTINGS ─────────────────────────────────────────────

    public long getDataFreshnessThresholdMs() {
        String val = appProps.getProperty("scanner.dataFreshnessThreshold.ms", "10000");
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return 10000L;
        }
    }

    public BigDecimal getNotionalUSDT() {
        String val = appProps.getProperty("scanner.notional.usdt", "10");
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return new BigDecimal("10");
        }
    }

    public BigDecimal getSlippageEstimate() {
        String val = appProps.getProperty("scanner.slippage.estimate.percent", "0.0002");
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return new BigDecimal("0.0002");
        }
    }

    public int getExpectedHoldHours() {
        String val = appProps.getProperty("scanner.funding.expected.hold.hours", "8");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 8;
        }
    }

    public boolean shouldShowNonProfitable() {
        return Boolean.parseBoolean(appProps.getProperty("scanner.show.nonProfit", "false"));
    }

    // ── FEES ─────────────────────────────────────────────────────────

    public BigDecimal getSpotTakerFee(String exchange) {
        String val = appProps.getProperty(exchange.toLowerCase() + ".spot.taker.fee", "0.001");
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return new BigDecimal("0.001");
        }
    }

    public BigDecimal getFuturesTakerFee(String exchange) {
        String val = appProps.getProperty(exchange.toLowerCase() + ".futures.taker.fee", "0.0004");
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return new BigDecimal("0.0004");
        }
    }

    public BigDecimal getTransferFee(String exchange, String asset) {
        if (exchange == null || asset == null) return BigDecimal.ZERO;
        String val = appProps.getProperty("transfer.fee." + exchange.toUpperCase() + "." + asset.toUpperCase(), "0");
        try {
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    // ── API KEYS ─────────────────────────────────────────────────────

    public String getBinanceApiKey()    { return apiProps.getProperty("binance.api.key", null); }
    public String getBinanceSecretKey() { return apiProps.getProperty("binance.secret.key", null); }
    public String getBybitApiKey()      { return apiProps.getProperty("bybit.api.key", null); }
    public String getBybitSecretKey()   { return apiProps.getProperty("bybit.secret.key", null); }

    // ── EXECUTION ────────────────────────────────────────────────────

    public boolean isExecutionEnabled() {
        return Boolean.parseBoolean(appProps.getProperty("trade.handler.enabled", "true"));
    }

    public boolean isSimulationMode() {
        return Boolean.parseBoolean(appProps.getProperty("trade.handler.simulation", "false"));
    }

    public int getMaxConcurrentTrades() {
        String val = appProps.getProperty("trade.handler.max.concurrent", "1");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public int getTradeHandlerThreadPoolSize() {
        String val = appProps.getProperty("trade.handler.threads", "4");
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return 4;
        }
    }

    // ── DEPOSIT ADDRESSES ────────────────────────────────────────────

    public String getDepositAddress(String exchange, String asset) {
        String key = "deposit.address." + exchange.toUpperCase() + "." + asset.toUpperCase();
        return appProps.getProperty(key, null);
    }

    // ── DEPOSIT MEMOS (XRP destination tag, etc.) ────────────────────
    // Returns null for assets that don't need a memo (BTC, SOL, LTC, etc.)

    public String getDepositMemo(String exchange, String asset) {
        if (exchange == null || asset == null) return null;
        String key = "deposit.memo." + exchange.toUpperCase() + "." + asset.toUpperCase();
        String val = appProps.getProperty(key, null);
        if (val == null || val.isEmpty()) return null;
        return val;
    }

    // ── WITHDRAW NETWORK (Binance) ────────────────────────────────────
    // e.g. withdraw.network.BTC=BTC, withdraw.network.SOL=SOL, withdraw.network.XRP=XRP

    public String getWithdrawNetwork(String asset) {
        if (asset == null) return null;
        return appProps.getProperty("withdraw.network." + asset.toUpperCase(), null);
    }

    // ── WITHDRAW CHAIN (Bybit) ────────────────────────────────────────
    // e.g. withdraw.chain.BTC=BTC, withdraw.chain.SOL=SOL, withdraw.chain.XRP=XRP

    public String getWithdrawChain(String asset) {
        if (asset == null) return null;
        return appProps.getProperty("withdraw.chain." + asset.toUpperCase(), null);
    }
}
