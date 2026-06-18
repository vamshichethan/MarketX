export const symbols = ['AAPL', 'MSFT', 'GOOG', 'NVDA', 'TSLA', 'JPM', 'SPY', 'QQQ'];

export function money(value, digits = 2) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--';
  }
  return Number(value).toLocaleString(undefined, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  });
}

export function integer(value) {
  if (value === null || value === undefined || Number.isNaN(Number(value))) {
    return '--';
  }
  return Number(value).toLocaleString();
}

export function signedClass(value) {
  const number = Number(value);
  if (number > 0) return 'font-mono text-emerald-300';
  if (number < 0) return 'font-mono text-red-300';
  return 'font-mono text-slate-300';
}

export function sideClass(side) {
  return side === 'BUY' ? 'text-emerald-300' : 'text-red-300';
}

export function normalizeRows(value) {
  if (Array.isArray(value)) return value;
  if (Array.isArray(value?.items)) return value.items;
  if (Array.isArray(value?.data)) return value.data;
  if (Array.isArray(value?.symbols)) return value.symbols;
  if (Array.isArray(value?.reports)) return value.reports;
  if (Array.isArray(value?.positions)) return value.positions;
  if (Array.isArray(value?.pnl)) return value.pnl;
  return [];
}

export function bidPrice(row) {
  return row?.bid ?? row?.bidPrice;
}

export function askPrice(row) {
  return row?.ask ?? row?.askPrice;
}
