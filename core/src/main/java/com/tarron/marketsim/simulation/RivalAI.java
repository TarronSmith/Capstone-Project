package com.tarron.marketsim.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tarron.marketsim.model.Item;
import com.tarron.marketsim.model.Shop;

/**
 * RivalAI
 *
 * Purpose:
 * - Stocks the rival shop during the BUY phase using demand observed in the previous round.
 *
 * Inputs:
 * - lastDesiredByItemName: per-item "desired" counts from the previous round (RoundManager snapshot)
 * - ItemCatalog: defines the available items and vendor costs
 *
 * Strategy:
 * 1) If there is no demand history, buy a simple starter mix from the catalog.
 * 2) Otherwise:
 *    - "Exploration floor": buy 1 unit of each demanded item (if affordable)
 *    - "Greedy fill": spend remaining cash on items that score well on demand share * (price/cost)
 *
 * Notes:
 * - This AI optimizes stocking decisions, not sales decisions.
 * - expectedRevenue is an optimistic proxy (assumes each bought unit sells at its full price).
 */
public class RivalAI {

	// Small epsilon to avoid floating point edge issues in budget checks.
	private static final double EPS = 1e-9;

	// Strategy labeling thresholds (based on cheap vs premium unit ratio).
	private static final double PREMIUM_LEAN_THRESHOLD = 0.60;
	private static final double CHEAP_LEAN_THRESHOLD = 0.40;

	// ------------------------------------------------------------
	// Result (returned to RoundManager / HUD)
	// ------------------------------------------------------------
	public static class Result {
		public final double remainingCash;

		// Simple label describing the final mix (cheap vs premium leaning).
		public final String strategyLabel;

		// Legacy demo ratios (tracked for HUD continuity).
		public final double demandPremiumRatio;
		public final double stockPremiumRatio;

		// Legacy demo planned counts (only counts the two demo anchor items).
		public final int plannedCheap;
		public final int plannedPremium;

		// Optimistic estimate based on price of each planned unit.
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

	public RivalAI(double vendorCheapCost, double vendorPremiumCost) {
		// Kept for backwards compatibility with older constructors.
		// Vendor costs are now read from ItemCatalog instead of stored per-type here.
	}

	// ============================================================
	// Public API
	// ============================================================

	/**
	 * Stocks the rival shop for the BUY phase.
	 *
	 * Core output:
	 * - Rival shop inventory is rebuilt from scratch (clears existing inventory first).
	 * - Returns a Result summary for HUD/diagnostics.
	 */
	public Result stockForBuyPhase(
			Shop rivalShop,
			double startingCash,
			Map<String, Integer> lastDesiredByItemName,
			ItemCatalog catalog,
			Item cheapItem,   
			Item premiumItem    
			) {
		if (rivalShop == null) {
			return new Result(startingCash, "EMPTY", 0.0, 0.0, 0, 0, 0.0);
		}

		rivalShop.clearInventory();

		Map<String, Integer> demand = sanitizeDemand(lastDesiredByItemName);
		int totalDemand = sumDemand(demand);

		Map<String, Item> nameToItem = buildNameToItem(catalog);

		// No usable demand history: fallback to starter mix.
		if (totalDemand <= 0 || nameToItem.isEmpty()) {
			double cashLeft = buyCatalogStarterMix(rivalShop, startingCash, catalog);

			int cheapCount = (cheapItem == null) ? 0 : rivalShop.getQuantity(cheapItem);
			int premCount  = (premiumItem == null) ? 0 : rivalShop.getQuantity(premiumItem);

			double stockPremRatio = ratio(premCount, cheapCount + premCount);

			return new Result(
					cashLeft,
					"UNKNOWN (catalog starter mix)",
					0.0,
					stockPremRatio,
					cheapCount,
					premCount,
					0.0
					);
		}

		// Legacy metric: how much of demand was for the premium anchor item.
		double demandPremiumRatio = computeDemandPremiumRatio(demand, premiumItem);

		// Build a plan.
		double cash = startingCash;
		Map<String, Integer> planCounts = new HashMap<>();
		double expectedRevenue = 0.0;

		// (1) Exploration floor: buy one of each demanded item if affordable.
		for (Map.Entry<String, Integer> e : demand.entrySet()) {
			String name = e.getKey();
			int wanted = (e.getValue() == null) ? 0 : e.getValue();
			if (wanted <= 0) continue;

			Item it = nameToItem.get(name);
			if (it == null) continue;

			double cost = vendorCostSafe(catalog, it);
			if (cost <= 0) continue;

			if (cash + EPS >= cost) {
				buyOne(rivalShop, it);
				inc(planCounts, name);

				cash -= cost;
				expectedRevenue += it.getPrice();

				// Reduce remaining demand after covering one unit.
				demand.put(name, Math.max(0, wanted - 1));
				totalDemand = Math.max(0, totalDemand - 1);
			}
		}

		// (2) Greedy fill: repeatedly buy the best-scoring demanded item while budget allows.
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

		// Legacy planned counts (only for the two demo anchor items).
		int plannedCheap   = (cheapItem == null) ? 0 : rivalShop.getQuantity(cheapItem);
		int plannedPremium = (premiumItem == null) ? 0 : rivalShop.getQuantity(premiumItem);

		double stockPremiumRatio = ratio(plannedPremium, plannedCheap + plannedPremium);
		String label = labelFromPremiumRatio(stockPremiumRatio);

		return new Result(
				cash,
				label,
				demandPremiumRatio,
				stockPremiumRatio,
				plannedCheap,
				plannedPremium,
				expectedRevenue
				);
	}

