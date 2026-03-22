import { useMemo, useState } from "react";
import { formatCurrency } from "../lib/format";

export default function TransactionPanel({ token, account, onSubmit, health }) {
  const [form, setForm] = useState({
    amount: "100.00",
    type: "DEBIT",
  });

  const readiness = useMemo(() => {
    if (!token) {
      return "Authenticate first";
    }
    if (!account?.accountNumber) {
      return "Load an account";
    }
    return "Ready";
  }, [account?.accountNumber, token]);

  return (
    <section className="panel p-6">
      <div className="grid gap-5 xl:grid-cols-[1fr_0.95fr]">
        <form
          className="rounded-[1.75rem] border border-slate-200 bg-white p-5"
          onSubmit={(event) => {
            event.preventDefault();
            onSubmit({
              accountNumber: account.accountNumber,
              amount: Number(form.amount),
              type: form.type,
            });
          }}
        >
          <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Movement</p>
          <h2 className="mt-2 font-display text-2xl text-ink">Post a transaction</h2>
          <div className="mt-5 grid gap-3">
            <input className="field bg-slate-50" value={account?.accountNumber ?? ""} disabled />
            <div className="grid gap-3 sm:grid-cols-2">
              <input
                className="field"
                type="number"
                min="0.01"
                step="0.01"
                value={form.amount}
                onChange={(event) => setForm((current) => ({ ...current, amount: event.target.value }))}
              />
              <select
                className="field"
                value={form.type}
                onChange={(event) => setForm((current) => ({ ...current, type: event.target.value }))}
              >
                <option value="DEBIT">DEBIT</option>
                <option value="CREDIT">CREDIT</option>
              </select>
            </div>
            <button
              className="button-primary mt-1"
              disabled={!token || !account?.accountNumber}
              type="submit"
            >
              Submit transaction
            </button>
          </div>
        </form>

        <div className="rounded-[1.75rem] border border-slate-200 bg-slate-50/70 p-5">
          <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Execution rails</p>
          <div className="mt-4 space-y-4">
            <div className="rounded-3xl bg-white p-4">
              <div className="text-sm font-semibold text-slate-900">Session readiness</div>
              <div className="mt-1 text-sm text-slate-600">{readiness}</div>
            </div>
            <div className="rounded-3xl bg-white p-4">
              <div className="text-sm font-semibold text-slate-900">Current balance</div>
              <div className="mt-1 text-sm text-slate-600">{formatCurrency(account?.balance)}</div>
            </div>
            <div className="rounded-3xl bg-ink p-4 text-white">
              <div className="text-sm font-semibold">Gateway health</div>
              <div className="mt-1 text-sm text-white/70">{health?.status ?? "Checking"}</div>
              <div className="mt-3 text-xs uppercase tracking-[0.18em] text-white/55">
                Routed via /api/payments/transactions
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
