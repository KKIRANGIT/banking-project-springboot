export default function StatusBanner({ toast }) {
  if (!toast) {
    return null;
  }

  const styles = {
    success: "border-emerald-200 bg-emerald-50 text-emerald-900",
    error: "border-rose-200 bg-rose-50 text-rose-900",
    info: "border-sky-200 bg-sky-50 text-sky-900",
  };

  return (
    <div className={`rounded-3xl border px-5 py-4 text-sm ${styles[toast.type] ?? styles.info}`}>
      <div className="font-medium">{toast.title}</div>
      <div className="mt-1 opacity-80">{toast.message}</div>
    </div>
  );
}
