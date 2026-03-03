package com.tarron.marketsim.model;

/**
 * CustomerProfile
 *
 * Responsibilities:
 * - Immutable behavioral weight container used by DecisionLogic.
 * - Encapsulates how a customer values price, quality, hype, loyalty, and stockout risk.
 *
 * Design:
 * - Pure data object (no logic).
 * - Stored on Customer and read during item scoring and shop selection.
 * - Profile generation is handled externally (e.g., CustomerProfileGenerator).
 */
public class CustomerProfile {

	// ============================================================
	// Item evaluation weights
	// ============================================================

	/**
	 * Influence of price when scoring items.
	 * Higher values increase sensitivity to price differences.
	 */
	public final double priceSensitivity;

	/**
	 * Influence of quality when scoring items.
	 * Higher values increase preference for higher-quality items.
	 */
	public final double qualitySensitivity;

	/**
	 * Influence of hype when scoring items.
	 * Higher values increase the effect of hype in decision scoring.
	 */
	public final double hypeBias;

	// ============================================================
	// Shop selection biases (cross-round behavior)
	// ============================================================

	/**
	 * Tendency to revisit the same shop across rounds.
	 * Higher values increase loyalty influence.
	 */
	public final double loyaltyBias;

	/**
	 * Tendency to avoid shops where desired items were unavailable.
	 * Higher values increase penalty for prior stockouts.
	 */
	public final double stockoutAversion;

	// ============================================================
	// Construction
	// ============================================================

	public CustomerProfile(
			double priceSensitivity,
			double qualitySensitivity,
			double hypeBias,
			double loyaltyBias,
			double stockoutAversion
			) {
		this.priceSensitivity = priceSensitivity;
		this.qualitySensitivity = qualitySensitivity;
		this.hypeBias = hypeBias;
		this.loyaltyBias = loyaltyBias;
		this.stockoutAversion = stockoutAversion;
	}
}