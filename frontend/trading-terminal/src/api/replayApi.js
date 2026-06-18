import { createClient } from './http';

const replay = createClient(import.meta.env.VITE_REPLAY_API || 'http://localhost:8087');

export const replayApi = {
  createSession: (payload) => replay.post('/replay/sessions', payload).then((res) => res.data),
  play: (sessionId) => replay.post(`/replay/sessions/${sessionId}/play`).then((res) => res.data),
  pause: (sessionId) => replay.post(`/replay/sessions/${sessionId}/pause`).then((res) => res.data),
  stop: (sessionId) => replay.post(`/replay/sessions/${sessionId}/stop`).then((res) => res.data),
  updateSpeed: (sessionId, speedMultiplier) => replay.post(`/replay/sessions/${sessionId}/speed`, { speedMultiplier }).then((res) => res.data),
  getSession: (sessionId) => replay.get(`/replay/sessions/${sessionId}`).then((res) => res.data),
  getSessions: () => replay.get('/replay/sessions').then((res) => res.data)
};
