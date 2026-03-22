export default function HeroPanel({ session, health, lastRefresh }) {
  const serviceCount =
    health?.components?.discoveryComposite?.components?.discoveryClient?.details?.services?.length ?? 0;

  return (
    <aside className="panel-dark relative overflow-hidden p-7 lg:p-8">
      <div className="absolute inset-x-0 top-0 h-32 bg-gradient-to-r from-ember/45 via-gold/25 to-lake/30 blur-3xl" />
      <div className="relative space-y-8">
        <div className="space-y-4">
          <span className="badge bg-white/10 text-white/80">React Control Deck</span>
          <div className="space-y-3">
            <h1 className="max-w-md font-display text-4xl leading-tight text-white md:text-5xl">
              Operate accounts, payments, and runtime health from one front desk.
            </h1>
            <p className="max-w-lg text-sm leading-7 text-white/74">
              This frontend is shaped around the gateway and microservice workflow already running in the repository:
              authenticate, inspect an account, post a transaction, and keep an eye on system readiness.
            </p>
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-3 lg:grid-cols-1 xl:grid-cols-3">
          <div className="rounded-3xl border border-white/10 bg-white/5 p-4">
            <p className="text-xs uppercase tracking-[0.18em] text-white/55">Gateway</p>
            <p className="mt-3 font-display text-3xl text-white">{health?.status ?? "Checking"}</p>
          </div>
          <div className="rounded-3xl border border-white/10 bg-white/5 p-4">
            <p className="text-xs uppercase tracking-[0.18em] text-white/55">Registered Services</p>
            <p className="mt-3 font-display text-3xl text-white">{serviceCount}</p>
          </div>
          <div className="rounded-3xl border border-white/10 bg-white/5 p-4">
            <p className="text-xs uppercase tracking-[0.18em] text-white/55">Operator</p>
            <p className="mt-3 text-lg font-semibold text-white">
              {session?.username ?? "Anonymous"}
            </p>
            <p className="text-sm text-white/60">{session?.role ?? "Sign in required"}</p>
          </div>
        </div>

        <div className="rounded-[1.75rem] border border-white/10 bg-white/5 p-5">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-xs uppercase tracking-[0.18em] text-white/55">System posture</p>
              <p className="mt-2 text-sm leading-7 text-white/74">
                Gateway health is polled from `/actuator/health`, while account and payment activity are driven through
                the routed API surface only.
              </p>
            </div>
            <div className="h-14 w-14 rounded-full bg-gradient-to-br from-gold to-ember" />
          </div>
          <div className="mt-5 flex flex-wrap gap-3 text-xs text-white/64">
            <span>Gateway port 8080</span>
            <span>Auth route enabled</span>
            <span>Last refresh {lastRefresh}</span>
          </div>
        </div>
      </div>
    </aside>
  );
}
