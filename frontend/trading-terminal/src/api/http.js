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

export function errorMessage(error, fallback = 'Request failed') {
  return error?.response?.data?.message
    || error?.response?.data?.error
    || error?.message
    || fallback;
}
