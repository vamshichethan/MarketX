package com.marketx.cli;

import com.marketx.engine.MatchingEngine;
import com.marketx.engine.OrderBook;
import com.marketx.model.Order;
import com.marketx.model.OrderSide;
import com.marketx.model.OrderType;
import com.marketx.model.Trade;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

public class ExchangeCLI {
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
                printOrderBook(command.getSymbol());
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

        Order order = result.getOrder();
        if (order.getOrderType() == OrderType.MARKET
                && result.getTrades().isEmpty()
                && result.getRemainingQuantity() > 0) {
            output.println(formatUnfilledMarketOrder(order, true));
            return;
        }

        output.println(formatAcceptedOrder(order, command.getQuantity()));

        for (Trade trade : result.getTrades()) {
            output.println(formatTradeExecuted(trade));
        }

        if (order.getOrderType() == OrderType.MARKET && result.getRemainingQuantity() > 0) {
            output.println(formatUnfilledMarketOrder(order, false));
        }
    }

    private void printOrderBook(String symbol) {
        OrderBook orderBook = matchingEngine.getOrCreateOrderBook(symbol);

        output.println("ORDER BOOK: " + orderBook.getSymbol());
        output.println();
        output.println("BIDS:");
        output.println("Price    Qty    OrderId");
        printOrders(orderBook.getBuyOrdersSnapshot());

        output.println();
        output.println("ASKS:");
        output.println("Price    Qty    OrderId");
        printOrders(orderBook.getSellOrdersSnapshot());
    }

    private void printOrders(List<Order> orders) {
        if (orders.isEmpty()) {
            output.println("(empty)");
            return;
        }

        for (Order order : orders) {
            output.printf("%-8s %-6d %d%n",
                    formatPrice(order.getPrice()),
                    order.getQuantity(),
                    order.getOrderId());
        }
    }

    private void printTrades() {
        List<Trade> trades = matchingEngine.getTradeHistory();

        if (trades.isEmpty()) {
            output.println("No trades executed yet.");
            return;
        }

        output.println("TradeId | Symbol | Qty | Price | BuyOrderId | SellOrderId | Time");
        for (Trade trade : trades) {
            output.printf("%-7d | %-6s | %-3d | %-5s | %-10d | %-11d | %s%n",
                    trade.getTradeId(),
                    trade.getSymbol(),
                    trade.getQuantity(),
                    formatPrice(trade.getPrice()),
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
        output.println("TRADES");
        output.println("HELP");
        output.println("EXIT");
    }

    private String formatAcceptedOrder(Order order, int originalQuantity) {
        if (order.getOrderType() == OrderType.MARKET) {
            return String.format("ORDER ACCEPTED: %s %s %s %d",
                    order.getSide(),
                    order.getOrderType(),
                    order.getSymbol(),
                    originalQuantity);
        }

        return String.format("ORDER ACCEPTED: %s %s %s %d @ %s",
                order.getSide(),
                order.getOrderType(),
                order.getSymbol(),
                originalQuantity,
                formatPrice(order.getPrice()));
    }

    private String formatTradeExecuted(Trade trade) {
        return String.format("TRADE EXECUTED: %s %d @ %s",
                trade.getSymbol(),
                trade.getQuantity(),
                formatPrice(trade.getPrice()));
    }

    private String formatUnfilledMarketOrder(Order order, boolean noTradesExecuted) {
        String oppositeSide = order.getSide() == OrderSide.BUY ? "sell" : "buy";

        if (noTradesExecuted) {
            return "ORDER NOT FILLED: No matching " + oppositeSide + " orders available";
        }

        return String.format("ORDER PARTIALLY FILLED: %d remaining quantity not filled",
                order.getQuantity());
    }

    private String formatPrice(BigDecimal price) {
        return price.stripTrailingZeros().toPlainString();
    }
}
