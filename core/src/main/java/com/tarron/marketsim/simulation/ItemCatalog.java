package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tarron.marketsim.model.Item;

/**
 * ItemCatalog
 *
 * Responsibilities:
 * - Defines the fixed set of items available in the market.
 * - Stores vendor acquisition cost per item.
 * - Provides ordered access for UI display and slot-based purchasing.
 *
 * Design:
 * - Items are created once during initialization.
 * - Sale price lives on Item; vendor cost is stored separately.
 * - Catalog order determines BUY phase slot mapping (1..N).
 */
public class ItemCatalog {

	// ============================================================
	// Catalog storage
	// ============================================================

	/** Ordered list used for display and slot lookup. */
	private final List<Item> items = new ArrayList<>();

	/** Vendor acquisition cost per item instance. */
	private final Map<Item, Double> vendorCost = new HashMap<>();

	// ============================================================
	// Initialization
	// ============================================================

	/**
	 * Builds the fixed catalog.
	 *
	 * Guidelines:
	 * - Price should align with typical customer wallet ranges.
	 * - Quality should scale roughly with price.
	 * - Vendor cost must remain below sale price to allow margin.
	 */
	public ItemCatalog() {
		add("Bargain",       2.0, 0, 1.2);
		add("CheapItem",     3.0, 1, 2.0);
		add("Decent",        5.0, 3, 3.2);
		add("Quality",       6.5, 4, 4.2);
		add("ExpensiveItem", 8.0, 5, 5.0);
		add("Luxury",       12.0, 7, 8.0);
	}

	/**
	 * Registers a new catalog item and its vendor cost.
	 * Intended for use only during construction.
	 */
	private void add(String name, double price, int quality, double cost) {
		Item item = new Item(name, price, quality);
		items.add(item);
		vendorCost.put(item, cost);
	}

	// ============================================================
	// Getters
	// ============================================================

	/**
	 * Returns catalog items in fixed display order.
	 * The returned list is unmodifiable.
	 */
	public List<Item> getItems() {
		return Collections.unmodifiableList(items);
	}

	/**
	 * Returns vendor acquisition cost for the given item.
	 * Returns positive infinity if the item is not part of the catalog.
	 */
	public double getVendorCost(Item item) {
		if (item == null) return Double.POSITIVE_INFINITY;
		return vendorCost.getOrDefault(item, Double.POSITIVE_INFINITY);
	}

	/**
	 * Slot lookup using 1-based indexing.
	 * Returns null if the slot is out of range.
	 */
	public Item getBySlot(int slot1Based) {
		int index = slot1Based - 1;
		if (index < 0 || index >= items.size()) return null;
		return items.get(index);
	}
}