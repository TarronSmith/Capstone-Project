package com.tarron.marketsim.model;

/**
 * Item
 *
 * Purpose:
 * - Immutable value object representing a product that can be stocked and sold.
 *
 * Used by:
 * - ItemCatalog (defines available items and vendor costs)
 * - Shop (inventory storage and sales)
 * - DecisionLogic (evaluates price and quality)
 * - RoundManager (per-item demand and sales tracking by name)
 *
 * Notes:
 * - Immutable: price and quality do not change during the simulation.
 * - Equality is value-based (name, price, quality) so items behave correctly
 *   when used in collections or compared across systems.
 */
public class Item {

	// Display name and identifier for demand/sales tracking
	private final String itemName;

	// Customer purchase price
	private final double price;

	// Abstract quality level used by decision scoring
	private final int quality;

	public Item(String name, double price, int quality) {
		this.itemName = name;
		this.price = price;
		this.quality = quality;
	}

	public double getPrice() {
		return price;
	}

	public String getName() {
		return itemName;
	}

	public int getQuality() {
		return quality;
	}

	/**
	 * Value equality.
	 * Two items are considered the same if their name, price, and quality match.
	 * Required for correct behavior in maps, sets, and inventory comparisons.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;

		Item other = (Item) obj;

		if (quality != other.quality) return false;
		if (Double.compare(other.price, price) != 0) return false;
		return itemName != null ? itemName.equals(other.itemName) : other.itemName == null;
	}

	/**
	 * Hash consistent with equals().
	 * Allows Item to be used safely as a key in hash-based collections.
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