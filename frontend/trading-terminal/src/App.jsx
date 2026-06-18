import { useEffect, useState } from 'react';
import Layout from './components/Layout';
import AnalyticsPage from './pages/AnalyticsPage';
import DashboardPage from './pages/DashboardPage';
import FixGatewayPage from './pages/FixGatewayPage';
import MarketDataPage from './pages/MarketDataPage';
import OrderBookPage from './pages/OrderBookPage';
import OrderEntryPage from './pages/OrderEntryPage';
import PnlPage from './pages/PnlPage';
import PositionsPage from './pages/PositionsPage';
import ReplayPage from './pages/ReplayPage';

const pageTitles = {
  dashboard: 'Dashboard',
  'order-entry': 'Order Entry',
  'order-book': 'Order Book',
  positions: 'Positions',
  pnl: 'PnL',
  'market-data': 'Market Data',
  analytics: 'Analytics',
  fix: 'FIX Gateway',
  replay: 'Historical Replay'
};

export default function App() {
  const [activePage, setActivePage] = useState('dashboard');
  const [symbol, setSymbol] = useState('AAPL');
  const [clock, setClock] = useState(new Date());

  useEffect(() => {
    const id = setInterval(() => setClock(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  return (
    <Layout activePage={activePage} onNavigate={setActivePage} symbol={symbol} onSymbolChange={setSymbol}>
      <div className="mb-4 flex items-center justify-between border-b border-slate-800 pb-3">
        <div>
          <h1 className="text-xl font-semibold text-slate-100">{pageTitles[activePage]}</h1>
          <div className="text-xs uppercase text-slate-500">MarketX / SIM+API / {symbol}</div>
        </div>
        <div className="rounded border border-slate-800 bg-slate-900 px-3 py-2 font-mono text-xs text-amber-300">
          {clock.toLocaleTimeString()}
        </div>
      </div>
      {activePage === 'dashboard' && <DashboardPage />}
      {activePage === 'order-entry' && <OrderEntryPage selectedSymbol={symbol} />}
      {activePage === 'order-book' && <OrderBookPage selectedSymbol={symbol} onSymbolChange={setSymbol} />}
      {activePage === 'positions' && <PositionsPage />}
      {activePage === 'pnl' && <PnlPage />}
      {activePage === 'market-data' && <MarketDataPage />}
      {activePage === 'analytics' && <AnalyticsPage />}
      {activePage === 'fix' && <FixGatewayPage />}
      {activePage === 'replay' && <ReplayPage />}
    </Layout>
  );
}
