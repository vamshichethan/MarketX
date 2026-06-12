package com.marketx.common.events;

public final class KafkaTopics {
    public static final String ORDERS_SUBMITTED = "orders.submitted";
    public static final String ORDERS_RISK_APPROVED = "orders.risk.approved";
    public static final String ORDERS_RISK_REJECTED = "orders.risk.rejected";
    public static final String TRADES_EXECUTED = "trades.executed";
    public static final String MARKET_PRICES = "market.prices";

    private KafkaTopics() {
    }
}
