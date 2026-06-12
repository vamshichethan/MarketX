import { createClient } from './http';

const oms = createClient(import.meta.env.VITE_OMS_API || 'http://localhost:8080');

export const omsApi = {
  submitOrder: (payload) => oms.post('/orders', payload).then((res) => res.data),
  getOrder: (orderId) => oms.get(`/orders/${orderId}`).then((res) => res.data),
  listOrders: (params = {}) => oms.get('/orders', { params }).then((res) => res.data),
  getOrderBook: (symbol) => oms.get(`/order-book/${symbol}`).then((res) => res.data),
  getTrades: (params = {}) => oms.get('/trades', { params }).then((res) => res.data)
};
