import { createClient } from './http';

const analytics = createClient(import.meta.env.VITE_ANALYTICS_API || 'http://localhost:8085');

export const analyticsApi = {
  getDashboard: () => analytics.get('/analytics/dashboard').then((res) => res.data)
};
