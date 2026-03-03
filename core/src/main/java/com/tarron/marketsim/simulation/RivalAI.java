package com.tarron.marketsim.simulation;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tarron.marketsim.model.Item;
import com.tarron.marketsim.model.Shop;

/**
 * RivalAI
 *
 * Responsibilities:
 * - Stocks the rival shop during BUY using demand observed in the previous round.
 *
 * Inputs:
 * - lastDesiredByItemName: per-item desired counts from the prior round (RoundManager snapshot)
 * - ItemCatalog: item list and vendor acquisition costs
 *
 * Strategy:
 * 1) If there is no usable demand history, buy a starter mix from the catalog.
 * 2) Otherwise:
 *    - Exploration: buy 1 unit of each demanded item when affordable
 *    - Greedy fill: spend remaining cash on items with the best score:
 *      demandShare * (price / vendorCost)
 *
 * Notes:
 * - This AI optimizes stocking, not shop choice or item choice during selling.
 * - expectedRevenue is optimistic (assumes every purchased unit sells at full price).
 */
public class RivalAI {

    private static final double EPS = 1e-9;

    // ============================================================
    // Result (returned to RoundManager / HUD)
    // ============================================================

    public static class Result {
        public final double remainingCash;
        public final String strategyLabel;
        public final double expectedRevenue;

        // Planned stock counts by item name.
        public final Map<String, Integer> plannedByItemName;

