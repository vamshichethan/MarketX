import {
  BarChart3,
  BookOpen,
  Gauge,
  LineChart,
  MessageSquareText,
  PieChart,
  RadioTower,
  RotateCcw,
  Send
} from 'lucide-react';

const items = [
  { id: 'dashboard', label: 'Dashboard', icon: Gauge },
  { id: 'order-entry', label: 'Order Entry', icon: Send },
  { id: 'order-book', label: 'Order Book', icon: BookOpen },
  { id: 'positions', label: 'Positions', icon: PieChart },
  { id: 'pnl', label: 'PnL', icon: LineChart },
  { id: 'market-data', label: 'Market Data', icon: RadioTower },
  { id: 'analytics', label: 'Analytics', icon: BarChart3 },
  { id: 'fix', label: 'FIX Gateway', icon: MessageSquareText },
  { id: 'replay', label: 'Historical Replay', icon: RotateCcw }
];

export default function Sidebar({ activePage, onNavigate }) {
  return (
    <aside className="w-60 border-r border-slate-800 bg-slate-950 p-3">
      <div className="mb-4 rounded border border-slate-800 bg-slate-900 p-3">
        <div className="text-xs uppercase text-slate-500">Workspace</div>
        <div className="font-mono text-sm text-amber-300">TRADER-1</div>
      </div>
      <nav className="space-y-1">
        {items.map((item) => {
          const Icon = item.icon;
          const active = activePage === item.id;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => onNavigate(item.id)}
              className={`flex w-full items-center gap-2 rounded px-3 py-2 text-left text-sm transition ${
                active
                  ? 'bg-amber-400 text-slate-950'
                  : 'text-slate-300 hover:bg-slate-900 hover:text-slate-100'
              }`}
            >
              <Icon size={16} />
              {item.label}
            </button>
          );
        })}
      </nav>
    </aside>
  );
}
