package com.tarron.marketsim.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Shop {

	private String name;
	private double revenueThisTurn;
	private int itemsSoldThisTurn;

	private Map<Item, Integer> inventory = new HashMap<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getRevenueThisTurn() {
		return revenueThisTurn;
	}

	public int getItemsSoldThisTurn() {
		return itemsSoldThisTurn;
	}

	public int getQuantity(Item item) {
		if (item == null) return 0;
		return inventory.getOrDefault(item, 0);
	}

	// ---Inventory---
	public void addItems(List<Item> items) {
		if (items == null) return;

		for (Item item : items) {
			if (item == null) continue;
			inventory.put(item, getQuantity(item) + 1);
		}
	}

	public boolean hasItem(Item item) {
		return getQuantity(item) > 0;
	}

	public void clearInventory() {
		inventory.clear();
	}

	//Removes exactly 1 of the item if present.
	//return true if removed, false if none in stock.
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

	// ---Selling/Turn states---
	public Item sell(Customer customer, Item A, Item B, DecisionLogic logic) {
		if (customer == null || logic == null) return null;

		Item choice = logic.iPick(customer, A, B);
		if (choice == null) return null;

		// Must have inventory
		if (!hasItem(choice)) return null;

		double price = choice.getPrice();

		// Customer must pay successfully
		if (!customer.spend(price)) return null;

		// Finalize sale: decrement inventory and update shop stats
		if (!removeOne(choice)) return null;

		revenueThisTurn += price;
		itemsSoldThisTurn++;

		return choice;
	}

	public void resetTurnStats() {
		revenueThisTurn = 0.0;
		itemsSoldThisTurn = 0;
	}
}
