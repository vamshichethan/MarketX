import { mockOrderBook, mockOrderResponse } from '../mockMarket';
import { createClient, withNetworkFallback } from './http';

const oms = createClient(import.meta.env.VITE_OMS_API || 'http://localhost:8080');

export const omsApi = {
  submitOrder: (payload) => withNetworkFallback(oms.post('/orders', payload), () => mockOrderResponse(payload)),
  getOrder: (orderId) => withNetworkFallback(oms.get(`/orders/${orderId}`), {
    orderId,
    status: 'ACCEPTED',
    message: 'Local demo order'
  }),
  listOrders: (params = {}) => withNetworkFallback(oms.get('/orders', { params }), []),
  getOrderBook: (symbol) => withNetworkFallback(oms.get(`/order-book/${symbol}`), () => mockOrderBook(symbol)),
  getTrades: (params = {}) => withNetworkFallback(oms.get('/trades', { params }), [])
};
