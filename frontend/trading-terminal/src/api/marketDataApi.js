import { mockMarketRows } from '../mockMarket';
import { createClient, withNetworkFallback } from './http';

const marketData = createClient(import.meta.env.VITE_MARKET_DATA_API || 'http://localhost:8084');

export const marketDataApi = {
  getLatest: () => withNetworkFallback(marketData.get('/market-data/latest'), () => mockMarketRows()),
  startFeed: () => withNetworkFallback(marketData.post('/market-data/start'), { status: 'STARTED' }),
  stopFeed: () => withNetworkFallback(marketData.post('/market-data/stop'), { status: 'STOPPED' }),
  tick: () => withNetworkFallback(marketData.post('/market-data/tick'), () => mockMarketRows())
};
