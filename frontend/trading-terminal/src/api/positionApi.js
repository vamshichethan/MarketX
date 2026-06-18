import { mockPositionRows } from '../mockMarket';
import { createClient, withNetworkFallback } from './http';

const positions = createClient(import.meta.env.VITE_POSITION_API || 'http://localhost:8081');

export const positionApi = {
  getPositions: (accountId) => withNetworkFallback(positions.get(`/positions/${accountId}`), () => mockPositionRows()),
  getPosition: (accountId, symbol) => withNetworkFallback(
    positions.get(`/positions/${accountId}/${symbol}`),
    () => mockPositionRows().find((row) => row.symbol === symbol)
  ),
  getAllPositions: () => withNetworkFallback(positions.get('/positions'), () => mockPositionRows())
};
