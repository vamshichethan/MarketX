import { useEffect, useMemo, useState } from 'react';
import { errorMessage } from '../api/http';
import { pnlApi } from '../api/pnlApi';
import DataTable from '../components/DataTable';
import StatCard from '../components/StatCard';
import { integer, money, signedClass } from '../utils';

export default function PnlPage() {
  const [accountId, setAccountId] = useState('TRADER-1');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function fetchPnl() {
    if (!accountId.trim()) return;
    try {
      const response = await pnlApi.getPnl(accountId.trim());
      setRows(Array.isArray(response) ? response : []);
      setError('');
    } catch (err) {
      setError(errorMessage(err, 'PnL API unavailable'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchPnl();
    const id = setInterval(fetchPnl, 2000);
    return () => clearInterval(id);
  }, [accountId]);

  const totals = useMemo(() => ({
    realized: rows.reduce((sum, row) => sum + Number(row.realizedPnl || 0), 0),
    unrealized: rows.reduce((sum, row) => sum + Number(row.unrealizedPnl || 0), 0),
    total: rows.reduce((sum, row) => sum + Number(row.totalPnl || 0), 0)
  }), [rows]);

  const columns = [
    { key: 'symbol', label: 'Symbol' },
    { key: 'netQuantity', label: 'Net Qty', align: 'right', render: (row) => <span className={signedClass(row.netQuantity)}>{integer(row.netQuantity)}</span> },
    { key: 'averagePrice', label: 'Avg Price', align: 'right', render: (row) => <span className="font-mono">{money(row.averagePrice)}</span> },
    { key: 'lastMarketPrice', label: 'Last Price', align: 'right', render: (row) => <span className="font-mono text-amber-300">{money(row.lastMarketPrice)}</span> },
    { key: 'realizedPnl', label: 'Realized', align: 'right', render: (row) => <span className={signedClass(row.realizedPnl)}>{money(row.realizedPnl)}</span> },
    { key: 'unrealizedPnl', label: 'Unrealized', align: 'right', render: (row) => <span className={signedClass(row.unrealizedPnl)}>{money(row.unrealizedPnl)}</span> },
    { key: 'totalPnl', label: 'Total PnL', align: 'right', render: (row) => <span className={signedClass(row.totalPnl)}>{money(row.totalPnl)}</span> }
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold uppercase text-slate-300">PnL</h2>
        <input value={accountId} onChange={(e) => setAccountId(e.target.value)} className="rounded border border-slate-700 bg-slate-900 px-3 py-2 font-mono text-sm outline-none focus:border-amber-400" />
      </div>
      <div className="grid gap-3 md:grid-cols-3">
        <StatCard title="Total Realized" value={money(totals.realized)} variant={totals.realized >= 0 ? 'positive' : 'negative'} />
        <StatCard title="Total Unrealized" value={money(totals.unrealized)} variant={totals.unrealized >= 0 ? 'positive' : 'negative'} />
        <StatCard title="Total PnL" value={money(totals.total)} variant={totals.total >= 0 ? 'positive' : 'negative'} />
      </div>
      <DataTable columns={columns} rows={rows} loading={loading} error={error} />
    </div>
  );
}
