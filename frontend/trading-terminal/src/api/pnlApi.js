import { createClient } from './http';

const pnl = createClient(import.meta.env.VITE_PNL_API || 'http://localhost:8082');

export const pnlApi = {
  getPnl: (accountId) => pnl.get(`/pnl/${accountId}`).then((res) => res.data),
  getSymbolPnl: (accountId, symbol) => pnl.get(`/pnl/${accountId}/${symbol}`).then((res) => res.data),
  publishMarketPrice: (payload) => pnl.post('/market-prices/publish', payload).then((res) => res.data)
};
