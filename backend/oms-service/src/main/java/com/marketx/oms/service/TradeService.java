package com.marketx.oms.service;

import com.marketx.oms.dto.TradeResponse;
import com.marketx.oms.entity.TradeEntity;
import com.marketx.oms.exchange.ExchangeClient;
import com.marketx.oms.repository.TradeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TradeService {
    private final TradeRepository tradeRepository;
    private final ExchangeClient exchangeClient;
    private final com.marketx.oms.client.PositionClient positionClient;

    public TradeService(
            TradeRepository tradeRepository,
            ExchangeClient exchangeClient,
            com.marketx.oms.client.PositionClient positionClient
    ) {
        this.tradeRepository = tradeRepository;
        this.exchangeClient = exchangeClient;
        this.positionClient = positionClient;
    }

    public void syncTradesFromExchange() {
        for (TradeResponse trade : exchangeClient.getTrades(null)) {
            tradeRepository.findByTradeId(trade.tradeId()).orElseGet(() -> {
                TradeEntity savedTrade = tradeRepository.save(toEntity(trade));
                positionClient.notifyTrade(trade);
                return savedTrade;
            });
        }
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
