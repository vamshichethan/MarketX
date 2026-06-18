package com.marketx.fixgateway.parser;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class FixMessageBuilder {
    private static final DateTimeFormatter FIX_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss");

    public String buildFillReport(
            String targetCompId,
            String clOrdId,
            String execId,
            String symbol,
            String side,
            int orderQty,
            BigDecimal lastPx,
            int lastQty,
            LocalDateTime transactTime
    ) {
        return base(targetCompId, clOrdId, execId)
                + "150=2|39=2|"
                + "55=" + symbol + "|"
                + "54=" + side + "|"
                + "38=" + orderQty + "|"
                + "14=" + orderQty + "|"
                + "151=0|"
                + "31=" + lastPx + "|"
                + "32=" + lastQty + "|"
                + "60=" + format(transactTime) + "|";
    }

    public String buildRejectedReport(
            String targetCompId,
            String clOrdId,
            String execId,
            String symbol,
            String side,
            String reason,
            LocalDateTime transactTime
    ) {
        return base(targetCompId, clOrdId, execId)
                + "150=8|39=8|"
                + "55=" + symbol + "|"
                + "54=" + side + "|"
                + "38=0|14=0|151=0|"
                + "58=" + reason + "|"
                + "60=" + format(transactTime) + "|";
    }

    public String buildCancelledReport(
            String targetCompId,
            String clOrdId,
            String execId,
            String symbol,
            String side,
            String reason,
            LocalDateTime transactTime
    ) {
        return base(targetCompId, clOrdId, execId)
                + "150=4|39=4|"
                + "55=" + symbol + "|"
                + "54=" + side + "|"
                + "38=0|14=0|151=0|"
                + "58=" + reason + "|"
                + "60=" + format(transactTime) + "|";
    }

    public String format(LocalDateTime time) {
        return (time == null ? LocalDateTime.now() : time).format(FIX_TIME);
    }

    private String base(String targetCompId, String clOrdId, String execId) {
        return "8=FIX.4.4|35=8|49=MARKETX|56=" + targetCompId + "|11=" + clOrdId + "|17=" + execId + "|";
    }
}
