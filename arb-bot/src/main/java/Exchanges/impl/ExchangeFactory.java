package Exchanges.impl;

import Config.JarvisConfig;
import Exchanges.Exchange_Client;

public class ExchangeFactory {

    public static Exchange_Client create(String exchangeName) {
        if (exchangeName == null || exchangeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Exchange name cannot be null or empty");
        }

        JarvisConfig config = JarvisConfig.getInstance();

        return switch (exchangeName.toUpperCase()) {
            case "BINANCE" -> {
                String apiKey = config.getBinanceApiKey();
                String secretKey = config.getBinanceSecretKey();
                if (apiKey == null || apiKey.isEmpty()) {
                    System.err.println("WARNING: Binance API key not configured");
                }
                yield new BinanceConnector(apiKey, secretKey);
            }
            case "BYBIT" -> {
                String apiKey = config.getBybitApiKey();
                String secretKey = config.getBybitSecretKey();
                if (apiKey == null || apiKey.isEmpty()) {
                    System.err.println("WARNING: Bybit API key not configured");
                }
                yield new BybitConnector(apiKey, secretKey);
            }
            default -> throw new IllegalArgumentException("Unknown exchange: " + exchangeName);
        };
    }
}
