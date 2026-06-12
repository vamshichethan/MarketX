import { Send } from 'lucide-react';
import { useEffect, useState } from 'react';
import { fixApi } from '../api/fixApi';
import { errorMessage } from '../api/http';
import DataTable from '../components/DataTable';

const examples = {
  buy: '8=FIX.4.4|35=D|49=CLIENT1|56=MARKETX|11=CLORD-1|55=AAPL|54=1|38=100|40=2|44=150.00|60=20260609-10:00:00|',
  sell: '8=FIX.4.4|35=D|49=CLIENT2|56=MARKETX|11=CLORD-2|55=AAPL|54=2|38=100|40=2|44=150.00|60=20260609-10:00:05|',
  cancel: '8=FIX.4.4|35=F|49=CLIENT1|56=MARKETX|11=CANCEL-1|41=CLORD-1|55=AAPL|54=1|60=20260609-10:05:00|'
};

export default function FixGatewayPage() {
  const [message, setMessage] = useState(examples.buy);
  const [result, setResult] = useState('');
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function fetchReports() {
    try {
      const response = await fixApi.getReports();
      setReports(Array.isArray(response) ? response : []);
      setError('');
    } catch (err) {
      setError(errorMessage(err, 'FIX reports API unavailable'));
    } finally {
      setLoading(false);
    }
  }

  async function submitFix() {
    setResult('');
    try {
      const response = await fixApi.sendMessage(message);
      setResult(JSON.stringify(response, null, 2));
      fetchReports();
    } catch (err) {
      setResult(errorMessage(err, 'FIX message failed'));
    }
  }

  useEffect(() => {
    fetchReports();
  }, []);

  const columns = [
    { key: 'clOrdId', label: 'ClOrdID', render: (row) => row.clOrdId || row.clOrdID || row.orderId || '--' },
    { key: 'rawFixReport', label: 'Raw FIX Report', render: (row) => <span className="font-mono text-xs">{row.rawFixReport || row.rawReport || row.rawMessage || '--'}</span> },
    { key: 'createdAt', label: 'Created At', render: (row) => <span className="font-mono text-slate-400">{row.createdAt || '--'}</span> }
  ];

  return (
    <div className="grid gap-4 xl:grid-cols-[480px_1fr]">
      <section className="rounded border border-slate-800 bg-slate-900 p-4">
        <h2 className="mb-3 text-sm font-semibold uppercase text-slate-300">FIX Message</h2>
        <div className="mb-3 flex flex-wrap gap-2">
          <button onClick={() => setMessage(examples.buy)} className="rounded border border-emerald-800 px-3 py-1 text-xs text-emerald-300">New BUY LIMIT</button>
          <button onClick={() => setMessage(examples.sell)} className="rounded border border-red-800 px-3 py-1 text-xs text-red-300">New SELL LIMIT</button>
          <button onClick={() => setMessage(examples.cancel)} className="rounded border border-amber-800 px-3 py-1 text-xs text-amber-300">Cancel</button>
        </div>
        <textarea value={message} onChange={(e) => setMessage(e.target.value)} className="h-44 w-full rounded border border-slate-700 bg-slate-950 p-3 font-mono text-xs text-slate-100 outline-none focus:border-amber-400" />
        <button onClick={submitFix} className="mt-3 flex items-center gap-2 rounded bg-amber-400 px-3 py-2 text-sm font-semibold text-slate-950 hover:bg-amber-300">
          <Send size={16} />
          Submit FIX
        </button>
        {result && <pre className="mt-3 max-h-56 overflow-auto rounded border border-slate-800 bg-slate-950 p-3 text-xs text-slate-300">{result}</pre>}
      </section>
      <section>
        <h2 className="mb-3 text-sm font-semibold uppercase text-slate-300">Execution Reports</h2>
        <DataTable columns={columns} rows={reports} loading={loading} error={error} />
      </section>
    </div>
  );
}
