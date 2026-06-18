import { useEffect, useState } from 'react';
import { errorMessage } from '../api/http';
import { omsApi } from '../api/omsApi';
import DataTable from '../components/DataTable';
import StatCard from '../components/StatCard';
import { mockOrderBook } from '../mockMarket';
import { integer, money, symbols } from '../utils';

export default function OrderBookPage({ selectedSymbol, onSymbolChange }) {
  const [book, setBook] = useState(() => mockOrderBook(selectedSymbol));
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function fetchBook() {
    try {
      const response = await omsApi.getOrderBook(selectedSymbol);
      setBook(response);
      setError('');
    } catch (err) {
      setBook(mockOrderBook(selectedSymbol));
      setError(`${errorMessage(err, 'Order book API unavailable')} - showing simulated depth`);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchBook();
    const id = setInterval(fetchBook, 1000);
    return () => clearInterval(id);
  }, [selectedSymbol]);

  const columns = [
    { key: 'venue', label: 'Venue', render: (row) => <span className="font-mono text-slate-300">{row.venue || 'SMART'}</span> },
    { key: 'price', label: 'Price', align: 'right', render: (row) => <span className="font-mono">{money(row.price)}</span> },
    { key: 'quantity', label: 'Quantity', align: 'right', render: (row) => <span className="font-mono">{integer(row.quantity)}</span> },
    { key: 'orders', label: 'Orders', align: 'right', render: (row) => <span className="font-mono text-slate-400">{integer(row.orders)}</span> }
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold uppercase text-slate-300">Order Book</h2>
        <select value={selectedSymbol} onChange={(e) => onSymbolChange(e.target.value)} className="rounded border border-slate-700 bg-slate-900 px-3 py-2 text-sm outline-none focus:border-amber-400">
          {symbols.map((symbol) => <option key={symbol}>{symbol}</option>)}
        </select>
      </div>

      <div className="grid gap-3 md:grid-cols-3">
        <StatCard title="Best Bid" value={money(book?.bestBid)} variant="positive" />
        <StatCard title="Best Ask" value={money(book?.bestAsk)} variant="negative" />
        <StatCard title="Spread" value={money(book?.spread)} variant="warning" />
      </div>

      {error && <div className="rounded border border-amber-900 bg-amber-950/20 p-3 text-xs text-amber-200">{error}</div>}

      <div className="grid gap-4 lg:grid-cols-2">
        <section className="rounded border border-slate-800 bg-slate-900 p-3">
          <div className="mb-2 flex items-center justify-between">
            <h3 className="text-xs font-semibold uppercase text-emerald-300">Bids</h3>
            <span className="font-mono text-xs text-slate-500">Depth {integer((book?.bids || []).reduce((sum, row) => sum + Number(row.quantity || 0), 0))}</span>
          </div>
          <DataTable columns={columns} rows={book?.bids || []} loading={loading} emptyText="No bids" />
        </section>
        <section className="rounded border border-slate-800 bg-slate-900 p-3">
          <div className="mb-2 flex items-center justify-between">
            <h3 className="text-xs font-semibold uppercase text-red-300">Asks</h3>
            <span className="font-mono text-xs text-slate-500">Depth {integer((book?.asks || []).reduce((sum, row) => sum + Number(row.quantity || 0), 0))}</span>
          </div>
          <DataTable columns={columns} rows={book?.asks || []} loading={loading} emptyText="No asks" />
        </section>
      </div>

      <section className="rounded border border-slate-800 bg-slate-900 p-4">
        <div className="grid gap-3 text-xs md:grid-cols-4">
          <div><span className="text-slate-500">Last</span><div className="font-mono text-lg text-amber-300">{money(book?.lastPrice)}</div></div>
          <div><span className="text-slate-500">Mid</span><div className="font-mono text-lg text-slate-100">{money((Number(book?.bestBid || 0) + Number(book?.bestAsk || 0)) / 2)}</div></div>
          <div><span className="text-slate-500">Imbalance</span><div className={`font-mono text-lg ${Number(book?.imbalance) >= 0 ? 'text-emerald-300' : 'text-red-300'}`}>{integer(book?.imbalance)}</div></div>
          <div><span className="text-slate-500">Market State</span><div className="font-mono text-lg text-emerald-300">OPEN</div></div>
        </div>
      </section>
    </div>
  );
}
