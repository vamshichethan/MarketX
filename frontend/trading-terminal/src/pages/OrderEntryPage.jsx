import { RotateCcw, Send } from 'lucide-react';
import { useState } from 'react';
import { errorMessage } from '../api/http';
import { omsApi } from '../api/omsApi';
import { symbols } from '../utils';

const initialForm = {
  accountId: 'TRADER-1',
  symbol: 'AAPL',
  side: 'BUY',
  type: 'LIMIT',
  quantity: 100,
  price: 150
};

export default function OrderEntryPage({ selectedSymbol }) {
  const [form, setForm] = useState({ ...initialForm, symbol: selectedSymbol });
  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  async function submitOrder(event) {
    event.preventDefault();
    setSubmitting(true);
    setResult(null);
    try {
      const payload = {
        ...form,
        quantity: Number(form.quantity),
        price: form.type === 'MARKET' ? undefined : Number(form.price)
      };
      const response = await omsApi.submitOrder(payload);
      setResult({ type: response.status === 'REJECTED' ? 'error' : 'success', data: response });
    } catch (error) {
      setResult({ type: 'error', message: errorMessage(error, 'Order submission failed') });
    } finally {
      setSubmitting(false);
    }
  }

  function reset() {
    setForm({ ...initialForm, symbol: selectedSymbol });
    setResult(null);
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[420px_1fr]">
      <form onSubmit={submitOrder} className="rounded border border-slate-800 bg-slate-900 p-4">
        <h2 className="mb-4 text-sm font-semibold uppercase text-slate-300">Order Ticket</h2>
        <div className="grid gap-3">
          <label className="text-xs text-slate-400">
            Account ID
            <input className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-amber-400" value={form.accountId} onChange={(e) => update('accountId', e.target.value)} />
          </label>
          <label className="text-xs text-slate-400">
            Symbol
            <select className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-amber-400" value={form.symbol} onChange={(e) => update('symbol', e.target.value)}>
              {symbols.map((symbol) => <option key={symbol}>{symbol}</option>)}
            </select>
          </label>
          <div className="grid grid-cols-2 gap-3">
            <label className="text-xs text-slate-400">
              Side
              <select className={`mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 font-semibold outline-none focus:border-amber-400 ${form.side === 'BUY' ? 'text-emerald-300' : 'text-red-300'}`} value={form.side} onChange={(e) => update('side', e.target.value)}>
                <option>BUY</option>
                <option>SELL</option>
              </select>
            </label>
            <label className="text-xs text-slate-400">
              Type
              <select className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-amber-400" value={form.type} onChange={(e) => update('type', e.target.value)}>
                <option>LIMIT</option>
                <option>MARKET</option>
              </select>
            </label>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <label className="text-xs text-slate-400">
              Quantity
              <input className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 font-mono text-slate-100 outline-none focus:border-amber-400" type="number" min="1" value={form.quantity} onChange={(e) => update('quantity', e.target.value)} />
            </label>
            <label className="text-xs text-slate-400">
              Price
              <input className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 font-mono text-slate-100 outline-none focus:border-amber-400 disabled:opacity-40" type="number" min="0" step="0.01" disabled={form.type === 'MARKET'} value={form.price} onChange={(e) => update('price', e.target.value)} />
            </label>
          </div>
          <div className="flex gap-2 pt-2">
            <button disabled={submitting} className="flex flex-1 items-center justify-center gap-2 rounded bg-amber-400 px-3 py-2 text-sm font-semibold text-slate-950 hover:bg-amber-300 disabled:opacity-50">
              <Send size={16} />
              Submit Order
            </button>
            <button type="button" onClick={reset} className="flex items-center justify-center gap-2 rounded border border-slate-700 px-3 py-2 text-sm text-slate-300 hover:bg-slate-800">
              <RotateCcw size={16} />
              Reset
            </button>
          </div>
        </div>
      </form>

      <section className="rounded border border-slate-800 bg-slate-900 p-4">
        <h2 className="mb-4 text-sm font-semibold uppercase text-slate-300">Submission Result</h2>
        {!result && <div className="text-sm text-slate-500">Submit an order to see status, risk result, and OMS response.</div>}
        {result && (
          <div className={`rounded border p-4 text-sm ${result.type === 'error' ? 'border-red-900 bg-red-950/30 text-red-200' : 'border-emerald-900 bg-emerald-950/30 text-emerald-200'}`}>
            {result.message ? result.message : (
              <div className="space-y-2">
                <div><span className="text-slate-400">Order ID:</span> <span className="font-mono">{result.data.orderId}</span></div>
                <div><span className="text-slate-400">Status:</span> <span className="font-mono">{result.data.status}</span></div>
                <div><span className="text-slate-400">Message:</span> {result.data.message || 'Submitted'}</div>
                {!!result.data.rejectionReasons?.length && (
                  <div><span className="text-slate-400">Risk:</span> {result.data.rejectionReasons.join('; ')}</div>
                )}
              </div>
            )}
          </div>
        )}
      </section>
    </div>
  );
}
