import { RotateCcw, Send } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { errorMessage } from '../api/http';
import { omsApi } from '../api/omsApi';
import { mockMarketRows, mockOrderResponse } from '../mockMarket';
import { money, symbols } from '../utils';

const initialForm = {
  accountId: 'TRADER-1',
  symbol: 'AAPL',
  side: 'BUY',
  type: 'LIMIT',
  quantity: 100,
  price: 150,
  timeInForce: 'DAY',
  venue: 'SMART',
  clientOrderId: '',
  strategy: 'MANUAL'
};

export default function OrderEntryPage({ selectedSymbol }) {
  const [form, setForm] = useState({ ...initialForm, symbol: selectedSymbol });
  const [result, setResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const selectedMarket = mockMarketRows().find((row) => row.symbol === selectedSymbol);
    setForm((current) => ({ ...current, symbol: selectedSymbol, price: selectedMarket ? selectedMarket.ask.toFixed(2) : current.price }));
  }, [selectedSymbol]);

  const market = useMemo(() => mockMarketRows().find((row) => row.symbol === form.symbol), [form.symbol]);
  const referencePrice = form.type === 'MARKET' ? market?.ask : form.price;
  const notional = Number(form.quantity || 0) * Number(referencePrice || 0);

  function update(field, value) {
    if (field === 'symbol') {
      const selectedMarket = mockMarketRows().find((row) => row.symbol === value);
      setForm((current) => ({ ...current, symbol: value, price: selectedMarket ? selectedMarket.ask.toFixed(2) : current.price }));
      return;
    }
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
      const responseLooksMock = /mock/i.test(response.message || '');
      const enriched = responseLooksMock
        ? { ...mockOrderResponse(form, response.message), orderId: response.orderId, status: response.status || 'ACCEPTED' }
        : { ...mockOrderResponse(form, response.message), ...response };
      setResult({ type: enriched.status === 'REJECTED' ? 'error' : 'success', data: enriched });
    } catch (error) {
      const simulated = mockOrderResponse(form);
      setResult({
        type: simulated.status === 'REJECTED' ? 'error' : 'success',
        data: simulated,
        message: simulated.status === 'REJECTED' ? undefined : `${errorMessage(error, 'OMS unavailable')} - ${simulated.message}`
      });
    } finally {
      setSubmitting(false);
    }
  }

  function reset() {
    const selectedMarket = mockMarketRows().find((row) => row.symbol === selectedSymbol);
    setForm({ ...initialForm, symbol: selectedSymbol, price: selectedMarket ? selectedMarket.ask.toFixed(2) : initialForm.price });
    setResult(null);
  }

  return (
    <div className="grid gap-4 xl:grid-cols-[460px_1fr]">
      <form onSubmit={submitOrder} className="rounded border border-slate-800 bg-slate-900 p-4">
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-sm font-semibold uppercase text-slate-300">Order Ticket</h2>
          <span className="rounded border border-slate-700 bg-slate-950 px-2 py-1 font-mono text-xs text-amber-300">{form.strategy}</span>
        </div>
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
              Time in Force
              <select className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-amber-400" value={form.timeInForce} onChange={(e) => update('timeInForce', e.target.value)}>
                <option>DAY</option>
                <option>IOC</option>
                <option>FOK</option>
                <option>GTC</option>
              </select>
            </label>
            <label className="text-xs text-slate-400">
              Venue
              <select className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-amber-400" value={form.venue} onChange={(e) => update('venue', e.target.value)}>
                <option>SMART</option>
                <option>XNAS</option>
                <option>ARCX</option>
                <option>BATS</option>
                <option>IEX</option>
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
          <div className="grid grid-cols-2 gap-3">
            <label className="text-xs text-slate-400">
              Client Order ID
              <input className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 font-mono text-slate-100 outline-none focus:border-amber-400" placeholder="AUTO" value={form.clientOrderId} onChange={(e) => update('clientOrderId', e.target.value)} />
            </label>
            <label className="text-xs text-slate-400">
              Strategy
              <select className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 text-slate-100 outline-none focus:border-amber-400" value={form.strategy} onChange={(e) => update('strategy', e.target.value)}>
                <option>MANUAL</option>
                <option>VWAP</option>
                <option>TWAP</option>
                <option>POV</option>
              </select>
            </label>
          </div>
          <div className="grid gap-2 rounded border border-slate-800 bg-slate-950 p-3 text-xs md:grid-cols-3">
            <div><span className="text-slate-500">Bid</span><div className="font-mono text-emerald-300">{money(market?.bid)}</div></div>
            <div><span className="text-slate-500">Ask</span><div className="font-mono text-red-300">{money(market?.ask)}</div></div>
            <div><span className="text-slate-500">Est. Notional</span><div className="font-mono text-slate-100">{money(notional)}</div></div>
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
            <div className="space-y-2">
              {result.message && <div>{result.message}</div>}
              {result.data && (
                <>
                <div><span className="text-slate-400">Order ID:</span> <span className="font-mono">{result.data.orderId}</span></div>
                <div><span className="text-slate-400">Status:</span> <span className="font-mono">{result.data.status}</span></div>
                <div><span className="text-slate-400">Venue:</span> <span className="font-mono">{result.data.venue || form.venue}</span></div>
                <div><span className="text-slate-400">Leaves:</span> <span className="font-mono">{result.data.leavesQuantity ?? '--'}</span></div>
                <div><span className="text-slate-400">Route Status:</span> <span className="font-mono">{result.data.routeStatus || 'WORKING'}</span></div>
                <div><span className="text-slate-400">OMS Message:</span> {result.data.message || 'Submitted'}</div>
                {!!result.data.rejectionReasons?.length && (
                  <div><span className="text-slate-400">Risk:</span> {result.data.rejectionReasons.join('; ')}</div>
                )}
                {!!result.data.executionReports?.length && (
                  <div className="mt-3 border-t border-emerald-900/60 pt-3">
                    <div className="mb-2 text-xs uppercase text-slate-400">Execution Timeline</div>
                    <div className="space-y-2">
                      {result.data.executionReports.map((report) => (
                        <div key={`${report.stage}-${report.detail}`} className="grid grid-cols-[70px_70px_1fr] gap-2 text-xs">
                          <span className="font-mono text-amber-300">{report.stage}</span>
                          <span className="font-mono text-slate-400">{report.time}</span>
                          <span>{report.detail}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                </>
              )}
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