	// ============================================================
	// Planning helpers
	// ============================================================

	/**
	 * Candidate chosen by greedy planner.
	 */
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

	/**
	 * Selects the best affordable item under remaining demand.
	 *
	 * Score:
	 * - demandShare = remainingDemand / totalDemand
	 * - marginScore = price / vendorCost
	 * - score = demandShare * marginScore
	 */
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
			if (cost <= 0) continue;
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
	// Demand sanitization / mapping
	// ============================================================

	/**
	 * Filters null keys and non-positive counts.
	 */
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

	/**
	 * Builds a lookup for catalog items by name.
	 * The catalog item instances are the authoritative keys for vendor cost lookups.
	 */
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

	/**
	 * Legacy demand ratio: demand share for the premium anchor item name.
	 */
	private double computeDemandPremiumRatio(Map<String, Integer> demand, Item premiumItem) {
		if (premiumItem == null || premiumItem.getName() == null) return 0.0;

		int total = 0;
		int prem = 0;

		for (Map.Entry<String, Integer> e : demand.entrySet()) {
			int v = (e.getValue() == null) ? 0 : e.getValue();
			if (v <= 0) continue;
			total += v;
			if (premiumItem.getName().equals(e.getKey())) prem += v;
		}

		return (total == 0) ? 0.0 : (prem / (double) total);
	}

	// ============================================================
	// Fallback stocking: starter mix
	// ============================================================

	/**
	 * Buys items in catalog order until budget cannot afford the next cost.
	 * This gives a deterministic "some of everything" baseline when no demand exists.
	 */
	private double buyCatalogStarterMix(Shop rivalShop, double startingCash, ItemCatalog catalog) {
		if (rivalShop == null || catalog == null) return startingCash;

		List<Item> items = catalog.getItems();
		if (items == null || items.isEmpty()) return startingCash;

		double cash = startingCash;
		int i = 0;

		while (true) {
			Item it = items.get(i % items.size());
			i++;

			if (it == null) continue;

			double cost = vendorCostSafe(catalog, it);
			if (cost <= 0) continue;

			if (cash + EPS < cost) break;

			buyOne(rivalShop, it);
			cash -= cost;
		}

		return cash;
	}

	// ============================================================
	// Labeling / ratios
	// ============================================================

	private String labelFromPremiumRatio(double unitPremiumRatio) {
		if (unitPremiumRatio > PREMIUM_LEAN_THRESHOLD) return "CATALOG-PREMIUM-LEAN";
		if (unitPremiumRatio < CHEAP_LEAN_THRESHOLD) return "CATALOG-CHEAP-LEAN";
		return "CATALOG-BALANCED";
	}

	private double ratio(int numerator, int denom) {
		return (denom == 0) ? 0.0 : (numerator / (double) denom);
	}

	// ============================================================
	// Small utilities
	// ============================================================

	private static void buyOne(Shop shop, Item item) {
		shop.addItems(java.util.Arrays.asList(item));
	}

	private static void inc(Map<String, Integer> map, String key) {
		map.put(key, map.getOrDefault(key, 0) + 1);
	}

	private static double vendorCostSafe(ItemCatalog catalog, Item it) {
		if (catalog == null || it == null) return Double.POSITIVE_INFINITY;

		double cost = catalog.getVendorCost(it);
		if (Double.isInfinite(cost) || Double.isNaN(cost)) return Double.POSITIVE_INFINITY;

		return cost;
	}
}