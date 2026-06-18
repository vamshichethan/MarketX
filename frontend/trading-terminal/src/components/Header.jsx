import { Activity, Circle, Server } from 'lucide-react';
import { symbols } from '../utils';

export default function Header({ symbol, onSymbolChange }) {
  return (
    <header className="flex min-h-16 items-center justify-between border-b border-slate-800 bg-slate-950 px-4">
      <div>
        <div className="text-lg font-semibold text-slate-100">MarketX Trading Terminal</div>
        <div className="text-xs uppercase text-slate-500">Institutional Electronic Trading Platform</div>
      </div>

      <div className="flex items-center gap-3">
        <label className="flex items-center gap-2 text-xs text-slate-400">
          Symbol
          <select
            value={symbol}
            onChange={(event) => onSymbolChange(event.target.value)}
            className="rounded border border-slate-700 bg-slate-900 px-2 py-1 font-mono text-slate-100 outline-none focus:border-amber-400"
          >
            {symbols.map((item) => <option key={item}>{item}</option>)}
          </select>
        </label>

        <div className="hidden items-center gap-2 rounded border border-slate-800 bg-slate-900 px-3 py-2 text-xs text-slate-300 md:flex">
          <Server size={14} className="text-amber-300" />
          SIM+API
        </div>
        <div className="hidden items-center gap-2 rounded border border-emerald-900 bg-emerald-950/30 px-3 py-2 text-xs text-emerald-300 md:flex">
          <Circle size={10} fill="currentColor" />
          UI ONLINE
        </div>
        <div className="hidden items-center gap-2 rounded border border-slate-800 bg-slate-900 px-3 py-2 text-xs text-slate-300 lg:flex">
          <Activity size={14} className="text-slate-400" />
          POLLING
        </div>
      </div>
    </header>
  );
}
