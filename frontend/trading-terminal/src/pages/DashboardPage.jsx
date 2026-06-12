import { useEffect, useMemo, useState } from 'react';
import { analyticsApi } from '../api/analyticsApi';
import { errorMessage } from '../api/http';
import { marketDataApi } from '../api/marketDataApi';
import { pnlApi } from '../api/pnlApi';
import DataTable from '../components/DataTable';
import StatCard from '../components/StatCard';
import { integer, money, normalizeRows, signedClass, symbols } from '../utils';

function bySymbol(rows, symbol) {
  return rows.find((row) => row.symbol === symbol) || {};
}

export default function DashboardPage() {
  const [marketRows, setMarketRows] = useState([]);
  const [analyticsRows, setAnalyticsRows] = useState([]);
  const [pnlRows, setPnlRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  async function fetchDashboard() {
    try {
      const [market, analytics, pnl] = await Promise.allSettled([
        marketDataApi.getLatest(),
        analyticsApi.getDashboard(),
        pnlApi.getPnl('TRADER-1')
      ]);

      if (market.status === 'fulfilled') setMarketRows(normalizeRows(market.value));
      if (analytics.status === 'fulfilled') setAnalyticsRows(normalizeRows(analytics.value?.symbols || analytics.value));
      if (pnl.status === 'fulfilled') setPnlRows(normalizeRows(pnl.value));

      const rejected = [market, analytics, pnl].find((result) => result.status === 'rejected');
      setError(rejected ? errorMessage(rejected.reason, 'Some dashboard APIs are unavailable') : '');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchDashboard();
    const id = setInterval(fetchDashboard, 3000);
    return () => clearInterval(id);
  }, []);

  const totals = useMemo(() => {
    const totalPnl = pnlRows.reduce((sum, row) => sum + Number(row.totalPnl || 0), 0);
    const totalVolume = analyticsRows.reduce((sum, row) => sum + Number(row.volume || 0), 0);
    const tradeCount = analyticsRows.reduce((sum, row) => sum + Number(row.tradeCount || 0), 0);
    return { totalPnl, totalVolume, tradeCount };
  }, [analyticsRows, pnlRows]);

  const overviewRows = symbols.map((symbol) => {
    const market = bySymbol(marketRows, symbol);
    const analytics = bySymbol(analyticsRows, symbol);
    return {
      symbol,
      price: market.price,
      spread: market.spread ?? (market.ask && market.bid ? Number(market.ask) - Number(market.bid) : null),
      vwap: analytics.vwap,
      volume: analytics.volume ?? market.volume,
      tradeCount: analytics.tradeCount
    };
  });

  const columns = [
    { key: 'symbol', label: 'Symbol' },
    { key: 'price', label: 'Price', align: 'right', render: (row) => <span className="font-mono text-amber-300">{money(row.price)}</span> },
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
            subtitle="Latest market data"
            variant="warning"
          />
        ))}
        <StatCard title="Total PnL" value={money(totals.totalPnl)} variant={totals.totalPnl >= 0 ? 'positive' : 'negative'} />
        <StatCard title="Total Volume" value={integer(totals.totalVolume)} />
        <StatCard title="Trade Count" value={integer(totals.tradeCount)} />
      </div>

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
