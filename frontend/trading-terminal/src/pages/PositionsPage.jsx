import { useEffect, useState } from 'react';
import { errorMessage } from '../api/http';
import { positionApi } from '../api/positionApi';
import DataTable from '../components/DataTable';
import { integer, money, signedClass } from '../utils';

export default function PositionsPage() {
  const [accountId, setAccountId] = useState('TRADER-1');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function fetchPositions() {
    if (!accountId.trim()) return;
    try {
      const response = await positionApi.getPositions(accountId.trim());
      setRows(Array.isArray(response) ? response : []);
      setError('');
    } catch (err) {
      setError(errorMessage(err, 'Position API unavailable'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchPositions();
  }, [accountId]);

  const columns = [
    { key: 'symbol', label: 'Symbol' },
    { key: 'netQuantity', label: 'Net Quantity', align: 'right', render: (row) => <span className={signedClass(row.netQuantity)}>{integer(row.netQuantity)}</span> },
    { key: 'averagePrice', label: 'Avg Price', align: 'right', render: (row) => <span className="font-mono">{money(row.averagePrice)}</span> },
    { key: 'positionType', label: 'Position Type' },
    { key: 'updatedAt', label: 'Updated At', render: (row) => <span className="font-mono text-slate-400">{row.updatedAt || '--'}</span> }
  ];

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold uppercase text-slate-300">Positions</h2>
        <input value={accountId} onChange={(e) => setAccountId(e.target.value)} className="rounded border border-slate-700 bg-slate-900 px-3 py-2 font-mono text-sm outline-none focus:border-amber-400" />
      </div>
      <DataTable columns={columns} rows={rows} loading={loading} error={error} />
    </div>
  );
}
