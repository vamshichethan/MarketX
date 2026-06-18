import { advanceMockReplaySession, createMockReplaySession } from '../mockMarket';
import { createClient, withNetworkFallback } from './http';

const replay = createClient(import.meta.env.VITE_REPLAY_API || 'http://localhost:8087');
let localSession = createMockReplaySession({ filePath: 'data/replay/sample-aapl-session.csv', speedMultiplier: 1 });

export const replayApi = {
  createSession: (payload) => withNetworkFallback(replay.post('/replay/sessions', payload), () => {
    localSession = createMockReplaySession(payload);
    return localSession;
  }),
  play: (sessionId) => withNetworkFallback(replay.post(`/replay/sessions/${sessionId}/play`), () => {
    localSession = { ...localSession, sessionId, status: 'RUNNING' };
    return localSession;
  }),
  pause: (sessionId) => withNetworkFallback(replay.post(`/replay/sessions/${sessionId}/pause`), () => {
    localSession = { ...localSession, sessionId, status: 'PAUSED' };
    return localSession;
  }),
  stop: (sessionId) => withNetworkFallback(replay.post(`/replay/sessions/${sessionId}/stop`), () => {
    localSession = { ...localSession, sessionId, status: 'STOPPED' };
    return localSession;
  }),
  updateSpeed: (sessionId, speedMultiplier) => withNetworkFallback(replay.post(`/replay/sessions/${sessionId}/speed`, { speedMultiplier }), () => {
    localSession = { ...localSession, sessionId, speedMultiplier };
    return localSession;
  }),
  getSession: (sessionId) => withNetworkFallback(replay.get(`/replay/sessions/${sessionId}`), () => {
    localSession = advanceMockReplaySession({ ...localSession, sessionId });
    return localSession;
  }),
  getSessions: () => withNetworkFallback(replay.get('/replay/sessions'), () => [localSession])
};
