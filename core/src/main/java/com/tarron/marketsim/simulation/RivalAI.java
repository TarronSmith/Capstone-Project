package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.List;

import com.tarron.marketsim.model.Item;
import com.tarron.marketsim.model.Shop;

public class RivalAI {

    public static class Result {
        public final double remainingCash;

        // Labels / ratios
        public final String strategyLabel;

        // From last round DESIRED
        public final double demandPremiumRatio;

        // From what the AI actually bought
        public final double stockPremiumRatio;

        // What the AI planned to buy (unit counts)
        public final int plannedCheap;
        public final int plannedPremium;

        // Expected revenue for this plan (based on last desired)
        public final double expectedRevenue;

        public Result(
                double remainingCash,
                String strategyLabel,
                double demandPremiumRatio,
                double stockPremiumRatio,
                int plannedCheap,
                int plannedPremium,
                double expectedRevenue
        ) {
            this.remainingCash = remainingCash;
            this.strategyLabel = strategyLabel;
            this.demandPremiumRatio = demandPremiumRatio;
            this.stockPremiumRatio = stockPremiumRatio;
            this.plannedCheap = plannedCheap;
            this.plannedPremium = plannedPremium;
            this.expectedRevenue = expectedRevenue;
        }
    }

    private final double vendorCheapCost;
    private final double vendorPremiumCost;

    public RivalAI(double vendorCheapCost, double vendorPremiumCost) {
        this.vendorCheapCost = vendorCheapCost;
        this.vendorPremiumCost = vendorPremiumCost;
    }

    public Result stockForBuyPhase(
            Shop rivalShop,
            double startingCash,
            int lastDesiredCheap,
            int lastDesiredPremium,
            Item cheapItem,
            Item premiumItem
    ) {
        rivalShop.clearInventory();

        int totalDesired = lastDesiredCheap + lastDesiredPremium;
        if (totalDesired == 0) {
            double cashLeft = buyStarterMix(rivalShop, startingCash, cheapItem, premiumItem);

            // Starter mix: treat demand ratio as 0.0 since no knowledge
            int cheapCount = rivalShop.getQuantity(cheapItem);
            int premCount = rivalShop.getQuantity(premiumItem);
            double stockPremRatio = ratio(premCount, cheapCount + premCount);

            String label = labelFromMix(cheapCount, premCount);
            return new Result(
                    cashLeft,
                    "UNKNOWN (starter mix)",
                    0.0,
                    stockPremRatio,
                    cheapCount,
                    premCount,
                    0.0
            );
        }

        double demandPremiumRatio = lastDesiredPremium / (double) totalDesired;

        // Search best plan under budget to maximize expected revenue.
        Plan best = findBestPlan(startingCash, lastDesiredCheap, lastDesiredPremium, cheapItem, premiumItem);

        // Apply plan to inventory
        addN(rivalShop, cheapItem, best.cheapCount);
        addN(rivalShop, premiumItem, best.premiumCount);

        double spent = best.cheapCount * vendorCheapCost + best.premiumCount * vendorPremiumCost;
        double remaining = startingCash - spent;

        double stockPremiumRatio = ratio(best.premiumCount, best.cheapCount + best.premiumCount);

        String label = labelFromMix(best.cheapCount, best.premiumCount);

        return new Result(
                remaining,
                label,
                demandPremiumRatio,
                stockPremiumRatio,
                best.cheapCount,
                best.premiumCount,
                best.expectedRevenue
        );
    }

    // -------------------- Internal helpers --------------------

    private static class Plan {
        int cheapCount;
        int premiumCount;
        double expectedRevenue;

        Plan(int cheapCount, int premiumCount, double expectedRevenue) {
            this.cheapCount = cheapCount;
            this.premiumCount = premiumCount;
            this.expectedRevenue = expectedRevenue;
        }
    }

    private Plan findBestPlan(
            double cash,
            int desiredCheap,
            int desiredPremium,
            Item cheapItem,
            Item premiumItem
    ) {
        int maxPremium = (int) Math.floor(cash / vendorPremiumCost);
        Plan best = new Plan(0, 0, -1);

        for (int p = 0; p <= maxPremium; p++) {
            double cashLeft = cash - p * vendorPremiumCost;
            int c = (int) Math.floor(cashLeft / vendorCheapCost);

            int expectedPremiumSold = Math.min(p, desiredPremium);
            int expectedCheapSold = Math.min(c, desiredCheap);

            double revenue = expectedPremiumSold * premiumItem.getPrice()
                           + expectedCheapSold * cheapItem.getPrice();

            if (revenue > best.expectedRevenue) {
                best = new Plan(c, p, revenue);
            } else if (Math.abs(revenue - best.expectedRevenue) < 0.0001) {
                int bestUnits = Math.min(best.premiumCount, desiredPremium) + Math.min(best.cheapCount, desiredCheap);
                int units = expectedPremiumSold + expectedCheapSold;

                double bestSpent = best.cheapCount * vendorCheapCost + best.premiumCount * vendorPremiumCost;
                double spent = c * vendorCheapCost + p * vendorPremiumCost;

                if (units > bestUnits) best = new Plan(c, p, revenue);
                else if (units == bestUnits && spent > bestSpent) best = new Plan(c, p, revenue);
            }
        }

        return best;
    }

    private void addN(Shop shop, Item item, int count) {
        if (count <= 0) return;
        List<Item> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) items.add(item);
        shop.addItems(items);
    }

    private String labelFromMix(int cheapCount, int premiumCount) {
        int total = cheapCount + premiumCount;
        if (total == 0) return "EMPTY";

        double unitPremiumRatio = premiumCount / (double) total;

        if (unitPremiumRatio > 0.60) return "PREMIUM-LEAN";
        if (unitPremiumRatio < 0.40) return "CHEAP-LEAN";
        return "BALANCED";
    }

    private double ratio(int numerator, int denom) {
        return (denom == 0) ? 0.0 : (numerator / (double) denom);
    }

    private double buyStarterMix(Shop rivalShop, double startingCash, Item cheapItem, Item premiumItem) {
        double cash = startingCash;
        boolean toggle = true;

        while (cash >= vendorCheapCost) {
            if (toggle && cash >= vendorPremiumCost) {
                cash -= vendorPremiumCost;
                rivalShop.addItems(java.util.Arrays.asList(premiumItem));
            } else {
                cash -= vendorCheapCost;
                rivalShop.addItems(java.util.Arrays.asList(cheapItem));
            }
            toggle = !toggle;
        }
        return cash;
    }
}
