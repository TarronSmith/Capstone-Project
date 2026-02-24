package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tarron.marketsim.model.Customer;
import com.tarron.marketsim.model.DecisionLogic;
import com.tarron.marketsim.model.Item;
import com.tarron.marketsim.model.Shop;

/**
 * MarketEngine
 *
 * Responsibility:
 * - Owns the market simulation state for a single running session.
 * - Orchestrates phase flow (BUY -> SELL -> RESULTS -> BUY).
 * - Delegates:
 *      Market population generation to MarketPlan
 *      Customer motion/spawn to CustomerSpawner
 *      Shop choice + item choice to DecisionLogic
 *      Rival restocking to RivalAI
 *      Persistent counters + per-round state to RoundManager
 *
 * Notes:
 * - UI calls input methods (buy, begin sell, reset) and reads getters for rendering.
 * - Engine runs the SELL phase progression and resolves purchases when customers arrive.
 */
public class MarketEngine {

	// ============================================================
	// Configuration
	// ============================================================

	private final int minCustomers;
	private final int maxCustomers;

	private final double startingCash;

	// Legacy vendor costs (kept because FirstScreen constructor provides them)
	private final double vendorCheapCost;
	private final double vendorPremiumCost;

	private final float customerRadius;

	// ============================================================
	// Core model objects
	// ============================================================

	private final Shop playerShop = new Shop();
	private final Shop rivalShop  = new Shop();

	private final ItemCatalog catalog = new ItemCatalog();
	private final DecisionLogic logic = new DecisionLogic();

	private final MarketPlan marketPlan;
	private final RivalAI rivalAI;
	private final RoundManager roundManager;
	private final CustomerSpawner spawner = new CustomerSpawner();

	// Reference items retained for cheap/premium legacy counters
	private final Item cheapItem;
	private final Item premiumItem;

	// ============================================================
	// Runtime state (session)
	// ============================================================

	private int numCustomers;

	private int marketCheapCount = 0;
	private int marketValueCount = 0;
	private int marketConspCount = 0;

	// Engine owns the current SELL-phase crowd (movement + transaction logs)
	private final List<CustomerSpawner.VisualCustomer> crowd = new ArrayList<>();

	private boolean initialized = false;

	public MarketEngine(
			int initialCustomers,
			int minCustomers,
			int maxCustomers,
			double startingCash,
			double vendorCheapCost,
			double vendorPremiumCost,
			float customerRadius
			) {
		this.minCustomers = minCustomers;
		this.maxCustomers = maxCustomers;
		this.startingCash = startingCash;
		this.vendorCheapCost = vendorCheapCost;
		this.vendorPremiumCost = vendorPremiumCost;
		this.customerRadius = customerRadius;

		this.numCustomers = clampEven(initialCustomers);

		// Keep stable references for the classic "cheap vs premium" tracking.
		this.cheapItem = findCatalogItemByName("CheapItem");
		this.premiumItem = findCatalogItemByName("ExpensiveItem");

		playerShop.setName("Player Shop");
		rivalShop.setName("Rival Shop");

		marketPlan = new MarketPlan(maxCustomers);
		rivalAI = new RivalAI(vendorCheapCost, vendorPremiumCost);
		roundManager = new RoundManager(startingCash, true);
	}

	// ============================================================
	// Lifecycle
	// ============================================================

	public void initAtLaunch() {
		if (initialized) return;
		initialized = true;

		marketPlan.reroll();
		updateMarketCountsForCurrentSelection();
		hardReset(true);
	}

	// ============================================================
	// Phase helpers
	// ============================================================

	public boolean isBuyPhase()     { return roundManager.getPhase() == RoundManager.Phase.BUY; }
	public boolean isSellPhase()    { return roundManager.getPhase() == RoundManager.Phase.SELL; }
	public boolean isResultsPhase() { return roundManager.getPhase() == RoundManager.Phase.RESULTS; }

	public RoundManager.Phase getPhase() { return roundManager.getPhase(); }

	// ============================================================
	// Session control
	// ============================================================

