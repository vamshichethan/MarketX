import { mockPnlRows } from '../mockMarket';
import { createClient, withNetworkFallback } from './http';

const pnl = createClient(import.meta.env.VITE_PNL_API || 'http://localhost:8082');

export const pnlApi = {
  getPnl: (accountId) => withNetworkFallback(pnl.get(`/pnl/${accountId}`), () => mockPnlRows()),
  getSymbolPnl: (accountId, symbol) => withNetworkFallback(
    pnl.get(`/pnl/${accountId}/${symbol}`),
    () => mockPnlRows().find((row) => row.symbol === symbol)
  ),
  publishMarketPrice: (payload) => withNetworkFallback(pnl.post('/market-prices/publish', payload), {
    status: 'PUBLISHED',
    ...payload
  })
};
