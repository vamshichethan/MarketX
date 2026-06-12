export default function DataTable({ columns, rows, loading, error, emptyText = 'No data' }) {
  if (loading) {
    return <div className="rounded border border-slate-800 bg-slate-900 p-4 text-sm text-slate-400">Loading...</div>;
  }

  if (error) {
    return <div className="rounded border border-red-900 bg-red-950/30 p-4 text-sm text-red-300">{error}</div>;
  }

  return (
    <div className="overflow-hidden rounded border border-slate-800 bg-slate-900">
      <div className="overflow-x-auto">
        <table className="min-w-full text-left text-xs">
          <thead className="border-b border-slate-800 bg-slate-950 text-[11px] uppercase text-slate-400">
            <tr>
              {columns.map((column) => (
                <th key={column.key} className={`whitespace-nowrap px-3 py-2 font-semibold ${column.align === 'right' ? 'text-right' : ''}`}>
                  {column.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {rows?.length ? rows.map((row, index) => (
              <tr key={row.id || row.orderId || row.tradeId || row.symbol || index} className="hover:bg-slate-800/50">
                {columns.map((column) => (
                  <td key={column.key} className={`whitespace-nowrap px-3 py-2 ${column.align === 'right' ? 'text-right' : ''}`}>
                    {column.render ? column.render(row, index) : row[column.key] ?? '--'}
                  </td>
                ))}
              </tr>
            )) : (
              <tr>
                <td className="px-3 py-6 text-center text-slate-500" colSpan={columns.length}>{emptyText}</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
