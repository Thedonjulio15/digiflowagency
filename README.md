# DigiFlow Agency Website

A high-converting marketing website for DigiFlow Agency — a digital growth agency specialising in social media, lead generation, web design, booking systems, and business automation.

## Purpose

The site is engineered with two goals:
1. **Prove capability** — portfolio case studies, real metrics, and service breakdowns show what DigiFlow can deliver
2. **Generate qualified leads** — a 3-step qualification form captures prospect info, qualifies budget and intent, and routes submissions via Netlify Forms

## Tech Stack

- **Framework:** TanStack Start (React 19, SSR)
- **Routing:** TanStack Router (file-based)
- **Styling:** Tailwind CSS v4
- **Forms:** Netlify Forms (serverless, no backend needed)
- **Deployment:** Netlify

## Running Locally

```bash
npm install
npm run dev
```

The dev server starts on `http://localhost:3000`. For Netlify feature emulation (forms):

```bash
npx netlify dev --port 8889
```

## Environment

The marketing site needs no environment variables. The **FLOW market screener** (`/screener`)
optionally uses one:

| Variable | Purpose |
|----------|---------|
| `TWELVE_DATA_API_KEY` | Free [Twelve Data](https://twelvedata.com/) key for real OHLC. Without it the screener renders on **mock data** with a visible banner. |

Set it in the Netlify UI (Site settings → Environment variables) or a local `.env` for dev.

## FLOW Market Screener (`/screener`)

A structure-based **trend-bias screener** for a top-down workflow (Daily HTF bias → 4H
continuation → intraday entry). For each asset it computes BULL/BEAR/SIDE + a strength %
from **real price structure**, deterministically in code — no AI, no fabricated data.

### How the signal is built (`src/lib/screener/`)
- `twelvedata.ts` — fetches Daily + 4H OHLC for the whole universe in **2 batch requests**
  (server-side only; the API key never reaches the browser).
- `indicators.ts` — plain EMA / Wilder-ATR / swing-pivot math (no dependency, auditable).
- `bias.ts` — per timeframe combines three votes (EMA-9/21/50 stack, EMA-21 slope, swing
  HH-HL/LH-LL structure) + ATR-expansion; blends into a 0-100 strength. Daily = HTF;
  `aligned` = Daily & 4H agree.
- `server.ts` — `getScreener` server function with a 30-min in-memory cache (decouples data
  collection from page render; on key-missing/API-error it serves stale cache or mock).
- `calendar.ts` — ForexFactory weekly JSON (server-side, avoids CORS) for high-impact events.

### Data notes / limits
- Twelve Data free tier: **800 credits/day, 8 req/min**. The batch design keeps a full refresh
  to ~52 credits, well within budget at a 30-min cache.
- Free tier **excludes international indices** (DAX/FTSE/Nikkei) — intentionally not in the
  universe. US indices use ETF proxies (QQQ/DIA/SPY) for the OHLC data.
- `WTI` and the exact index symbols should be verified on first live deploy; swap/drop any that
  404 on the free tier (see `universe.ts`).

### ⚠️ Honest limitation
This is a **mechanical trend classifier**: it describes the *current* trend state, it does not
predict, and it whipsaws in ranges. **Not financial advice — backtest before trusting it live.**

### Not in v1 (deferred)
No news feed / no AI, no 1H timeframe in the engine, no Telegram/push alerts, no auth, no
backtester. Hardening the cache (Netlify Blobs / scheduled refresh) is the next step if traffic
grows.
