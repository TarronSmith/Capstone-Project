package com.tarron.marketsim.simulation;

import java.util.Random;

import com.tarron.marketsim.model.CustomerProfile;

/**
 * CustomerProfileGenerator
 *
 * Responsibilities:
 * - Generates CustomerProfile instances using a fixed stat budget.
 * - Distributes preference weights across price, quality, hype, loyalty, and stockout.
 *
 * Model:
 * - Each stat starts at a base value.
 * - Remaining points from a shared pool are randomly distributed.
 * - Final values are normalized to the 0.0-1.0 range.
 */
public class CustomerProfileGenerator {

	// ============================================================
	// Budget configuration
	// ============================================================

	/** Total integer points available across all stats. */
	private static final int STAT_POOL = 100;

	/** Minimum starting value per stat before random allocation. */
	private static final int BASE = 10;

	// ============================================================
	// Generation
	// ============================================================

	/**
	 * Generates a randomized CustomerProfile.
	 *
	 * Process:
	 * - Initialize each stat to BASE.
	 * - Distribute remaining pool points randomly.
	 * - Normalize values into the 0.0-1.0 range.
	 */
	public CustomerProfile generate(Random rng) {
		if (rng == null) rng = new Random();

		int price = BASE;
		int quality = BASE;
		int hype = BASE;
		int loyalty = BASE;
		int stockout = BASE;

		int remaining = STAT_POOL - (BASE * 5);

		for (int i = 0; i < remaining; i++) {
			switch (rng.nextInt(5)) {
			case 0: price++; break;
			case 1: quality++; break;
			case 2: hype++; break;
			case 3: loyalty++; break;
			default: stockout++; break;
			}
		}

		return new CustomerProfile(
				price / 100.0,
				quality / 100.0,
				hype / 100.0,
				loyalty / 100.0,
				stockout / 100.0
				);
	}
}