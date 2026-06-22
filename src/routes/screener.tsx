import { createFileRoute } from '@tanstack/react-router'
import { useEffect, useMemo, useRef, useState } from 'react'
import './screener.css'
import { getScreener } from '@/lib/screener/server'
import type { AssetSignal, Bias, CalEvent, Category, ScreenerData } from '@/lib/screener/types'

export const Route = createFileRoute('/screener')({
  loader: () => getScreener(),
  component: Screener,
})

const CATS: (Category | 'ALL')[] = ['ALL', 'FOREX', 'METALS', 'OIL', 'INDICES', 'STOCKS']
const TF_TABS: [string, string][] = [['1W', '1W'], ['1D', '1D'], ['4H', '240'], ['1H', '60'], ['15M', '15'], ['5M', '5']]

const biasClass = (b: Bias) => (b === 'BULL' ? 'bull' : b === 'BEAR' ? 'bear' : 'side')
const biasLabel = (b: Bias) => (b === 'BULL' ? '▲ BULL' : b === 'BEAR' ? '▼ BEAR' : '— SIDE')
const biasVar = (b: Bias) => (b === 'BULL' ? 'var(--bull)' : b === 'BEAR' ? 'var(--bear)' : 'var(--side)')

function fmtPrice(s: AssetSignal): string {
  if (s.price == null) return '—'
  if (s.cat === 'FOREX') return s.price < 10 ? s.price.toFixed(5) : s.price.toFixed(3)
  return s.price.toFixed(2)
}

function newsFlag(s: AssetSignal, calendar: CalEvent[]): 'high' | 'med' | null {
  const evs = calendar.filter((e) => s.currencies.includes(e.currency))
  if (evs.some((e) => e.impact === 'High')) return 'high'
  if (evs.some((e) => e.impact === 'Medium')) return 'med'
  return null
}

