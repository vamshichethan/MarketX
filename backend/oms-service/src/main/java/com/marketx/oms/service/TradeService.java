package com.marketx.oms.service;

import com.marketx.common.events.TradeExecutedEvent;
import com.marketx.oms.dto.TradeResponse;
import com.marketx.oms.entity.TradeEntity;
import com.marketx.oms.exchange.ExchangeClient;
import com.marketx.oms.kafka.TradeEventPublisher;
import com.marketx.oms.metrics.OmsMetricsService;
import com.marketx.oms.repository.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.List;

@Service
public class TradeService {
    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

    private final TradeRepository tradeRepository;
    private final ExchangeClient exchangeClient;
    private final TradeEventPublisher tradeEventPublisher;
    private final OmsMetricsService metricsService;

    public TradeService(
            TradeRepository tradeRepository,
            ExchangeClient exchangeClient,
            TradeEventPublisher tradeEventPublisher,
            OmsMetricsService metricsService
    ) {
        this.tradeRepository = tradeRepository;
        this.exchangeClient = exchangeClient;
        this.tradeEventPublisher = tradeEventPublisher;
        this.metricsService = metricsService;
    }

    public void syncTradesFromExchange() {
        for (TradeResponse trade : exchangeClient.getTrades(null)) {
            tradeRepository.findByTradeId(trade.tradeId()).orElseGet(() -> {
            TradeEntity savedTrade = tradeRepository.save(toEntity(trade));
            tradeEventPublisher.publishTradeExecuted(toTradeExecutedEvent(trade));
            log.info("Trade executed tradeId={} symbol={} quantity={} price={} buyOrderId={} sellOrderId={}",
                    trade.tradeId(),
                    trade.symbol(),
                    trade.quantity(),
                    trade.price(),
                    trade.buyOrderId(),
                    trade.sellOrderId());
            metricsService.recordOrdersFilled(2);
            return savedTrade;
        });
        }
    }

    public void storeTradeEvent(TradeExecutedEvent event) {
        tradeRepository.findByTradeId(event.tradeId()).orElseGet(() -> {
            TradeEntity savedTrade = tradeRepository.save(toEntity(event));
            log.info("Trade executed tradeId={} symbol={} quantity={} price={} buyOrderId={} sellOrderId={}",
                    event.tradeId(),
                    event.symbol(),
                    event.quantity(),
                    event.price(),
                    event.buyOrderId(),
                    event.sellOrderId());
            metricsService.recordOrdersFilled(2);
            return savedTrade;
        });
    }

    public List<TradeResponse> getTrades(String symbol) {
        return tradeRepository.findAll().stream()
                .filter(trade -> symbol == null || symbol.isBlank() || trade.getSymbol().equalsIgnoreCase(symbol))
                .map(this::toResponse)
                .toList();
    }

    private TradeEntity toEntity(TradeResponse response) {
        TradeEntity entity = new TradeEntity();
        entity.setTradeId(response.tradeId());
        entity.setSymbol(response.symbol());
        entity.setQuantity(response.quantity());
        entity.setPrice(response.price());
        entity.setBuyOrderId(response.buyOrderId());
        entity.setSellOrderId(response.sellOrderId());
        entity.setBuyAccountId(response.buyAccountId());
        entity.setSellAccountId(response.sellAccountId());
        entity.setAggressorSide(response.aggressorSide());
        entity.setExecutedAt(response.executedAt());
        return entity;
    }

    private TradeEntity toEntity(TradeExecutedEvent event) {
        TradeEntity entity = new TradeEntity();
        entity.setTradeId(event.tradeId());
        entity.setSymbol(event.symbol());
        entity.setQuantity(event.quantity());
        entity.setPrice(event.price());
        entity.setBuyOrderId(event.buyOrderId());
        entity.setSellOrderId(event.sellOrderId());
        entity.setBuyAccountId(event.buyAccountId());
        entity.setSellAccountId(event.sellAccountId());
        entity.setAggressorSide(com.marketx.oms.enums.OrderSide.valueOf(event.aggressorSide()));
        entity.setExecutedAt(event.executedAt());
        return entity;
    }

    private TradeExecutedEvent toTradeExecutedEvent(TradeResponse trade) {
        return new TradeExecutedEvent(
                UUID.randomUUID().toString(),
                trade.tradeId(),
                trade.symbol(),
                trade.quantity(),
                trade.price(),
                trade.buyOrderId(),
                trade.sellOrderId(),
                trade.buyAccountId(),
                trade.sellAccountId(),
                trade.aggressorSide().name(),
                trade.executedAt()
        );
    }

    private TradeResponse toResponse(TradeEntity entity) {
        return new TradeResponse(
                entity.getTradeId(),
                entity.getSymbol(),
                entity.getQuantity(),
                entity.getPrice(),
                entity.getBuyOrderId(),
                entity.getSellOrderId(),
                entity.getBuyAccountId(),
                entity.getSellAccountId(),
                entity.getAggressorSide(),
                entity.getExecutedAt()
        );
    }
}
