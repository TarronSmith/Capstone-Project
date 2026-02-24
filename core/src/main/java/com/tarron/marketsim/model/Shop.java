package com.tarron.marketsim.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shop
 *
 * Purpose:
 * - Represents a seller in the market.
 * - Maintains inventory, processes purchases, and tracks per-round performance.
 *
 * Used by:
 * - MarketEngine (phase control, resets, stocking)
 * - DecisionLogic (via getItemsInStock)
 * - RoundManager/HUD (revenue and sales statistics)
 *
 * Notes:
 * - Inventory is stored as quantity counts per Item.
 * - Revenue and sales counters reset each SELL phase.
 */
public class Shop {

	// Display name used in UI and customer memory
	private String name;

	// Per-round performance metrics
	private double revenueThisTurn;
	private int itemsSoldThisTurn;

	// Inventory: Item -> quantity
	private Map<Item, Integer> inventory = new HashMap<>();

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

	public double getRevenueThisTurn() { return revenueThisTurn; }
	public int getItemsSoldThisTurn() { return itemsSoldThisTurn; }

	/**
	 * Returns current quantity for an item.
	 */
	public int getQuantity(Item item) {
		if (item == null) return 0;
		return inventory.getOrDefault(item, 0);
	}

	// ============================================================
	// Inventory Management
	// ============================================================

	/**
	 * Adds items to inventory (one unit per entry).
	 */
	public void addItems(List<Item> items) {
		if (items == null) return;
		for (Item item : items) {
			if (item == null) continue;
			inventory.put(item, getQuantity(item) + 1);
		}
	}

	/**
	 * Returns true if at least one unit is available.
	 */
	public boolean hasItem(Item item) {
		return getQuantity(item) > 0;
	}

	/**
	 * Removes all inventory.
	 * Used when starting a new BUY phase depending on round policy.
	 */
	public void clearInventory() {
		inventory.clear();
	}

	/**
	 * Returns the unique item types currently available (quantity > 0).
	 * Used by DecisionLogic for customer choice.
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
		if (newQty == 0) inventory.remove(item);
		else inventory.put(item, newQty);

		return true;
	}

	// ============================================================
	// Selling (multi-item system)
	// ============================================================

	/**
	 * Processes a customer purchase.
	 *
	 * Flow:
	 * 1) Customer selects preferred item from available inventory
	 * 2) Wallet is checked and charged
	 * 3) Inventory is decremented
	 * 4) Revenue and sales counters are updated
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
		revenueThisTurn = 0.0;
		itemsSoldThisTurn = 0;
	}
}