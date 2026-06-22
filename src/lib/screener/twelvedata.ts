import type { Asset, Candle } from './types'

// ─── Twelve Data OHLC fetch (server-side only) ────────────────────────────────
//
// Strategy: one multi-symbol BATCH request per interval (`symbol=A,B,C…`), so a
// full refresh of the whole universe is ~2 HTTP calls instead of ~50 — which is
// what keeps us inside the free tier's 8 req/min limit and lets it complete
// inside a single serverless request.
//
// If a future free-tier change disallows batching, swap the implementation of
// `fetchInterval` for a throttled per-symbol loop (≤8/min) behind a scheduled
// function — nothing else in the app needs to change.

const BASE = 'https://api.twelvedata.com/time_series'
const OUTPUT_SIZE = 150 // enough for EMA(50) + swing structure

export class MissingKeyError extends Error {}

interface TDValue {
  datetime: string
  open: string
  high: string
  low: string
  close: string
}

function toCandles(values: TDValue[] | undefined): Candle[] {
  if (!values || !Array.isArray(values)) return []
  // Twelve Data returns newest-first; normalise to oldest → newest.
  return values
    .map((v) => ({
      t: v.datetime,
      o: +v.open,
      h: +v.high,
      l: +v.low,
      c: +v.close,
    }))
    .filter((c) => Number.isFinite(c.c))
    .reverse()
}

/** Fetch one interval for the whole universe in a single batch request. */
async function fetchInterval(assets: Asset[], interval: string, apiKey: string): Promise<Map<string, Candle[]>> {
  const symbols = assets.map((a) => a.td).join(',')
  const url = `${BASE}?symbol=${encodeURIComponent(symbols)}&interval=${interval}&outputsize=${OUTPUT_SIZE}&apikey=${apiKey}`

  const res = await fetch(url)
  if (!res.ok) throw new Error(`Twelve Data ${interval} HTTP ${res.status}`)
  const json: any = await res.json()

  const out = new Map<string, Candle[]>()

  // Single-symbol responses come back flat ({ meta, values }); multi-symbol
  // responses are keyed by the requested symbol string.
  if (json && json.values) {
    out.set(assets[0].td, toCandles(json.values))
    return out
  }
  if (json && (json.status === 'error' || json.code)) {
    throw new Error(`Twelve Data ${interval}: ${json.message || 'error'}`)
  }
  for (const asset of assets) {
    const entry = json?.[asset.td]
    if (entry && entry.values) out.set(asset.td, toCandles(entry.values))
  }
  return out
}

export interface OhlcBundle {
  daily: Map<string, Candle[]>
  h4: Map<string, Candle[]>
}

/** Fetch Daily + 4H OHLC for every asset. Throws on missing key / API error. */
export async function fetchOhlc(assets: Asset[]): Promise<OhlcBundle> {
  const apiKey = process.env.TWELVE_DATA_API_KEY
  if (!apiKey) throw new MissingKeyError('TWELVE_DATA_API_KEY not set')

  const [daily, h4] = await Promise.all([
    fetchInterval(assets, '1day', apiKey),
    fetchInterval(assets, '4h', apiKey),
  ])
  return { daily, h4 }
}
