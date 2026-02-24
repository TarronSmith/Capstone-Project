package com.tarron.marketsim.model;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * DecisionLogic
 *
 * Purpose:
 * - Centralizes customer decision-making rules.
 * - Answers two questions:
 *   1) Which shop should this customer visit? (shop choice)
 *   2) Which item should this customer buy from a list? (item choice)
 *
 * Design notes:
 * - Uses lightweight scoring functions rather than hard rules to keep behavior tunable.
 * - Adds small stochastic noise to reduce deterministic tie behavior.
 * - Reads CustomerProfile memory fields (loyalty + stockout aversion) when selecting shops.
 */
public class DecisionLogic {

	// Spending-type labels used by Customer.getSpendingType()
	private static final String TYPE_VALUE = "Value Buyer";
	private static final String TYPE_CHEAP = "Cheap Buyer";
	private static final String TYPE_CONSP = "Conspicuous Buyer";

	/**
	 * Small randomness to break perfect ties and avoid identical customer clones.
	 * Set to 0.0 for deterministic behavior.
	 */
	private static final double NOISE = 0.05;

	/**
	 * Memory modifiers applied during shop choice:
	 * - LOYALTY_BONUS rewards revisiting the last shop (scaled by profile.loyalty)
	 * - STOCKOUT_PENALTY discourages revisiting a shop where the customer failed last round
	 *   (scaled by profile.stockoutAversion)
	 */
	private static final double LOYALTY_BONUS = 0.75;
	private static final double STOCKOUT_PENALTY = 1.25;

	/**
	 * Sentinel score used when a shop has no affordable items.
	 * Avoids -Infinity interactions once noise is added.
	 */
	private static final double VERY_BAD = -1_000_000.0;

	// ============================================================
	// SHOP CHOICE
	// ============================================================

	/**
	 * Chooses which shop the customer will visit this SELL phase.
	 *
	 * Score components:
	 * 1) Base attraction: score of the best affordable item in each shop
	 * 2) Loyalty modifier: prefers the last visited shop
	 * 3) Stockout modifier: avoids the last shop if it failed previously
	 * 4) Small noise to reduce deterministic ties
	 *
	 * Notes:
	 * - Does not move/spawn customers; it only returns a Shop reference.
	 * - CustomerSpawner/MarketEngine calls this when assigning targetShop.
	 */
	public Shop pickShop(Customer customer, Shop playerShop, Shop rivalShop) {
		if (customer == null) return playerShop;
		if (playerShop == null) return rivalShop;
		if (rivalShop == null) return playerShop;

		CustomerProfile profile = customer.getProfile();
		String lastShop = customer.getLastShopName();
		boolean gotWhatWanted = customer.gotWhatWantedLastTime();

		double playerScore = scoreShopForCustomer(customer, playerShop);
		double rivalScore  = scoreShopForCustomer(customer, rivalShop);

		// Both shops are effectively unavailable (no affordable items).
		// Fall back to last shop when possible, otherwise default to player.
		if (playerScore <= VERY_BAD && rivalScore <= VERY_BAD) {
			if (lastShop != null) {
				if (lastShop.equals(playerShop.getName())) return playerShop;
				if (lastShop.equals(rivalShop.getName())) return rivalShop;
			}
			return playerShop;
		}

		// Loyalty bonus: increases score of the shop visited last time.
		if (lastShop != null) {
			if (lastShop.equals(playerShop.getName())) {
				playerScore += LOYALTY_BONUS * profile.loyalty;
			} else if (lastShop.equals(rivalShop.getName())) {
				rivalScore += LOYALTY_BONUS * profile.loyalty;
			}
		}

		// Stockout penalty: decreases score of the last shop if it failed last round.
		if (!gotWhatWanted && lastShop != null) {
			if (lastShop.equals(playerShop.getName())) {
				playerScore -= STOCKOUT_PENALTY * profile.stockoutAversion;
			} else if (lastShop.equals(rivalShop.getName())) {
				rivalScore -= STOCKOUT_PENALTY * profile.stockoutAversion;
			}
		}

		// Noise reduces predictable ties between equal-scoring shops.
		playerScore += noise();
		rivalScore  += noise();

		if (playerScore > rivalScore) return playerShop;
		if (rivalScore > playerScore) return rivalShop;

		// Exact tie: random fallback (keeps behavior from always favoring one side).
		return ThreadLocalRandom.current().nextBoolean() ? playerShop : rivalShop;
	}

	/**
	 * Computes a shop attractiveness score for a customer.
	 *
	 * Method:
	 * - Find the best affordable item (using pickFromList)
	 * - Score that item by customer type (without noise for the shop base score)
	 */
	private double scoreShopForCustomer(Customer customer, Shop shop) {
		if (shop == null || customer == null) return VERY_BAD;

		List<Item> options = shop.getItemsInStock();
		if (options == null || options.isEmpty()) return VERY_BAD;

		Item best = pickFromList(customer, options);
		if (best == null) return VERY_BAD;

		return scoreByType(customer.getSpendingType(), best.getPrice(), best.getQuality());
	}