        public Result(
                double remainingCash,
                String strategyLabel,
                double expectedRevenue,
                Map<String, Integer> plannedByItemName
        ) {
            this.remainingCash = remainingCash;
            this.strategyLabel = strategyLabel;
            this.expectedRevenue = expectedRevenue;
            this.plannedByItemName = (plannedByItemName == null)
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new HashMap<>(plannedByItemName));
        }
    }

    // ============================================================
    // Public API
    // ============================================================

    public Result stockForBuyPhase(
            Shop rivalShop,
            double startingCash,
            Map<String, Integer> lastDesiredByItemName,
            ItemCatalog catalog
    ) {
        if (rivalShop == null) {
            return new Result(startingCash, "EMPTY", 0.0, Collections.emptyMap());
        }

        rivalShop.clearInventory();

        Map<String, Integer> demand = sanitizeDemand(lastDesiredByItemName);
        int totalDemand = sumDemand(demand);

        Map<String, Item> nameToItem = buildNameToItem(catalog);

        // No usable demand history: fallback to starter mix.
        if (totalDemand <= 0 || nameToItem.isEmpty()) {
            PlanOutcome starter = buyCatalogStarterMix(rivalShop, startingCash, catalog);
            return new Result(
                    starter.cashLeft,
                    "STARTER_MIX",
                    starter.expectedRevenue,
                    starter.planCounts
            );
        }

        double cash = startingCash;
        Map<String, Integer> planCounts = new HashMap<>();
        double expectedRevenue = 0.0;

        // Exploration: buy one of each demanded item if affordable.
        for (Map.Entry<String, Integer> e : demand.entrySet()) {
            String name = e.getKey();
            int wanted = (e.getValue() == null) ? 0 : e.getValue();
            if (wanted <= 0) continue;

            Item it = nameToItem.get(name);
            if (it == null) continue;

            double cost = vendorCostSafe(catalog, it);
            if (!isFinitePositive(cost)) continue;

            if (cash + EPS >= cost) {
                buyOne(rivalShop, it);
                inc(planCounts, name);

                cash -= cost;
                expectedRevenue += it.getPrice();

                demand.put(name, Math.max(0, wanted - 1));
                totalDemand = Math.max(0, totalDemand - 1);
            }
        }

        // Greedy fill: repeatedly buy the best-scoring demanded item while affordable.
        while (true) {
            Pick best = pickBestAffordableItem(demand, totalDemand, cash, catalog, nameToItem);
            if (best == null) break;

            buyOne(rivalShop, best.item);
            inc(planCounts, best.name);

            cash -= best.cost;
            expectedRevenue += best.item.getPrice();

            int remaining = Math.max(0, demand.getOrDefault(best.name, 0) - 1);
            demand.put(best.name, remaining);
            totalDemand = Math.max(0, totalDemand - 1);
        }

        return new Result(
                cash,
                labelFromPlan(planCounts),
                expectedRevenue,
                planCounts
        );
    }

    // ============================================================
    // Planning helpers
    // ============================================================

    private static class Pick {
        final String name;
        final Item item;
        final double cost;
        final double score;

        Pick(String name, Item item, double cost, double score) {
            this.name = name;
            this.item = item;
            this.cost = cost;
            this.score = score;
        }
    }

    private Pick pickBestAffordableItem(
            Map<String, Integer> demand,
            int totalDemand,
            double cash,
            ItemCatalog catalog,
            Map<String, Item> nameToItem
    ) {
        Item bestItem = null;
        String bestName = null;
        double bestScore = -1.0;
        double bestCost = 0.0;

        for (Map.Entry<String, Integer> e : demand.entrySet()) {
            String name = e.getKey();
            int remainingDemand = (e.getValue() == null) ? 0 : e.getValue();
            if (remainingDemand <= 0) continue;

            Item it = nameToItem.get(name);
            if (it == null) continue;

            double cost = vendorCostSafe(catalog, it);
            if (!isFinitePositive(cost)) continue;
            if (cash + EPS < cost) continue;

            double demandShare = (totalDemand <= 0) ? 0.0 : (remainingDemand / (double) totalDemand);
            double marginScore = it.getPrice() / cost;

            double score = demandShare * marginScore;

            if (score > bestScore) {
                bestScore = score;
                bestItem = it;
                bestName = name;
                bestCost = cost;
            }
        }

        if (bestItem == null) return null;
        return new Pick(bestName, bestItem, bestCost, bestScore);
    }

    // ============================================================
    // Demand mapping
    // ============================================================

    private Map<String, Integer> sanitizeDemand(Map<String, Integer> lastDesiredByItemName) {
        Map<String, Integer> out = new HashMap<>();
        if (lastDesiredByItemName == null) return out;

        for (Map.Entry<String, Integer> e : lastDesiredByItemName.entrySet()) {
            String name = e.getKey();
            int v = (e.getValue() == null) ? 0 : e.getValue();
            if (name != null && v > 0) out.put(name, v);
        }
        return out;
    }

    private int sumDemand(Map<String, Integer> demand) {
        int total = 0;
        for (Integer v : demand.values()) {
            if (v != null && v > 0) total += v;
        }
        return total;
    }

    private Map<String, Item> buildNameToItem(ItemCatalog catalog) {
        Map<String, Item> map = new HashMap<>();
        if (catalog == null) return map;

        List<Item> items = catalog.getItems();
        if (items == null) return map;

        for (Item it : items) {
            if (it == null) continue;
            String name = it.getName();
            if (name == null) continue;
            map.put(name, it);
        }
        return map;
    }

    // ============================================================
    // Fallback stocking: starter mix
    // ============================================================

    private static class PlanOutcome {
        final double cashLeft;
        final double expectedRevenue;
        final Map<String, Integer> planCounts;

        PlanOutcome(double cashLeft, double expectedRevenue, Map<String, Integer> planCounts) {
            this.cashLeft = cashLeft;
            this.expectedRevenue = expectedRevenue;
            this.planCounts = planCounts;
        }
    }

    private PlanOutcome buyCatalogStarterMix(Shop rivalShop, double startingCash, ItemCatalog catalog) {
        if (rivalShop == null || catalog == null) {
            return new PlanOutcome(startingCash, 0.0, new HashMap<>());
        }

        List<Item> items = catalog.getItems();
        if (items == null || items.isEmpty()) {
            return new PlanOutcome(startingCash, 0.0, new HashMap<>());
        }

        double cash = startingCash;
        double expectedRevenue = 0.0;
        Map<String, Integer> planCounts = new HashMap<>();

        int i = 0;
        while (true) {
            Item it = items.get(i % items.size());
            i++;

            if (it == null || it.getName() == null) continue;

            double cost = vendorCostSafe(catalog, it);
            if (!isFinitePositive(cost)) continue;

            if (cash + EPS < cost) break;

            buyOne(rivalShop, it);
            inc(planCounts, it.getName());
            cash -= cost;
            expectedRevenue += it.getPrice();
        }

        return new PlanOutcome(cash, expectedRevenue, planCounts);
    }

    // ============================================================
    // Labeling
    // ============================================================

    private String labelFromPlan(Map<String, Integer> planCounts) {
        if (planCounts == null || planCounts.isEmpty()) return "EMPTY_PLAN";
        return "DEMAND_DRIVEN";
    }

    // ============================================================
    // Utilities
    // ============================================================

    private static void buyOne(Shop shop, Item item) {
        shop.addItems(java.util.Arrays.asList(item));
    }

    private static void inc(Map<String, Integer> map, String key) {
        if (key == null) return;
        map.put(key, map.getOrDefault(key, 0) + 1);
    }

    private static double vendorCostSafe(ItemCatalog catalog, Item it) {
        if (catalog == null || it == null) return Double.POSITIVE_INFINITY;

        double cost = catalog.getVendorCost(it);
        if (Double.isInfinite(cost) || Double.isNaN(cost)) return Double.POSITIVE_INFINITY;

        return cost;
    }

    private static boolean isFinitePositive(double v) {
        return !Double.isNaN(v) && !Double.isInfinite(v) && v > 0.0;
    }
}