import { useEffect, useMemo, useState } from 'react';
import { analyticsApi } from '../api/analyticsApi';
import { errorMessage } from '../api/http';
import { marketDataApi } from '../api/marketDataApi';
import { pnlApi } from '../api/pnlApi';
import DataTable from '../components/DataTable';
import StatCard from '../components/StatCard';
import { mockAnalyticsRows, mockIntradaySeries, mockMarketRows, mockPnlRows } from '../mockMarket';
import { askPrice, bidPrice, integer, money, normalizeRows, symbols } from '../utils';
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

function bySymbol(rows, symbol) {
  return rows.find((row) => row.symbol === symbol) || {};
}

export default function DashboardPage() {
  const [marketRows, setMarketRows] = useState(() => mockMarketRows());
  const [analyticsRows, setAnalyticsRows] = useState(() => mockAnalyticsRows());
  const [pnlRows, setPnlRows] = useState(() => mockPnlRows());
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [selectedSymbol, setSelectedSymbol] = useState('AAPL');
  const [tick, setTick] = useState(0);

  async function fetchDashboard() {
    try {
      const [market, analytics, pnl] = await Promise.allSettled([
        marketDataApi.getLatest(),
        analyticsApi.getDashboard(),
        pnlApi.getPnl('TRADER-1')
      ]);

      const fallbackMarketRows = mockMarketRows(tick);
      const fallbackAnalyticsRows = mockAnalyticsRows(tick);
      const fallbackPnlRows = mockPnlRows(tick);

      if (market.status === 'fulfilled') {
        const rows = normalizeRows(market.value);
        setMarketRows(rows.length >= symbols.length ? rows : fallbackMarketRows);
      } else setMarketRows(fallbackMarketRows);

      if (analytics.status === 'fulfilled') {
        const rows = normalizeRows(analytics.value?.symbols || analytics.value);
        setAnalyticsRows(rows.length >= symbols.length ? rows : fallbackAnalyticsRows);
      } else setAnalyticsRows(fallbackAnalyticsRows);

      if (pnl.status === 'fulfilled') {
        const rows = normalizeRows(pnl.value);
        setPnlRows(rows.length ? rows : fallbackPnlRows);
      } else setPnlRows(fallbackPnlRows);

      const rejected = [market, analytics, pnl].find((result) => result.status === 'rejected');
      setError(rejected ? `${errorMessage(rejected.reason, 'Some dashboard APIs are unavailable')} - showing simulated market tape` : '');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchDashboard();
    const id = setInterval(fetchDashboard, 3000);
    return () => clearInterval(id);
  }, []);

  useEffect(() => {
    const id = setInterval(() => {
      setTick((current) => {
        const next = current + 1;
        setMarketRows(mockMarketRows(next));
        setAnalyticsRows(mockAnalyticsRows(next));
        setPnlRows(mockPnlRows(next));
        return next;
      });
    }, 1000);
    return () => clearInterval(id);
  }, []);

  const totals = useMemo(() => {
    const totalPnl = pnlRows.reduce((sum, row) => sum + Number(row.totalPnl || 0), 0);
    const totalVolume = analyticsRows.reduce((sum, row) => sum + Number(row.volume || 0), 0);
    const tradeCount = analyticsRows.reduce((sum, row) => sum + Number(row.tradeCount || 0), 0);
    return { totalPnl, totalVolume, tradeCount };
  }, [analyticsRows, pnlRows]);

  const chartRows = useMemo(() => mockIntradaySeries(selectedSymbol), [selectedSymbol, marketRows, tick]);

  const overviewRows = symbols.map((symbol) => {
    const market = bySymbol(marketRows, symbol);
    const analytics = bySymbol(analyticsRows, symbol);
    const bid = bidPrice(market);
    const ask = askPrice(market);
    return {
      symbol,
      price: market.price,
      spread: market.spread ?? (ask && bid ? Number(ask) - Number(bid) : null),
      vwap: analytics.vwap,
      volume: analytics.volume ?? market.volume,
      tradeCount: analytics.tradeCount,
      dayChange: market.dayChange,
      dayChangePct: market.dayChangePct
    };
  });

  const columns = [
    { key: 'symbol', label: 'Symbol' },
    { key: 'price', label: 'Price', align: 'right', render: (row) => <span className="font-mono text-amber-300">{money(row.price)}</span> },
    { key: 'dayChange', label: 'Chg', align: 'right', render: (row) => <span className={`font-mono ${Number(row.dayChange) >= 0 ? 'text-emerald-300' : 'text-red-300'}`}>{money(row.dayChange)}</span> },
    { key: 'dayChangePct', label: 'Chg %', align: 'right', render: (row) => <span className={`font-mono ${Number(row.dayChangePct) >= 0 ? 'text-emerald-300' : 'text-red-300'}`}>{money(row.dayChangePct)}%</span> },
    { key: 'spread', label: 'Spread', align: 'right', render: (row) => <span className="font-mono">{money(row.spread)}</span> },
    { key: 'vwap', label: 'VWAP', align: 'right', render: (row) => <span className="font-mono">{money(row.vwap)}</span> },
    { key: 'volume', label: 'Volume', align: 'right', render: (row) => <span className="font-mono">{integer(row.volume)}</span> },
    { key: 'tradeCount', label: 'Trade Count', align: 'right', render: (row) => <span className="font-mono">{integer(row.tradeCount)}</span> }
  ];

  return (
    <div className="space-y-4">
      <div className="grid gap-3 md:grid-cols-3 xl:grid-cols-6">
        {symbols.map((symbol) => (
          <StatCard
            key={symbol}
            title={`${symbol} Last`}
            value={money(bySymbol(marketRows, symbol).price)}
            subtitle={`${money(bySymbol(marketRows, symbol).dayChangePct)}% today`}
            variant={Number(bySymbol(marketRows, symbol).dayChange) >= 0 ? 'positive' : 'negative'}
          />
        ))}
        <StatCard title="Total PnL" value={money(totals.totalPnl)} variant={totals.totalPnl >= 0 ? 'positive' : 'negative'} />
        <StatCard title="Total Volume" value={integer(totals.totalVolume)} />
        <StatCard title="Trade Count" value={integer(totals.tradeCount)} />
      </div>

      <section className="grid gap-4 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div className="rounded border border-slate-800 bg-slate-900 p-4">
          <div className="mb-3 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-semibold uppercase text-slate-300">Intraday Tape</h2>
              <div className="text-xs text-slate-500">Consolidated simulated NBBO and volume</div>
            </div>
            <select value={selectedSymbol} onChange={(event) => setSelectedSymbol(event.target.value)} className="rounded border border-slate-700 bg-slate-950 px-3 py-2 text-sm font-mono outline-none focus:border-amber-400">
              {symbols.map((symbol) => <option key={symbol}>{symbol}</option>)}
            </select>
          </div>
          <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={chartRows} margin={{ left: 0, right: 8, top: 8, bottom: 0 }}>
                <defs>
                  <linearGradient id="priceFill" x1="0" x2="0" y1="0" y2="1">
                    <stop offset="5%" stopColor="#f59e0b" stopOpacity={0.35} />
                    <stop offset="95%" stopColor="#f59e0b" stopOpacity={0.02} />
                  </linearGradient>
                </defs>
                <CartesianGrid stroke="#1e293b" vertical={false} />
                <XAxis dataKey="time" tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis domain={['dataMin - 0.5', 'dataMax + 0.5']} tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} width={56} />
                <Tooltip contentStyle={{ background: '#020617', border: '1px solid #334155', borderRadius: 4 }} labelStyle={{ color: '#e2e8f0' }} />
                <Area type="monotone" dataKey="price" stroke="#f59e0b" fill="url(#priceFill)" strokeWidth={2} dot={false} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="rounded border border-slate-800 bg-slate-900 p-4">
          <h2 className="mb-3 text-sm font-semibold uppercase text-slate-300">Desk Risk</h2>
          <div className="space-y-3 text-sm">
            <div className="flex justify-between border-b border-slate-800 pb-2"><span className="text-slate-400">Gross Exposure</span><span className="font-mono text-slate-100">{money(pnlRows.reduce((sum, row) => sum + Math.abs(Number(row.quantity || 0) * Number(row.marketPrice || 0)), 0))}</span></div>
            <div className="flex justify-between border-b border-slate-800 pb-2"><span className="text-slate-400">Net Shares</span><span className="font-mono text-slate-100">{integer(pnlRows.reduce((sum, row) => sum + Number(row.quantity || 0), 0))}</span></div>
            <div className="flex justify-between border-b border-slate-800 pb-2"><span className="text-slate-400">Buying Power</span><span className="font-mono text-emerald-300">{money(1250000)}</span></div>
            <div className="flex justify-between"><span className="text-slate-400">Risk State</span><span className="font-mono text-emerald-300">NORMAL</span></div>
          </div>
        </div>
      </section>

      <section>
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase text-slate-300">Market Overview</h2>
          {error && <span className="text-xs text-amber-300">{error}</span>}
        </div>
        <DataTable columns={columns} rows={overviewRows} loading={loading} />
      </section>
    </div>
  );
}
