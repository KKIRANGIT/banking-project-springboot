import { useEffect, useState } from "react";
import { formatCurrency } from "../lib/format";

const blankAccount = {
  accountNumber: "",
  accountHolderName: "",
  balance: "0.00",
  status: "ACTIVE",
};

export default function AccountPanel({ token, activeAccountNumber, onLookup, onCreate, account }) {
  const [lookupValue, setLookupValue] = useState(activeAccountNumber || "");
  const [form, setForm] = useState(blankAccount);

  useEffect(() => {
    setLookupValue(activeAccountNumber || "");
  }, [activeAccountNumber]);

  return (
    <section className="panel p-6">
      <div className="grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">
        <div className="rounded-[1.75rem] border border-slate-200 bg-slate-50/70 p-5">
          <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Account lens</p>
          <h2 className="mt-2 font-display text-2xl text-ink">Lookup and inspect</h2>
          <div className="mt-5 flex flex-col gap-3 md:flex-row">
            <input
              className="field"
              placeholder="Account number"
              value={lookupValue}
              onChange={(event) => setLookupValue(event.target.value)}
            />
            <button
              className="button-primary whitespace-nowrap"
              type="button"
              disabled={!token || !lookupValue.trim()}
              onClick={() => onLookup(lookupValue)}
            >
              Load account
            </button>
          </div>

          <div className="mt-6 rounded-3xl bg-white p-5">
            <div className="flex items-start justify-between gap-4">
              <div>
                <div className="text-sm font-semibold text-slate-900">{account?.accountHolderName ?? "No account loaded"}</div>
                <div className="mt-1 text-sm text-slate-500">{account?.accountNumber ?? "Use the search above."}</div>
              </div>
              <span className="badge bg-slate-100 text-slate-700">{account?.status ?? "UNKNOWN"}</span>
            </div>
            <div className="mt-6 grid gap-4 sm:grid-cols-2">
              <div>
                <div className="text-xs uppercase tracking-[0.18em] text-slate-500">Balance</div>
                <div className="mt-2 text-2xl font-display text-ink">{formatCurrency(account?.balance)}</div>
              </div>
              <div>
                <div className="text-xs uppercase tracking-[0.18em] text-slate-500">Version</div>
                <div className="mt-2 text-2xl font-display text-ink">{account?.version ?? "—"}</div>
              </div>
            </div>
          </div>
        </div>

        <form
          className="rounded-[1.75rem] border border-slate-200 bg-white p-5"
          onSubmit={(event) => {
            event.preventDefault();
            onCreate({
              ...form,
              balance: Number(form.balance),
            }).then(() => setForm(blankAccount));
          }}
        >
          <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Onboard account</p>
          <h2 className="mt-2 font-display text-2xl text-ink">Create a fresh account</h2>
          <div className="mt-5 grid gap-3">
            <input
              className="field"
              placeholder="Account number"
              value={form.accountNumber}
              onChange={(event) => setForm((current) => ({ ...current, accountNumber: event.target.value }))}
            />
            <input
              className="field"
              placeholder="Account holder name"
              value={form.accountHolderName}
              onChange={(event) => setForm((current) => ({ ...current, accountHolderName: event.target.value }))}
            />
            <input
              className="field"
              placeholder="Opening balance"
              type="number"
              min="0"
              step="0.01"
              value={form.balance}
              onChange={(event) => setForm((current) => ({ ...current, balance: event.target.value }))}
            />
            <select
              className="field"
              value={form.status}
              onChange={(event) => setForm((current) => ({ ...current, status: event.target.value }))}
            >
              <option value="ACTIVE">ACTIVE</option>
              <option value="SUSPENDED">SUSPENDED</option>
              <option value="CLOSED">CLOSED</option>
            </select>
            <button className="button-secondary mt-1" disabled={!token} type="submit">
              Create account
            </button>
          </div>
        </form>
      </div>
    </section>
  );
}
