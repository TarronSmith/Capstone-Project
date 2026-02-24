package com.tarron.marketsim.model;

/**
 * CustomerProfile
 *
 * Purpose:
 * - Defines behavioral weight parameters used by decision models.
 * - Separates customer psychology from decision algorithms.
 *
 * Usage:
 * - Attached to each Customer instance.
 * - Read by DecisionLogic when evaluating items and shop choice.
 *
 * Notes:
 * - All values are relative weights (typically 0.0–1.0).
 * - Higher values increase the influence of that factor on decisions.
 */
public class CustomerProfile {

	/**
	 * Sensitivity to price.
	 * Higher values increase preference for lower-priced items.
	 */
	public final double priceSensitivity;

	/**
	 * Sensitivity to quality.
	 * Higher values increase preference for higher-quality items.
	 */
	public final double qualitySensitivity;

	/**
	 * Preference for expensive items as a status signal.
	 * Influences conspicuous consumption behavior.
	 */
	public final double prestigeBias;

	/**
	 * Tendency to revisit the same shop across rounds.
	 * Used as a positive modifier in shop selection.
	 */
	public final double loyalty;

	/**
	 * Tendency to avoid shops where the desired item was unavailable.
	 * Used as a negative modifier in shop selection.
	 */
	public final double stockoutAversion;

	/**
	 * Immutable behavioral parameter set.
	 */
	public CustomerProfile(
			double priceSensitivity,
			double qualitySensitivity,
			double prestigeBias,
			double loyalty,
			double stockoutAversion
			) {
		this.priceSensitivity = priceSensitivity;
		this.qualitySensitivity = qualitySensitivity;
		this.prestigeBias = prestigeBias;
		this.loyalty = loyalty;
		this.stockoutAversion = stockoutAversion;
	}
}