	public void hardReset(boolean wipeKnowledge) {
		crowd.clear();
		roundManager.hardReset(playerShop, rivalShop, wipeKnowledge);

		RivalAI.Result res = rivalAI.stockForBuyPhase(
				rivalShop,
				roundManager.getRivalCash(),
				roundManager.getLastDesiredByItemName(),
				catalog,
				cheapItem,
				premiumItem
				);
		roundManager.applyRivalAIResult(res);
	}

	public void rerollMarketAndReset() {
		marketPlan.reroll();
		updateMarketCountsForCurrentSelection();
		hardReset(true);
	}

	// ============================================================
	// BUY phase controls
	// ============================================================

	public void incCustomersBy2() {
		if (!isBuyPhase()) return;
		numCustomers = clampEven(numCustomers + 2);
		updateMarketCountsForCurrentSelection();
	}

	public void decCustomersBy2() {
		if (!isBuyPhase()) return;
		numCustomers = clampEven(numCustomers - 2);
		updateMarketCountsForCurrentSelection();
	}

	/**
	 * Buy one unit of a catalog item using a 1-based slot number (1..N).
	 */
	public void buyCatalogSlot(int slot1Based) {
		Item item = catalog.getBySlot(slot1Based);
		if (item == null) return;
		buyItem(item);
	}

	/**
	 * Buy one unit of a specific item into player inventory.
	 * Uses the vendor cost from ItemCatalog.
	 */
	public void buyItem(Item item) {
		if (!isBuyPhase()) return;
		if (item == null) return;

		double cost = getVendorCost(item);
		if (Double.isInfinite(cost) || Double.isNaN(cost)) return;
		if (roundManager.getPlayerCash() < cost) return;

		roundManager.setPlayerCash(roundManager.getPlayerCash() - cost);
		playerShop.addItems(Collections.singletonList(item));
	}

	// Legacy bindings (optional, kept for compatibility)
	public void buyCheap() {
		if (cheapItem == null) return;
		buyItem(cheapItem);
	}

	public void buyPremium() {
		if (premiumItem == null) return;
		buyItem(premiumItem);
	}

	// ============================================================
	// Phase transitions
	// ============================================================

	public void beginSellPhase(
			float startX,
			float startY,
			float spacing,
			float playerX,
			float playerY,
			float shopW,
			float shopH,
			float rivalX,
			float rivalY,
			float paddingInside
			) {
		if (!isBuyPhase()) return;

		roundManager.beginSellPhase(playerShop, rivalShop);
		crowd.clear();

		// Spawner assigns each customer a target shop using DecisionLogic.pickShop(...)
		crowd.addAll(spawner.spawnCustomersFromPlanWithShopChoice(
				marketPlan,
				logic,
				numCustomers,
				playerShop,
				rivalShop,
				startX,
				startY,
				spacing,
				playerX,
				playerY,
				shopW,
				shopH,
				rivalX,
				rivalY,
				customerRadius,
				paddingInside
				));
	}

	public void endResultsAndStartNextBuy() {
		if (!isResultsPhase()) return;

		roundManager.startNextBuyPhase(playerShop, rivalShop);
		crowd.clear();

		// Rival restock uses last round per-item desired counts
		RivalAI.Result res = rivalAI.stockForBuyPhase(
				rivalShop,
				roundManager.getRivalCash(),
				roundManager.getLastDesiredByItemName(),
				catalog,
				cheapItem,
				premiumItem
				);
		roundManager.applyRivalAIResult(res);
	}

	// ============================================================
	// SELL phase update loop
	// ============================================================

	public void update(float delta) {
		if (!isSellPhase()) return;

		for (CustomerSpawner.VisualCustomer vc : crowd) {
			vc.update(delta);

			if (vc.justArrived()) {
				processArrivedCustomer(vc);
			}
		}

		// Once all customers arrive and transact, move to RESULTS
		if (spawner.isSellPhaseOver(crowd)) {
			roundManager.setResultsPhase();
		}
	}

