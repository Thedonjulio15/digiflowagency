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
import java.net.URI;
import java.util.*;
import java.util.function.BiConsumer;

public class BybitConnector implements Exchange_Client {

    private static final String WS_SPOT    = "wss://stream.bybit.com/v5/public/spot";
    private static final String WS_FUTURES = "wss://stream.bybit.com/v5/public/linear";
    private static final String FUNDING_URL       = "https://api.bybit.com/v5/market/funding/history";
    private static final String BALANCE_URL       = "https://api.bybit.com/v5/account/wallet-balance";
    private static final String ORDER_URL         = "https://api.bybit.com/v5/order/create";
    private static final String ORDER_HISTORY_URL = "https://api.bybit.com/v5/order/history";
    private static final String WITHDRAW_URL      = "https://api.bybit.com/v5/asset/withdraw/create";

    private final String apiKey;
    private final String secretKey;
    private final OkHttpClient http = new OkHttpClient();
    private final List<BybitWebSocket> activeSockets = new ArrayList<>();

    private BiConsumer<String, BigDecimal> spotCallback;
    private BiConsumer<String, BigDecimal> futuresCallback;

    public BybitConnector(String apiKey, String secretKey) {
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        System.out.println("Bybit connector ready");
    }

    @Override public String name() { return "BYBIT"; }

    public void setSpotPriceCallback(BiConsumer<String, BigDecimal> cb)    { this.spotCallback = cb; }
    public void setFuturesPriceCallback(BiConsumer<String, BigDecimal> cb) { this.futuresCallback = cb; }

    // ── WEBSOCKETS ───────────────────────────────────────────────────

    @Override
    public void startDepthStreams(List<String> symbols) {
        for (String symbol : symbols) {
            try {
                BybitWebSocket ws = new BybitWebSocket(
                    new URI(WS_SPOT), symbol, true, this::handleSpotMessage);
                ws.connect();
                activeSockets.add(ws);
            } catch (Exception e) {
                System.err.println("Bybit spot WS failed for " + symbol + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void startFuturesDepthStreams(List<String> symbols) {
        for (String symbol : symbols) {
            try {
                BybitWebSocket ws = new BybitWebSocket(
                    new URI(WS_FUTURES), symbol, false, this::handleFuturesMessage);
                ws.connect();
                activeSockets.add(ws);
            } catch (Exception e) {
                System.err.println("Bybit futures WS failed for " + symbol + ": " + e.getMessage());
            }
        }
    }

    private void handleSpotMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            if (!json.has("topic") || !json.has("data")) return;
            JsonObject data = json.getAsJsonObject("data");
            if (!data.has("s") || !data.has("b") || !data.has("a")) return;
            String symbol = data.get("s").getAsString();
            JsonArray bids = data.getAsJsonArray("b");
            JsonArray asks = data.getAsJsonArray("a");
            if (bids.isEmpty() || asks.isEmpty()) return;
            BigDecimal bid = new BigDecimal(bids.get(0).getAsJsonArray().get(0).getAsString());
            BigDecimal ask = new BigDecimal(asks.get(0).getAsJsonArray().get(0).getAsString());
            BigDecimal mid = bid.add(ask).divide(BigDecimal.valueOf(2), 8, java.math.RoundingMode.HALF_UP);
            if (spotCallback != null) spotCallback.accept(symbol, mid);
        } catch (Exception e) {
            System.err.println("Bybit spot parse error: " + e.getMessage());
        }
    }

    private void handleFuturesMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            if (!json.has("topic") || !json.has("data")) return;
            JsonObject data = json.getAsJsonObject("data");
            if (!data.has("s") || !data.has("b") || !data.has("a")) return;
            String symbol = data.get("s").getAsString();
            JsonArray bids = data.getAsJsonArray("b");
            JsonArray asks = data.getAsJsonArray("a");
            if (bids.isEmpty() || asks.isEmpty()) return;
            BigDecimal bid = new BigDecimal(bids.get(0).getAsJsonArray().get(0).getAsString());
            BigDecimal ask = new BigDecimal(asks.get(0).getAsJsonArray().get(0).getAsString());
            BigDecimal mid = bid.add(ask).divide(BigDecimal.valueOf(2), 8, java.math.RoundingMode.HALF_UP);
            if (futuresCallback != null) futuresCallback.accept(symbol, mid);
        } catch (Exception e) {
            System.err.println("Bybit futures parse error: " + e.getMessage());
        }
    }

