package main;

import Config.JarvisConfig;
import Exchanges.impl.BinanceConnector;
import Exchanges.impl.BybitConnector;
import Exchanges.impl.ExchangeFactory;
import Logger.ArcReactorCSV;
import Scanner_Core.OpportunityScanner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    private static BinanceConnector binance;
    private static BybitConnector bybit;
    private static ArcReactorCSV csvLogger;
    private static Thread scannerThread;
    private static OpportunityScanner scanner;

    public static void main(String[] args) throws Exception {

        printBanner();

        // ── STEP 1: CONFIG ──────────────────────────────────────────
        JarvisConfig config = JarvisConfig.getInstance();

        List<String> symbolList = config.getSymbols();
        Set<String> symbols = new HashSet<>(symbolList);

        if (symbols.isEmpty()) {
            symbols.add("BTCUSDT");
            symbols.add("SOLUSDT");
            System.out.println("No symbols in config. Using defaults.");
        }

        System.out.println("Symbols : " + String.join(", ", symbols));
        System.out.println("Notional: $" + config.getNotionalUSDT());
        System.out.println("Mode    : " + (config.isSimulationMode() ? "SIMULATION" : "LIVE"));

        // ── STEP 2: CONNECTORS ──────────────────────────────────────
        binance = (BinanceConnector) ExchangeFactory.create("BINANCE");
        bybit   = (BybitConnector)   ExchangeFactory.create("BYBIT");

        // ── STEP 3: AUTH CHECK — halt if either exchange fails ──────
        boolean authOk = runAuthCheck();
        if (!authOk) {
            System.out.println("Fix API issues above then restart.");
            System.exit(1);
        }

        // ── STEP 4: CSV LOGGER ──────────────────────────────────────
        String csvPath = "arc_reactor_opportunities_" +
            LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        csvLogger = new ArcReactorCSV(csvPath);

        // ── STEP 5: FUNDING RATES ───────────────────────────────────
        Map<String, BigDecimal> allFundingRates = new HashMap<>();
        allFundingRates.putAll(binance.getAllFundingRates());
        allFundingRates.putAll(bybit.getAllFundingRates());
        System.out.println("Funding rates loaded: " + allFundingRates.size());

        // ── STEP 6: SCANNER ─────────────────────────────────────────
        scanner = new OpportunityScanner(config, allFundingRates, csvLogger, symbols);
        scanner.setConnectors(binance, bybit);

        // ── STEP 7: WIRE CALLBACKS ──────────────────────────────────
        binance.setSpotPriceCallback((sym, price)    -> scanner.updateBinanceSpotPrice(sym, price));
        binance.setFuturesPriceCallback((sym, price) -> scanner.updateBinanceFuturesPrice(sym, price));
        bybit.setSpotPriceCallback((sym, price)      -> scanner.updateBybitSpotPrice(sym, price));
        bybit.setFuturesPriceCallback((sym, price)   -> scanner.updateBybitFuturesPrice(sym, price));

        // ── STEP 8: WEBSOCKETS ──────────────────────────────────────
        binance.startDepthStreams(symbolList);
        binance.startFuturesDepthStreams(symbolList);
        bybit.startDepthStreams(symbolList);
        bybit.startFuturesDepthStreams(symbolList);

        Thread.sleep(3000); // let connections stabilise

        // ── STEP 9: START POLLING ───────────────────────────────────
        startScannerPolling(scanner, symbols);

        // ── STEP 10: SHUTDOWN HOOK ──────────────────────────────────
        installShutdownHook();

        printReady();

        Thread.currentThread().join();
    }

    // ── AUTH CHECK ───────────────────────────────────────────────────

    private static boolean runAuthCheck() {
        System.out.println();
        System.out.println("══════════════════════════════════════");
        System.out.println(" AUTH CHECK");
        System.out.println("══════════════════════════════════════");

        boolean ok = true;

        try {
            Map<String, BigDecimal> balances = binance.getAllBalances(false);
            System.out.println(" ✅ Binance Spot:");
            for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        } catch (Exception e) {
            System.out.println(" ❌ Binance balance check FAILED: " + e.getMessage());
            ok = false;
        }

        try {
            Map<String, BigDecimal> balances = bybit.getAllBalances(false);
            System.out.println(" ✅ Bybit UNIFIED:");
            for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        } catch (Exception e) {
            System.out.println(" ❌ Bybit balance check FAILED: " + e.getMessage());
            ok = false;
        }

        System.out.println("══════════════════════════════════════");
        System.out.println();
        return ok;
    }

    // ── SCANNER POLLING ──────────────────────────────────────────────

    private static void startScannerPolling(OpportunityScanner scanner, Set<String> symbols) {
        scannerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    for (String symbol : symbols) {
                        scanner.checkOpportunity(symbol);
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("Scanner polling error: " + e.getMessage());
                }
            }
        });
        scannerThread.setDaemon(false);
        scannerThread.setName("Scanner-Poller");
        scannerThread.start();
    }

    // ── SHUTDOWN HOOK ────────────────────────────────────────────────

    private static void installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            try {
                if (scanner != null)      scanner.shutdown();
                if (scannerThread != null) scannerThread.interrupt();
                if (csvLogger != null)    csvLogger.close();
                if (binance != null)      binance.close();
                if (bybit != null)        bybit.close();
            } catch (Exception e) {
                System.err.println("Shutdown error: " + e.getMessage());
            }
            System.out.println("Offline.");
        }));
    }

    // ── BANNER ───────────────────────────────────────────────────────

    private static void printBanner() {
        System.out.println();
        System.out.println("══════════════════════════════════════");
        System.out.println(" CASH & CARRY ARBITRAGE BOT");
        System.out.println("══════════════════════════════════════");
        System.out.println();
    }

    private static void printReady() {
        System.out.println();
        System.out.println("══════════════════════════════════════");
        System.out.println(" SCANNER RUNNING — CTRL+C to stop");
        System.out.println("══════════════════════════════════════");
        System.out.println();
    }
}
