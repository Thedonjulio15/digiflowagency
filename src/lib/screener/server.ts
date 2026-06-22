import { createServerFn } from '@tanstack/react-start'
import type { ScreenerData } from './types'
import { ASSET_UNIVERSE } from './universe'
import { computeSignal } from './bias'
import { fetchOhlc } from './twelvedata'
import { fetchCalendar } from './calendar'
import { MOCK_CALENDAR, MOCK_SIGNALS } from './mock'

// ─── Server function: getScreener ─────────────────────────────────────────────
// Runs server-side only (API key never reaches the client). Data collection is
// decoupled from rendering via a module-scope cache: the page reads the cache
// instantly; a real refresh only happens when the cache is older than TTL.
//
// Module-scope memory is per-instance and lost on cold start — fine for v1's
// low traffic. Hardening (shared cache across instances / scheduled refresh via
// Netlify Blobs) is a documented next step, not needed to ship.

const TTL_MS = 30 * 60 * 1000 // 30 minutes

let cache: { data: ScreenerData; ts: number } | null = null

async function buildData(): Promise<ScreenerData> {
  const { daily, h4 } = await fetchOhlc(ASSET_UNIVERSE)
  const signals = ASSET_UNIVERSE.map((a) => computeSignal(a, daily.get(a.td), h4.get(a.td)))
  const calendar = await fetchCalendar()
  return { signals, calendar, updatedAt: new Date().toISOString(), source: 'live' }
}

export const getScreener = createServerFn({ method: 'GET' }).handler(async (): Promise<ScreenerData> => {
  if (cache && Date.now() - cache.ts < TTL_MS) return cache.data

  try {
    const data = await buildData()
    cache = { data, ts: Date.now() }
    return data
  } catch (err) {
    // Serve a stale cache if we have one, otherwise fall back to mock so the
    // page still renders (UI shows a banner based on `source`).
    if (cache) return { ...cache.data, source: 'stale' }
    return {
      signals: MOCK_SIGNALS,
      calendar: MOCK_CALENDAR,
      updatedAt: new Date().toISOString(),
      source: 'mock',
      error: err instanceof Error ? err.message : String(err),
    }
  }
})
