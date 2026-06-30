package Exchanges.impl;

import Exchanges.Exchange_Client;
import Model.TradeRecords;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.*;
import java.util.function.BiConsumer;

public class BinanceConnector implements Exchange_Client {

    private static final String WS_SPOT    = "wss://stream.binance.com:9443/ws";
    private static final String WS_FUTURES = "wss://fstream.binance.com/ws";
    private static final String FUNDING_URL     = "https://fapi.binance.com/fapi/v1/fundingRate";
    private static final String ORDER_URL_SPOT  = "https://api.binance.com/api/v3/order";
    private static final String ORDER_URL_FUT   = "https://fapi.binance.com/fapi/v1/order";
    private static final String WITHDRAW_URL    = "https://api.binance.com/sapi/v1/capital/withdraw/apply";

    private final String apiKey;
    private final String secretKey;
    private final OkHttpClient http = new OkHttpClient();
    private final List<BinanceWebSocket> activeSockets = new ArrayList<>();

    private BiConsumer<String, BigDecimal> spotCallback;
    private BiConsumer<String, BigDecimal> futuresCallback;

    public BinanceConnector(String apiKey, String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        System.out.println("Binance connector ready");
    }

    @Override public String name() { return "BINANCE"; }

    public void setSpotPriceCallback(BiConsumer<String, BigDecimal> cb)    { this.spotCallback = cb; }
    public void setFuturesPriceCallback(BiConsumer<String, BigDecimal> cb) { this.futuresCallback = cb; }

    // ── WEBSOCKETS ───────────────────────────────────────────────────

    @Override
    public void startDepthStreams(List<String> symbols) {
        for (String symbol : symbols) {
            try {
                URI uri = new URI(WS_SPOT + "/" + symbol.toLowerCase() + "@bookTicker");
                BinanceWebSocket ws = new BinanceWebSocket(uri, symbol, true, this::handleSpotMessage);
                ws.connect();
                activeSockets.add(ws);
            } catch (Exception e) {
                System.err.println("Binance spot WS failed for " + symbol + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void startFuturesDepthStreams(List<String> symbols) {
        for (String symbol : symbols) {
            try {
                URI uri = new URI(WS_FUTURES + "/" + symbol.toLowerCase() + "@bookTicker");
                BinanceWebSocket ws = new BinanceWebSocket(uri, symbol, false, this::handleFuturesMessage);
                ws.connect();
                activeSockets.add(ws);
            } catch (Exception e) {
                System.err.println("Binance futures WS failed for " + symbol + ": " + e.getMessage());
            }
        }
    }

    private void handleSpotMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            if (!json.has("s") || !json.has("b") || !json.has("a")) return;
            String symbol = json.get("s").getAsString();
            BigDecimal bid = new BigDecimal(json.get("b").getAsString());
            BigDecimal ask = new BigDecimal(json.get("a").getAsString());
            BigDecimal mid = bid.add(ask).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            if (spotCallback != null) spotCallback.accept(symbol, mid);
        } catch (Exception e) {
            System.err.println("Binance spot parse error: " + e.getMessage());
        }
    }

    private void handleFuturesMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            if (!json.has("s") || !json.has("b") || !json.has("a")) return;
            String symbol = json.get("s").getAsString();
            BigDecimal bid = new BigDecimal(json.get("b").getAsString());
            BigDecimal ask = new BigDecimal(json.get("a").getAsString());
            BigDecimal mid = bid.add(ask).divide(BigDecimal.valueOf(2), 8, RoundingMode.HALF_UP);
            if (futuresCallback != null) futuresCallback.accept(symbol, mid);
        } catch (Exception e) {
            System.err.println("Binance futures parse error: " + e.getMessage());
        }
    }

    // ── FUNDING RATES ────────────────────────────────────────────────

