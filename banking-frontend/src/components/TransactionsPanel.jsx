import { formatCurrency, formatDateTime } from "../lib/format";

export default function TransactionsPanel({ transactions, activeAccountNumber, onRefresh, token }) {
  return (
    <section className="panel p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Ledger slice</p>
          <h2 className="mt-2 font-display text-2xl text-ink">Recent transactions</h2>
          <p className="mt-2 text-sm leading-7 text-slate-600">
            A focused view of the active account’s transaction stream from the payment service.
          </p>
        </div>
        <button
          className="button-secondary"
          type="button"
          disabled={!token || !activeAccountNumber}
          onClick={() => onRefresh(activeAccountNumber)}
        >
          Refresh list
        </button>
      </div>

      <div className="mt-6 overflow-hidden rounded-[1.75rem] border border-slate-200">
        <div className="hidden grid-cols-[0.8fr_1fr_0.8fr_0.8fr_1fr] gap-4 bg-slate-100/80 px-5 py-4 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500 md:grid">
          <span>ID</span>
          <span>Type</span>
          <span>Amount</span>
          <span>Status</span>
          <span>Created</span>
        </div>
        <div className="bg-white">
          {transactions.length ? (
            transactions.map((item) => (
              <div
                className="grid gap-3 border-t border-slate-100 px-5 py-4 text-sm md:grid-cols-[0.8fr_1fr_0.8fr_0.8fr_1fr] md:items-center"
                key={item.id}
              >
                <span className="font-semibold text-slate-900">#{item.id}</span>
                <span className="text-slate-700">{item.type}</span>
                <span className="text-slate-700">{formatCurrency(item.amount)}</span>
                <span className="badge bg-slate-100 text-slate-700">{item.status}</span>
                <span className="text-slate-500">{formatDateTime(item.createdAt)}</span>
              </div>
            ))
          ) : (
            <div className="px-5 py-12 text-center text-sm text-slate-500">
              Load an account and start posting transactions to populate this ledger.
            </div>
          )}
        </div>
      </div>
    </section>
  );
}
