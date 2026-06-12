package com.marketx.cli;

import com.marketx.engine.MatchingEngine;
import com.marketx.engine.OrderBook;
import com.marketx.engine.OrderBookLevel;
import com.marketx.model.ExecutionReport;
import com.marketx.model.Order;
import com.marketx.model.OrderSide;
import com.marketx.model.OrderType;
import com.marketx.model.Trade;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Scanner;

public class ExchangeCLI {
    private static final int DISPLAY_DEPTH_LEVELS = 5;

    private final MatchingEngine matchingEngine;
    private final CommandParser commandParser;
    private final Scanner scanner;
    private final PrintStream output;

    public ExchangeCLI(MatchingEngine matchingEngine) {
        this(matchingEngine, new Scanner(System.in), System.out);
    }

    public ExchangeCLI(MatchingEngine matchingEngine, Scanner scanner, PrintStream output) {
        this.matchingEngine = matchingEngine;
        this.commandParser = new CommandParser();
        this.scanner = scanner;
        this.output = output;
    }

    public void start() {
        output.println("MarketX Exchange Simulator");
        output.println("Type HELP to see available commands.");

        boolean running = true;
        while (running) {
            output.print("> ");

            if (!scanner.hasNextLine()) {
                break;
            }

            CommandParser.ParsedCommand command = commandParser.parse(scanner.nextLine());
            running = handleCommand(command);
        }
    }

    private boolean handleCommand(CommandParser.ParsedCommand command) {
        switch (command.getCommandType()) {
            case PLACE:
                placeOrder(command);
                return true;
            case BOOK:
            case DEPTH:
                printOrderBook(command.getSymbol());
                return true;
            case CANCEL:
                cancelOrder(command.getOrderId());
                return true;
            case MODIFY:
                modifyOrder(command);
                return true;
            case ORDER:
                printOrder(command.getOrderId());
                return true;
            case REPORTS:
                printReports(command.getOrderId());
                return true;
            case TRADES:
                printTrades();
                return true;
            case HELP:
                printHelp();
                return true;
            case EXIT:
                output.println("Goodbye.");
                return false;
            case INVALID:
            default:
                output.println("ERROR: " + command.getErrorMessage());
                return true;
        }
    }

    private void placeOrder(CommandParser.ParsedCommand command) {
        MatchingEngine.OrderPlacementResult result = matchingEngine.placeOrder(
                command.getSide(),
                command.getOrderType(),
                command.getSymbol(),
                command.getQuantity(),
                command.getPrice()
        );

        printPlacementResult(result);
    }

    private void modifyOrder(CommandParser.ParsedCommand command) {
        MatchingEngine.OrderPlacementResult result = matchingEngine.modifyOrder(
                command.getOrderId(),
                command.getQuantity(),
                command.getPrice()
        );

        printPlacementResult(result);
    }

    private void printPlacementResult(MatchingEngine.OrderPlacementResult result) {
        if (result.isRejected()) {
            output.println("ORDER REJECTED: " + result.getMessage());
            return;
        }

        if (!result.isSuccess()) {
            output.println("ERROR: " + result.getMessage());
            return;
        }

        if (result.getOrder().getOrderType() == OrderType.MARKET
                && result.getTrades().isEmpty()
                && result.getRemainingQuantity() > 0) {
            output.println(formatUnfilledMarketOrder(result, true));
            output.println();
            printOrderBook(result.getOrder().getSymbol());
            return;
        }

        output.println(result.isModified() ? formatModifiedOrder(result) : formatAcceptedOrder(result));

        for (Trade trade : result.getTrades()) {
            output.println(formatTradeExecuted(trade));
        }

        if (result.getOrder().getOrderType() == OrderType.MARKET && result.getRemainingQuantity() > 0) {
            output.println(formatUnfilledMarketOrder(result, false));
        }

        output.println();
        printOrderBook(result.getOrder().getSymbol());
    }

    private void cancelOrder(String orderId) {
        MatchingEngine.EngineActionResult result = matchingEngine.cancelOrder(orderId);

        if (!result.isSuccess()) {
            output.println("ERROR: " + result.getMessage());
            return;
        }

        output.println(result.getMessage());
        output.println();
        printOrderBook(result.getOrder().getSymbol());
    }

    private void printOrder(String orderId) {
        Order order = matchingEngine.getOrder(orderId);

        if (order == null) {
            output.println("ORDER NOT FOUND: " + orderId);
            return;
        }

        output.println("OrderId: " + order.getOrderId());
        output.println("Symbol: " + order.getSymbol());
        output.println("Side: " + order.getSide());
        output.println("Type: " + order.getOrderType());
        output.println("Original Quantity: " + order.getOriginalQuantity());
        output.println("Remaining Quantity: " + order.getRemainingQuantity());
        output.println("Price: " + formatDepthPrice(order.getPrice()));
        output.println("Status: " + order.getStatus());
        output.println("Timestamp: " + order.getTimestamp());
    }

    private void printReports(String orderId) {
        List<ExecutionReport> reports = matchingEngine.getExecutionReports(orderId);

        if (reports.isEmpty()) {
            output.println("No execution reports found for " + orderId + ".");
            return;
        }

        output.println("ExecutionId | OrderId | Symbol | Side | Status | ExecQty | ExecPrice | Remaining | Message | Time");
        for (ExecutionReport report : reports) {
            output.printf("%-11s | %-7s | %-6s | %-4s | %-16s | %-7d | %-9s | %-9d | %s | %s%n",
                    report.getExecutionId(),
                    report.getOrderId(),
                    report.getSymbol(),
                    report.getSide(),
                    report.getStatus(),
                    report.getExecutedQuantity(),
                    formatReportPrice(report.getExecutedPrice()),
                    report.getRemainingQuantity(),
                    report.getMessage(),
                    report.getTimestamp());
        }
    }

