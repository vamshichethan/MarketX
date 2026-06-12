import { useEffect, useState } from 'react';
import { errorMessage } from '../api/http';
import { omsApi } from '../api/omsApi';
import DataTable from '../components/DataTable';
import StatCard from '../components/StatCard';
import { money, symbols } from '../utils';

export default function OrderBookPage({ selectedSymbol, onSymbolChange }) {
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function fetchBook() {
    try {
      const response = await omsApi.getOrderBook(selectedSymbol);
      setBook(response);
      setError('');
    } catch (err) {
      setError(errorMessage(err, 'Order book API unavailable'));
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
    { key: 'price', label: 'Price', align: 'right', render: (row) => <span className="font-mono">{money(row.price)}</span> },
    { key: 'quantity', label: 'Quantity', align: 'right', render: (row) => <span className="font-mono">{row.quantity}</span> }
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

      <div className="grid gap-4 lg:grid-cols-2">
        <section>
          <h3 className="mb-2 text-xs font-semibold uppercase text-emerald-300">Bids</h3>
          <DataTable columns={columns} rows={book?.bids || []} loading={loading} error={error} emptyText="No bids" />
        </section>
        <section>
          <h3 className="mb-2 text-xs font-semibold uppercase text-red-300">Asks</h3>
          <DataTable columns={columns} rows={book?.asks || []} loading={loading} error={error} emptyText="No asks" />
        </section>
      </div>
    </div>
  );
}
