import { createClient } from './http';

const marketData = createClient(import.meta.env.VITE_MARKET_DATA_API || 'http://localhost:8084');

export const marketDataApi = {
  getLatest: () => marketData.get('/market-data/latest').then((res) => res.data),
  startFeed: () => marketData.post('/market-data/start').then((res) => res.data),
  stopFeed: () => marketData.post('/market-data/stop').then((res) => res.data),
  tick: () => marketData.post('/market-data/tick').then((res) => res.data)
};
