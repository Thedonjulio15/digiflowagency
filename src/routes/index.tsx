import { createFileRoute } from '@tanstack/react-router'
import { useState, useEffect, useRef } from 'react'

export const Route = createFileRoute('/')({
  component: DigiFlowHome,
})

// ─── Nav ────────────────────────────────────────────────────────────────────

function Nav() {
  const [scrolled, setScrolled] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)

  useEffect(() => {
    const handler = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', handler)
    return () => window.removeEventListener('scroll', handler)
  }, [])

  const links = [
    { label: 'Services', href: '#services' },
    { label: 'Portfolio', href: '#portfolio' },
    { label: 'Results', href: '#testimonials' },
    { label: 'About', href: '#about' },
  ]

  return (
    <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${scrolled ? 'nav-blur' : 'bg-transparent'}`}>
      <div className="max-w-6xl mx-auto px-6 py-4 flex items-center justify-between">
        <a href="#" className="flex items-center gap-3 group">
          <img src="/digiflow-logo.jpeg" alt="DigiFlow Agency" className="h-10 w-auto rounded-lg" />
        </a>

        <div className="hidden md:flex items-center gap-8">
          {links.map(l => (
            <a key={l.href} href={l.href} className="text-white/70 hover:text-white transition-colors duration-200 text-sm font-medium">
              {l.label}
            </a>
          ))}
        </div>

        <a
          href="#qualify"
          className="hidden md:inline-flex cta-button text-white font-semibold px-5 py-2.5 rounded-xl text-sm"
        >
          Get Free Strategy Call
        </a>

        <button
          className="md:hidden text-white p-2"
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label="Toggle menu"
        >
          <div className="w-6 flex flex-col gap-1.5">
            <span className={`h-0.5 bg-white transition-all ${menuOpen ? 'rotate-45 translate-y-2' : ''}`} />
            <span className={`h-0.5 bg-white transition-all ${menuOpen ? 'opacity-0' : ''}`} />
            <span className={`h-0.5 bg-white transition-all ${menuOpen ? '-rotate-45 -translate-y-2' : ''}`} />
          </div>
        </button>
      </div>

      {menuOpen && (
        <div className="md:hidden nav-blur border-t border-white/10">
          <div className="px-6 py-4 flex flex-col gap-4">
            {links.map(l => (
              <a key={l.href} href={l.href} className="text-white/80 hover:text-white text-base font-medium" onClick={() => setMenuOpen(false)}>
                {l.label}
              </a>
            ))}
            <a href="#qualify" className="cta-button text-white font-semibold px-5 py-3 rounded-xl text-sm text-center mt-2" onClick={() => setMenuOpen(false)}>
              Get Free Strategy Call
            </a>
          </div>
        </div>
      )}
    </nav>
  )
}

// ─── Hero ────────────────────────────────────────────────────────────────────

function Hero() {
  return (
    <section className="relative min-h-screen flex items-center hero-grid overflow-hidden pt-20">
      {/* Radial glow */}
      <div className="absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[700px] h-[700px] rounded-full opacity-20"
        style={{ background: 'radial-gradient(circle, #1A1AFF 0%, transparent 70%)' }} />
      <div className="absolute top-20 right-10 w-64 h-64 rounded-full opacity-10"
        style={{ background: 'radial-gradient(circle, #4444FF 0%, transparent 70%)' }} />

      <div className="relative max-w-6xl mx-auto px-6 py-24 grid md:grid-cols-2 gap-16 items-center w-full">
        <div className="fade-in-up">
          <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full mb-6 text-sm font-medium"
            style={{ background: 'rgba(26,26,255,0.15)', border: '1px solid rgba(26,26,255,0.3)', color: '#a0a0ff' }}>
            <span className="w-2 h-2 rounded-full bg-blue-400 animate-pulse" />
            Accepting New Clients — Limited Spots Available
          </div>

          <h1 className="text-4xl md:text-5xl lg:text-6xl font-black leading-tight mb-6">
            <span className="text-white">Turn Your Business Into a</span>{' '}
            <span className="gradient-text">Lead-Generating Machine</span>
          </h1>

          <p className="text-lg text-white/60 leading-relaxed mb-8 max-w-lg">
            DigiFlow builds the digital infrastructure ambitious businesses need to attract clients 24/7 — without lifting a finger. Social media, web design, automations, and booking systems that work while you sleep.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 mb-12">
            <a href="#qualify" className="cta-button text-white font-bold px-8 py-4 rounded-xl text-base text-center">
              Claim Your Free Strategy Call →
            </a>
            <a href="#portfolio" className="text-white/70 hover:text-white font-semibold px-8 py-4 rounded-xl text-base border border-white/20 hover:border-white/40 transition-all text-center">
              See Our Work
            </a>
          </div>

          <div className="flex items-center gap-8">
            {[
              { num: '50+', label: 'Clients Scaled' },
              { num: '3x', label: 'Average Lead Increase' },
              { num: '98%', label: 'Client Retention' },
            ].map(s => (
              <div key={s.label}>
                <div className="text-2xl font-black text-white">{s.num}</div>
                <div className="text-xs text-white/50 mt-0.5">{s.label}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="relative hidden md:block">
          <div className="float-animation">
            <div className="relative rounded-2xl overflow-hidden blue-glow"
              style={{ background: 'linear-gradient(135deg, rgba(26,26,255,0.15) 0%, rgba(8,8,26,0.8) 100%)', border: '1px solid rgba(26,26,255,0.3)', padding: '2px' }}>
              <div className="rounded-2xl overflow-hidden" style={{ background: '#0D0D2A' }}>
                <HeroDashboardMockup />
              </div>
            </div>
          </div>

          {/* Floating badges */}
          <div className="absolute -top-4 -left-6 px-4 py-2 rounded-xl text-sm font-semibold shadow-xl" style={{ background: '#1A1AFF' }}>
            +127% More Leads
          </div>
          <div className="absolute -bottom-4 -right-6 px-4 py-2 rounded-xl text-sm font-semibold shadow-xl" style={{ background: 'rgba(26,26,255,0.9)', border: '1px solid rgba(255,255,255,0.2)' }}>
            Booked: 14 calls today
          </div>
        </div>
      </div>

      <div className="absolute bottom-8 left-1/2 -translate-x-1/2 flex flex-col items-center gap-2 text-white/30 text-xs">
        <span>Scroll to explore</span>
        <div className="w-0.5 h-8 bg-gradient-to-b from-white/30 to-transparent" />
      </div>
    </section>
  )
}

function HeroDashboardMockup() {
  return (
    <div className="p-6 space-y-4 min-h-[340px]">
      <div className="flex items-center justify-between mb-4">
        <div className="text-white/60 text-xs font-mono">DIGIFLOW DASHBOARD</div>
        <div className="flex gap-1">
          {['bg-red-500','bg-yellow-400','bg-green-400'].map(c => <div key={c} className={`w-2.5 h-2.5 rounded-full ${c}`} />)}
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        {[
          { label: 'New Leads', value: '24', delta: '+12%', color: '#1A1AFF' },
          { label: 'Bookings', value: '8', delta: '+31%', color: '#22c55e' },
          { label: 'Reach', value: '14.2K', delta: '+89%', color: '#a855f7' },
          { label: 'Revenue', value: '$8.4K', delta: '+44%', color: '#f59e0b' },
        ].map(m => (
          <div key={m.label} className="rounded-xl p-3" style={{ background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.08)' }}>
            <div className="text-white/40 text-xs mb-1">{m.label}</div>
            <div className="text-white text-xl font-bold">{m.value}</div>
            <div className="text-xs font-medium mt-1" style={{ color: m.color }}>{m.delta} this week</div>
          </div>
        ))}
      </div>

      <div className="rounded-xl p-3" style={{ background: 'rgba(26,26,255,0.1)', border: '1px solid rgba(26,26,255,0.2)' }}>
        <div className="text-white/50 text-xs mb-2">LEAD PIPELINE</div>
        <div className="space-y-2">
          {[
            { name: 'Sarah M. — Restaurant', stage: 'Booked', pct: 90 },
            { name: 'John K. — Contractor', stage: 'Qualified', pct: 60 },
            { name: 'Emma R. — Salon', stage: 'Nurture', pct: 35 },
          ].map(l => (
            <div key={l.name} className="flex items-center gap-3">
              <div className="text-white/70 text-xs w-36 truncate">{l.name}</div>
              <div className="flex-1 h-1.5 rounded-full bg-white/10">
                <div className="h-full rounded-full" style={{ width: `${l.pct}%`, background: '#1A1AFF' }} />
              </div>
              <div className="text-white/40 text-xs w-16 text-right">{l.stage}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

// ─── Social Proof Bar ────────────────────────────────────────────────────────

function SocialProofBar() {
  return (
    <section className="py-8 border-y" style={{ borderColor: 'rgba(26,26,255,0.2)', background: 'rgba(26,26,255,0.05)' }}>
      <div className="max-w-6xl mx-auto px-6">
        <p className="text-center text-white/40 text-xs uppercase tracking-widest mb-6 font-medium">Trusted by growing businesses across industries</p>
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4 items-center">
          {[
            'Restaurant Groups', 'Real Estate Teams', 'Health & Wellness', 'Trades & Contractors', 'E-commerce Brands'
          ].map(name => (
            <div key={name} className="text-center text-white/30 text-sm font-semibold tracking-wide hover:text-white/60 transition-colors">{name}</div>
          ))}
        </div>
      </div>
    </section>
  )
}

// ─── Services ────────────────────────────────────────────────────────────────

const services = [
  {
    icon: '📱',
    title: 'Social Media That Sells',
    tag: 'Growth',
    description: 'We don\'t just post content — we build audiences that become buyers. Strategic content calendars, engaging copy, and platform algorithms working for you.',
    bullets: ['Content creation & scheduling', 'Audience growth strategy', 'Engagement & community management', 'Ad campaign management'],
    color: '#1A1AFF',
  },
  {
    icon: '🎯',
    title: 'Lead Generation & Booking',
    tag: 'Revenue',
    description: 'Your funnel is either leaking money or making it. We build airtight systems that capture, qualify, and convert strangers into paying clients — automatically.',
    bullets: ['Custom lead capture funnels', 'Automated booking systems', 'CRM setup & integration', 'Lead nurture sequences'],
    color: '#22c55e',
    featured: true,
  },
  {
    icon: '🌐',
    title: 'Web Design & Development',
    tag: 'Presence',
    description: 'Your website is your 24/7 salesperson. We build fast, beautiful sites engineered to convert visitors into leads — not just look pretty.',
    bullets: ['Conversion-optimised design', 'Mobile-first development', 'Landing pages & sales pages', 'SEO foundation'],
    color: '#a855f7',
  },
  {
    icon: '⚡',
    title: 'Business Automation',
    tag: 'Efficiency',
    description: 'Stop doing manually what a machine can do better. We map your workflow and automate the repetitive tasks that drain your time and team.',
    bullets: ['Workflow automation (Zapier/Make)', 'Email & SMS sequences', 'Invoice & proposal automation', 'Reporting dashboards'],
    color: '#f59e0b',
  },
]

function Services() {
  return (
    <section id="services" className="py-24 px-6">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-16">
          <div className="inline-block px-4 py-1.5 rounded-full text-xs font-semibold uppercase tracking-widest mb-4"
            style={{ background: 'rgba(26,26,255,0.15)', color: '#a0a0ff', border: '1px solid rgba(26,26,255,0.3)' }}>
            What We Do
          </div>
          <h2 className="text-3xl md:text-5xl font-black text-white mb-4">
            Everything Your Business Needs to<br />
            <span className="gradient-text">Dominate Online</span>
          </h2>
          <p className="text-white/50 text-lg max-w-2xl mx-auto">
            Most agencies sell you one thing. We build the entire digital ecosystem that compounds your growth month after month.
          </p>
        </div>

        <div className="grid md:grid-cols-2 gap-6">
          {services.map(s => (
            <div
              key={s.title}
              className={`card-hover rounded-2xl p-8 relative overflow-hidden ${s.featured ? 'glow-pulse' : ''}`}
              style={{
                background: s.featured
                  ? `linear-gradient(135deg, rgba(26,26,255,0.2) 0%, rgba(8,8,26,0.9) 100%)`
                  : 'rgba(255,255,255,0.03)',
                border: `1px solid ${s.featured ? 'rgba(26,26,255,0.5)' : 'rgba(255,255,255,0.08)'}`,
              }}
            >
              {s.featured && (
                <div className="absolute top-4 right-4 px-3 py-1 rounded-full text-xs font-bold"
                  style={{ background: '#1A1AFF', color: 'white' }}>
                  Most Popular
                </div>
              )}

              <div className="flex items-start gap-4 mb-5">
                <div className="service-icon-bg w-12 h-12 rounded-xl flex items-center justify-center text-2xl flex-shrink-0">
                  {s.icon}
                </div>
                <div>
                  <span className="text-xs font-bold uppercase tracking-widest" style={{ color: s.color }}>{s.tag}</span>
                  <h3 className="text-xl font-bold text-white mt-0.5">{s.title}</h3>
                </div>
              </div>

              <p className="text-white/60 leading-relaxed mb-6 text-sm">{s.description}</p>

              <ul className="space-y-2">
                {s.bullets.map(b => (
                  <li key={b} className="flex items-center gap-3 text-sm text-white/70">
                    <span className="w-1.5 h-1.5 rounded-full flex-shrink-0" style={{ background: s.color }} />
                    {b}
                  </li>
                ))}
              </ul>

              <a href="#qualify" className="inline-flex items-center gap-2 mt-6 text-sm font-semibold transition-colors hover:gap-3"
                style={{ color: s.color }}>
                Learn more →
              </a>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

// ─── Process ─────────────────────────────────────────────────────────────────

function Process() {
  const steps = [
    { num: '01', title: 'Discovery Call', desc: 'We learn your goals, current situation, and biggest growth blockers in a focused 30-minute strategy session.' },
    { num: '02', title: 'Custom Roadmap', desc: 'We map out exactly what needs to happen to hit your targets — no fluff, no upsells, just a clear action plan.' },
    { num: '03', title: 'Build & Launch', desc: 'Our team executes. Systems go live, content gets published, automations activate. Fast turnaround, no drama.' },
    { num: '04', title: 'Scale & Optimise', desc: 'We track what\'s working, double down on winners, and continuously improve your results every single month.' },
  ]

  return (
    <section className="py-24 px-6" style={{ background: 'rgba(26,26,255,0.04)' }}>
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-16">
          <div className="inline-block px-4 py-1.5 rounded-full text-xs font-semibold uppercase tracking-widest mb-4"
            style={{ background: 'rgba(26,26,255,0.15)', color: '#a0a0ff', border: '1px solid rgba(26,26,255,0.3)' }}>
            How It Works
          </div>
          <h2 className="text-3xl md:text-5xl font-black text-white mb-4">
            From Zero to <span className="gradient-text">Fully Automated</span><br />in 4 Simple Steps
          </h2>
        </div>

        <div className="grid md:grid-cols-4 gap-6">
          {steps.map((s, i) => (
            <div key={s.num} className="relative">
              {i < steps.length - 1 && (
                <div className="hidden md:block absolute top-6 left-full w-full h-0.5 z-0"
                  style={{ background: 'linear-gradient(90deg, rgba(26,26,255,0.5), rgba(26,26,255,0.1))' }} />
              )}
              <div className="relative z-10">
                <div className="w-12 h-12 rounded-xl flex items-center justify-center font-black text-sm mb-4"
                  style={{ background: 'rgba(26,26,255,0.2)', border: '1px solid rgba(26,26,255,0.4)', color: '#a0a0ff' }}>
                  {s.num}
                </div>
                <h3 className="text-lg font-bold text-white mb-2">{s.title}</h3>
                <p className="text-white/50 text-sm leading-relaxed">{s.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

// ─── Portfolio ────────────────────────────────────────────────────────────────

const portfolioItems = [
  {
    title: 'Bella Cucina Restaurant',
    category: 'Social Media + Web Design',
    result: '+340% Instagram reach in 90 days',
    description: 'Full brand refresh, content strategy, and a new booking-integrated website for this upscale Italian restaurant.',
    gradient: 'from-orange-900 to-red-900',
    tag: 'F&B',
    metrics: [{ label: 'Monthly Reach', val: '42K' }, { label: 'Table Bookings', val: '+68%' }, { label: 'Revenue', val: '+$14K/mo' }],
  },
  {
    title: 'Apex Roofing Co.',
    category: 'Lead Generation + Automation',
    result: '47 qualified leads in the first month',
    description: 'Built a complete lead capture funnel with automated follow-up sequences. Roofing business was drowning in unqualified calls — now they pre-screen every lead.',
    gradient: 'from-slate-800 to-slate-700',
    tag: 'Trades',
    metrics: [{ label: 'Qualified Leads', val: '47/mo' }, { label: 'Cost Per Lead', val: '-62%' }, { label: 'Close Rate', val: '+29%' }],
  },
  {
    title: 'GlowUp Aesthetics Clinic',
    category: 'Booking System + Social Media',
    result: 'Fully booked 3 weeks in advance',
    description: 'Automated booking system integrated with Instagram lead ads. Staff now spend zero time on scheduling — the system fills the calendar for them.',
    gradient: 'from-pink-900 to-purple-900',
    tag: 'Health',
    metrics: [{ label: 'Calendar Fill Rate', val: '94%' }, { label: 'No-Shows', val: '-71%' }, { label: 'New Clients', val: '+83/mo' }],
  },
  {
    title: 'Vantage Real Estate Group',
    category: 'Web Design + Lead Generation',
    result: 'New site converts 3x better than old one',
    description: 'Complete website rebuild with an IDX integration and automated lead qualification. Agents now receive pre-qualified buyer and seller leads daily.',
    gradient: 'from-blue-900 to-indigo-900',
    tag: 'Real Estate',
    metrics: [{ label: 'Conversion Rate', val: '8.4%' }, { label: 'Monthly Leads', val: '112' }, { label: 'Cost Per Lead', val: '$18' }],
  },
  {
    title: 'FitCore Gym & Studio',
    category: 'Full Digital Package',
    result: '200 new member sign-ups in 60 days',
    description: 'Complete digital presence — social media, email automation, referral program, and a membership-optimised website built to convert trial visitors.',
    gradient: 'from-green-900 to-teal-900',
    tag: 'Fitness',
    metrics: [{ label: 'New Members', val: '200' }, { label: 'Trial Conversion', val: '61%' }, { label: 'Retention', val: '+22%' }],
  },
  {
    title: 'Horizon Legal Advisory',
    category: 'Web Design + Automation',
    result: 'Intake process fully automated',
    description: 'Designed a trust-building website with an automated client intake and document request system — freeing up 15+ staff hours per week.',
    gradient: 'from-gray-800 to-zinc-900',
    tag: 'Legal',
    metrics: [{ label: 'Staff Hours Saved', val: '15/wk' }, { label: 'Intake Time', val: '-80%' }, { label: 'Consultations', val: '+41%' }],
  },
]

function Portfolio() {
  const [active, setActive] = useState<string | null>(null)

  return (
    <section id="portfolio" className="py-24 px-6">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-16">
          <div className="inline-block px-4 py-1.5 rounded-full text-xs font-semibold uppercase tracking-widest mb-4"
            style={{ background: 'rgba(26,26,255,0.15)', color: '#a0a0ff', border: '1px solid rgba(26,26,255,0.3)' }}>
            Our Work
          </div>
          <h2 className="text-3xl md:text-5xl font-black text-white mb-4">
            Real Businesses. <span className="gradient-text">Real Results.</span>
          </h2>
          <p className="text-white/50 text-lg max-w-2xl mx-auto">
            We don't deal in vanity metrics. Every project is measured against one thing: did it make you more money?
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-6">
          {portfolioItems.map(item => (
            <div
              key={item.title}
              className="card-hover rounded-2xl overflow-hidden cursor-pointer relative group"
              style={{ border: '1px solid rgba(255,255,255,0.08)' }}
              onMouseEnter={() => setActive(item.title)}
              onMouseLeave={() => setActive(null)}
            >
              {/* Visual block */}
              <div className={`h-48 bg-gradient-to-br ${item.gradient} relative overflow-hidden`}>
                <div className="absolute inset-0 portfolio-overlay" />
                <div className="absolute top-4 left-4">
                  <span className="px-3 py-1 rounded-full text-xs font-bold"
                    style={{ background: 'rgba(26,26,255,0.7)', color: 'white', border: '1px solid rgba(26,26,255,0.5)' }}>
                    {item.tag}
                  </span>
                </div>
                <div className="absolute bottom-4 left-4 right-4">
                  <div className="text-white font-black text-xl leading-tight">{item.title}</div>
                  <div className="text-white/60 text-xs mt-1">{item.category}</div>
                </div>
              </div>

              {/* Content */}
              <div className="p-5" style={{ background: 'rgba(255,255,255,0.02)' }}>
                <div className="flex items-center gap-2 mb-3">
                  <span className="w-2 h-2 rounded-full bg-green-400" />
                  <span className="text-green-400 text-sm font-semibold">{item.result}</span>
                </div>
                <p className="text-white/55 text-sm leading-relaxed mb-4">{item.description}</p>

                <div className="grid grid-cols-3 gap-2 pt-4" style={{ borderTop: '1px solid rgba(255,255,255,0.06)' }}>
                  {item.metrics.map(m => (
                    <div key={m.label} className="text-center">
                      <div className="text-white font-black text-base">{m.val}</div>
                      <div className="text-white/40 text-xs">{m.label}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>

        <div className="text-center mt-12">
          <a href="#qualify" className="cta-button inline-flex text-white font-bold px-8 py-4 rounded-xl text-base">
            Start Your Success Story →
          </a>
        </div>
      </div>
    </section>
  )
}

// ─── Testimonials ─────────────────────────────────────────────────────────────

const testimonials = [
  {
    name: 'Marcus T.',
    role: 'Owner, Apex Roofing Co.',
    quote: 'Before DigiFlow I was buying garbage leads from lead aggregators. Now I wake up to pre-qualified prospects who already know our prices and are ready to book. It\'s a completely different business.',
    stars: 5,
    result: '47 qualified leads/month',
    avatar: 'MT',
  },
  {
    name: 'Priya S.',
    role: 'Director, GlowUp Aesthetics',
    quote: 'I was spending 3 hours a day managing the booking calendar. DigiFlow automated the whole thing. Now I\'m fully booked 3 weeks out and I\'ve reclaimed my time. Worth every cent.',
    stars: 5,
    result: 'Calendar at 94% capacity',
    avatar: 'PS',
  },
  {
    name: 'Daniel R.',
    role: 'Managing Partner, Vantage Real Estate',
    quote: 'Our old site was an embarrassment. The new one converts at 8.4% — industry average is under 2%. DigiFlow understands that a website isn\'t a brochure, it\'s a sales tool.',
    stars: 5,
    result: '112 leads/month from website',
    avatar: 'DR',
  },
  {
    name: 'Lauren C.',
    role: 'Head Chef/Owner, Bella Cucina',
    quote: 'They grew our Instagram from 800 followers to over 18,000 in 4 months. But more importantly — people actually come in because of our social media now. It directly drives revenue.',
    stars: 5,
    result: '18K followers → real customers',
    avatar: 'LC',
  },
  {
    name: 'James O.',
    role: 'CEO, FitCore Gym',
    quote: 'Two hundred new members in 60 days. I thought that was an exaggeration until it happened to us. The referral automation alone pays for DigiFlow three times over each month.',
    stars: 5,
    result: '200 new members in 60 days',
    avatar: 'JO',
  },
  {
    name: 'Amara K.',
    role: 'Senior Partner, Horizon Legal',
    quote: 'Our intake used to take 3 staff members and a full day. Now it\'s completely automated — clients submit their documents, sign agreements, and schedule consultations without us lifting a finger.',
    stars: 5,
    result: '15 staff hours saved weekly',
    avatar: 'AK',
  },
]

function Testimonials() {
  return (
    <section id="testimonials" className="py-24 px-6" style={{ background: 'rgba(26,26,255,0.03)' }}>
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-16">
          <div className="inline-block px-4 py-1.5 rounded-full text-xs font-semibold uppercase tracking-widest mb-4"
            style={{ background: 'rgba(26,26,255,0.15)', color: '#a0a0ff', border: '1px solid rgba(26,26,255,0.3)' }}>
            Client Results
          </div>
          <h2 className="text-3xl md:text-5xl font-black text-white mb-4">
            Don't Take Our Word For It
          </h2>
          <p className="text-white/50 text-lg">These are real clients. Real numbers. Real businesses that trusted us and won.</p>
        </div>

        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          {testimonials.map(t => (
            <div key={t.name} className="testimonial-card card-hover rounded-2xl p-6">
              <div className="flex gap-0.5 mb-4">
                {Array(t.stars).fill(0).map((_, i) => (
                  <span key={i} className="text-yellow-400 text-sm">★</span>
                ))}
              </div>

              <blockquote className="text-white/75 text-sm leading-relaxed mb-5 italic">
                "{t.quote}"
              </blockquote>

              <div className="flex items-center gap-3 pt-4" style={{ borderTop: '1px solid rgba(255,255,255,0.07)' }}>
                <div className="w-10 h-10 rounded-full flex items-center justify-center font-bold text-xs flex-shrink-0"
                  style={{ background: 'rgba(26,26,255,0.4)', color: 'white', border: '1px solid rgba(26,26,255,0.5)' }}>
                  {t.avatar}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="font-bold text-white text-sm">{t.name}</div>
                  <div className="text-white/40 text-xs">{t.role}</div>
                </div>
                <div className="text-right flex-shrink-0">
                  <div className="text-xs font-semibold text-green-400">{t.result}</div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

// ─── About / Why Us ───────────────────────────────────────────────────────────

function About() {
  return (
    <section id="about" className="py-24 px-6">
      <div className="max-w-6xl mx-auto grid md:grid-cols-2 gap-16 items-center">
        <div>
          <div className="inline-block px-4 py-1.5 rounded-full text-xs font-semibold uppercase tracking-widest mb-4"
            style={{ background: 'rgba(26,26,255,0.15)', color: '#a0a0ff', border: '1px solid rgba(26,26,255,0.3)' }}>
            Why DigiFlow
          </div>
          <h2 className="text-3xl md:text-4xl font-black text-white mb-6">
            We're Not a Marketing Agency.<br />
            <span className="gradient-text">We're Your Growth Partner.</span>
          </h2>
          <p className="text-white/60 leading-relaxed mb-6">
            Most agencies take your money, send monthly reports full of graphs that look good but mean nothing, and disappear when results don't materialise.
          </p>
          <p className="text-white/60 leading-relaxed mb-8">
            DigiFlow is different. We're obsessive about one metric: revenue. Every system we build, every post we publish, every automation we configure is engineered to move that number.
          </p>

          <div className="space-y-4">
            {[
              { icon: '✓', text: 'Revenue-first thinking — vanity metrics don\'t pay bills' },
              { icon: '✓', text: 'Full-stack digital — strategy through execution' },
              { icon: '✓', text: 'Transparent reporting — you always know exactly what\'s happening' },
              { icon: '✓', text: 'Fast execution — most clients see results within 30 days' },
            ].map(item => (
              <div key={item.text} className="flex items-start gap-3">
                <span className="w-5 h-5 rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5"
                  style={{ background: '#1A1AFF', color: 'white' }}>
                  {item.icon}
                </span>
                <span className="text-white/70 text-sm">{item.text}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          {[
            { num: '50+', label: 'Clients Scaled', sub: 'Across 12 industries' },
            { num: '3.2x', label: 'Avg Lead Growth', sub: 'Within first 90 days' },
            { num: '$2.4M+', label: 'Revenue Generated', sub: 'For our clients in 2025' },
            { num: '98%', label: 'Retention Rate', sub: 'Clients stay because it works' },
          ].map(s => (
            <div key={s.label} className="rounded-2xl p-6 text-center card-hover"
              style={{ background: 'rgba(26,26,255,0.08)', border: '1px solid rgba(26,26,255,0.2)' }}>
              <div className="text-3xl font-black text-white mb-1">{s.num}</div>
              <div className="text-white font-semibold text-sm mb-1">{s.label}</div>
              <div className="text-white/40 text-xs">{s.sub}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

// ─── Lead Qualification Form ──────────────────────────────────────────────────

const QUALIFY_FORM_NAME = 'lead-qualification'

type FormStep = 1 | 2 | 3

interface FormData {
  name: string
  email: string
  phone: string
  businessName: string
  industry: string
  monthlyRevenue: string
  biggestChallenge: string
  servicesInterested: string[]
  timeline: string
  budget: string
  message: string
}

function LeadForm() {
  const [step, setStep] = useState<FormStep>(1)
  const [submitted, setSubmitted] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [data, setData] = useState<FormData>({
    name: '', email: '', phone: '', businessName: '', industry: '',
    monthlyRevenue: '', biggestChallenge: '', servicesInterested: [],
    timeline: '', budget: '', message: '',
  })

  const set = (field: keyof FormData) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
    setData(d => ({ ...d, [field]: e.target.value }))
  }

  const toggleService = (s: string) => {
    setData(d => ({
      ...d,
      servicesInterested: d.servicesInterested.includes(s)
        ? d.servicesInterested.filter(x => x !== s)
        : [...d.servicesInterested, s],
    }))
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSubmitting(true)
    setError('')

    const payload = {
      'form-name': QUALIFY_FORM_NAME,
      ...data,
      servicesInterested: data.servicesInterested.join(', '),
    }

    try {
      const res = await fetch('/__forms.html', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams(payload as Record<string, string>).toString(),
      })

      if (res.ok) {
        setSubmitted(true)
      } else {
        setError('Something went wrong. Please try again or email us directly.')
      }
    } catch {
      setError('Network error. Please check your connection and try again.')
    } finally {
      setSubmitting(false)
    }
  }

  const inputClass = "w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-white placeholder-white/30 text-sm focus:outline-none focus:border-blue-500 transition-colors"
  const selectClass = `${inputClass} appearance-none`
  const labelClass = "block text-sm font-medium text-white/70 mb-2"

  const step1Valid = data.name && data.email && data.businessName && data.industry
  const step2Valid = data.monthlyRevenue && data.biggestChallenge && data.servicesInterested.length > 0

  return (
    <section id="qualify" className="py-24 px-6">
      <div className="max-w-2xl mx-auto">
        <div className="text-center mb-12">
          <div className="inline-block px-4 py-1.5 rounded-full text-xs font-semibold uppercase tracking-widest mb-4"
            style={{ background: 'rgba(26,26,255,0.15)', color: '#a0a0ff', border: '1px solid rgba(26,26,255,0.3)' }}>
            Free Strategy Call
          </div>
          <h2 className="text-3xl md:text-5xl font-black text-white mb-4">
            Let's Figure Out Exactly<br />
            <span className="gradient-text">How to Grow Your Business</span>
          </h2>
          <p className="text-white/50">
            Answer a few quick questions so we can prepare for your free 30-minute strategy session. We only accept clients we can genuinely help — this form helps us both save time.
          </p>
        </div>

        {submitted ? (
          <div className="rounded-2xl p-10 text-center blue-glow"
            style={{ background: 'linear-gradient(135deg, rgba(26,26,255,0.15), rgba(8,8,26,0.9))', border: '1px solid rgba(26,26,255,0.4)' }}>
            <div className="text-5xl mb-4">🎉</div>
            <h3 className="text-2xl font-black text-white mb-3">You're on the list!</h3>
            <p className="text-white/60 mb-6">
              We've received your application. Our team reviews every submission personally and will reach out within 24 hours to schedule your strategy call.
            </p>
            <div className="inline-block px-6 py-3 rounded-xl text-sm font-semibold"
              style={{ background: 'rgba(26,26,255,0.2)', border: '1px solid rgba(26,26,255,0.4)', color: '#a0a0ff' }}>
              Check your inbox — we'll be in touch soon
            </div>
          </div>
        ) : (
          <div className="rounded-2xl p-8 md:p-10"
            style={{ background: 'rgba(255,255,255,0.03)', border: '1px solid rgba(255,255,255,0.08)' }}>

            {/* Progress bar */}
            <div className="flex items-center gap-3 mb-8">
              {([1, 2, 3] as FormStep[]).map(s => (
                <div key={s} className="flex items-center gap-3 flex-1">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold flex-shrink-0 transition-all ${step >= s ? 'text-white' : 'text-white/30'}`}
                    style={{ background: step >= s ? '#1A1AFF' : 'rgba(255,255,255,0.05)', border: `1px solid ${step >= s ? '#1A1AFF' : 'rgba(255,255,255,0.1)'}` }}>
                    {step > s ? '✓' : s}
                  </div>
                  {s < 3 && <div className="flex-1 h-0.5 rounded-full" style={{ background: step > s ? '#1A1AFF' : 'rgba(255,255,255,0.08)' }} />}
                </div>
              ))}
            </div>

            <form name={QUALIFY_FORM_NAME} method="POST" data-netlify="true" netlify-honeypot="bot-field" onSubmit={handleSubmit}>
              <input type="hidden" name="form-name" value={QUALIFY_FORM_NAME} />
              <p className="hidden"><input name="bot-field" /></p>

              {/* Step 1: Contact Info */}
              {step === 1 && (
                <div className="space-y-5">
                  <div>
                    <h3 className="text-xl font-bold text-white mb-1">Tell us about yourself</h3>
                    <p className="text-white/40 text-sm">Basic info so we can put a name to the business</p>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className={labelClass}>Full Name *</label>
                      <input type="text" name="name" value={data.name} onChange={set('name')} placeholder="John Smith" required className={inputClass} />
                    </div>
                    <div>
                      <label className={labelClass}>Business Name *</label>
                      <input type="text" name="businessName" value={data.businessName} onChange={set('businessName')} placeholder="Acme Inc." required className={inputClass} />
                    </div>
                  </div>

                  <div>
                    <label className={labelClass}>Email Address *</label>
                    <input type="email" name="email" value={data.email} onChange={set('email')} placeholder="john@acmeinc.com" required className={inputClass} />
                  </div>

                  <div>
                    <label className={labelClass}>Phone Number</label>
                    <input type="tel" name="phone" value={data.phone} onChange={set('phone')} placeholder="+1 (555) 000-0000" className={inputClass} />
                  </div>

                  <div>
                    <label className={labelClass}>Industry *</label>
                    <select name="industry" value={data.industry} onChange={set('industry')} required className={selectClass}>
                      <option value="">Select your industry</option>
                      {['Restaurant / F&B', 'Real Estate', 'Health & Wellness / Aesthetics', 'Trades & Construction', 'Retail / E-commerce', 'Legal / Professional Services', 'Fitness / Sports', 'Finance / Insurance', 'Other'].map(i => (
                        <option key={i} value={i}>{i}</option>
                      ))}
                    </select>
                  </div>

                  <button type="button" onClick={() => step1Valid && setStep(2)}
                    className={`w-full cta-button text-white font-bold py-4 rounded-xl text-base transition-opacity ${!step1Valid ? 'opacity-40 cursor-not-allowed' : ''}`}
                    disabled={!step1Valid}>
                    Next: Your Business Situation →
                  </button>
                </div>
              )}

              {/* Step 2: Qualify */}
              {step === 2 && (
                <div className="space-y-5">
                  <div>
                    <h3 className="text-xl font-bold text-white mb-1">Your business situation</h3>
                    <p className="text-white/40 text-sm">This helps us prepare specific recommendations for your call</p>
                  </div>

                  <div>
                    <label className={labelClass}>Current Monthly Revenue *</label>
                    <select name="monthlyRevenue" value={data.monthlyRevenue} onChange={set('monthlyRevenue')} required className={selectClass}>
                      <option value="">Select range</option>
                      {['Under $5K/month', '$5K–$20K/month', '$20K–$50K/month', '$50K–$100K/month', '$100K+/month'].map(r => (
                        <option key={r} value={r}>{r}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className={labelClass}>Biggest Growth Challenge *</label>
                    <select name="biggestChallenge" value={data.biggestChallenge} onChange={set('biggestChallenge')} required className={selectClass}>
                      <option value="">What's holding you back?</option>
                      {[
                        'Not enough leads coming in',
                        'Leads are low quality / not converting',
                        'No consistent online presence',
                        'Spending too much time on admin',
                        'Can\'t scale without more staff',
                        'Not sure what\'s working and what\'s not',
                        'Competing against cheaper competitors',
                      ].map(c => <option key={c} value={c}>{c}</option>)}
                    </select>
                  </div>

                  <div>
                    <label className={labelClass}>Services You're Interested In * <span className="text-white/30">(select all that apply)</span></label>
                    <div className="grid grid-cols-2 gap-2">
                      {['Social Media', 'Lead Generation', 'Web Design', 'Automation', 'Booking Systems', 'Full Package'].map(s => (
                        <button
                          type="button"
                          key={s}
                          onClick={() => toggleService(s)}
                          className={`py-2.5 px-4 rounded-xl text-sm font-medium transition-all text-left ${data.servicesInterested.includes(s) ? 'text-white' : 'text-white/50 hover:text-white/80'}`}
                          style={{
                            background: data.servicesInterested.includes(s) ? 'rgba(26,26,255,0.3)' : 'rgba(255,255,255,0.04)',
                            border: `1px solid ${data.servicesInterested.includes(s) ? 'rgba(26,26,255,0.6)' : 'rgba(255,255,255,0.08)'}`,
                          }}
                        >
                          {data.servicesInterested.includes(s) ? '✓ ' : ''}{s}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="flex gap-3">
                    <button type="button" onClick={() => setStep(1)}
                      className="flex-1 text-white/60 hover:text-white font-semibold py-4 rounded-xl text-base border border-white/10 hover:border-white/20 transition-all">
                      ← Back
                    </button>
                    <button type="button" onClick={() => step2Valid && setStep(3)}
                      className={`flex-[2] cta-button text-white font-bold py-4 rounded-xl text-base transition-opacity ${!step2Valid ? 'opacity-40 cursor-not-allowed' : ''}`}
                      disabled={!step2Valid}>
                      Almost done →
                    </button>
                  </div>
                </div>
              )}

              {/* Step 3: Final */}
              {step === 3 && (
                <div className="space-y-5">
                  <div>
                    <h3 className="text-xl font-bold text-white mb-1">Final details</h3>
                    <p className="text-white/40 text-sm">Last few questions — we promise it's worth it</p>
                  </div>

                  <div>
                    <label className={labelClass}>When are you looking to get started?</label>
                    <select name="timeline" value={data.timeline} onChange={set('timeline')} className={selectClass}>
                      <option value="">Select timeline</option>
                      {['ASAP — I need this now', 'Within the next 30 days', 'Next 1–3 months', 'Just exploring for now'].map(t => (
                        <option key={t} value={t}>{t}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className={labelClass}>Monthly Marketing Budget</label>
                    <select name="budget" value={data.budget} onChange={set('budget')} className={selectClass}>
                      <option value="">Select budget range</option>
                      {['Under $500/month', '$500–$1,500/month', '$1,500–$3,000/month', '$3,000–$5,000/month', '$5,000+/month'].map(b => (
                        <option key={b} value={b}>{b}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className={labelClass}>Anything else we should know?</label>
                    <textarea name="message" value={data.message} onChange={set('message')} rows={3}
                      placeholder="Tell us about your goals, current situation, or any questions you have..."
                      className={`${inputClass} resize-none`} />
                  </div>

                  {error && (
                    <div className="p-3 rounded-xl text-sm text-red-300" style={{ background: 'rgba(239,68,68,0.1)', border: '1px solid rgba(239,68,68,0.3)' }}>
                      {error}
                    </div>
                  )}

                  <div className="flex gap-3">
                    <button type="button" onClick={() => setStep(2)}
                      className="flex-1 text-white/60 hover:text-white font-semibold py-4 rounded-xl text-base border border-white/10 hover:border-white/20 transition-all">
                      ← Back
                    </button>
                    <button type="submit" disabled={submitting}
                      className="flex-[2] cta-button text-white font-bold py-4 rounded-xl text-base disabled:opacity-60">
                      {submitting ? 'Submitting...' : 'Book My Free Strategy Call →'}
                    </button>
                  </div>

                  <p className="text-white/30 text-xs text-center">
                    No spam. No pushy sales calls. Just a genuine conversation about your business.
                  </p>
                </div>
              )}
            </form>
          </div>
        )}
      </div>
    </section>
  )
}

// ─── FAQ ──────────────────────────────────────────────────────────────────────

function FAQ() {
  const [open, setOpen] = useState<number | null>(null)
  const faqs = [
    {
      q: 'How quickly can we expect to see results?',
      a: 'Most clients see measurable improvement within the first 30 days. Lead generation and automation systems often show results within the first week. Social media growth typically takes 60–90 days to compound significantly.'
    },
    {
      q: 'What makes DigiFlow different from other agencies?',
      a: 'We measure everything in revenue, not impressions. We take on fewer clients than most agencies specifically so we can dedicate real attention to each one. And we build systems that keep working even if you ever stop working with us — because we want referrals, not dependency.'
    },
    {
      q: 'What size businesses do you work with?',
      a: 'We work best with businesses doing $5K–$100K/month who are serious about scaling. We\'re not the right fit for startups with no revenue yet, or enterprise companies with huge in-house teams. We thrive in the middle.'
    },
    {
      q: 'Do I need to sign a long-term contract?',
      a: 'We offer both project-based work and monthly retainers. For ongoing services like social media or lead generation, most clients stay on a month-to-month retainer after an initial 3-month engagement. We earn your continued business every single month.'
    },
    {
      q: 'What\'s included in the free strategy call?',
      a: 'A genuine 30-minute deep dive into your business — your goals, current digital situation, and the specific opportunities we can identify. You\'ll leave with actionable insights regardless of whether you hire us. No sales pressure, no scripts.'
    },
  ]

  return (
    <section className="py-24 px-6" style={{ background: 'rgba(26,26,255,0.03)' }}>
      <div className="max-w-3xl mx-auto">
        <div className="text-center mb-12">
          <h2 className="text-3xl md:text-4xl font-black text-white mb-3">Frequently Asked Questions</h2>
          <p className="text-white/50">Everything you want to know before booking a call</p>
        </div>

        <div className="space-y-3">
          {faqs.map((faq, i) => (
            <div key={i} className="rounded-2xl overflow-hidden" style={{ border: '1px solid rgba(255,255,255,0.08)' }}>
              <button
                onClick={() => setOpen(open === i ? null : i)}
                className="w-full flex items-center justify-between p-5 text-left hover:bg-white/3 transition-colors"
              >
                <span className="font-semibold text-white text-sm pr-8">{faq.q}</span>
                <span className={`text-white/40 text-xl flex-shrink-0 transition-transform ${open === i ? 'rotate-45' : ''}`}>+</span>
              </button>
              {open === i && (
                <div className="px-5 pb-5">
                  <div className="section-divider mb-4" />
                  <p className="text-white/60 text-sm leading-relaxed">{faq.a}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

// ─── Final CTA ────────────────────────────────────────────────────────────────

function FinalCTA() {
  return (
    <section className="py-24 px-6">
      <div className="max-w-4xl mx-auto text-center">
        <div className="rounded-3xl p-12 relative overflow-hidden"
          style={{ background: 'linear-gradient(135deg, rgba(26,26,255,0.25) 0%, rgba(8,8,26,0.9) 100%)', border: '1px solid rgba(26,26,255,0.4)' }}>
          <div className="absolute inset-0 opacity-20"
            style={{ background: 'radial-gradient(circle at 50% 0%, #1A1AFF, transparent 70%)' }} />

          <div className="relative">
            <div className="text-4xl mb-4">⚡</div>
            <h2 className="text-3xl md:text-5xl font-black text-white mb-4">
              Your competitors aren't waiting.<br />
              <span className="gradient-text">Neither should you.</span>
            </h2>
            <p className="text-white/60 text-lg mb-8 max-w-xl mx-auto">
              Every day without a working lead system is a day your competition is winning clients that should be yours. Let's fix that — starting this week.
            </p>

            <a href="#qualify" className="cta-button inline-flex text-white font-black px-10 py-5 rounded-xl text-lg">
              Get My Free Strategy Call →
            </a>

            <p className="text-white/30 text-sm mt-4">30 minutes. Free. No obligation. Just real insights for your business.</p>
          </div>
        </div>
      </div>
    </section>
  )
}

// ─── Footer ───────────────────────────────────────────────────────────────────

function Footer() {
  return (
    <footer className="py-12 px-6 border-t" style={{ borderColor: 'rgba(255,255,255,0.06)' }}>
      <div className="max-w-6xl mx-auto">
        <div className="grid md:grid-cols-4 gap-10 mb-10">
          <div className="md:col-span-2">
            <img src="/digiflow-logo.jpeg" alt="DigiFlow Agency" className="h-12 w-auto rounded-lg mb-4" />
            <p className="text-white/50 text-sm leading-relaxed max-w-sm">
              We build the digital systems ambitious businesses need to attract, qualify, and convert leads — automatically.
            </p>
          </div>

          <div>
            <h4 className="text-white font-bold text-sm mb-4 uppercase tracking-wider">Services</h4>
            <ul className="space-y-2">
              {['Social Media', 'Lead Generation', 'Web Design', 'Automation', 'Booking Systems'].map(s => (
                <li key={s}><a href="#services" className="text-white/50 hover:text-white text-sm transition-colors">{s}</a></li>
              ))}
            </ul>
          </div>

          <div>
            <h4 className="text-white font-bold text-sm mb-4 uppercase tracking-wider">Company</h4>
            <ul className="space-y-2">
              {[
                { label: 'Our Work', href: '#portfolio' },
                { label: 'Client Results', href: '#testimonials' },
                { label: 'About Us', href: '#about' },
                { label: 'Free Strategy Call', href: '#qualify' },
              ].map(l => (
                <li key={l.label}><a href={l.href} className="text-white/50 hover:text-white text-sm transition-colors">{l.label}</a></li>
              ))}
            </ul>
          </div>
        </div>

        <div className="section-divider mb-6" />

        <div className="flex flex-col md:flex-row items-center justify-between gap-4">
          <p className="text-white/30 text-xs">© 2025 DigiFlow Agency. All rights reserved.</p>
          <p className="text-white/30 text-xs">Built to convert. Engineered to scale.</p>
        </div>
      </div>
    </footer>
  )
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function DigiFlowHome() {
  return (
    <div className="min-h-screen" style={{ background: '#08081A' }}>
      <Nav />
      <Hero />
      <SocialProofBar />
      <Services />
      <Process />
      <Portfolio />
      <Testimonials />
      <About />
      <LeadForm />
      <FAQ />
      <FinalCTA />
      <Footer />
    </div>
  )
}
