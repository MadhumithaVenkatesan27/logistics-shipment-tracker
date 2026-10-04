import axios from 'axios';

// Environment configuration: set VITE_API_BASE_URL (see .env.example)
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: `${API_BASE_URL}/api`,
  headers: { 'Content-Type': 'application/json' },
  timeout: 10000,
});

export async function getShipments(status) {
  const params = status ? { status } : {};
  const { data } = await api.get('/shipments', { params });
  return data;
}

export async function getShipment(id) {
  const { data } = await api.get(`/shipments/${id}`);
  return data;
}

export async function createShipment(payload) {
  const { data } = await api.post('/shipments', payload);
  return data;
}

export async function updateShipment(id, payload) {
  const { data } = await api.put(`/shipments/${id}`, payload);
  return data;
}

export async function deleteShipment(id) {
  await api.delete(`/shipments/${id}`);
}

/** Turns any axios error into a user-friendly message (never shows raw technical details). */
export function getErrorMessage(error) {
  if (error?.response?.data?.message && error.response.status < 500) {
    return error.response.data.message;
  }
  if (error?.response) {
    return 'The server had a problem handling the request. Please try again later.';
  }
  return 'Unable to reach the server. Please check that the backend is running and try again.';
}

/** Field-level errors returned by the backend ({ fieldName: message }), or {}. */
export function getFieldErrors(error) {
  return error?.response?.data?.fieldErrors || {};
}
