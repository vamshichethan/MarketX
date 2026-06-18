import { symbols } from './utils';

const symbolMeta = {
  AAPL: { name: 'Apple Inc.', base: 213.42, sector: 'Technology' },
  MSFT: { name: 'Microsoft Corp.', base: 498.21, sector: 'Technology' },
  GOOG: { name: 'Alphabet Class C', base: 177.64, sector: 'Communication Services' },
  NVDA: { name: 'NVIDIA Corp.', base: 143.85, sector: 'Semiconductors' },
  TSLA: { name: 'Tesla Inc.', base: 182.18, sector: 'Consumer Discretionary' },
  JPM: { name: 'JPMorgan Chase', base: 268.72, sector: 'Financials' },
  SPY: { name: 'SPDR S&P 500 ETF', base: 602.47, sector: 'ETF' },
  QQQ: { name: 'Invesco QQQ Trust', base: 528.34, sector: 'ETF' }
};

function wave(seed, scale = 1) {
  const seconds = Date.now() / 1000;
  return (Math.sin(seconds / 7 + seed) + Math.cos(seconds / 13 + seed * 0.7)) * scale;
}

export function mockMarketRows(tick = 0) {
  return symbols.map((symbol, index) => {
    const meta = symbolMeta[symbol];
    const localPulse = Math.sin((tick + index * 3) / 5) * meta.base * 0.0009;
    const move = wave(index + 1, meta.base * 0.0025) + localPulse;
    const price = meta.base + move;
    const spread = Math.max(0.01, meta.base * (0.00012 + (index % 3) * 0.00004));
    const bid = price - spread / 2;
    const ask = price + spread / 2;
    const dayChange = move + wave(index + 4, meta.base * 0.0009);
    const dayChangePct = (dayChange / meta.base) * 100;

    return {
      symbol,
      name: meta.name,
      sector: meta.sector,
      price,
      bid,
      ask,
      spread,
      dayChange,
      dayChangePct,
      volume: Math.round(850000 + index * 212000 + Math.abs(wave(index + 9, 140000))),
      vwap: price - wave(index + 2, meta.base * 0.0007),
      tradeCount: Math.round(2600 + index * 415 + Math.abs(wave(index + 12, 450)))
    };
  });
}

export function mockAnalyticsRows(tick = 0) {
  return mockMarketRows(tick).map((row, index) => ({
    symbol: row.symbol,
    latestPrice: row.price,
    vwap: row.vwap,
    spread: row.spread,
    volume: row.volume,
    tradeCount: row.tradeCount,
    advParticipation: 8 + index * 1.7,
    volatility: 11 + (index % 5) * 3.2
  }));
}

export function mockPnlRows(tick = 0) {
  return mockMarketRows(tick).map((row, index) => {
    const quantity = [1200, -800, 500, 950, -300, 700, 1100, -450][index];
    const averagePrice = row.price - wave(index + 3, row.price * 0.006);
    const unrealizedPnl = (row.price - averagePrice) * quantity;
    const realizedPnl = wave(index + 8, 2400);

    return {
      symbol: row.symbol,
      quantity,
      netQuantity: quantity,
      averagePrice,
      marketPrice: row.price,
      lastMarketPrice: row.price,
      realizedPnl,
      unrealizedPnl,
      totalPnl: realizedPnl + unrealizedPnl,
      positionType: quantity >= 0 ? 'LONG' : 'SHORT',
      updatedAt: new Date().toLocaleTimeString()
    };
  });
}

export function mockPositionRows(tick = 0) {
  return mockPnlRows(tick).map((row, index) => ({
    symbol: row.symbol,
    netQuantity: row.netQuantity,
    averagePrice: row.averagePrice,
    marketValue: row.netQuantity * row.lastMarketPrice,
    dayPnl: row.unrealizedPnl,
    positionType: row.positionType,
    custodian: ['GSCO', 'JPM', 'MSCO', 'BAML'][index % 4],
    updatedAt: row.updatedAt
  }));
}

export function mockOrderBook(symbol) {
  const row = mockMarketRows().find((item) => item.symbol === symbol) || mockMarketRows()[0];
  const tick = row.price > 300 ? 0.05 : 0.01;
  const levels = 10;
  const bids = [];
  const asks = [];

  for (let index = 0; index < levels; index += 1) {
    const depth = Math.round(100 + index * 75 + Math.abs(wave(index + row.symbol.length, 90)));
    bids.push({
      price: row.bid - tick * index,
      quantity: depth,
      venue: ['XNAS', 'ARCX', 'BATS', 'IEX'][index % 4],
      orders: 2 + (index % 5)
    });
    asks.push({
      price: row.ask + tick * index,
      quantity: depth + 40,
      venue: ['XNAS', 'EDGX', 'BATS', 'IEX'][index % 4],
      orders: 1 + (index % 6)
    });
  }

  return {
    symbol,
    bestBid: row.bid,
    bestAsk: row.ask,
    spread: row.spread,
    bids,
    asks,
    lastPrice: row.price,
    imbalance: bids.reduce((sum, item) => sum + item.quantity, 0) - asks.reduce((sum, item) => sum + item.quantity, 0)
  };
}

