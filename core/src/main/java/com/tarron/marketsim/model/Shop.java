package com.tarron.marketsim.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shop
 *
 * Responsibilities:
 * - Stores inventory (Item -> quantity).
 * - Processes customer purchases.
 * - Tracks per-round revenue and units sold.
 *
 * Lifecycle:
 * - Inventory persists across phases unless explicitly cleared.
 * - Revenue and sales counters reset each SELL phase.
 */
public class Shop {

	// ============================================================
	// Identity
	// ============================================================

	/** Display name used in UI and customer memory. */
	private String name;

	// ============================================================
	// Per-round performance
	// ============================================================

	/** Revenue accumulated during the current SELL phase. */
	private double revenueThisTurn;

	/** Units sold during the current SELL phase. */
	private int itemsSoldThisTurn;

	// ============================================================
	// Inventory
	// ============================================================

	/** Inventory map: Item -> quantity. */
	private final Map<Item, Integer> inventory = new HashMap<>();

	// ============================================================
	// Getters
	// ============================================================

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public double getRevenueThisTurn() { return revenueThisTurn; }
	public int getItemsSoldThisTurn() { return itemsSoldThisTurn; }

	/**
	 * Returns current quantity for the given item.
	 */
	public int getQuantity(Item item) {
		if (item == null) return 0;
		return inventory.getOrDefault(item, 0);
	}

	// ============================================================
	// Inventory management
	// ============================================================

	/**
	 * Adds one unit per entry in the provided list.
	 */
	public void addItems(List<Item> items) {
		if (items == null) return;

		for (Item item : items) {
			if (item == null) continue;
			inventory.put(item, getQuantity(item) + 1);
		}
	}

	/**
	 * Returns true if at least one unit of the item exists.
	 */
	public boolean hasItem(Item item) {
		return getQuantity(item) > 0;
	}

	/**
	 * Removes all inventory.
	 * Typically used at BUY phase transitions depending on round policy.
	 */
	public void clearInventory() {
		inventory.clear();
	}

	/**
	 * Returns unique item types currently in stock (quantity > 0).
	 * Used by DecisionLogic during customer evaluation.
	 */
	public List<Item> getItemsInStock() {
		List<Item> out = new ArrayList<>();

		for (Map.Entry<Item, Integer> e : inventory.entrySet()) {
			if (e.getKey() != null && e.getValue() != null && e.getValue() > 0) {
				out.add(e.getKey());
			}
		}

		return out;
	}

	/**
	 * Removes a single unit of the specified item.
	 */
	private boolean removeOne(Item item) {
		int qty = getQuantity(item);
		if (qty <= 0) return false;

		int newQty = qty - 1;
		if (newQty == 0) {
			inventory.remove(item);
		} else {
			inventory.put(item, newQty);
		}

		return true;
	}

	// ============================================================
	// Selling
	// ============================================================

	/**
	 * Processes a customer purchase.
	 *
	 * Flow:
	 * 1) Customer selects preferred item from available stock.
	 * 2) Wallet is checked and charged.
	 * 3) Inventory is decremented.
	 * 4) Revenue and unit counters are updated.
	 *
	 * Returns the purchased item, or null if no transaction occurs.
	 */
	public Item sell(Customer customer, DecisionLogic logic) {
		if (customer == null || logic == null) return null;

		List<Item> options = getItemsInStock();
		if (options.isEmpty()) return null;

		Item choice = logic.pickFromList(customer, options);
		if (choice == null) return null;
		if (!hasItem(choice)) return null;

		double price = choice.getPrice();
		if (!customer.spend(price)) return null;
		if (!removeOne(choice)) return null;

		revenueThisTurn += price;
		itemsSoldThisTurn++;

		return choice;
	}

	/**
	 * Clears per-round performance metrics.
	 * Called at the start of each SELL phase.
	 */
	public void resetTurnStats() {
		revenueThisTurn = 0;
		itemsSoldThisTurn = 0;
	}
}