    private void printOrderBook(String symbol) {
        OrderBook orderBook = matchingEngine.getOrCreateOrderBook(symbol);

        output.println("ORDER BOOK: " + orderBook.getSymbol());
        output.println();
        output.println("BIDS:");
        output.println("Price      Quantity");
        printLevels(orderBook.getTopBuyLevels(DISPLAY_DEPTH_LEVELS));

        output.println();
        output.println("ASKS:");
        output.println("Price      Quantity");
        printLevels(orderBook.getTopSellLevels(DISPLAY_DEPTH_LEVELS));

        output.println();
        output.println("BEST BID: " + formatNullablePrice(orderBook.getBestBid()));
        output.println("BEST ASK: " + formatNullablePrice(orderBook.getBestAsk()));
        output.println("SPREAD: " + formatNullablePrice(orderBook.getSpread()));
    }

    private void printLevels(List<OrderBookLevel> levels) {
        if (levels.isEmpty()) {
            output.println("EMPTY");
            return;
        }

        for (OrderBookLevel level : levels) {
            output.printf("%-10s %d%n",
                    formatDepthPrice(level.getPrice()),
                    level.getTotalQuantity());
        }
    }

    private void printTrades() {
        List<Trade> trades = matchingEngine.getTradeHistory();

        if (trades.isEmpty()) {
            output.println("No trades executed yet.");
            return;
        }

        output.println("TradeId | Symbol | Qty | Price | AggressorSide | BuyOrderId | SellOrderId | Time");
        for (Trade trade : trades) {
            output.printf("%-7s | %-6s | %-3d | %-5s | %-13s | %-10s | %-11s | %s%n",
                    trade.getTradeId(),
                    trade.getSymbol(),
                    trade.getQuantity(),
                    formatTradePrice(trade.getPrice()),
                    trade.getAggressorSide(),
                    trade.getBuyOrderId(),
                    trade.getSellOrderId(),
                    trade.getTimestamp());
        }
    }

    private void printHelp() {
        output.println("Available commands:");
        output.println("PLACE BUY LIMIT AAPL 100 150");
        output.println("PLACE SELL LIMIT AAPL 100 150");
        output.println("PLACE BUY MARKET AAPL 50");
        output.println("PLACE SELL MARKET AAPL 50");
        output.println("BOOK AAPL");
        output.println("DEPTH AAPL");
        output.println("CANCEL ORD-1");
        output.println("MODIFY ORD-1 200 151");
        output.println("ORDER ORD-1");
        output.println("REPORTS ORD-1");
        output.println("TRADES");
        output.println("HELP");
        output.println("EXIT");
    }

    private String formatAcceptedOrder(MatchingEngine.OrderPlacementResult result) {
        if (result.getOrder().getOrderType() == OrderType.MARKET) {
            return String.format("ORDER ACCEPTED: %s %s %s %s %d",
                    result.getOrder().getOrderId(),
                    result.getOrder().getSide(),
                    result.getOrder().getOrderType(),
                    result.getOrder().getSymbol(),
                    result.getOrder().getOriginalQuantity());
        }

        return String.format("ORDER ACCEPTED: %s %s %s %s %d @ %s",
                result.getOrder().getOrderId(),
                result.getOrder().getSide(),
                result.getOrder().getOrderType(),
                result.getOrder().getSymbol(),
                result.getOrder().getOriginalQuantity(),
                formatTradePrice(result.getOrder().getPrice()));
    }

    private String formatModifiedOrder(MatchingEngine.OrderPlacementResult result) {
        return String.format("ORDER MODIFIED: %s %s %s %s %d @ %s",
                result.getOrder().getOrderId(),
                result.getOrder().getSide(),
                result.getOrder().getOrderType(),
                result.getOrder().getSymbol(),
                result.getOrder().getOriginalQuantity(),
                formatTradePrice(result.getOrder().getPrice()));
    }

    private String formatTradeExecuted(Trade trade) {
        return String.format("TRADE EXECUTED: %s %d @ %s",
                trade.getSymbol(),
                trade.getQuantity(),
                formatTradePrice(trade.getPrice()));
    }

    private String formatUnfilledMarketOrder(MatchingEngine.OrderPlacementResult result, boolean noTradesExecuted) {
        String oppositeSide = result.getOrder().getSide() == OrderSide.BUY ? "sell" : "buy";

        if (noTradesExecuted) {
            return "ORDER NOT FILLED: No matching " + oppositeSide + " orders available";
        }

        return String.format("ORDER PARTIALLY FILLED: %d remaining quantity not filled",
                result.getRemainingQuantity());
    }

    private String formatTradePrice(BigDecimal price) {
        return price.stripTrailingZeros().toPlainString();
    }

    private String formatDepthPrice(BigDecimal price) {
        return price.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String formatReportPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return "N/A";
        }

        return formatTradePrice(price);
    }

    private String formatNullablePrice(BigDecimal price) {
        if (price == null) {
            return "N/A";
        }

        return formatDepthPrice(price);
    }
}