export function mockOrderResponse(order, transportMessage = '') {
  const id = `MX-${Date.now().toString().slice(-7)}`;
  const accepted = Number(order.quantity) > 0 && (order.type === 'MARKET' || Number(order.price) > 0);

  if (!accepted) {
    return {
      orderId: id,
      status: 'REJECTED',
      message: 'Rejected by pre-trade validation',
      rejectionReasons: ['Quantity and limit price must be positive']
    };
  }

  return {
    orderId: id,
    status: order.type === 'MARKET' ? 'FILLED' : 'ACCEPTED',
    message: transportMessage && !/mock/i.test(transportMessage) ? transportMessage : 'Accepted by OMS, routed through SMART order router',
    venue: order.venue || 'SMART',
    filledQuantity: order.type === 'MARKET' ? Number(order.quantity) : 0,
    leavesQuantity: order.type === 'MARKET' ? 0 : Number(order.quantity),
    avgFillPrice: order.type === 'MARKET' ? Number(order.price || 0) : null,
    routeStatus: order.type === 'MARKET' ? 'DONE' : 'WORKING',
    executionReports: [
      { stage: 'NEW', time: new Date().toLocaleTimeString(), detail: 'Client order received and normalized' },
      { stage: 'RISK', time: new Date().toLocaleTimeString(), detail: 'Pre-trade checks passed: notional, quantity, symbol, account' },
      { stage: 'ROUTE', time: new Date().toLocaleTimeString(), detail: `Sent to ${order.venue || 'SMART'} with ${order.timeInForce || 'DAY'} time in force` }
    ]
  };
}

export function mockIntradaySeries(symbol) {
  const row = mockMarketRows().find((item) => item.symbol === symbol) || mockMarketRows()[0];
  return Array.from({ length: 28 }, (_, index) => {
    const minutes = 9 * 60 + 30 + index * 12;
    const hour = Math.floor(minutes / 60);
    const minute = minutes % 60;
    return {
      time: `${hour}:${String(minute).padStart(2, '0')}`,
      price: row.price + Math.sin(index / 2) * row.price * 0.0018 + Math.cos(index / 5) * row.price * 0.0009,
      volume: Math.round(12000 + index * 2400 + Math.abs(Math.sin(index) * 9000))
    };
  });
}

export function parseFixMessage(message) {
  return message
    .split('|')
    .filter(Boolean)
    .reduce((fields, pair) => {
      const [tag, ...rest] = pair.split('=');
      fields[tag] = rest.join('=');
      return fields;
    }, {});
}

export function mockFixAck(message) {
  const fields = parseFixMessage(message);
  const clOrdId = fields['11'] || `CL-${Date.now().toString().slice(-6)}`;
  const symbol = fields['55'] || 'AAPL';
  const side = fields['54'] === '2' ? 'SELL' : 'BUY';
  const quantity = Number(fields['38'] || 0);
  const price = Number(fields['44'] || mockMarketRows().find((row) => row.symbol === symbol)?.price || 0);
  const messageType = fields['35'] === 'F' ? 'CANCEL_REQUEST' : 'NEW_ORDER_SINGLE';
  const execType = messageType === 'CANCEL_REQUEST' ? '4' : '0';
  const ordStatus = messageType === 'CANCEL_REQUEST' ? '4' : '0';
  const now = new Date().toISOString();
  const execId = `EX-${Date.now().toString().slice(-7)}`;
  const text = messageType === 'CANCEL_REQUEST'
    ? `Cancel accepted for ${fields['41'] || clOrdId}`
    : `${side} ${quantity} ${symbol} accepted at ${price.toFixed(2)}`;

  return {
    clOrdId,
    execId,
    symbol,
    side,
    quantity,
    price,
    messageType,
    status: messageType === 'CANCEL_REQUEST' ? 'CANCELLED' : 'NEW',
    rawFixReport: `8=FIX.4.4|35=8|49=MARKETX|56=${fields['49'] || 'CLIENT'}|11=${clOrdId}|17=${execId}|150=${execType}|39=${ordStatus}|55=${symbol}|54=${fields['54'] || '1'}|38=${quantity}|44=${price.toFixed(2)}|58=${text}|60=${now}|`,
    createdAt: now
  };
}

export function mockInitialFixReports() {
  return [
    mockFixAck('8=FIX.4.4|35=D|49=CLIENT1|56=MARKETX|11=CLORD-1001|55=AAPL|54=1|38=250|40=2|44=213.40|'),
    mockFixAck('8=FIX.4.4|35=D|49=CLIENT2|56=MARKETX|11=CLORD-1002|55=NVDA|54=2|38=150|40=2|44=143.90|')
  ];
}

export function createMockReplaySession({ filePath, speedMultiplier = 1 }) {
  return {
    sessionId: `RPL-${Date.now().toString().slice(-6)}`,
    status: 'CREATED',
    speedMultiplier,
    currentIndex: 0,
    totalTicks: 240,
    fileName: filePath?.split('/').pop() || 'sample-aapl-session.csv',
    filePath,
    symbol: 'AAPL',
    lastPrice: 213.42,
    replayedVolume: 0,
    startedAt: new Date().toLocaleTimeString()
  };
}

export function advanceMockReplaySession(session) {
  if (!session || session.status !== 'RUNNING') return session;
  const step = Math.max(1, Number(session.speedMultiplier || 1));
  const currentIndex = Math.min(session.totalTicks, session.currentIndex + step);
  const done = currentIndex >= session.totalTicks;
  const price = 213.42 + Math.sin(currentIndex / 8) * 0.72 + Math.cos(currentIndex / 21) * 0.28;

  return {
    ...session,
    status: done ? 'COMPLETED' : 'RUNNING',
    currentIndex,
    lastPrice: price,
    replayedVolume: session.replayedVolume + step * 1200,
    updatedAt: new Date().toLocaleTimeString()
  };
}