	// ============================================================
	// ITEM CHOICE
	// ============================================================

	/**
	 * Picks the single best item from a list, subject to affordability.
	 *
	 * Behavior:
	 * - Filters out items the customer cannot afford
	 * - Scores remaining items using type-specific scoring
	 * - Adds small noise to reduce repeated perfect ties
	 * - Applies deterministic tie-break rules when scores match closely
	 */
	public Item pickFromList(Customer customer, List<Item> items) {
		if (customer == null || items == null || items.isEmpty()) return null;

		double wallet = customer.getWallet();
		String type = customer.getSpendingType();

		Item best = null;
		double bestScore = Double.NEGATIVE_INFINITY;

		for (Item item : items) {
			if (item == null) continue;

			double price = item.getPrice();
			int quality = item.getQuality();

			if (wallet < price) continue;

			double score = scoreByType(type, price, quality);
			score += noise();

			if (best == null || score > bestScore) {
				best = item;
				bestScore = score;
			} else if (Math.abs(score - bestScore) < 1e-9) {
				best = tieBreak(best, item, type);
			}
		}

		return best;
	}

	/**
	 * Legacy/demo API for earlier two-item systems.
	 * Internally delegates to pickFromList to keep behavior consistent.
	 */
	public Item iPick(Customer customer, Item A, Item B) {
		if (A == null && B == null) return null;
		if (A == null) return (customer != null && customer.getWallet() >= B.getPrice()) ? B : null;
		if (B == null) return (customer != null && customer.getWallet() >= A.getPrice()) ? A : null;
		return pickFromList(customer, java.util.Arrays.asList(A, B));
	}

	// ============================================================
	// SCORING MODELS
	// ============================================================

	/**
	 * Dispatches to a scoring function based on spending type label.
	 * Higher score = more preferred.
	 */
	private double scoreByType(String type, double price, int quality) {
		if (TYPE_CHEAP.equals(type)) return cheapScore(price, quality);
		if (TYPE_VALUE.equals(type)) return valueScore(price, quality);
		if (TYPE_CONSP.equals(type)) return conspScore(price, quality);
		return valueScore(price, quality);
	}

	/**
	 * Cheap Buyer:
	 * - Strong reward for low price
	 * - Small reward for quality
	 */
	private double cheapScore(double price, int quality) {
		double priceTerm = 10.0 / (price + 1.0);
		double qualityTerm = 0.25 * quality;
		return priceTerm + qualityTerm;
	}

	/**
	 * Value Buyer:
	 * - Rewards quality/price ratio
	 * - Adds direct quality reward
	 * - Applies a mild price penalty to avoid overpaying
	 */
	private double valueScore(double price, int quality) {
		double valueTerm = (double) quality / (price + 1.0);
		double qualityTerm = 0.75 * quality;
		double pricePenalty = 0.10 * price;
		return (4.0 * valueTerm) + qualityTerm - pricePenalty;
	}

	/**
	 * Conspicuous Buyer:
	 * - Higher price can increase desirability (status signaling)
	 * - Quality still matters strongly
	 */
	private double conspScore(double price, int quality) {
		double prestigeTerm = 0.50 * price;
		double qualityTerm = 1.00 * quality;
		return prestigeTerm + qualityTerm;
	}

	/**
	 * Symmetric noise around 0 used for tie-breaking.
	 */
	private double noise() {
		if (NOISE <= 0.0) return 0.0;
		return ThreadLocalRandom.current().nextDouble(-NOISE, NOISE);
	}

	// ============================================================
	// TIE-BREAK RULES
	// ============================================================

	/**
	 * Deterministic tie-breaker when two items score effectively the same.
	 * The rules depend on the customer type to keep the tie-break consistent with intent.
	 */
	private Item tieBreak(Item currentBest, Item challenger, String type) {
		double pA = currentBest.getPrice();
		double pB = challenger.getPrice();
		int qA = currentBest.getQuality();
		int qB = challenger.getQuality();

		// Cheap Buyer: prefer cheaper, then higher quality.
		if (TYPE_CHEAP.equals(type)) {
			if (pB < pA) return challenger;
			if (pA < pB) return currentBest;
			if (qB > qA) return challenger;
			return currentBest;
		}

		// Conspicuous Buyer: prefer more expensive, then higher quality.
		if (TYPE_CONSP.equals(type)) {
			if (pB > pA) return challenger;
			if (pA > pB) return currentBest;
			if (qB > qA) return challenger;
			return currentBest;
		}

		// Value Buyer: prefer better quality/price ratio, then quality, then cheaper.
		double vA = (double) qA / (pA + 1.0);
		double vB = (double) qB / (pB + 1.0);

		if (vB > vA) return challenger;
		if (vA > vB) return currentBest;

		if (qB > qA) return challenger;
		if (qA > qB) return currentBest;

		if (pB < pA) return challenger;
		return currentBest;
	}
}