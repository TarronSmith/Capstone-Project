package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.tarron.marketsim.model.Customer;
import com.tarron.marketsim.model.CustomerProfile;

/**
 * MarketPlan
 *
 * Responsibilities:
 * - Builds a session-level plan used to spawn customers each SELL phase.
 * - Provides a stable sequence of "pair slots" (each slot spawns two customers).
 * - Generates customers using:
 *   - wallet sampled from a fixed market distribution
 *   - profile produced by CustomerProfileGenerator
 *
 * Notes:
 * - The plan is generated once per reroll and reused across rounds.
 * - Customer counts are kept even to match the pair-based spawn model.
 * - No archetypes or spending types are encoded here.
 */
public class MarketPlan {

	// Wallet distribution for the market. Keep aligned with catalog prices.
	private static final double[] MARKET_WALLETS = {
			0.0, 2.0, 3.0, 4.0, 5.0,
			6.0, 8.0, 9.0, 10.0, 12.0, 15.0
	};

	private final int maxCustomers;
	private final CustomerProfileGenerator profileGenerator;

	// Each entry represents a pair slot id. One slot spawns two customers.
	private final List<Integer> pairSlots = new ArrayList<>();

	private boolean initialized = false;

	private long sessionSeed = 0L;
	private Random rng;

	public MarketPlan(int maxCustomers, CustomerProfileGenerator profileGenerator) {
		this.maxCustomers = maxCustomers;
		this.profileGenerator = profileGenerator;
	}

	// ============================================================
	// Session planning
	// ============================================================

	/** Reroll plan with a new seed. */
	public void reroll() {
		reroll(System.nanoTime());
	}

	/** Reroll plan with an explicit seed (useful for debugging/replays). */
	public void reroll(long seed) {
		this.sessionSeed = seed;
		this.rng = new Random(sessionSeed);

		pairSlots.clear();

		int maxPairs = Math.max(0, maxCustomers / 2);
		for (int i = 0; i < maxPairs; i++) {
			pairSlots.add(i);
		}

		Collections.shuffle(pairSlots, rng);
		initialized = true;
	}

	/** Ensures a plan exists before reading from it. */
	public void ensureInitialized() {
		if (!initialized) reroll();
	}

	// ============================================================
	// Pair slot access
	// ============================================================

	/**
	 * Returns the pair slot ids needed for totalCustomers.
	 * totalCustomers is forced even and clamped to maxCustomers.
	 */
	public List<Integer> getPairSlotsForCustomers(int totalCustomers) {
		ensureInitialized();

		int evenCustomers = clampEvenToMax(totalCustomers);
		int pairsNeeded = Math.min(evenCustomers / 2, pairSlots.size());

		List<Integer> slice = new ArrayList<>(pairsNeeded);
		for (int i = 0; i < pairsNeeded; i++) {
			slice.add(pairSlots.get(i));
		}
		return slice;
	}

	/**
	 * Returns counts for UI/debug.
	 * No archetype mix; only totals.
	 */
	public MarketCounts getCountsForCustomers(int totalCustomers) {
		ensureInitialized();

		int evenCustomers = clampEvenToMax(totalCustomers);
		int pairsNeeded = Math.min(evenCustomers / 2, pairSlots.size());

		return new MarketCounts(evenCustomers, pairsNeeded);
	}

	// ============================================================
	// Customer creation
	// ============================================================

	/**
	 * Creates a Customer using:
	 * - wallet sampled from MARKET_WALLETS
	 * - profile produced by CustomerProfileGenerator (or null)
	 */
	public Customer makeCustomer() {
		ensureInitialized();

		double wallet = pickFrom(MARKET_WALLETS);

		CustomerProfile profile =
				(profileGenerator == null) ? null : profileGenerator.generate(rng);

		return new Customer(wallet, profile);
	}

	/** Exposes the session seed for debugging/replays. */
	public long getSessionSeed() {
		ensureInitialized();
		return sessionSeed;
	}

	// ============================================================
	// Internals
	// ============================================================

	private int clampEvenToMax(int totalCustomers) {
		int even = (totalCustomers % 2 == 0) ? totalCustomers : totalCustomers + 1;
		if (even < 0) even = 0;
		return Math.min(even, maxCustomers);
	}

	private double pickFrom(double[] options) {
		if (options == null || options.length == 0) return 0.0;
		return options[rng.nextInt(options.length)];
	}

	public static class MarketCounts {
		public final int totalCustomers;
		public final int totalPairs;

		public MarketCounts(int totalCustomers, int totalPairs) {
			this.totalCustomers = totalCustomers;
			this.totalPairs = totalPairs;
		}
	}
}