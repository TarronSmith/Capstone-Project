package com.tarron.marketsim.model;

/**
 * Item
 *
 * Responsibilities:
 * - Immutable product definition used throughout the simulation.
 * - Represents a sellable unit with fixed price and quality.
 *
 * Used by:
 * - ItemCatalog (defines available products and vendor costs)
 * - Shop (inventory storage and sales)
 * - DecisionLogic (price/quality scoring)
 * - RoundManager (per-item demand/sales tracking by name)
 *
 * Design:
 * - Immutable: all fields are final.
 * - Value-based equality (name, price, quality).
 */
public class Item {

	// ============================================================
	// Core fields
	// ============================================================

	/** Display name and identifier for demand/sales tracking. */
	private final String itemName;

	/** Customer-facing purchase price. */
	private final double price;

	/** Abstract quality level used in decision scoring. */
	private final int quality;

	// ============================================================
	// Construction
	// ============================================================

	public Item(String name, double price, int quality) {
		this.itemName = name;
		this.price = price;
		this.quality = quality;
	}

	// ============================================================
	// Accessors
	// ============================================================

	public double getPrice() {
		return price;
	}

	public String getName() {
		return itemName;
	}

	public int getQuality() {
		return quality;
	}

	// ============================================================
	// Equality / hashing (value-based)
	// ============================================================

	/**
	 * Two items are equal if name, price, and quality match.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;

		Item other = (Item) obj;

		if (quality != other.quality) return false;
		if (Double.compare(other.price, price) != 0) return false;
		return itemName != null
				? itemName.equals(other.itemName)
						: other.itemName == null;
	}

	/**
	 * Hash consistent with equals().
	 * Safe for use in hash-based collections.
	 */
	@Override
	public int hashCode() {
		int result = (itemName != null) ? itemName.hashCode() : 0;
		long temp = Double.doubleToLongBits(price);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + quality;
		return result;
	}
}