import { createClient } from './http';

const fix = createClient(import.meta.env.VITE_FIX_API || 'http://localhost:8086');

export const fixApi = {
  sendMessage: (message) => fix.post('/fix/messages', { message }).then((res) => res.data),
  getReports: () => fix.get('/fix/reports').then((res) => res.data)
};
