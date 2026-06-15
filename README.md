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

No environment variables required. The site is fully static except for Netlify Forms submissions.