    // ── FUNDING RATES ────────────────────────────────────────────────

    @Override
    public Map<String, BigDecimal> getAllFundingRates() {
        Map<String, BigDecimal> rates = new HashMap<>();
        try {
            List<String> symbols = Config.JarvisConfig.getInstance().getSymbols();
            for (String symbol : symbols) {
                try {
                    String url = FUNDING_URL + "?category=linear&symbol=" + symbol + "&limit=1";
                    Request req = new Request.Builder().url(url).get().build();
                    try (Response resp = http.newCall(req).execute()) {
                        if (!resp.isSuccessful()) continue;
                        JsonObject json = JsonParser.parseString(resp.body().string()).getAsJsonObject();
                        if (!json.has("result")) continue;
                        JsonArray list = json.getAsJsonObject("result").getAsJsonArray("list");
                        if (list.isEmpty()) continue;
                        rates.put(symbol, new BigDecimal(list.get(0).getAsJsonObject().get("fundingRate").getAsString()));
                    }
                } catch (Exception e) {
                    System.err.println("Bybit funding rate error for " + symbol + ": " + e.getMessage());
                }
            }
            System.out.println("Bybit funding rates loaded: " + rates.size());
        } catch (Exception e) {
            System.err.println("Bybit funding rates error: " + e.getMessage());
        }
        return rates;
    }

    // ── BALANCE ──────────────────────────────────────────────────────