	/**
	 * Resolves a single customer's transaction once they reach their target shop.
	 *
	 * Steps:
	 * 1) Compute the desired item based on the full market (both shops' in-stock types).
	 * 2) Execute the purchase from the customer's chosen shop.
	 * 3) Update per-item and legacy counters in RoundManager.
	 * 4) Record transaction details onto the VisualCustomer for UI display.
	 * 5) Write back customer memory for the next round.
	 */
	private void processArrivedCustomer(CustomerSpawner.VisualCustomer vc) {
		if (vc == null || vc.model == null || vc.targetShop == null) return;

		Customer c = vc.model;
		Shop chosenShop = vc.targetShop;

		// Desired item based on what exists anywhere in the market (affordable + preference scoring)
		List<Item> marketOptions = new ArrayList<>();
		marketOptions.addAll(playerShop.getItemsInStock());
		marketOptions.addAll(rivalShop.getItemsInStock());

		Item desired = logic.pickFromList(c, marketOptions);
		if (desired != null) {
			roundManager.incDesiredItem(desired.getName());

			if (sameItem(desired, cheapItem)) roundManager.incDesiredCheap();
			else if (sameItem(desired, premiumItem)) roundManager.incDesiredPremium();
		}

		// Purchase occurs at the chosen shop using that shop's inventory only
		vc.walletBefore = c.getWallet();
		Item bought = chosenShop.sell(c, logic);

		if (bought != null) {
			roundManager.incSoldItem(bought.getName());

			if (sameItem(bought, cheapItem)) roundManager.incSoldCheap();
			else if (sameItem(bought, premiumItem)) roundManager.incSoldPremium();
		}

		vc.walletAfter = c.getWallet();
		vc.spent = Math.max(0, vc.walletBefore - vc.walletAfter);

		vc.boughtName = (bought == null) ? null : bought.getName();
		vc.purchaseResult = (bought == null) ? "bought nothing" : ("bought " + vc.boughtName);

		// Customer memory informs next round shop choice (loyalty + stockout aversion)
		c.setLastShopName(chosenShop.getName());
		c.setGotWhatWantedLastTime(desired != null && desired.equals(bought));
	}

	// ============================================================
	// Getters (UI)
	// ============================================================

	public Shop getPlayerShop() { return playerShop; }
	public Shop getRivalShop() { return rivalShop; }

	public List<CustomerSpawner.VisualCustomer> getCrowd() { return crowd; }

	public int getNumCustomers() { return numCustomers; }

	public int getMarketCheapCount() { return marketCheapCount; }
	public int getMarketValueCount() { return marketValueCount; }
	public int getMarketConspCount() { return marketConspCount; }

	public double getPlayerCash() { return roundManager.getPlayerCash(); }
	public double getRivalCash() { return roundManager.getRivalCash(); }

	public Item getCheapItem() { return cheapItem; }
	public Item getPremiumItem() { return premiumItem; }

	public double getVendorCheapCost() { return vendorCheapCost; }
	public double getVendorPremiumCost() { return vendorPremiumCost; }

	public RoundManager getRoundManager() { return roundManager; }

	public List<Item> getCatalogItems() { return catalog.getItems(); }

	public double getVendorCost(Item item) { return catalog.getVendorCost(item); }

	// ============================================================
	// Internals
	// ============================================================

	private void updateMarketCountsForCurrentSelection() {
		MarketPlan.MarketCounts c = marketPlan.getCountsForCustomers(numCustomers);
		marketCheapCount = c.cheapCount;
		marketValueCount = c.valueCount;
		marketConspCount = c.conspCount;
	}

	private int clampEven(int value) {
		int v = value;
		if (v % 2 != 0) v++;
		if (v < minCustomers) v = minCustomers;
		if (v > maxCustomers) v = maxCustomers;
		if (v % 2 != 0) v++;
		if (v > maxCustomers) v = maxCustomers - (maxCustomers % 2);
		return v;
	}

	private boolean sameItem(Item a, Item b) {
		if (a == b) return true;
		if (a == null || b == null) return false;
		return a.getName() != null && a.getName().equals(b.getName());
	}

	private Item findCatalogItemByName(String name) {
		if (name == null) return null;
		for (Item i : catalog.getItems()) {
			if (i != null && name.equals(i.getName())) return i;
		}
		return null;
	}
}