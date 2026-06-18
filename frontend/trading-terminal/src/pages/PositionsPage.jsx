import { useEffect, useState } from 'react';
import { errorMessage } from '../api/http';
import { positionApi } from '../api/positionApi';
import DataTable from '../components/DataTable';
import StatCard from '../components/StatCard';
import { mockPositionRows } from '../mockMarket';
import { integer, money, normalizeRows, signedClass } from '../utils';

export default function PositionsPage() {
  const [accountId, setAccountId] = useState('TRADER-1');
  const [rows, setRows] = useState(() => mockPositionRows());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function fetchPositions() {
    if (!accountId.trim()) return;
    try {
      const response = await positionApi.getPositions(accountId.trim());
      const nextRows = normalizeRows(response);
      setRows(nextRows.length ? nextRows : mockPositionRows());
      setError('');
    } catch (err) {
      setRows(mockPositionRows());
      setError(`${errorMessage(err, 'Position API unavailable')} - showing simulated book`);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchPositions();
    const id = setInterval(() => setRows(mockPositionRows()), 1000);
    return () => clearInterval(id);
  }, [accountId]);

  const gross = rows.reduce((sum, row) => sum + Math.abs(Number(row.marketValue || 0)), 0);
  const net = rows.reduce((sum, row) => sum + Number(row.marketValue || 0), 0);
  const dayPnl = rows.reduce((sum, row) => sum + Number(row.dayPnl || 0), 0);

  const columns = [
    { key: 'symbol', label: 'Symbol' },
    { key: 'netQuantity', label: 'Net Quantity', align: 'right', render: (row) => <span className={signedClass(row.netQuantity)}>{integer(row.netQuantity)}</span> },
    { key: 'averagePrice', label: 'Avg Price', align: 'right', render: (row) => <span className="font-mono">{money(row.averagePrice)}</span> },
    { key: 'marketValue', label: 'Market Value', align: 'right', render: (row) => <span className={signedClass(row.marketValue)}>{money(row.marketValue)}</span> },
    { key: 'dayPnl', label: 'Day PnL', align: 'right', render: (row) => <span className={signedClass(row.dayPnl)}>{money(row.dayPnl)}</span> },
    { key: 'positionType', label: 'Position Type' },
    { key: 'custodian', label: 'Custodian', render: (row) => <span className="font-mono">{row.custodian || 'GSCO'}</span> },
    { key: 'updatedAt', label: 'Updated At', render: (row) => <span className="font-mono text-slate-400">{row.updatedAt || '--'}</span> }
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold uppercase text-slate-300">Positions</h2>
        <input value={accountId} onChange={(e) => setAccountId(e.target.value)} className="rounded border border-slate-700 bg-slate-900 px-3 py-2 font-mono text-sm outline-none focus:border-amber-400" />
      </div>
      <div className="grid gap-3 md:grid-cols-3">
        <StatCard title="Gross Market Value" value={money(gross)} />
        <StatCard title="Net Market Value" value={money(net)} variant={net >= 0 ? 'positive' : 'negative'} />
        <StatCard title="Live Day PnL" value={money(dayPnl)} variant={dayPnl >= 0 ? 'positive' : 'negative'} />
      </div>
      {error && <div className="rounded border border-amber-900 bg-amber-950/20 p-3 text-xs text-amber-200">{error}</div>}
      <DataTable columns={columns} rows={rows} loading={loading} />
    </div>
  );
}
