package com.marketx.replay.service;

import com.marketx.replay.dto.ReplayTick;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CsvReplayLoader {
    private static final Logger log = LoggerFactory.getLogger(CsvReplayLoader.class);

    public List<ReplayTick> load(Path filePath) {
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("Replay CSV file not found: " + filePath);
        }

        try {
            AtomicInteger lineNumber = new AtomicInteger(0);
            return Files.lines(filePath)
                    .map(line -> parseLine(line, lineNumber.incrementAndGet()))
                    .filter(row -> row != null)
                    .sorted(Comparator.comparing(ReplayTick::timestamp))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read replay CSV file: " + filePath, exception);
        }
    }

    private ReplayTick parseLine(String line, int lineNumber) {
        if (lineNumber == 1 && line.toLowerCase().startsWith("timestamp,")) {
            return null;
        }
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] values = line.split(",");
        if (values.length != 6) {
            log.warn("Skipping invalid replay CSV row {}: expected 6 columns but found {}", lineNumber, values.length);
            return null;
        }

        try {
            LocalDateTime timestamp = LocalDateTime.parse(values[0].trim());
            String symbol = values[1].trim().toUpperCase();
            BigDecimal price = new BigDecimal(values[2].trim());
            long volume = Long.parseLong(values[3].trim());
            BigDecimal bidPrice = new BigDecimal(values[4].trim());
            BigDecimal askPrice = new BigDecimal(values[5].trim());
            BigDecimal spread = askPrice.subtract(bidPrice);
            return new ReplayTick(timestamp, symbol, price, volume, bidPrice, askPrice, spread);
        } catch (RuntimeException exception) {
            log.warn("Skipping invalid replay CSV row {}: {}", lineNumber, exception.getMessage());
            return null;
        }
    }
}
