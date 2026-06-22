import type { AssetSignal, Bias, CalEvent } from './types'
import { ASSET_UNIVERSE } from './universe'

// ─── Mock fixture ─────────────────────────────────────────────────────────────
// Used when TWELVE_DATA_API_KEY is absent or the API hard-fails, so the page
// always renders (with a visible banner). Values are deterministic/plausible,
// NOT real market data.

const SEED_PRICES: Record<string, number> = {
  EURUSD: 1.0842, GBPUSD: 1.2715, USDJPY: 157.32, USDCHF: 0.8951, AUDUSD: 0.6634,
  USDCAD: 1.3688, NZDUSD: 0.6112, GBPJPY: 200.01, EURJPY: 170.6, AUDJPY: 104.3,
  GBPAUD: 1.9165, EURGBP: 0.8527, AUDCAD: 0.9082, GBPCAD: 1.7402, EURCAD: 1.4842,
  XAUUSD: 3284.5, WTI: 67.4, US100: 478.2, US30: 432.1, SPX500: 587.3,
  AAPL: 213.4, TSLA: 351.7, NVDA: 131.2, MSFT: 445.6, AMZN: 222.1, META: 638.4,
}

function hash(s: string): number {
  let h = 0
  for (let i = 0; i < s.length; i++) h = (Math.imul(31, h) + s.charCodeAt(i)) | 0
  return Math.abs(h)
}

export const MOCK_SIGNALS: AssetSignal[] = ASSET_UNIVERSE.map((a) => {
  const r = hash(a.symbol)
  const dailyBias: Bias = ['BULL', 'BEAR', 'SIDE'][r % 3] as Bias
  const h4Bias: Bias = ['BULL', 'BEAR', 'SIDE'][(r >> 2) % 3] as Bias
  const aligned = dailyBias !== 'SIDE' && dailyBias === h4Bias
  const expanding = (r >> 4) % 3 === 0
  let strength = 30 + (r % 60)
  if (aligned) strength = Math.min(100, strength + 10)
  if (dailyBias === 'SIDE') strength = Math.min(strength, 38)
  const dPct = +(((r % 200) - 100) / 100).toFixed(2)
  const wPct = +((((r >> 3) % 400) - 200) / 100).toFixed(2)
  return {
    symbol: a.symbol,
    label: a.label,
    cat: a.cat,
    tv: a.tv,
    currencies: a.currencies,
    price: SEED_PRICES[a.symbol] ?? 100,
    dPct,
    wPct,
    htfBias: dailyBias,
    dailyBias,
    h4Bias,
    strength,
    aligned,
    expanding,
    note: aligned ? (expanding ? 'Aligned · expanding' : 'MTF aligned') : dailyBias === 'SIDE' ? 'Ranging' : 'Forming',
    hasData: true,
  }
})

export const MOCK_CALENDAR: CalEvent[] = [
  { currency: 'USD', title: 'Core PCE Price Index m/m', impact: 'High', time: '12:30', forecast: '0.2%', previous: '0.3%' },
  { currency: 'EUR', title: 'ECB President Lagarde Speaks', impact: 'Medium', time: '08:00' },
  { currency: 'GBP', title: 'Retail Sales m/m', impact: 'Medium', time: '06:00', forecast: '0.4%', previous: '-0.1%' },
]
