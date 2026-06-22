import type { Asset, AssetSignal, Bias, Candle } from './types'
import { atr, ema, swings } from './indicators'

// ─── Deterministic trend-bias engine ─────────────────────────────────────────
//
// IMPORTANT: this is a MECHANICAL TREND CLASSIFIER. It describes the *current*
// trend state from real price structure — it does NOT predict, and it will
// whipsaw in ranges. Backtest before trusting it live.
//
// Per timeframe we take three independent directional votes and combine them:
//   1. MA stack   — EMA(9/21/50) ordering relative to price
//   2. Slope      — EMA(21) slope over the last 5 bars, normalised by ATR
//   3. Structure  — recent swing highs/lows (HH/HL = up, LH/LL = down)
// Strength (0-100) blends vote agreement, ATR-normalised MA separation, and
// slope magnitude, with a small bonus when ATR is expanding.

const MIN_BARS = 55 // need enough history for EMA(50) + structure

interface TFResult {
  bias: Bias
  score: number
  expanding: boolean
}

function timeframeBias(candles: Candle[] | undefined): TFResult {
  if (!candles || candles.length < MIN_BARS) return { bias: 'SIDE', score: 0, expanding: false }

  const closes = candles.map((c) => c.c)
  const n = closes.length - 1
  const price = closes[n]
  const e9 = ema(closes, 9)
  const e21 = ema(closes, 21)
  const e50 = ema(closes, 50)
  const a = atr(candles, 14)
  const atrNow = a[n] && !Number.isNaN(a[n]) ? a[n] : price * 0.01

  // 1. MA stack
  const up = price > e9[n] && e9[n] > e21[n] && e21[n] > e50[n]
  const down = price < e9[n] && e9[n] < e21[n] && e21[n] < e50[n]
  const stackVote = up ? 1 : down ? -1 : 0

  // 2. Slope of EMA(21) over 5 bars, normalised by ATR
  const slope = (e21[n] - e21[n - 5]) / atrNow
  const slopeVote = slope > 0.15 ? 1 : slope < -0.15 ? -1 : 0

  // 3. Structure
  const { highs, lows } = swings(candles, 3)
  let structVote = 0
  if (highs.length >= 2 && lows.length >= 2) {
    const lastH = highs[highs.length - 1].price
    const prevH = highs[highs.length - 2].price
    const lastL = lows[lows.length - 1].price
    const prevL = lows[lows.length - 2].price
    if (lastH > prevH && lastL > prevL) structVote = 1
    else if (lastH < prevH && lastL < prevL) structVote = -1
  }

  const net = stackVote + slopeVote + structVote
  const bias: Bias = net > 0 ? 'BULL' : net < 0 ? 'BEAR' : 'SIDE'

  // ATR expansion: current vs mean ATR over the last ~50 bars
  const recent = a.slice(Math.max(0, n - 50)).filter((x) => !Number.isNaN(x))
  const atrMean = recent.length ? recent.reduce((s, x) => s + x, 0) / recent.length : atrNow
  const expanding = atrNow > atrMean * 1.1

  // Strength
  const dirAgreement = Math.abs(net) / 3 // 0..1
  const sep = Math.min(Math.abs((e9[n] - e50[n]) / atrNow) / 2, 1) // ~2 ATR span = full
  const slopeN = Math.min(Math.abs(slope), 1)
  let score = Math.round(100 * (0.5 * dirAgreement + 0.3 * sep + 0.2 * slopeN))
  if (expanding) score = Math.min(100, score + 8)
  if (bias === 'SIDE') score = Math.min(score, 40) // ranges never look "strong"

  return { bias, score, expanding }
}

function pct(curr: number, prev: number): number {
  if (!prev) return 0
  return +(((curr - prev) / prev) * 100).toFixed(2)
}

/** Build the full per-asset signal from Daily + 4H candles. */
export function computeSignal(asset: Asset, daily: Candle[] | undefined, h4: Candle[] | undefined): AssetSignal {
  const d = timeframeBias(daily)
  const h = timeframeBias(h4)
  const hasData = !!(daily && daily.length > 0)

  const price = hasData ? daily![daily!.length - 1].c : null
  let dPct = 0
  let wPct = 0
  if (daily && daily.length > 6) {
    const c = daily.map((x) => x.c)
    const n = c.length - 1
    dPct = pct(c[n], c[n - 1])
    wPct = pct(c[n], c[n - 5])
  }

  const aligned = d.bias !== 'SIDE' && d.bias === h.bias
  let strength = Math.round(0.6 * d.score + 0.4 * h.score)
  if (aligned) strength = Math.min(100, strength + 5)
  const expanding = d.expanding || h.expanding

  const note = !hasData
    ? 'No data'
    : aligned && expanding
      ? 'Aligned · expanding'
      : aligned
        ? 'MTF aligned'
        : d.bias !== 'SIDE' && h.bias !== 'SIDE' && d.bias !== h.bias
          ? 'D/4H conflict'
          : d.bias === 'SIDE'
            ? 'Ranging'
            : 'Forming'

  return {
    symbol: asset.symbol,
    label: asset.label,
    cat: asset.cat,
    tv: asset.tv,
    currencies: asset.currencies,
    price,
    dPct,
    wPct,
    htfBias: d.bias,
    dailyBias: d.bias,
    h4Bias: h.bias,
    strength: hasData ? strength : 0,
    aligned,
    expanding,
    note,
    hasData,
  }
}