function Screener() {
  const initial = Route.useLoaderData()
  const [data, setData] = useState<ScreenerData>(initial)
  const [cat, setCat] = useState<Category | 'ALL'>('ALL')
  const [biasFilter, setBiasFilter] = useState<'BULL' | 'BEAR' | 'ALIGNED' | null>(null)
  const [selected, setSelected] = useState<string | null>(null)
  const [refreshing, setRefreshing] = useState(false)
  const [clock, setClock] = useState('')
  const [chart, setChart] = useState<{ sym: AssetSignal; tf: string } | null>(null)

  // Live UTC clock (client-only to avoid hydration mismatch)
  useEffect(() => {
    const tick = () => {
      const n = new Date()
      setClock(
        `${String(n.getUTCHours()).padStart(2, '0')}:${String(n.getUTCMinutes()).padStart(2, '0')}:${String(
          n.getUTCSeconds(),
        ).padStart(2, '0')} UTC`,
      )
    }
    tick()
    const id = setInterval(tick, 1000)
    return () => clearInterval(id)
  }, [])

  async function refresh() {
    setRefreshing(true)
    try {
      setData(await getScreener())
    } finally {
      setRefreshing(false)
    }
  }

  // Poll every 5 minutes (server caches, so this is cheap)
  const refreshRef = useRef(refresh)
  refreshRef.current = refresh
  useEffect(() => {
    const id = setInterval(() => refreshRef.current(), 5 * 60 * 1000)
    return () => clearInterval(id)
  }, [])

  // Keyboard shortcuts
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setChart(null)
      if ((e.key === 'r' || e.key === 'R') && !chart) refresh()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chart])

  const rows = useMemo(() => {
    let list = data.signals
    if (cat !== 'ALL') list = list.filter((s) => s.cat === cat)
    if (biasFilter === 'BULL') list = list.filter((s) => s.htfBias === 'BULL')
    else if (biasFilter === 'BEAR') list = list.filter((s) => s.htfBias === 'BEAR')
    else if (biasFilter === 'ALIGNED') list = list.filter((s) => s.aligned)
    return [...list].sort((a, b) => {
      const aw = (a.aligned ? 2 : 0) + (a.expanding ? 1 : 0)
      const bw = (b.aligned ? 2 : 0) + (b.expanding ? 1 : 0)
      if (aw !== bw) return bw - aw
      return b.strength - a.strength
    })
  }, [data.signals, cat, biasFilter])

  const counts = useMemo(
    () => ({
      bull: data.signals.filter((s) => s.htfBias === 'BULL').length,
      bear: data.signals.filter((s) => s.htfBias === 'BEAR').length,
      side: data.signals.filter((s) => s.htfBias === 'SIDE').length,
    }),
    [data.signals],
  )

  const sel = selected ? data.signals.find((s) => s.symbol === selected) ?? null : null
  const updated = new Date(data.updatedAt)

  return (
    <div className="flow">
      <div className="flow-header">
        <div className="flow-logo">
          FL<span>O</span>W
        </div>
        <div className="flow-header-meta">
          <span className="flow-clock">{clock || '--:--:-- UTC'}</span>
          <span>
            Updated {String(updated.getUTCHours()).padStart(2, '0')}:{String(updated.getUTCMinutes()).padStart(2, '0')} UTC
          </span>
        </div>
        <button className="flow-refresh" onClick={refresh} disabled={refreshing}>
          {refreshing ? '…' : '↻'} REFRESH
        </button>
      </div>

      {data.source === 'mock' && (
        <div className="flow-banner mock">
          ⚠ MOCK DATA — no <code>TWELVE_DATA_API_KEY</code> set or the data API failed. Numbers below are not real.
          {data.error ? ` (${data.error})` : ''}
        </div>
      )}
      {data.source === 'stale' && (
        <div className="flow-banner stale">Showing last good data — the latest refresh failed.</div>
      )}

      <div className="flow-layout">
        {/* Screener */}
        <div className="flow-panel">
          <div className="flow-panel-header">
            <span className="flow-panel-title">Market Screener</span>
            <div className="flow-filters">
              {CATS.map((c) => (
                <button key={c} className={`flow-filter ${cat === c ? 'active' : ''}`} onClick={() => setCat(c)}>
                  {c}
                </button>
              ))}
              <button
                className={`flow-filter ${biasFilter === 'BULL' ? 'active' : ''}`}
                style={{ marginLeft: 10 }}
                onClick={() => setBiasFilter(biasFilter === 'BULL' ? null : 'BULL')}
              >
                ↑ BULL
              </button>
              <button
                className={`flow-filter ${biasFilter === 'BEAR' ? 'active' : ''}`}
                onClick={() => setBiasFilter(biasFilter === 'BEAR' ? null : 'BEAR')}
              >
                ↓ BEAR
              </button>
              <button
                className={`flow-filter ${biasFilter === 'ALIGNED' ? 'active' : ''}`}
                onClick={() => setBiasFilter(biasFilter === 'ALIGNED' ? null : 'ALIGNED')}
              >
                ⟳ ALIGNED
              </button>
            </div>
          </div>

          <div className="flow-body">
            <table className="flow-table">
              <thead>
                <tr>
                  <th>ASSET</th>
                  <th>HTF BIAS</th>
                  <th>D / 4H</th>
                  <th>STRENGTH</th>
                  <th className="r">D%</th>
                  <th className="r">W%</th>
                  <th>NEWS</th>
                </tr>
              </thead>
              <tbody>
                {rows.length === 0 && (
                  <tr>
                    <td colSpan={7}>
                      <div className="flow-empty">No assets match filter</div>
                    </td>
                  </tr>
                )}
                {rows.map((s) => {
                  const flag = newsFlag(s, data.calendar)
                  return (
                    <tr
                      key={s.symbol}
                      className={selected === s.symbol ? 'selected' : ''}
                      onClick={() => setSelected(s.symbol)}
                    >
                      <td>
                        <div className="flow-asset-name">
                          {s.label}
                          {s.aligned && <span style={{ color: 'var(--bull)', fontSize: 10, marginLeft: 3 }}>✓</span>}
                        </div>
                        <div className="flow-asset-cat">{s.cat}</div>
                      </td>
                      <td>
                        <span className={`flow-bias ${biasClass(s.htfBias)}`}>
                          <span className="flow-dot" />
                          {biasLabel(s.htfBias)}
                        </span>
                      </td>
                      <td>
                        <div className="flow-tf-row">
                          <div className={`flow-tf ${biasClass(s.dailyBias)}`}>D</div>
                          <div className={`flow-tf ${biasClass(s.h4Bias)}`}>4H</div>
                        </div>
                      </td>
                      <td>
                        <div className="flow-str-wrap">
                          <div className="flow-str-bar">
                            <div
                              className="flow-str-fill"
                              style={{ width: `${s.strength}%`, background: biasVar(s.htfBias) }}
                            />
                          </div>
                          <span className="flow-str-val">{s.strength}%</span>
                          {s.expanding && (
                            <span className="flow-expand" title="ATR expanding (volatility build-up)">
                              ⚡
                            </span>
                          )}
                        </div>
                      </td>
                      <td className={`flow-pct ${s.dPct > 0 ? 'pos' : s.dPct < 0 ? 'neg' : 'flat'}`}>
                        {s.dPct > 0 ? '+' : ''}
                        {s.dPct}%
                      </td>
                      <td className={`flow-pct ${s.wPct > 0 ? 'pos' : s.wPct < 0 ? 'neg' : 'flat'}`}>
                        {s.wPct > 0 ? '+' : ''}
                        {s.wPct}%
                      </td>
                      <td>
                        {flag ? (
                          <span className={`flow-news-flag ${flag}`} title="Economic event today" />
                        ) : (
                          <span style={{ color: 'var(--muted)' }}>—</span>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <div className="flow-disclaimer">
            Mechanical trend classifier from real price structure (EMA stack · swing structure · ATR). Describes current
            trend state, not a prediction — whipsaws in ranges. Not financial advice. Backtest before trusting live.
          </div>
        </div>

        {/* Right column */}
        <div className="flow-right">
          {sel ? (
            <div className="flow-detail">
              <div className="flow-detail-top">
                <div>
                  <div className="flow-detail-name">{sel.label}</div>
                  <div className="flow-asset-cat" style={{ marginTop: 2 }}>
                    {sel.cat} · {sel.note}
                  </div>
                </div>
                <div>
                  <div className="flow-detail-price">{fmtPrice(sel)}</div>
                  <div
                    className="flow-detail-chg"
                    style={{ color: sel.dPct > 0 ? 'var(--bull)' : sel.dPct < 0 ? 'var(--bear)' : 'var(--muted)' }}
                  >
                    {sel.dPct > 0 ? '+' : ''}
                    {sel.dPct}% today
                  </div>
                </div>
              </div>
              <div className="flow-detail-grid">
                <div className="flow-cell">
                  <div className="flow-cell-label">HTF Bias</div>
                  <div className="flow-cell-val" style={{ color: biasVar(sel.htfBias) }}>
                    {biasLabel(sel.htfBias)}
                  </div>
                </div>
                <div className="flow-cell">
                  <div className="flow-cell-label">4H Bias</div>
                  <div className="flow-cell-val" style={{ color: biasVar(sel.h4Bias) }}>
                    {biasLabel(sel.h4Bias)}
                  </div>
                </div>
                <div className="flow-cell">
                  <div className="flow-cell-label">Strength</div>
                  <div className="flow-cell-val">{sel.strength}%</div>
                </div>
              </div>
              <div className="flow-chart-btns">
                <button onClick={() => setChart({ sym: sel, tf: '1D' })}>DAILY</button>
                <button onClick={() => setChart({ sym: sel, tf: '240' })}>4H</button>
                <button onClick={() => setChart({ sym: sel, tf: '5' })}>5M</button>
              </div>
            </div>
          ) : (
            <div className="flow-placeholder">← Select an asset</div>
          )}

          <div className="flow-overview">
            <div className="flow-ov-card">
              <div className="flow-ov-label">BULLISH</div>
              <div className="flow-ov-val bull">{counts.bull}</div>
            </div>
            <div className="flow-ov-card">
              <div className="flow-ov-label">BEARISH</div>
              <div className="flow-ov-val bear">{counts.bear}</div>
            </div>
            <div className="flow-ov-card">
              <div className="flow-ov-label">SIDEWAYS</div>
              <div className="flow-ov-val side">{counts.side}</div>
            </div>
          </div>

          <div className="flow-section-header">
            <span>High-impact events today</span>
          </div>
          <div className="flow-cal-list">
            {data.calendar.length === 0 && <div className="flow-empty">No events today</div>}
            {[...data.calendar]
              .sort((a, b) => a.time.localeCompare(b.time))
              .map((e, i) => (
                <div className="flow-cal-event" key={`${e.currency}-${e.title}-${i}`}>
                  <div className="flow-cal-top">
                    <span className="flow-cal-cur">{e.currency}</span>
                    <span className="flow-cal-time">{e.time}</span>
                  </div>
                  <div className="flow-cal-title">{e.title}</div>
                  <div>
                    <span className={`flow-cal-impact ${e.impact}`}>{e.impact.toUpperCase()}</span>
                    {e.forecast && (
                      <span className="flow-cal-fcst">
                        Fcst: <span>{e.forecast}</span> Prev: <span>{e.previous || '—'}</span>
                      </span>
                    )}
                  </div>
                </div>
              ))}
          </div>
        </div>
      </div>

      {/* Chart overlay */}
      {chart && (
        <div className="flow-overlay" onClick={(e) => e.target === e.currentTarget && setChart(null)}>
          <div className="flow-overlay-inner">
            <div className="flow-overlay-header">
              <div className="flow-overlay-title">{chart.sym.label}</div>
              <div className="flow-tf-tabs">
                {TF_TABS.map(([label, tf]) => (
                  <button
                    key={tf}
                    className={`flow-tf-btn ${chart.tf === tf ? 'active' : ''}`}
                    onClick={() => setChart({ sym: chart.sym, tf })}
                  >
                    {label}
                  </button>
                ))}
              </div>
              <button className="flow-close" onClick={() => setChart(null)}>
                ✕
              </button>
            </div>
            <div className="flow-iframe-wrap">
              <iframe
                title="chart"
                src={`https://www.tradingview.com/widgetembed/?frameElementId=tv_chart&symbol=${encodeURIComponent(
                  chart.sym.tv,
                )}&interval=${chart.tf}&theme=dark&style=1&timezone=UTC&withdateranges=1&hide_side_toolbar=0&allow_symbol_change=0&save_image=0&details=1`}
                allowFullScreen
              />
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
