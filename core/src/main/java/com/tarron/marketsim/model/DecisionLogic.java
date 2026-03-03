package com.tarron.marketsim.model;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * DecisionLogic
 *
 * Responsibilities:
 * - Selects a shop for a customer during SELL.
 * - Selects an item for a customer from a list of options.
 *
 * Inputs:
 * - CustomerProfile drives scoring weights.
 * - Customer memory influences shop choice (loyalty and stockout avoidance).
 *
 * Notes:
 * - Stateless; all state lives on Customer / Shop / Item.
 * - Small noise is used to avoid permanent ties.
 */
public class DecisionLogic {

	// ============================================================
	// Tunables
	// ============================================================

	/** Randomness to break ties. Set to 0.0 for deterministic behavior. */
	private static final double NOISE = 0.05;

	/** Shop-choice modifiers. */
	private static final double LOYALTY_BONUS = 0.75;
	private static final double STOCKOUT_PENALTY = 1.25;

	/** Sentinel score for "no options / no affordable items". */
	private static final double VERY_BAD = -1_000_000.0;

	/** Fallback profile used only if a Customer has a null profile reference. */
	private static final CustomerProfile DEFAULT_PROFILE =
			new CustomerProfile(0.5, 0.5, 0.0, 0.2, 0.2);

	// ============================================================
	// Shop choice
	// ============================================================

	/**
	 * Selects which shop the customer will visit this SELL phase.
	 *
	 * Score components:
	 * - Best affordable item score within each shop (baseline)
	 * - Loyalty bonus for last visited shop
	 * - Stockout penalty if last visit failed to satisfy desire
	 * - Noise to avoid deterministic ties
	 */
	public Shop pickShop(Customer customer, Shop playerShop, Shop rivalShop) {
		if (customer == null) return playerShop;
		if (playerShop == null) return rivalShop;
		if (rivalShop == null) return playerShop;

		CustomerProfile p = safe(customer.getProfile());
		String lastShop = customer.getLastShopName();
		boolean gotWhatWanted = customer.gotWhatWantedLastTime();

		double playerScore = bestAffordableItemScore(customer, playerShop);
		double rivalScore  = bestAffordableItemScore(customer, rivalShop);

		if (playerScore <= VERY_BAD && rivalScore <= VERY_BAD) {
			if (lastShop != null) {
				if (lastShop.equals(playerShop.getName())) return playerShop;
				if (lastShop.equals(rivalShop.getName())) return rivalShop;
			}
			return playerShop;
		}

		if (lastShop != null) {
			if (lastShop.equals(playerShop.getName())) {
				playerScore += LOYALTY_BONUS * p.loyaltyBias;
			} else if (lastShop.equals(rivalShop.getName())) {
				rivalScore += LOYALTY_BONUS * p.loyaltyBias;
			}
		}

		if (!gotWhatWanted && lastShop != null) {
			if (lastShop.equals(playerShop.getName())) {
				playerScore -= STOCKOUT_PENALTY * p.stockoutAversion;
			} else if (lastShop.equals(rivalShop.getName())) {
				rivalScore -= STOCKOUT_PENALTY * p.stockoutAversion;
			}
		}

		playerScore += noise();
		rivalScore  += noise();

		if (playerScore > rivalScore) return playerShop;
		if (rivalScore > playerScore) return rivalShop;

		return ThreadLocalRandom.current().nextBoolean() ? playerShop : rivalShop;
	}

	/**
	 * Returns the best (highest) score among the shop's affordable items.
	 * Noise is not applied here to keep shop baseline deterministic.
	 */
	private double bestAffordableItemScore(Customer customer, Shop shop) {
		if (customer == null || shop == null) return VERY_BAD;

		List<Item> options = shop.getItemsInStock();
		if (options == null || options.isEmpty()) return VERY_BAD;

		double wallet = customer.getWallet();
		double best = VERY_BAD;

		for (Item it : options) {
			if (it == null) continue;
			if (wallet + 1e-9 < it.getPrice()) continue;

			double s = scoreItem(customer, it);
			if (s > best) best = s;
		}

		return best;
	}

	// ============================================================
	// Item choice
	// ============================================================

	/**
	 * Picks the best affordable item from a list.
	 * Adds noise per item to reduce permanent ties.
	 */
	public Item pickFromList(Customer customer, List<Item> items) {
		if (customer == null || items == null || items.isEmpty()) return null;

		double wallet = customer.getWallet();

		Item best = null;
		double bestScore = Double.NEGATIVE_INFINITY;

		for (Item item : items) {
			if (item == null) continue;
			if (wallet + 1e-9 < item.getPrice()) continue;

			double score = scoreItem(customer, item) + noise();

			if (best == null || score > bestScore) {
				best = item;
				bestScore = score;
			} else if (Math.abs(score - bestScore) < 1e-9) {
				best = tieBreak(best, item, safe(customer.getProfile()));
			}
		}

		return best;
	}

	/** Two-item convenience wrapper. */
	public Item iPick(Customer customer, Item a, Item b) {
		if (customer == null) return null;
		if (a == null && b == null) return null;
		if (a == null) return (customer.getWallet() >= b.getPrice()) ? b : null;
		if (b == null) return (customer.getWallet() >= a.getPrice()) ? a : null;
		return pickFromList(customer, java.util.Arrays.asList(a, b));
	}

	// ============================================================
	// Scoring
	// ============================================================

	/**
	 * Scores an item using profile weights.
	 *
	 * Current Item fields:
	 * - price
	 * - quality
	 *
	 * If Item later gains a hype field, include it in hypeTerm.
	 */
	private double scoreItem(Customer customer, Item item) {
		if (customer == null || item == null) return VERY_BAD;

		CustomerProfile p = safe(customer.getProfile());

		double price = item.getPrice();
		double quality = item.getQuality();

		double priceTerm = -p.priceSensitivity * price;
		double qualityTerm = p.qualitySensitivity * quality;

		double hype = 0.0;
		double hypeTerm = p.hypeBias * hype;

		return priceTerm + qualityTerm + hypeTerm;
	}

	private CustomerProfile safe(CustomerProfile p) {
		return (p != null) ? p : DEFAULT_PROFILE;
	}

	private double noise() {
		if (NOISE <= 0.0) return 0.0;
		return ThreadLocalRandom.current().nextDouble(-NOISE, NOISE);
	}

	// ============================================================
	// Tie-break
	// ============================================================

	/**
	 * Tie-breaker aligned with profile priorities (quality-first vs price-first).
	 */
	private Item tieBreak(Item a, Item b, CustomerProfile p) {
		if (a == null) return b;
		if (b == null) return a;

		double pa = a.getPrice();
		double pb = b.getPrice();
		int qa = a.getQuality();
		int qb = b.getQuality();

		if (p.qualitySensitivity >= p.priceSensitivity) {
			if (qb > qa) return b;
			if (qa > qb) return a;

			if (pb < pa) return b;
			if (pa < pb) return a;

			return ThreadLocalRandom.current().nextBoolean() ? a : b;
		}

		if (pb < pa) return b;
		if (pa < pb) return a;

		if (qb > qa) return b;
		if (qa > qb) return a;

		return ThreadLocalRandom.current().nextBoolean() ? a : b;
	}
}