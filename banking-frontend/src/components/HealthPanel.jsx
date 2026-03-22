export default function HealthPanel({ health }) {
  const components = Object.entries(health?.components ?? {});

  return (
    <section className="panel p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Observability</p>
          <h2 className="mt-2 font-display text-2xl text-ink">Gateway health snapshot</h2>
          <p className="mt-2 text-sm leading-7 text-slate-600">
            Live gateway actuator data, useful for checking the front door before posting account or payment calls.
          </p>
        </div>
        <span className="badge bg-slate-900 text-white">{health?.status ?? "UNKNOWN"}</span>
      </div>

      <div className="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {components.length ? (
          components.map(([name, value]) => (
            <div className="rounded-[1.5rem] border border-slate-200 bg-slate-50/70 p-4" key={name}>
              <div className="text-xs uppercase tracking-[0.18em] text-slate-500">{name}</div>
              <div className="mt-3 text-lg font-semibold text-slate-900">{value?.status ?? "UNKNOWN"}</div>
              <div className="mt-2 text-sm text-slate-600">
                {value?.description ?? value?.details?.services?.join(", ") ?? "No extra details"}
              </div>
            </div>
          ))
        ) : (
          <div className="rounded-[1.5rem] border border-dashed border-slate-300 px-4 py-10 text-sm text-slate-500">
            Health details are still loading.
          </div>
        )}
      </div>
    </section>
  );
}
