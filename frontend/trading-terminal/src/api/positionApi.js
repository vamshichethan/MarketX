import { createClient } from './http';

const positions = createClient(import.meta.env.VITE_POSITION_API || 'http://localhost:8081');

export const positionApi = {
  getPositions: (accountId) => positions.get(`/positions/${accountId}`).then((res) => res.data),
  getPosition: (accountId, symbol) => positions.get(`/positions/${accountId}/${symbol}`).then((res) => res.data),
  getAllPositions: () => positions.get('/positions').then((res) => res.data)
};
