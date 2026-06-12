import Header from './Header';
import Sidebar from './Sidebar';

export default function Layout({ activePage, onNavigate, symbol, onSymbolChange, children }) {
  return (
    <div className="flex min-h-screen bg-slate-950 text-slate-100">
      <Sidebar activePage={activePage} onNavigate={onNavigate} />
      <div className="flex min-w-0 flex-1 flex-col">
        <Header symbol={symbol} onSymbolChange={onSymbolChange} />
        <main className="min-w-0 flex-1 overflow-auto p-4">{children}</main>
      </div>
    </div>
  );
}
