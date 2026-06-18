import { mockAnalyticsRows } from '../mockMarket';
import { createClient, withNetworkFallback } from './http';

const analytics = createClient(import.meta.env.VITE_ANALYTICS_API || 'http://localhost:8085');

export const analyticsApi = {
  getDashboard: () => withNetworkFallback(analytics.get('/analytics/dashboard'), () => ({
    symbols: mockAnalyticsRows()
  }))
};
