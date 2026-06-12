package com.marketx.cli;

import com.marketx.model.OrderSide;
import com.marketx.model.OrderType;

import java.math.BigDecimal;

public class CommandParser {
    public ParsedCommand parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            return ParsedCommand.invalid("Command cannot be empty. Type HELP to see available commands.");
        }

        String[] tokens = input.trim().split("\\s+");
        String command = tokens[0].toUpperCase();

        switch (command) {
            case "PLACE":
                return parsePlaceCommand(tokens);
            case "BOOK":
                return parseSymbolCommand(CommandType.BOOK, tokens);
            case "DEPTH":
                return parseSymbolCommand(CommandType.DEPTH, tokens);
            case "CANCEL":
                return parseOrderIdCommand(CommandType.CANCEL, tokens);
            case "MODIFY":
                return parseModifyCommand(tokens);
            case "ORDER":
                return parseOrderIdCommand(CommandType.ORDER, tokens);
            case "REPORTS":
                return parseOrderIdCommand(CommandType.REPORTS, tokens);
            case "TRADES":
                return tokens.length == 1
                        ? ParsedCommand.valid(CommandType.TRADES)
                        : ParsedCommand.invalid("TRADES does not accept extra values.");
            case "HELP":
                return tokens.length == 1
                        ? ParsedCommand.valid(CommandType.HELP)
                        : ParsedCommand.invalid("HELP does not accept extra values.");
            case "EXIT":
                return tokens.length == 1
                        ? ParsedCommand.valid(CommandType.EXIT)
                        : ParsedCommand.invalid("EXIT does not accept extra values.");
            default:
                return ParsedCommand.invalid("Unknown command. Type HELP to see available commands.");
        }
    }

    private ParsedCommand parsePlaceCommand(String[] tokens) {
        if (tokens.length < 5) {
            return ParsedCommand.invalid("Invalid PLACE command. Type HELP to see examples.");
        }

        OrderSide side = parseEnum(OrderSide.class, tokens[1]);
        OrderType orderType = parseEnum(OrderType.class, tokens[2]);

        if (side == null) {
            return ParsedCommand.invalid("Invalid side. Use BUY or SELL.");
        }

        if (orderType == null) {
            return ParsedCommand.invalid("Invalid order type. Use MARKET or LIMIT.");
        }

        String symbol = tokens[3].toUpperCase();

        if (orderType == OrderType.MARKET && tokens.length != 5 && tokens.length != 6) {
            return ParsedCommand.invalid("Market order format: PLACE BUY MARKET AAPL 50");
        }

        if (orderType == OrderType.LIMIT && tokens.length != 6) {
            return ParsedCommand.invalid("Limit order format: PLACE BUY LIMIT AAPL 100 150");
        }

        Integer quantity = parseInteger(tokens[4]);
        if (quantity == null) {
            return ParsedCommand.invalid("Quantity must be a whole number.");
        }

        BigDecimal price = BigDecimal.ZERO;
        if (orderType == OrderType.LIMIT) {
            price = parsePrice(tokens[5]);
            if (price == null) {
                return ParsedCommand.invalid("Limit price must be a valid number.");
            }
        } else if (tokens.length == 6) {
            price = parsePrice(tokens[5]);
            if (price == null) {
                return ParsedCommand.invalid("Market order price must be a valid number when provided.");
            }
        }

        return ParsedCommand.place(side, orderType, symbol, quantity, price);
    }

    private ParsedCommand parseSymbolCommand(CommandType commandType, String[] tokens) {
        if (tokens.length != 2) {
            return ParsedCommand.invalid(commandType + " command format: " + commandType + " AAPL");
        }

        return ParsedCommand.symbol(commandType, tokens[1].toUpperCase());
    }

    private ParsedCommand parseOrderIdCommand(CommandType commandType, String[] tokens) {
        if (tokens.length != 2) {
            return ParsedCommand.invalid(commandType + " command format: " + commandType + " ORD-1");
        }

        return ParsedCommand.orderId(commandType, tokens[1].toUpperCase());
    }

    private ParsedCommand parseModifyCommand(String[] tokens) {
        if (tokens.length != 4) {
            return ParsedCommand.invalid("MODIFY command format: MODIFY ORD-1 200 151");
        }

        Integer quantity = parseInteger(tokens[2]);
        if (quantity == null) {
            return ParsedCommand.invalid("Modified quantity must be a whole number.");
        }

        BigDecimal price = parsePrice(tokens[3]);
        if (price == null) {
            return ParsedCommand.invalid("Modified price must be a valid number.");
        }

        return ParsedCommand.modify(tokens[1].toUpperCase(), quantity, price);
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private BigDecimal parsePrice(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value) {
        try {
            return Enum.valueOf(enumType, value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public enum CommandType {
        PLACE,
        BOOK,
        DEPTH,
        CANCEL,
        MODIFY,
        ORDER,
        REPORTS,
        TRADES,
        HELP,
        EXIT,
        INVALID
    }

    public static class ParsedCommand {
        private final CommandType commandType;
        private final String errorMessage;
        private final OrderSide side;
        private final OrderType orderType;
        private final String symbol;
        private final int quantity;
        private final BigDecimal price;
        private final String orderId;

        private ParsedCommand(
                CommandType commandType,
                String errorMessage,
                OrderSide side,
                OrderType orderType,
                String symbol,
                int quantity,
                BigDecimal price,
                String orderId
        ) {
            this.commandType = commandType;
            this.errorMessage = errorMessage;
            this.side = side;
            this.orderType = orderType;
            this.symbol = symbol;
            this.quantity = quantity;
            this.price = price;
            this.orderId = orderId;
        }

        public static ParsedCommand valid(CommandType commandType) {
            return new ParsedCommand(commandType, null, null, null, null, 0, BigDecimal.ZERO, null);
        }

        public static ParsedCommand place(
                OrderSide side,
                OrderType orderType,
                String symbol,
                int quantity,
                BigDecimal price
        ) {
            return new ParsedCommand(CommandType.PLACE, null, side, orderType, symbol, quantity, price, null);
        }

        public static ParsedCommand symbol(CommandType commandType, String symbol) {
            return new ParsedCommand(commandType, null, null, null, symbol, 0, BigDecimal.ZERO, null);
        }

        public static ParsedCommand orderId(CommandType commandType, String orderId) {
            return new ParsedCommand(commandType, null, null, null, null, 0, BigDecimal.ZERO, orderId);
        }

        public static ParsedCommand modify(String orderId, int quantity, BigDecimal price) {
            return new ParsedCommand(CommandType.MODIFY, null, null, null, null, quantity, price, orderId);
        }

        public static ParsedCommand invalid(String errorMessage) {
            return new ParsedCommand(CommandType.INVALID, errorMessage, null, null, null, 0, BigDecimal.ZERO, null);
        }

        public CommandType getCommandType() {
            return commandType;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public OrderSide getSide() {
            return side;
        }

        public OrderType getOrderType() {
            return orderType;
        }

        public String getSymbol() {
            return symbol;
        }

        public int getQuantity() {
            return quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public String getOrderId() {
            return orderId;
        }
    }
}