    @Override
    public Map<String, BigDecimal> getAllFundingRates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        try {
            Request req = new Request.Builder().url(FUNDING_URL).get().build();
            try (Response resp = http.newCall(req).execute()) {
                if (!resp.isSuccessful()) return rates;
                JsonArray array = JsonParser.parseString(resp.body().string()).getAsJsonArray();
                Map<String, JsonObject> latest = new HashMap<>();
                for (int i = 0; i < array.size(); i++) {
                    JsonObject obj = array.get(i).getAsJsonObject();
                    String sym = obj.get("symbol").getAsString();
                    long time = obj.get("fundingTime").getAsLong();
                    if (!latest.containsKey(sym) || time > latest.get(sym).get("fundingTime").getAsLong()) {
                        latest.put(sym, obj);
                    }
                }
                latest.forEach((sym, obj) ->
                    rates.put(sym, new BigDecimal(obj.get("fundingRate").getAsString()))
                );
                System.out.println("Binance funding rates loaded: " + rates.size());
            }
        } catch (Exception e) {
            System.err.println("Binance funding rates error: " + e.getMessage());
        }
        return rates;
    }

    // ── BALANCE ──────────────────────────────────────────────────────

    @Override
    public BigDecimal getBalance(String asset, boolean isFutures) throws Exception {
        if (apiKey == null || secretKey == null) throw new IllegalStateException("Binance API keys missing");

        long ts = System.currentTimeMillis();
        String queryString = "timestamp=" + ts + "&recvWindow=5000";
        String sig = calculateHmac(queryString, secretKey);

        String baseUrl = isFutures ? "https://fapi.binance.com" : "https://api.binance.com";
        String path    = isFutures ? "/fapi/v2/account" : "/api/v3/account";

        HttpUrl url = HttpUrl.parse(baseUrl + path).newBuilder()
            .addQueryParameter("timestamp", String.valueOf(ts))
            .addQueryParameter("recvWindow", "5000")
            .addQueryParameter("signature", sig)
            .build();

        Request req = new Request.Builder()
            .url(url)
            .addHeader("X-MBX-APIKEY", apiKey)
            .get()
            .build();

        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) throw new Exception("Binance balance HTTP " + resp.code() + ": " + body);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonArray balances = json.getAsJsonArray("balances");
            for (int i = 0; i < balances.size(); i++) {
                JsonObject b = balances.get(i).getAsJsonObject();
                if (b.get("asset").getAsString().equals(asset)) {
                    return new BigDecimal(b.get("free").getAsString());
                }
            }
            return BigDecimal.ZERO;
        }
    }

    public Map<String, BigDecimal> getAllBalances(boolean isFutures) throws Exception {
        if (apiKey == null || secretKey == null) throw new IllegalStateException("Binance API keys missing");

        long ts = System.currentTimeMillis();
        String queryString = "timestamp=" + ts + "&recvWindow=5000";
        String sig = calculateHmac(queryString, secretKey);

        String baseUrl = isFutures ? "https://fapi.binance.com" : "https://api.binance.com";
        String path    = isFutures ? "/fapi/v2/account" : "/api/v3/account";

        HttpUrl url = HttpUrl.parse(baseUrl + path).newBuilder()
            .addQueryParameter("timestamp", String.valueOf(ts))
            .addQueryParameter("recvWindow", "5000")
            .addQueryParameter("signature", sig)
            .build();

        Request req = new Request.Builder()
            .url(url)
            .addHeader("X-MBX-APIKEY", apiKey)
            .get()
            .build();

        Map<String, BigDecimal> result = new HashMap<>();
        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) throw new Exception("Binance balance HTTP " + resp.code() + ": " + body);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            JsonArray balances = json.getAsJsonArray("balances");
            for (int i = 0; i < balances.size(); i++) {
                JsonObject bal = balances.get(i).getAsJsonObject();
                String asset = bal.get("asset").getAsString();
                BigDecimal free = new BigDecimal(bal.get("free").getAsString());
                if (free.compareTo(BigDecimal.ZERO) > 0) {
                    result.put(asset, free);
                }
            }
        }
        return result;
    }

    // ── ORDER PLACEMENT ──────────────────────────────────────────────

    @Override
    public TradeRecords.TradeLeg placeOrder(String symbol, String side, BigDecimal quantity, boolean isFutures) throws Exception {
        if (apiKey == null || secretKey == null) throw new IllegalStateException("Binance API keys missing");

        long ts = System.currentTimeMillis();
        String baseParams = String.format(
            "symbol=%s&side=%s&type=MARKET&quantity=%s&timestamp=%d&recvWindow=5000",
            symbol, side.toUpperCase(), quantity.toPlainString(), ts
        );
        String sig      = calculateHmac(baseParams, secretKey);
        String endpoint = isFutures ? ORDER_URL_FUT : ORDER_URL_SPOT;

        Request req = new Request.Builder()
            .url(endpoint + "?" + baseParams + "&signature=" + sig)
            .addHeader("X-MBX-APIKEY", apiKey)
            .post(okhttp3.RequestBody.create("", null))
            .build();

        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                return failedLeg(symbol, side, quantity, isFutures, "HTTP " + resp.code() + ": " + body);
            }
            JsonObject result = JsonParser.parseString(body).getAsJsonObject();
            String orderId = String.valueOf(result.get("orderId").getAsLong());

            if (isFutures) {
                // Binance futures POST returns avgPrice="0" in the initial ack for market orders.
                // Must poll GET /fapi/v1/order to get the actual fill price.
                String avgPriceStr = result.has("avgPrice") ? result.get("avgPrice").getAsString() : "";
                boolean pricePresent = !avgPriceStr.isEmpty()
                    && !avgPriceStr.equals("0")
                    && !avgPriceStr.startsWith("0.00");

                if (!pricePresent) {
                    return pollBinanceFuturesOrderFill(orderId, symbol, side, quantity);
                }

                BigDecimal executedPrice = new BigDecimal(avgPriceStr);
                BigDecimal totalFee = executedPrice.multiply(quantity).multiply(BigDecimal.valueOf(takerFee()));

                System.out.println("Binance futures order filled: " + symbol + " " + side
                    + " id=" + orderId + " price=" + executedPrice.toPlainString()
                    + " fee=" + totalFee.toPlainString());

                return new TradeRecords.TradeLeg(
                    orderId, name(), symbol, side, "FUTURES",
                    quantity, executedPrice, totalFee,
                    System.currentTimeMillis(), "FILLED", null
                );

            } else {
                // Spot: fills array gives per-fill price and commission
                BigDecimal executedPrice;
                BigDecimal totalFee = BigDecimal.ZERO;

                if (result.has("fills") && result.getAsJsonArray("fills").size() > 0) {
                    JsonArray fills = result.getAsJsonArray("fills");
                    BigDecimal totalCost = BigDecimal.ZERO;
                    BigDecimal totalQtyFilled = BigDecimal.ZERO;
                    for (int i = 0; i < fills.size(); i++) {
                        JsonObject fill = fills.get(i).getAsJsonObject();
                        BigDecimal fillPrice = new BigDecimal(fill.get("price").getAsString());
                        BigDecimal fillQty   = new BigDecimal(fill.get("qty").getAsString());
                        BigDecimal fillFee   = new BigDecimal(fill.get("commission").getAsString());
                        totalCost      = totalCost.add(fillPrice.multiply(fillQty));
                        totalQtyFilled = totalQtyFilled.add(fillQty);
                        totalFee       = totalFee.add(fillFee);
                    }
                    executedPrice = totalQtyFilled.compareTo(BigDecimal.ZERO) > 0
                        ? totalCost.divide(totalQtyFilled, 8, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                } else {
                    // Fallback if fills array is missing
                    BigDecimal cumQuote = new BigDecimal(result.get("cummulativeQuoteQty").getAsString());
                    BigDecimal execQty  = new BigDecimal(result.get("executedQty").getAsString());
                    executedPrice = execQty.compareTo(BigDecimal.ZERO) > 0
                        ? cumQuote.divide(execQty, 8, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                    totalFee = executedPrice.multiply(quantity).multiply(BigDecimal.valueOf(takerFee()));
                }

                System.out.println("Binance spot order filled: " + symbol + " " + side
                    + " id=" + orderId + " price=" + executedPrice.toPlainString()
                    + " fee=" + totalFee.toPlainString());

                return new TradeRecords.TradeLeg(
                    orderId, name(), symbol, side, "SPOT",
                    quantity, executedPrice, totalFee,
                    System.currentTimeMillis(), "FILLED", null
                );
            }
        }
    }

    // Poll GET /fapi/v1/order until FILLED — called when avgPrice is "0" in the POST response.
    private TradeRecords.TradeLeg pollBinanceFuturesOrderFill(
            String orderId, String symbol, String side, BigDecimal quantity) throws Exception {

        final int MAX_POLLS      = 10;
        final long POLL_MS       = 500;

        for (int i = 0; i < MAX_POLLS; i++) {
            Thread.sleep(POLL_MS);

            long ts = System.currentTimeMillis();
            String queryStr = "symbol=" + symbol + "&orderId=" + orderId
                + "&timestamp=" + ts + "&recvWindow=5000";
            String sig = calculateHmac(queryStr, secretKey);

            Request pollReq = new Request.Builder()
                .url(ORDER_URL_FUT + "?" + queryStr + "&signature=" + sig)
                .addHeader("X-MBX-APIKEY", apiKey)
                .get()
                .build();

            try (Response pollResp = http.newCall(pollReq).execute()) {
                String pollBody = pollResp.body() != null ? pollResp.body().string() : "";
                if (!pollResp.isSuccessful()) continue;

                JsonObject order  = JsonParser.parseString(pollBody).getAsJsonObject();
                String status     = order.get("status").getAsString();

                if (!status.equals("FILLED")) {
                    System.out.println(" Binance futures poll #" + (i + 1) + ": status=" + status + ", waiting...");
                    continue;
                }

                String avgPriceStr  = order.has("avgPrice") ? order.get("avgPrice").getAsString() : "0";
                BigDecimal executedPrice = new BigDecimal(avgPriceStr.isEmpty() ? "0" : avgPriceStr);
                BigDecimal totalFee      = executedPrice.multiply(quantity).multiply(BigDecimal.valueOf(takerFee()));

                System.out.println("Binance futures order filled: " + symbol + " " + side
                    + " id=" + orderId + " price=" + executedPrice.toPlainString()
                    + " fee=" + totalFee.toPlainString());

                return new TradeRecords.TradeLeg(
                    orderId, name(), symbol, side, "FUTURES",
                    quantity, executedPrice, totalFee,
                    System.currentTimeMillis(), "FILLED", null
                );
            }
        }

        System.err.println("Binance futures fill poll timed out for orderId=" + orderId + " after " + MAX_POLLS + " attempts");
        return failedLeg(symbol, side, quantity, true, "Fill poll timeout — orderId=" + orderId);
    }

    // ── TRANSFER ─────────────────────────────────────────────────────

    @Override
    public boolean transferAsset(String asset, BigDecimal quantity, String toAddress, String memo) throws Exception {
        if (apiKey == null || secretKey == null) throw new IllegalStateException("Binance API keys missing");

        String network = Config.JarvisConfig.getInstance().getWithdrawNetwork(asset);
        if (network == null || network.isEmpty()) {
            throw new Exception("No withdraw network for " + asset
                + " — set withdraw.network." + asset.toUpperCase() + " in Application.properties");
        }

        long ts = System.currentTimeMillis();
        StringBuilder paramsBuilder = new StringBuilder();
        paramsBuilder.append("coin=").append(asset)
            .append("&network=").append(network)
            .append("&amount=").append(quantity.toPlainString())
            .append("&address=").append(toAddress);
        // Include addressTag only when a memo was provided (XRP destination tag, etc.)
        if (memo != null && !memo.isEmpty()) {
            paramsBuilder.append("&addressTag=").append(memo);
        }
        paramsBuilder.append("&timestamp=").append(ts).append("&recvWindow=5000");
        String baseParams = paramsBuilder.toString();
        String sig = calculateHmac(baseParams, secretKey);

        Request req = new Request.Builder()
            .url(WITHDRAW_URL + "?" + baseParams + "&signature=" + sig)
            .addHeader("X-MBX-APIKEY", apiKey)
            .post(okhttp3.RequestBody.create("", null))
            .build();

        try (Response resp = http.newCall(req).execute()) {
            if (resp.isSuccessful()) {
                System.out.println("Binance withdrawal initiated: " + quantity + " " + asset + " via " + network);
                return true;
            }
            String body = resp.body() != null ? resp.body().string() : "";
            System.err.println("Binance withdrawal failed " + resp.code() + ": " + body);
            return false;
        }
    }

    // ── FEES ─────────────────────────────────────────────────────────

    @Override public double takerFee() { return 0.001; }
    @Override public double makerFee() { return 0.001; }

    // ── SHUTDOWN ─────────────────────────────────────────────────────

    @Override
    public void close() {
        for (BinanceWebSocket ws : activeSockets) {
            try { ws.shutdown(); } catch (Exception ignored) {}
        }
        activeSockets.clear();
        System.out.println("Binance connectors closed");
    }

    // ── HMAC ─────────────────────────────────────────────────────────

    private String calculateHmac(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
            byte[] hmac = mac.doFinal(message.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC failed: " + e.getMessage(), e);
        }
    }

    // ── HELPERS ──────────────────────────────────────────────────────

    private TradeRecords.TradeLeg failedLeg(String symbol, String side, BigDecimal qty, boolean isFutures, String msg) {
        return new TradeRecords.TradeLeg(
            "N/A", name(), symbol, side,
            isFutures ? "FUTURES" : "SPOT",
            qty, BigDecimal.ZERO, BigDecimal.ZERO,
            System.currentTimeMillis(), "REJECTED", msg
        );
    }

    // ── WEBSOCKET INNER CLASS ─────────────────────────────────────────

    private class BinanceWebSocket extends WebSocketClient {
        private final String symbol;
        private final boolean isSpot;
        private final java.util.function.Consumer<String> handler;
        private volatile boolean reconnect = true;

        BinanceWebSocket(URI uri, String symbol, boolean isSpot, java.util.function.Consumer<String> handler) {
            super(uri);
            this.symbol  = symbol;
            this.isSpot  = isSpot;
            this.handler = handler;
        }

        @Override public void onOpen(ServerHandshake h) {
            System.out.println("Binance " + (isSpot ? "spot" : "futures") + " connected: " + symbol);
        }

        @Override public void onMessage(String msg) {
            try { handler.accept(msg); } catch (Exception ignored) {}
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            if (!reconnect) return;
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    this.reconnect();
                } catch (Exception ignored) {}
            }, "Binance-Reconnect-" + symbol).start();
        }

        @Override public void onError(Exception e) {
            System.err.println("Binance WS error " + symbol + ": " + e.getMessage());
        }

        void shutdown() { reconnect = false; this.close(); }
    }
}
