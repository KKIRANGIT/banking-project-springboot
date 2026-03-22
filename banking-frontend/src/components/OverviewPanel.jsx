import { accountMask, formatCurrency } from "../lib/format";

function StatusPill({ label, tone }) {
  const tones = {
    good: "bg-emerald-50 text-emerald-800",
    warn: "bg-amber-50 text-amber-800",
    neutral: "bg-slate-100 text-slate-700",
  };

  return <span className={`badge ${tones[tone] ?? tones.neutral}`}>{label}</span>;
}

export default function OverviewPanel({ account, transactions, health, session }) {
  const transactionVolume = transactions.reduce((sum, item) => sum + Number(item.amount), 0);

  return (
    <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <div className="panel p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Selected account</p>
        <p className="mt-3 font-display text-3xl text-ink">{accountMask(account?.accountNumber)}</p>
        <p className="mt-2 text-sm text-slate-600">{account?.accountHolderName ?? "Load an account to continue."}</p>
      </div>
      <div className="panel p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Live balance</p>
        <p className="mt-3 font-display text-3xl text-ink">{formatCurrency(account?.balance)}</p>
        <div className="mt-3">
          <StatusPill
            label={account?.status ?? "UNKNOWN"}
            tone={account?.status === "ACTIVE" ? "good" : "warn"}
          />
        </div>
      </div>
      <div className="panel p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Activity count</p>
        <p className="mt-3 font-display text-3xl text-ink">{transactions.length}</p>
        <p className="mt-2 text-sm text-slate-600">Visible transactions for the active account.</p>
      </div>
      <div className="panel p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Gateway posture</p>
        <p className="mt-3 font-display text-3xl text-ink">{health?.status ?? "..."}</p>
        <p className="mt-2 text-sm text-slate-600">
          {session ? `${session.role} session active` : "Authenticate to start using the control deck."}
        </p>
        <p className="mt-3 text-xs uppercase tracking-[0.18em] text-slate-500">
          Visible volume {formatCurrency(transactionVolume)}
        </p>
      </div>
    </section>
  );
}
