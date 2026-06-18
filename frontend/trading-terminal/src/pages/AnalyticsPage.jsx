import { useEffect, useState } from 'react';
import { Bar, BarChart, CartesianGrid, Legend, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { analyticsApi } from '../api/analyticsApi';
import { errorMessage } from '../api/http';
import DataTable from '../components/DataTable';
import { mockAnalyticsRows } from '../mockMarket';
import { integer, money, normalizeRows } from '../utils';

export default function AnalyticsPage() {
  const [rows, setRows] = useState(() => mockAnalyticsRows());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function fetchAnalytics() {
    try {
      const response = await analyticsApi.getDashboard();
      const nextRows = normalizeRows(response?.symbols || response);
      setRows(nextRows.length ? nextRows : mockAnalyticsRows());
      setError('');
    } catch (err) {
      setRows(mockAnalyticsRows());
      setError(`${errorMessage(err, 'Analytics API unavailable')} - showing simulated analytics`);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchAnalytics();
    const id = setInterval(fetchAnalytics, 3000);
    const tickId = setInterval(() => setRows(mockAnalyticsRows()), 1000);
    return () => {
      clearInterval(id);
      clearInterval(tickId);
    };
  }, []);

  const priceRows = rows.filter((row) => row.latestPrice !== null && row.latestPrice !== undefined);

  const columns = [
    { key: 'symbol', label: 'Symbol' },
    { key: 'vwap', label: 'VWAP', align: 'right', render: (row) => <span className="font-mono">{money(row.vwap)}</span> },
    { key: 'spread', label: 'Spread', align: 'right', render: (row) => <span className="font-mono">{money(row.spread)}</span> },
    { key: 'volume', label: 'Volume', align: 'right', render: (row) => <span className="font-mono">{integer(row.volume)}</span> },
    { key: 'tradeCount', label: 'Trade Count', align: 'right', render: (row) => <span className="font-mono">{integer(row.tradeCount)}</span> }
  ];

  return (
    <div className="space-y-4">
      <h2 className="text-sm font-semibold uppercase text-slate-300">Analytics Dashboard</h2>
      {error && <div className="rounded border border-amber-900 bg-amber-950/20 p-3 text-xs text-amber-200">{error}</div>}
      <DataTable columns={columns} rows={rows} loading={loading} />
      <div className="grid gap-4 xl:grid-cols-2">
        <ChartPanel title="Volume by Symbol">
          <BarChart data={rows}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
            <XAxis dataKey="symbol" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" />
            <Tooltip contentStyle={{ background: '#0f172a', border: '1px solid #334155' }} />
            <Legend />
            <Bar dataKey="volume" fill="#f59e0b" />
          </BarChart>
        </ChartPanel>
        <ChartPanel title="Trade Count by Symbol">
          <BarChart data={rows}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
            <XAxis dataKey="symbol" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" />
            <Tooltip contentStyle={{ background: '#0f172a', border: '1px solid #334155' }} />
            <Legend />
            <Bar dataKey="tradeCount" fill="#22c55e" />
          </BarChart>
        </ChartPanel>
      </div>
      {!!priceRows.length && (
        <ChartPanel title="Latest Price by Symbol">
          <LineChart data={priceRows}>
            <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
            <XAxis dataKey="symbol" stroke="#94a3b8" />
            <YAxis stroke="#94a3b8" domain={['auto', 'auto']} />
            <Tooltip contentStyle={{ background: '#0f172a', border: '1px solid #334155' }} />
            <Legend />
            <Line type="monotone" dataKey="latestPrice" stroke="#f59e0b" strokeWidth={2} dot={{ r: 4 }} />
          </LineChart>
        </ChartPanel>
      )}
    </div>
  );
}

function ChartPanel({ title, children }) {
  return (
    <section className="h-80 rounded border border-slate-800 bg-slate-900 p-4">
      <h3 className="mb-3 text-xs font-semibold uppercase text-slate-400">{title}</h3>
      <ResponsiveContainer width="100%" height="88%">
        {children}
      </ResponsiveContainer>
    </section>
  );
}
