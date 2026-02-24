package com.tarron.marketsim.model;

/**
 * Customer
 *
 * Purpose:
 * - Represents a single market participant during the SELL phase.
 * - Encapsulates purchasing constraints (wallet), preference type, and behavioral memory.
 *
 * Role in simulation:
 * - Used by DecisionLogic to evaluate item choices and shop selection.
 * - Updated by MarketEngine after each visit to record satisfaction and loyalty signals.
 *
 * Notes:
 * - Customer is stateful across rounds (wallet resets, memory persists).
 * - Behavior tuning is driven through CustomerProfile rather than hard-coded logic.
 */
public class Customer {

	// Primary spending archetype label
	// Used by DecisionLogic scoring functions
	private final String spendingType; // "Cheap Buyer", "Value Buyer", "Conspicuous Buyer"

	// Available money for purchases during the SELL phase
	private double wallet;

	// Behavioral parameters (loyalty, stockout aversion, prestige weighting, etc.)
	private final CustomerProfile profile;

	// Memory from previous round (used for shop choice feedback loop)
	private String lastShopName = null;
	private boolean gotWhatWantedLastTime = true;

	/**
	 * Constructs a customer with an inferred default profile
	 * based on the spendingType label.
	 */
	public Customer(String spendingType, double wallet) {
		this(spendingType, wallet, defaultProfileFor(spendingType));
	}

	/**
	 * Constructs a customer with an explicit behavioral profile.
	 */
	public Customer(String spendingType, double wallet, CustomerProfile profile) {
		this.spendingType = spendingType;
		this.wallet = wallet;
		this.profile = (profile != null) ? profile : defaultProfileFor(spendingType);
	}

	// ------------------------------------------------------------
	// Basic accessors
	// ------------------------------------------------------------

	public String getSpendingType() {
		return spendingType;
	}

	public double getWallet() {
		return wallet;
	}

	public void setWallet(double wallet) {
		this.wallet = wallet;
	}

	public CustomerProfile getProfile() {
		return profile;
	}

	// ------------------------------------------------------------
	// Purchase mechanics
	// ------------------------------------------------------------

	/**
	 * Attempts to spend the given amount from the wallet.
	 *
	 * Returns:
	 * - true if the purchase succeeds
	 * - false if funds are insufficient or amount is invalid
	 */
	public boolean spend(double amount) {
		if (amount < 0) return false;
		if (wallet < amount) return false;
		wallet -= amount;
		return true;
	}

	// ------------------------------------------------------------
	// Behavioral memory (used by DecisionLogic.pickShop)
	// ------------------------------------------------------------

	public String getLastShopName() {
		return lastShopName;
	}

	public void setLastShopName(String lastShopName) {
		this.lastShopName = lastShopName;
	}

	public boolean gotWhatWantedLastTime() {
		return gotWhatWantedLastTime;
	}

	public void setGotWhatWantedLastTime(boolean v) {
		this.gotWhatWantedLastTime = v;
	}

	// ------------------------------------------------------------
	// Default profile construction
	// ------------------------------------------------------------

	/**
	 * Maps spending archetypes to baseline behavioral profiles.
	 *
	 * Profiles control:
	 * - Loyalty strength
	 * - Stockout aversion
	 * - Prestige sensitivity
	 * - Price vs quality weighting
	 */
	private static CustomerProfile defaultProfileFor(String typeLabel) {
		if (typeLabel == null) {
			return new CustomerProfile(0.5, 0.5, 0.0, 0.2, 0.2);
		}

		switch (typeLabel) {
		case "Cheap Buyer":
			return new CustomerProfile(
					1.0,  // price sensitivity
					0.2,  // quality sensitivity
					0.0,  // prestige sensitivity
					0.2,  // loyalty
					0.3   // stockout aversion
					);

		case "Value Buyer":
			return new CustomerProfile(
					0.7,
					0.7,
					0.0,
					0.3,
					0.4
					);

		case "Conspicuous Buyer":
			return new CustomerProfile(
					0.2,
					0.9,
					0.8,
					0.4,
					0.2
					);

		default:
			return new CustomerProfile(0.5, 0.5, 0.0, 0.2, 0.2);
		}
	}
}