import { mockFixAck, mockInitialFixReports } from '../mockMarket';
import { createClient, withNetworkFallback } from './http';

const fix = createClient(import.meta.env.VITE_FIX_API || 'http://localhost:8086');

export const fixApi = {
  sendMessage: (message) => withNetworkFallback(fix.post('/fix/messages', { message }), () => mockFixAck(message)),
  getReports: () => withNetworkFallback(fix.get('/fix/reports'), () => mockInitialFixReports())
};