    @Override
    public BigDecimal getBalance(String asset, boolean isFutures) throws Exception {
        if (apiKey == null || secretKey == null) throw new IllegalStateException("Bybit API keys missing");

        long ts = System.currentTimeMillis();
        String recvWindow = "5000";
        String queryStr   = "accountType=UNIFIED";
        String sigPayload = ts + apiKey + recvWindow + queryStr;
        String sig        = calculateHmac(sigPayload, secretKey);

        HttpUrl url = HttpUrl.parse(BALANCE_URL).newBuilder()
            .addQueryParameter("accountType", "UNIFIED")
            .build();

        Request req = new Request.Builder()
            .url(url)
            .addHeader("X-BAPI-API-KEY",      apiKey)
            .addHeader("X-BAPI-SIGN",          sig)
            .addHeader("X-BAPI-TIMESTAMP",     String.valueOf(ts))
            .addHeader("X-BAPI-RECV-WINDOW",   recvWindow)
            .get()
            .build();

        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) throw new Exception("Bybit balance HTTP " + resp.code() + ": " + body);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.get("retCode").getAsInt() != 0)
                throw new Exception("Bybit API error: " + json.get("retMsg").getAsString());
            JsonArray list = json.getAsJsonObject("result").getAsJsonArray("list");
            if (list.isEmpty()) return BigDecimal.ZERO;
            JsonArray coins = list.get(0).getAsJsonObject().getAsJsonArray("coin");
            for (int i = 0; i < coins.size(); i++) {
                JsonObject coin = coins.get(i).getAsJsonObject();
                if (coin.get("coin").getAsString().equals(asset)) {
                    return new BigDecimal(coin.get("walletBalance").getAsString());
                }
            }
            return BigDecimal.ZERO;
        }
    }

    public Map<String, BigDecimal> getAllBalances(boolean isFutures) throws Exception {
        if (apiKey == null || secretKey == null) throw new IllegalStateException("Bybit API keys missing");

        long ts = System.currentTimeMillis();
        String recvWindow = "5000";
        String queryStr   = "accountType=UNIFIED";
        String sigPayload = ts + apiKey + recvWindow + queryStr;
        String sig        = calculateHmac(sigPayload, secretKey);

        HttpUrl url = HttpUrl.parse(BALANCE_URL).newBuilder()
            .addQueryParameter("accountType", "UNIFIED")
            .build();

        Request req = new Request.Builder()
            .url(url)
            .addHeader("X-BAPI-API-KEY",      apiKey)
            .addHeader("X-BAPI-SIGN",          sig)
            .addHeader("X-BAPI-TIMESTAMP",     String.valueOf(ts))
            .addHeader("X-BAPI-RECV-WINDOW",   recvWindow)
            .get()
            .build();

        Map<String, BigDecimal> result = new HashMap<>();
        try (Response resp = http.newCall(req).execute()) {
            String body = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) throw new Exception("Bybit balance HTTP " + resp.code() + ": " + body);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.get("retCode").getAsInt() != 0)
                throw new Exception("Bybit API error: " + json.get("retMsg").getAsString());
            JsonArray list = json.getAsJsonObject("result").getAsJsonArray("list");
            if (list == null || list.isEmpty()) return result;
            JsonArray coins = list.get(0).getAsJsonObject().getAsJsonArray("coin");
            if (coins == null) return result;
            for (int i = 0; i < coins.size(); i++) {
                JsonObject coin = coins.get(i).getAsJsonObject();
                String asset    = coin.get("coin").getAsString();
                BigDecimal bal  = new BigDecimal(coin.get("walletBalance").getAsString());
                if (bal.compareTo(BigDecimal.ZERO) > 0) {
                    result.put(asset, bal);
                }
            }
        }
        return result;
    }

    // ── ORDER PLACEMENT ──────────────────────────────────────────────

    @Override
    public TradeRecords.TradeLeg placeOrder(String symbol, String side, BigDecimal quantity, boolean isFutures) throws Exception {
        if (apiKey == null || secretKey == null) throw new IllegalStateException("Bybit API keys missing");

        long ts = System.currentTimeMillis();
        String recvWindow = "5000";
        String category   = isFutures ? "linear" : "spot";
        String bybitSide  = side.equalsIgnoreCase("SELL") ? "Sell" : "Buy";

        String body = String.format(
            "{\"category\":\"%s\",\"symbol\":\"%s\",\"side\":\"%s\",\"orderType\":\"Market\",\"qty\":\"%s\"}",
            category, symbol, bybitSide, quantity.toPlainString()
        );

        String sigPayload = ts + apiKey + recvWindow + body;
        String sig        = calculateHmac(sigPayload, secretKey);

        Request req = new Request.Builder()
            .url(ORDER_URL)
            .addHeader("X-BAPI-API-KEY",      apiKey)
            .addHeader("X-BAPI-SIGN",          sig)
            .addHeader("X-BAPI-TIMESTAMP",     String.valueOf(ts))
            .addHeader("X-BAPI-RECV-WINDOW",   recvWindow)
            .addHeader("Content-Type",         "application/json")
            .post(okhttp3.RequestBody.create(body, okhttp3.MediaType.parse("application/json")))
            .build();

        try (Response resp = http.newCall(req).execute()) {
            String respBody = resp.body() != null ? resp.body().string() : "";
            if (!resp.isSuccessful()) {
                return failedLeg(symbol, side, quantity, isFutures, "HTTP " + resp.code() + ": " + respBody);
            }
            JsonObject json = JsonParser.parseString(respBody).getAsJsonObject();
            int retCode = json.get("retCode").getAsInt();
            if (retCode != 0) {
                return failedLeg(symbol, side, quantity, isFutures, json.get("retMsg").getAsString());
            }
            String orderId = json.getAsJsonObject("result").get("orderId").getAsString();
            System.out.println("Bybit order placed: " + symbol + " " + side + " id=" + orderId + " — polling for fill...");

            // Bybit create-order does not return fill data — poll order history for avgPrice and execFee
            return pollBybitOrderFill(orderId, symbol, side, quantity, isFutures, category);
        }
    }

    private TradeRecords.TradeLeg pollBybitOrderFill(
            String orderId, String symbol, String side,
            BigDecimal quantity, boolean isFutures, String category) throws Exception {

        final int MAX_POLLS       = 10;
        final long POLL_INTERVAL  = 500;

        for (int i = 0; i < MAX_POLLS; i++) {
            Thread.sleep(POLL_INTERVAL);

            long ts = System.currentTimeMillis();
            String recvWindow = "5000";
            String queryStr   = "category=" + category + "&orderId=" + orderId + "&symbol=" + symbol;
            String sigPayload = ts + apiKey + recvWindow + queryStr;
            String sig        = calculateHmac(sigPayload, secretKey);

            Request pollReq = new Request.Builder()
                .url(ORDER_HISTORY_URL + "?" + queryStr)
                .addHeader("X-BAPI-API-KEY",      apiKey)
                .addHeader("X-BAPI-SIGN",          sig)
                .addHeader("X-BAPI-TIMESTAMP",     String.valueOf(ts))
                .addHeader("X-BAPI-RECV-WINDOW",   recvWindow)
                .get()
                .build();

            try (Response pollResp = http.newCall(pollReq).execute()) {
                String pollBody = pollResp.body() != null ? pollResp.body().string() : "";
                if (!pollResp.isSuccessful()) continue;

                JsonObject pollJson  = JsonParser.parseString(pollBody).getAsJsonObject();
                if (pollJson.get("retCode").getAsInt() != 0) continue;

                JsonArray list = pollJson.getAsJsonObject("result").getAsJsonArray("list");
                if (list == null || list.isEmpty()) continue;

                JsonObject order   = list.get(0).getAsJsonObject();
                String orderStatus = order.get("orderStatus").getAsString();

                if (!orderStatus.equals("Filled")) {
                    System.out.println(" Bybit poll #" + (i + 1) + ": status=" + orderStatus + ", waiting...");
                    continue;
                }

                String avgPriceStr = order.has("avgPrice")    ? order.get("avgPrice").getAsString()    : "0";
                String execFeeStr  = order.has("cumExecFee")  ? order.get("cumExecFee").getAsString()  : "0";

                BigDecimal executedPrice = new BigDecimal(avgPriceStr.isEmpty() ? "0" : avgPriceStr);
                BigDecimal totalFee      = new BigDecimal(execFeeStr.isEmpty()  ? "0" : execFeeStr);

                if (totalFee.compareTo(BigDecimal.ZERO) == 0 && executedPrice.compareTo(BigDecimal.ZERO) > 0) {
                    totalFee = executedPrice.multiply(quantity).multiply(BigDecimal.valueOf(takerFee()));
                }

                System.out.println("Bybit order filled: " + symbol + " " + side
                    + " id=" + orderId + " price=" + executedPrice.toPlainString()
                    + " fee=" + totalFee.toPlainString());

                return new TradeRecords.TradeLeg(
                    orderId, name(), symbol, side,
                    isFutures ? "FUTURES" : "SPOT",
                    quantity, executedPrice, totalFee,
                    System.currentTimeMillis(), "FILLED", null
                );
            }
        }

        System.err.println("Bybit fill poll timed out for orderId=" + orderId + " after " + MAX_POLLS + " attempts");
        return failedLeg(symbol, side, quantity, isFutures, "Fill poll timeout — orderId=" + orderId);
    }

    // ── TRANSFER ─────────────────────────────────────────────────────

    @Override
    public boolean transferAsset(String asset, BigDecimal quantity, String toAddress, String memo) throws Exception {
        if (apiKey == null || secretKey == null) throw new IllegalStateException("Bybit API keys missing");

        String chain = Config.JarvisConfig.getInstance().getWithdrawChain(asset);
        if (chain == null || chain.isEmpty()) {
            throw new Exception("No withdraw chain for " + asset
                + " — set withdraw.chain." + asset.toUpperCase() + " in Application.properties");
        }

        long ts = System.currentTimeMillis();
        String recvWindow = "5000";

        // Build JSON body — include "tag" field only for memo chains (XRP, XLM, etc.)
        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("{")
            .append("\"coin\":\"").append(asset).append("\",")
            .append("\"chain\":\"").append(chain).append("\",")
            .append("\"address\":\"").append(toAddress).append("\",");
        if (memo != null && !memo.isEmpty()) {
            bodyBuilder.append("\"tag\":\"").append(memo).append("\",");
        }
        bodyBuilder.append("\"amount\":\"").append(quantity.toPlainString()).append("\",")
            .append("\"accountType\":\"UNIFIED\",")
            .append("\"feeType\":0}");

        String body       = bodyBuilder.toString();
        String sigPayload = ts + apiKey + recvWindow + body;
        String sig        = calculateHmac(sigPayload, secretKey);

        Request req = new Request.Builder()
            .url(WITHDRAW_URL)
            .addHeader("X-BAPI-API-KEY",      apiKey)
            .addHeader("X-BAPI-SIGN",          sig)
            .addHeader("X-BAPI-TIMESTAMP",     String.valueOf(ts))
            .addHeader("X-BAPI-RECV-WINDOW",   recvWindow)
            .addHeader("Content-Type",         "application/json")
            .post(okhttp3.RequestBody.create(body, okhttp3.MediaType.parse("application/json")))
            .build();

        try (Response resp = http.newCall(req).execute()) {
            String respBody = resp.body() != null ? resp.body().string() : "";
            JsonObject json = JsonParser.parseString(respBody).getAsJsonObject();
            if (json.get("retCode").getAsInt() == 0) {
                System.out.println("Bybit withdrawal initiated: " + quantity + " " + asset + " via " + chain);
                return true;
            }
            System.err.println("Bybit withdrawal failed: " + json.get("retMsg").getAsString());
            return false;
        }
    }

    // ── FEES ─────────────────────────────────────────────────────────

    @Override public double takerFee() { return 0.001; }
    @Override public double makerFee() { return 0.001; }

    // ── SHUTDOWN ─────────────────────────────────────────────────────

    @Override
    public void close() {
        for (BybitWebSocket ws : activeSockets) {
            try { ws.shutdown(); } catch (Exception ignored) {}
        }
        activeSockets.clear();
        System.out.println("Bybit connectors closed");
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

    private class BybitWebSocket extends WebSocketClient {
        private final String symbol;
        private final boolean isSpot;
        private final java.util.function.Consumer<String> handler;
        private volatile boolean reconnect = true;

        BybitWebSocket(URI uri, String symbol, boolean isSpot, java.util.function.Consumer<String> handler) {
            super(uri);
            this.symbol  = symbol;
            this.isSpot  = isSpot;
            this.handler = handler;
        }

        @Override
        public void onOpen(ServerHandshake h) {
            System.out.println("Bybit " + (isSpot ? "spot" : "futures") + " connected: " + symbol);
            try {
                this.send("{\"op\":\"subscribe\",\"args\":[\"orderbook.1." + symbol + "\"]}");
            } catch (Exception e) {
                System.err.println("Bybit subscription error for " + symbol + ": " + e.getMessage());
            }
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
            }, "Bybit-Reconnect-" + symbol).start();
        }

        @Override public void onError(Exception e) {
            System.err.println("Bybit WS error " + symbol + ": " + e.getMessage());
        }

        void shutdown() { reconnect = false; this.close(); }
    }
}
