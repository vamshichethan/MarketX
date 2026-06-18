import { Pause, Play, Square, Zap } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { errorMessage } from '../api/http';
import { replayApi } from '../api/replayApi';
import DataTable from '../components/DataTable';
import StatCard from '../components/StatCard';
import { advanceMockReplaySession, createMockReplaySession } from '../mockMarket';

const sampleFile = 'data/replay/sample-aapl-session.csv';
const speeds = [1, 2, 5, 10];

export default function ReplayPage() {
  const [filePath, setFilePath] = useState(sampleFile);
  const [speed, setSpeed] = useState(1);
  const [session, setSession] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  async function fetchSessions() {
    try {
      const response = await replayApi.getSessions();
      setSessions(Array.isArray(response) && response.length ? response : sessions);
      setError('');
    } catch (err) {
      if (!sessions.length) {
        const localSession = createMockReplaySession({ filePath, speedMultiplier: Number(speed) });
        setSessions([localSession]);
        setSession((current) => current || localSession);
      }
      setError(`${errorMessage(err, 'Replay API unavailable')} - using local replay engine`);
    }
  }

  async function refreshSession(sessionId = session?.sessionId) {
    if (!sessionId) return;
    try {
      const response = await replayApi.getSession(sessionId);
      setSession(response);
      setError('');
      fetchSessions();
    } catch (err) {
      setSession((current) => current?.sessionId === sessionId ? advanceMockReplaySession(current) : current);
      setSessions((current) => current.map((item) => item.sessionId === sessionId ? advanceMockReplaySession(item) : item));
      setError(`${errorMessage(err, 'Replay status unavailable')} - using local replay engine`);
    }
  }

  async function createSession() {
    setLoading(true);
    setError('');
    setMessage('');
    try {
      const response = await replayApi.createSession({ filePath, speedMultiplier: Number(speed) });
      setSession(response);
      setMessage(`Created ${response.sessionId}`);
      fetchSessions();
    } catch (err) {
      const localSession = createMockReplaySession({ filePath, speedMultiplier: Number(speed) });
      setSession(localSession);
      setSessions((current) => [localSession, ...current.filter((item) => item.sessionId !== localSession.sessionId)]);
      setMessage(`Created local session ${localSession.sessionId}`);
      setError(`${errorMessage(err, 'Create replay session failed')} - using local replay engine`);
    } finally {
      setLoading(false);
    }
  }

  async function runAction(action, label) {
    if (!session?.sessionId) {
      setError('Create or select a replay session first');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const response = await action(session.sessionId);
      setSession(response);
      setMessage(label);
      fetchSessions();
    } catch (err) {
      const nextStatus = label.includes('started') ? 'RUNNING' : label.includes('paused') ? 'PAUSED' : 'STOPPED';
      const updated = { ...session, status: nextStatus, updatedAt: new Date().toLocaleTimeString() };
      setSession(updated);
      setSessions((current) => current.map((item) => item.sessionId === updated.sessionId ? updated : item));
      setMessage(`${label} locally`);
      setError(`${errorMessage(err, `${label} failed`)} - using local replay engine`);
    } finally {
      setLoading(false);
    }
  }

  async function changeSpeed(nextSpeed) {
    setSpeed(nextSpeed);
    if (!session?.sessionId) return;
    setLoading(true);
    try {
      const response = await replayApi.updateSpeed(session.sessionId, nextSpeed);
      setSession(response);
      setMessage(`Speed set to ${nextSpeed}x`);
      fetchSessions();
    } catch (err) {
      const updated = { ...session, speedMultiplier: nextSpeed, updatedAt: new Date().toLocaleTimeString() };
      setSession(updated);
      setSessions((current) => current.map((item) => item.sessionId === updated.sessionId ? updated : item));
      setMessage(`Speed set to ${nextSpeed}x locally`);
      setError(`${errorMessage(err, 'Speed update failed')} - using local replay engine`);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    fetchSessions();
  }, []);

  useEffect(() => {
    if (!session?.sessionId) return undefined;
    const id = setInterval(() => {
      refreshSession(session.sessionId);
      setSession((current) => {
        const updated = advanceMockReplaySession(current);
        setSessions((items) => items.map((item) => item.sessionId === updated?.sessionId ? updated : item));
        return updated;
      });
    }, 1000);
    return () => clearInterval(id);
  }, [session?.sessionId]);

  const progress = useMemo(() => {
    if (!session?.totalTicks) return 0;
    return Math.min(100, Math.round((session.currentIndex / session.totalTicks) * 100));
  }, [session]);

  const columns = [
    { key: 'sessionId', label: 'Session' },
    { key: 'status', label: 'Status', render: (row) => <span className={statusClass(row.status)}>{row.status}</span> },
    { key: 'speedMultiplier', label: 'Speed', align: 'right', render: (row) => <span className="font-mono">{row.speedMultiplier}x</span> },
    { key: 'currentIndex', label: 'Index', align: 'right', render: (row) => <span className="font-mono">{row.currentIndex} / {row.totalTicks}</span> },
    { key: 'lastPrice', label: 'Last', align: 'right', render: (row) => <span className="font-mono text-amber-300">{row.lastPrice ? Number(row.lastPrice).toFixed(2) : '--'}</span> },
    { key: 'replayedVolume', label: 'Replay Vol', align: 'right', render: (row) => <span className="font-mono">{Number(row.replayedVolume || 0).toLocaleString()}</span> },
    { key: 'fileName', label: 'File', render: (row) => <span className="font-mono text-xs text-slate-400">{row.fileName}</span> }
  ];

  return (
    <div className="space-y-4">
      <div className="grid gap-4 xl:grid-cols-[420px_1fr]">
        <section className="rounded border border-slate-800 bg-slate-900 p-4">
          <h2 className="mb-4 text-sm font-semibold uppercase text-slate-300">Historical Session</h2>
          <div className="space-y-3">
            <label className="text-xs text-slate-400">
              CSV File
              <select value={filePath} onChange={(e) => setFilePath(e.target.value)} className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 font-mono text-slate-100 outline-none focus:border-amber-400">
                <option value={sampleFile}>{sampleFile}</option>
              </select>
            </label>
            <label className="text-xs text-slate-400">
              Speed
              <select value={speed} onChange={(e) => changeSpeed(Number(e.target.value))} className="mt-1 w-full rounded border border-slate-700 bg-slate-950 px-3 py-2 font-mono text-slate-100 outline-none focus:border-amber-400">
                {speeds.map((value) => <option key={value} value={value}>{value}x</option>)}
              </select>
            </label>
            <button disabled={loading} onClick={createSession} className="flex w-full items-center justify-center gap-2 rounded bg-amber-400 px-3 py-2 text-sm font-semibold text-slate-950 hover:bg-amber-300 disabled:opacity-50">
              <Zap size={16} />
              Create Session
            </button>
          </div>
        </section>

        <section className="rounded border border-slate-800 bg-slate-900 p-4">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-sm font-semibold uppercase text-slate-300">Replay Controls</h2>
            {session?.sessionId && <span className="font-mono text-xs text-amber-300">{session.sessionId}</span>}
          </div>
          <div className="grid gap-3 md:grid-cols-4">
            <StatCard title="Status" value={session?.status || '--'} variant={variantFor(session?.status)} />
            <StatCard title="Speed" value={session ? `${session.speedMultiplier}x` : `${speed}x`} />
            <StatCard title="Current Index" value={session ? `${session.currentIndex}` : '--'} />
            <StatCard title="Last Replay Price" value={session?.lastPrice ? Number(session.lastPrice).toFixed(2) : '--'} />
          </div>
          <div className="mt-4 h-3 overflow-hidden rounded bg-slate-950">
            <div className="h-full bg-amber-400 transition-all" style={{ width: `${progress}%` }} />
          </div>
          <div className="mt-2 flex justify-between text-xs text-slate-400">
            <span>Progress</span>
            <span className="font-mono">{progress}%</span>
          </div>
          <div className="mt-4 flex flex-wrap gap-2">
            <button onClick={() => runAction(replayApi.play, 'Replay started')} className="flex items-center gap-2 rounded border border-emerald-800 px-3 py-2 text-sm text-emerald-300 hover:bg-emerald-950/30"><Play size={15} />Play</button>
            <button onClick={() => runAction(replayApi.pause, 'Replay paused')} className="flex items-center gap-2 rounded border border-amber-800 px-3 py-2 text-sm text-amber-300 hover:bg-amber-950/30"><Pause size={15} />Pause</button>
            <button onClick={() => runAction(replayApi.stop, 'Replay stopped')} className="flex items-center gap-2 rounded border border-red-800 px-3 py-2 text-sm text-red-300 hover:bg-red-950/30"><Square size={15} />Stop</button>
          </div>
          {(message || error) && <div className={`mt-3 text-xs ${error ? 'text-red-300' : 'text-slate-400'}`}>{error || message}</div>}
        </section>
      </div>

      <section>
        <h2 className="mb-2 text-sm font-semibold uppercase text-slate-300">Replay Sessions</h2>
        {error && <div className="mb-3 rounded border border-amber-900 bg-amber-950/20 p-3 text-xs text-amber-200">{error}</div>}
        <DataTable columns={columns} rows={sessions} emptyText="No replay sessions" />
      </section>
    </div>
  );
}

function statusClass(status) {
  if (status === 'RUNNING') return 'font-mono text-emerald-300';
  if (status === 'PAUSED') return 'font-mono text-amber-300';
  if (status === 'STOPPED') return 'font-mono text-red-300';
  if (status === 'COMPLETED') return 'font-mono text-sky-300';
  return 'font-mono text-slate-300';
}

function variantFor(status) {
  if (status === 'RUNNING') return 'positive';
  if (status === 'PAUSED') return 'warning';
  if (status === 'STOPPED') return 'negative';
  return 'neutral';
}
