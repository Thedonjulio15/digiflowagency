// ─── Core types for the FLOW market screener ──────────────────────────────────

export type Category = 'FOREX' | 'METALS' | 'OIL' | 'INDICES' | 'STOCKS'

export type Bias = 'BULL' | 'BEAR' | 'SIDE'

/** A single OHLC candle, normalised oldest → newest. */
export interface Candle {
  t: string // ISO datetime
  o: number
  h: number
  l: number
  c: number
}

export interface Asset {
  symbol: string // internal id, e.g. 'EURUSD'
  label: string // display, e.g. 'EUR/USD'
  cat: Category
  td: string // Twelve Data symbol used to fetch OHLC, e.g. 'EUR/USD'
  tv: string // TradingView symbol used for the chart embed, e.g. 'FX:EURUSD'
  currencies: string[] // for news-flag matching, e.g. ['EUR','USD']
}

/** The computed signal shown per row. */
export interface AssetSignal {
  symbol: string
  label: string
  cat: Category
  tv: string
  currencies: string[]
  price: number | null
  dPct: number // daily % change
  wPct: number // ~weekly % change (5 sessions)
  htfBias: Bias // = daily bias (the HTF)
  dailyBias: Bias
  h4Bias: Bias
  strength: number // 0-100
  aligned: boolean // daily & 4H agree on a direction
  expanding: boolean // ATR expansion on either TF
  note: string // short human label
  hasData: boolean
}

export interface CalEvent {
  currency: string
  title: string
  impact: 'High' | 'Medium' | 'Low'
  time: string
  forecast?: string
  previous?: string
}

export interface ScreenerData {
  signals: AssetSignal[]
  calendar: CalEvent[]
  updatedAt: string
  /** live = fresh from Twelve Data, stale = served from an old cache after a
   *  fetch error, mock = no API key / hard failure (UI shows a banner). */
  source: 'live' | 'stale' | 'mock'
  error?: string
}
