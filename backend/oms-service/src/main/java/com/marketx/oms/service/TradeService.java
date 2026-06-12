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

    public TradeService(TradeRepository tradeRepository, ExchangeClient exchangeClient) {
        this.tradeRepository = tradeRepository;
        this.exchangeClient = exchangeClient;
    }

    public void syncTradesFromExchange() {
        for (TradeResponse trade : exchangeClient.getTrades(null)) {
            tradeRepository.findByTradeId(trade.tradeId()).orElseGet(() -> tradeRepository.save(toEntity(trade)));
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
                entity.getAggressorSide(),
                entity.getExecutedAt()
        );
    }
}
