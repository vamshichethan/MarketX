import { Play, Square, Zap } from 'lucide-react';
import { useEffect, useState } from 'react';
import { errorMessage } from '../api/http';
import { marketDataApi } from '../api/marketDataApi';
import DataTable from '../components/DataTable';
import { integer, money, normalizeRows, signedClass } from '../utils';

export default function MarketDataPage() {
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [actionMessage, setActionMessage] = useState('');

  async function fetchMarketData() {
    try {
      const response = await marketDataApi.getLatest();
      setRows(normalizeRows(response));
      setError('');
    } catch (err) {
      setError(errorMessage(err, 'Market data API unavailable'));
    } finally {
      setLoading(false);
    }
  }

  async function runAction(action, label) {
    try {
      await action();
      setActionMessage(`${label} sent`);
      fetchMarketData();
    } catch (err) {
      setActionMessage(errorMessage(err, `${label} failed`));
    }
  }

  useEffect(() => {
    fetchMarketData();
    const id = setInterval(fetchMarketData, 1000);
    return () => clearInterval(id);
  }, []);

  const columns = [
    { key: 'symbol', label: 'Symbol' },
    { key: 'price', label: 'Price', align: 'right', render: (row) => <span className="font-mono text-amber-300">{money(row.price)}</span> },
    { key: 'previousPrice', label: 'Prev', align: 'right', render: (row) => <span className="font-mono">{money(row.previousPrice)}</span> },
    { key: 'change', label: 'Change', align: 'right', render: (row) => <span className={signedClass(row.change)}>{money(row.change)}</span> },
    { key: 'changePercent', label: 'Change %', align: 'right', render: (row) => <span className={signedClass(row.changePercent)}>{money(row.changePercent)}%</span> },
    { key: 'volume', label: 'Volume', align: 'right', render: (row) => <span className="font-mono">{integer(row.volume)}</span> },
    { key: 'bid', label: 'Bid', align: 'right', render: (row) => <span className="font-mono text-emerald-300">{money(row.bid)}</span> },
    { key: 'ask', label: 'Ask', align: 'right', render: (row) => <span className="font-mono text-red-300">{money(row.ask)}</span> },
    { key: 'spread', label: 'Spread', align: 'right', render: (row) => <span className="font-mono">{money(row.spread ?? (row.ask && row.bid ? Number(row.ask) - Number(row.bid) : null))}</span> },
    { key: 'timestamp', label: 'Timestamp', render: (row) => <span className="font-mono text-slate-400">{row.timestamp || row.updatedAt || '--'}</span> }
  ];

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-sm font-semibold uppercase text-slate-300">Market Data</h2>
        <div className="flex gap-2">
          <button onClick={() => runAction(marketDataApi.startFeed, 'Start feed')} className="flex items-center gap-2 rounded border border-emerald-800 px-3 py-2 text-sm text-emerald-300 hover:bg-emerald-950/30"><Play size={15} />Start Feed</button>
          <button onClick={() => runAction(marketDataApi.stopFeed, 'Stop feed')} className="flex items-center gap-2 rounded border border-red-800 px-3 py-2 text-sm text-red-300 hover:bg-red-950/30"><Square size={15} />Stop Feed</button>
          <button onClick={() => runAction(marketDataApi.tick, 'Manual tick')} className="flex items-center gap-2 rounded border border-amber-800 px-3 py-2 text-sm text-amber-300 hover:bg-amber-950/30"><Zap size={15} />Manual Tick</button>
        </div>
      </div>
      {actionMessage && <div className="text-xs text-slate-400">{actionMessage}</div>}
      <DataTable columns={columns} rows={rows} loading={loading} error={error} />
    </div>
  );
}
