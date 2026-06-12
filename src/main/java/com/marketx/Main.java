package com.marketx;

import com.marketx.cli.ExchangeCLI;
import com.marketx.engine.MatchingEngine;

public class Main {
    public static void main(String[] args) {
        MatchingEngine matchingEngine = new MatchingEngine();
        ExchangeCLI cli = new ExchangeCLI(matchingEngine);
        cli.start();
    }
}
