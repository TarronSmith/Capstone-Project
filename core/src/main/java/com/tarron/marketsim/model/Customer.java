package com.tarron.marketsim.model;

/**
 * Customer
 *
 * Responsibilities:
 * - Represents a single market participant during the SELL phase.
 * - Stores purchasing constraints (wallet), behavioral parameters, and cross-round memory.
 * - Provides spending mechanics used by Shop and DecisionLogic.
 *
 * State:
 * - Wallet decreases when purchases succeed.
 * - Behavioral weighting is defined by CustomerProfile.
 * - Memory fields persist across rounds and influence future shop selection.
 */
public class Customer {

	// ============================================================
	// Defaults
	// ============================================================

	/** Fallback profile used when a null profile is supplied. */
	private static final CustomerProfile DEFAULT_PROFILE =
			new CustomerProfile(0.5, 0.5, 0.0, 0.2, 0.2);

	// ============================================================
	// Core state
	// ============================================================

	/** Available money for purchases during SELL phase. */
	private double wallet;

	/** Behavioral parameters (price/quality/hype/loyalty/stockout sensitivity). */
	private final CustomerProfile profile;

	// ============================================================
	// Cross-round memory
	// ============================================================

	/** Shop visited in the previous round. */
	private String lastShopName = null;

	/** Whether the desired item was successfully purchased last round. */
	private boolean gotWhatWantedLastTime = true;

	// ============================================================
	// Construction
	// ============================================================

	/**
	 * Creates a customer with a wallet and behavioral profile.
	 * If profile is null, DEFAULT_PROFILE is used.
	 */
	public Customer(double wallet, CustomerProfile profile) {
		this.wallet = wallet;
		this.profile = (profile != null) ? profile : DEFAULT_PROFILE;
	}

	// ============================================================
	// Getters
	// ============================================================

	public double getWallet() {
		return wallet;
	}

	public void setWallet(double wallet) {
		this.wallet = wallet;
	}

	public CustomerProfile getProfile() {
		return profile;
	}

	// ============================================================
	// Purchase mechanics
	// ============================================================

	/**
	 * Attempts to deduct an amount from the wallet.
	 * Returns true if the deduction succeeds.
	 * Fails if amount is negative or exceeds available funds.
	 */
	public boolean spend(double amount) {
		if (amount < 0) return false;
		if (wallet < amount) return false;

		wallet -= amount;
		return true;
	}

	// ============================================================
	// Memory (used by DecisionLogic for shop selection feedback)
	// ============================================================

	public String getLastShopName() {
		return lastShopName;
	}

	public void setLastShopName(String lastShopName) {
		this.lastShopName = lastShopName;
	}

	public boolean gotWhatWantedLastTime() {
		return gotWhatWantedLastTime;
	}

	public void setGotWhatWantedLastTime(boolean value) {
		this.gotWhatWantedLastTime = value;
	}
}