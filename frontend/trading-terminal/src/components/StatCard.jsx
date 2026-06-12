const variantClass = {
  positive: 'border-emerald-500/40 bg-emerald-950/30 text-emerald-300',
  negative: 'border-red-500/40 bg-red-950/30 text-red-300',
  warning: 'border-amber-500/40 bg-amber-950/30 text-amber-300',
  neutral: 'border-slate-700 bg-slate-900 text-slate-100'
};

export default function StatCard({ title, value, subtitle, variant = 'neutral' }) {
  return (
    <div className={`rounded border p-3 ${variantClass[variant] || variantClass.neutral}`}>
      <div className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">{title}</div>
      <div className="mt-1 truncate font-mono text-xl font-semibold">{value ?? '--'}</div>
      {subtitle && <div className="mt-1 truncate text-xs text-slate-400">{subtitle}</div>}
    </div>
  );
}
