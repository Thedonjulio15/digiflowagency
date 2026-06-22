import type { Candle } from './types'

// ─── Plain, auditable indicator math (no external dependency) ─────────────────
// Kept deliberately small and transparent: for a trading tool you want to be
// able to read exactly how every number is produced.

/** Exponential moving average series, aligned 1:1 with `values`. */
export function ema(values: number[], period: number): number[] {
  const k = 2 / (period + 1)
  const out: number[] = []
  let prev = 0
  values.forEach((v, i) => {
    prev = i === 0 ? v : v * k + prev * (1 - k)
    out.push(prev)
  })
  return out
}

function trueRanges(candles: Candle[]): number[] {
  return candles.map((c, i) => {
    if (i === 0) return c.h - c.l
    const pc = candles[i - 1].c
    return Math.max(c.h - c.l, Math.abs(c.h - pc), Math.abs(c.l - pc))
  })
}

/** Wilder ATR series. Leading values (< period) are NaN. */
export function atr(candles: Candle[], period: number): number[] {
  const tr = trueRanges(candles)
  const out: number[] = []
  let prev = 0
  for (let i = 0; i < tr.length; i++) {
    if (i < period - 1) {
      out.push(NaN)
    } else if (i === period - 1) {
      prev = tr.slice(0, period).reduce((a, b) => a + b, 0) / period
      out.push(prev)
    } else {
      prev = (prev * (period - 1) + tr[i]) / period
      out.push(prev)
    }
  }
  return out
}

export interface Pivot {
  i: number
  price: number
}

/** N-bar pivot highs/lows (a swing high is the strict max over ±lookback). */
export function swings(candles: Candle[], lookback: number): { highs: Pivot[]; lows: Pivot[] } {
  const highs: Pivot[] = []
  const lows: Pivot[] = []
  for (let i = lookback; i < candles.length - lookback; i++) {
    let isHigh = true
    let isLow = true
    for (let j = i - lookback; j <= i + lookback; j++) {
      if (j === i) continue
      if (candles[j].h >= candles[i].h) isHigh = false
      if (candles[j].l <= candles[i].l) isLow = false
    }
    if (isHigh) highs.push({ i, price: candles[i].h })
    if (isLow) lows.push({ i, price: candles[i].l })
  }
  return { highs, lows }
}
