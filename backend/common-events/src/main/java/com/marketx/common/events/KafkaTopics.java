package com.marketx.common.events;

public final class KafkaTopics {
    public static final String ORDERS_SUBMITTED = "orders.submitted";
    public static final String ORDERS_RISK_APPROVED = "orders.risk.approved";
    public static final String ORDERS_RISK_REJECTED = "orders.risk.rejected";
    public static final String ORDERS_CANCEL_REQUESTED = "orders.cancel.requested";
    public static final String ORDERS_CANCELLED = "orders.cancelled";
    public static final String TRADES_EXECUTED = "trades.executed";
    public static final String MARKET_DATA = "market-data";
    public static final String MARKET_PRICES = "market.prices";
    public static final String FIX_INBOUND = "fix.inbound";
    public static final String FIX_EXECUTION_REPORTS = "fix.execution.reports";

    private KafkaTopics() {
    }
}
