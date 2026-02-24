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
 * Responsibility:
 * - Defines the global set of items available in the market.
 * - Stores vendor acquisition cost for each item.
 *
 * Notes:
 * - Items are created once at initialization and treated as immutable.
 * - Vendor cost is maintained separately from Item (price is customer-facing).
 * - The catalog acts as the authoritative source for:
 *      Catalog display order
 *      Slot-based purchasing
 *      Vendor cost lookup
 */
public class ItemCatalog {

    // Ordered list used for UI display and slot selection (1..N)
    private final List<Item> items = new ArrayList<>();

    // Vendor acquisition cost per item instance
    private final Map<Item, Double> vendorCost = new HashMap<>();

    /**
     * Initializes the fixed catalog.
     *
     * Balance considerations:
     * - Prices should fit within typical customer wallet ranges.
     * - Quality increases roughly with price.
     * - Vendor cost should remain below sale price to allow margin.
     */
    public ItemCatalog() {
        add("Bargain",        2.0, 0, 1.2);
        add("CheapItem",      3.0, 1, 2.0);
        add("Decent",         5.0, 3, 3.2);
        add("Quality",        6.5, 4, 4.2);
        add("ExpensiveItem",  8.0, 5, 5.0);
        add("Luxury",        12.0, 7, 8.0);
    }

    /**
     * Creates a catalog item and registers its vendor cost.
     * Called only during initialization.
     */
    private void add(String name, double price, int quality, double cost) {
        Item item = new Item(name, price, quality);
        items.add(item);
        vendorCost.put(item, cost);
    }

    // ============================================================
    // Accessors
    // ============================================================

    /**
     * Returns catalog items in fixed display order.
     * The returned list is read-only.
     */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Returns vendor acquisition cost for a catalog item.
     * Returns +Infinity if the item is not recognized.
     */
    public double getVendorCost(Item item) {
        if (item == null) return Double.POSITIVE_INFINITY;
        return vendorCost.getOrDefault(item, Double.POSITIVE_INFINITY);
    }

    /**
     * Slot lookup (1-based).
     *
     * Used by BUY phase input:
     *   Key '1' -> slot 1 -> first catalog item
     *
     * Returns null if slot is out of range.
     */
    public Item getBySlot(int slot1Based) {
        int index = slot1Based - 1;
        if (index < 0 || index >= items.size()) return null;
        return items.get(index);
    }
}