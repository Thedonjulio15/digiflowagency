import type { CalEvent } from './types'

// ─── Economic calendar (ForexFactory weekly JSON, fetched server-side) ────────
// Server-side fetch avoids the browser CORS block. Falls back to a small
// hardcoded set if the feed is unreachable.

const FEED = 'https://nfs.faireconomy.media/ff_calendar_thisweek.json'

interface FFEvent {
  title: string
  country: string
  date: string
  impact: string
  forecast?: string
  previous?: string
}

const FALLBACK: CalEvent[] = [
  { currency: 'USD', title: 'Fed Chair Powell Speaks', impact: 'High', time: '14:00' },
  { currency: 'USD', title: 'CPI y/y', impact: 'High', time: '12:30', forecast: '2.4%', previous: '2.3%' },
  { currency: 'GBP', title: 'BOE Interest Rate Decision', impact: 'High', time: '11:00', forecast: '4.25%', previous: '4.50%' },
  { currency: 'EUR', title: 'ECB Monetary Policy Statement', impact: 'Medium', time: '13:45' },
  { currency: 'JPY', title: 'BOJ Policy Rate', impact: 'High', time: '03:00' },
]

export async function fetchCalendar(): Promise<CalEvent[]> {
  try {
    const res = await fetch(FEED)
    if (!res.ok) throw new Error(`calendar HTTP ${res.status}`)
    const events: FFEvent[] = await res.json()
    const today = new Date().toISOString().slice(0, 10)
    return events
      .filter((e) => e.date?.startsWith(today) && e.impact !== 'Low')
      .map((e) => ({
        currency: e.country,
        title: e.title,
        impact: (e.impact === 'High' ? 'High' : 'Medium') as CalEvent['impact'],
        time: e.date.slice(11, 16) || '—',
        forecast: e.forecast || undefined,
        previous: e.previous || undefined,
      }))
  } catch {
    return FALLBACK
  }
}
