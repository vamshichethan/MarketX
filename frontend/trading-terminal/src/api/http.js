import axios from 'axios';

export function createClient(baseURL) {
  return axios.create({
    baseURL,
    timeout: 5000,
    headers: {
      'Content-Type': 'application/json'
    }
  });
}

export function isNetworkUnavailable(error) {
  return !error?.response
    || error?.code === 'ECONNABORTED'
    || /network error|failed to fetch|timeout/i.test(error?.message || '');
}

export function withNetworkFallback(request, fallback) {
  return request
    .then((res) => res.data)
    .catch((error) => {
      if (!isNetworkUnavailable(error)) throw error;
      return typeof fallback === 'function' ? fallback(error) : fallback;
    });
}

export function errorMessage(error, fallback = 'Request failed') {
  return error?.response?.data?.message
    || error?.response?.data?.error
    || error?.message
    || fallback;
}
