import type { Asset } from './types'

// ─── Asset universe (trimmed to Twelve Data free-tier coverage) ───────────────
//
// `td`  → symbol used to fetch OHLC from Twelve Data.
// `tv`  → symbol used for the TradingView chart embed (independent of `td`).
//
// NOTE: Twelve Data's free tier blocks international indices (DAX/FTSE/Nikkei),
// so they are intentionally absent. US indices use liquid ETF proxies for the
// OHLC data (QQQ/DIA/SPY are guaranteed-free US equities and track the index
// closely enough for a trend read) while the chart still shows the index itself.
// WTI may also be unavailable on free — verify on deploy and drop if it 404s.

export const ASSET_UNIVERSE: Asset[] = [
  // Forex majors
  { symbol: 'EURUSD', label: 'EUR/USD', cat: 'FOREX', td: 'EUR/USD', tv: 'FX:EURUSD', currencies: ['EUR', 'USD'] },
  { symbol: 'GBPUSD', label: 'GBP/USD', cat: 'FOREX', td: 'GBP/USD', tv: 'FX:GBPUSD', currencies: ['GBP', 'USD'] },
  { symbol: 'USDJPY', label: 'USD/JPY', cat: 'FOREX', td: 'USD/JPY', tv: 'FX:USDJPY', currencies: ['USD', 'JPY'] },
  { symbol: 'USDCHF', label: 'USD/CHF', cat: 'FOREX', td: 'USD/CHF', tv: 'FX:USDCHF', currencies: ['USD', 'CHF'] },
  { symbol: 'AUDUSD', label: 'AUD/USD', cat: 'FOREX', td: 'AUD/USD', tv: 'FX:AUDUSD', currencies: ['AUD', 'USD'] },
  { symbol: 'USDCAD', label: 'USD/CAD', cat: 'FOREX', td: 'USD/CAD', tv: 'FX:USDCAD', currencies: ['USD', 'CAD'] },
  { symbol: 'NZDUSD', label: 'NZD/USD', cat: 'FOREX', td: 'NZD/USD', tv: 'FX:NZDUSD', currencies: ['NZD', 'USD'] },
  // Forex crosses (decent volume only)
  { symbol: 'GBPJPY', label: 'GBP/JPY', cat: 'FOREX', td: 'GBP/JPY', tv: 'FX:GBPJPY', currencies: ['GBP', 'JPY'] },
  { symbol: 'EURJPY', label: 'EUR/JPY', cat: 'FOREX', td: 'EUR/JPY', tv: 'FX:EURJPY', currencies: ['EUR', 'JPY'] },
  { symbol: 'AUDJPY', label: 'AUD/JPY', cat: 'FOREX', td: 'AUD/JPY', tv: 'FX:AUDJPY', currencies: ['AUD', 'JPY'] },
  { symbol: 'GBPAUD', label: 'GBP/AUD', cat: 'FOREX', td: 'GBP/AUD', tv: 'FX:GBPAUD', currencies: ['GBP', 'AUD'] },
  { symbol: 'EURGBP', label: 'EUR/GBP', cat: 'FOREX', td: 'EUR/GBP', tv: 'FX:EURGBP', currencies: ['EUR', 'GBP'] },
  { symbol: 'AUDCAD', label: 'AUD/CAD', cat: 'FOREX', td: 'AUD/CAD', tv: 'FX:AUDCAD', currencies: ['AUD', 'CAD'] },
  { symbol: 'GBPCAD', label: 'GBP/CAD', cat: 'FOREX', td: 'GBP/CAD', tv: 'FX:GBPCAD', currencies: ['GBP', 'CAD'] },
  { symbol: 'EURCAD', label: 'EUR/CAD', cat: 'FOREX', td: 'EUR/CAD', tv: 'FX:EURCAD', currencies: ['EUR', 'CAD'] },
  // Metals
  { symbol: 'XAUUSD', label: 'Gold', cat: 'METALS', td: 'XAU/USD', tv: 'OANDA:XAUUSD', currencies: ['USD'] },
  // Oil
  { symbol: 'WTI', label: 'WTI Crude', cat: 'OIL', td: 'WTI/USD', tv: 'TVC:USOIL', currencies: ['USD'] },
  // US indices (data via ETF proxy, chart via index)
  { symbol: 'US100', label: 'NASDAQ 100', cat: 'INDICES', td: 'QQQ', tv: 'FOREXCOM:NSXUSD', currencies: ['USD'] },
  { symbol: 'US30', label: 'Dow Jones', cat: 'INDICES', td: 'DIA', tv: 'FOREXCOM:DJI', currencies: ['USD'] },
  { symbol: 'SPX500', label: 'S&P 500', cat: 'INDICES', td: 'SPY', tv: 'FOREXCOM:SPXUSD', currencies: ['USD'] },
  // US stock CFDs
  { symbol: 'AAPL', label: 'Apple', cat: 'STOCKS', td: 'AAPL', tv: 'NASDAQ:AAPL', currencies: ['USD'] },
  { symbol: 'TSLA', label: 'Tesla', cat: 'STOCKS', td: 'TSLA', tv: 'NASDAQ:TSLA', currencies: ['USD'] },
  { symbol: 'NVDA', label: 'NVIDIA', cat: 'STOCKS', td: 'NVDA', tv: 'NASDAQ:NVDA', currencies: ['USD'] },
  { symbol: 'MSFT', label: 'Microsoft', cat: 'STOCKS', td: 'MSFT', tv: 'NASDAQ:MSFT', currencies: ['USD'] },
  { symbol: 'AMZN', label: 'Amazon', cat: 'STOCKS', td: 'AMZN', tv: 'NASDAQ:AMZN', currencies: ['USD'] },
  { symbol: 'META', label: 'Meta', cat: 'STOCKS', td: 'META', tv: 'NASDAQ:META', currencies: ['USD'] },
]
