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
 * Responsibilities:
 * - Owns simulation state for an active session.
 * - Controls phase flow (BUY -> SELL -> RESULTS -> BUY).
 * - Coordinates the simulation subsystems:
 *   - MarketPlan: customer population planning + customer creation
 *   - CustomerSpawner: VisualCustomer spawning + movement targets
 *   - DecisionLogic: shop selection + item selection
 *   - RivalAI: rival stocking decisions during BUY
 *   - RoundManager: cash, round count, counters, snapshots, game-over state
 *
 * Game loop:
 * - During SELL: update movement, process arrivals, then transition to RESULTS.
 * - During RESULTS: apply revenue, snapshot counters, advance round or end game, then restock rival for next BUY.
 */
public class MarketEngine {

	// ============================================================
	// Configuration
	// ============================================================

	private final int minCustomers;
	private final int maxCustomers;
	private final float customerRadius;

	// ============================================================
	// Core objects
	// ============================================================

	private final Shop playerShop = new Shop();
	private final Shop rivalShop  = new Shop();

	private final ItemCatalog catalog = new ItemCatalog();
	private final DecisionLogic logic = new DecisionLogic();

	private final MarketPlan marketPlan;
	private final RivalAI rivalAI;
	private final RoundManager roundManager;
	private final CustomerSpawner spawner = new CustomerSpawner();

	// ============================================================
	// Session state
	// ============================================================

	private int numCustomers;

	private int marketTotalCustomers = 0;
	private int marketTotalPairs = 0;

	private final List<CustomerSpawner.VisualCustomer> crowd = new ArrayList<>();

	private boolean initialized = false;

	public MarketEngine(
			int initialCustomers,
			int minCustomers,
			int maxCustomers,
			double startingCash,
			float customerRadius
			) {
		this.minCustomers = minCustomers;
		this.maxCustomers = maxCustomers;
		this.customerRadius = customerRadius;

		this.numCustomers = clampEven(initialCustomers);

		playerShop.setName("Player Shop");
		rivalShop.setName("Rival Shop");

		this.marketPlan = new MarketPlan(maxCustomers, new CustomerProfileGenerator());
		this.rivalAI = new RivalAI();

		// Default: 10 rounds (RoundManager can clamp/validate internally).
		this.roundManager = new RoundManager(startingCash, 10);
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

	public RoundManager.Phase getPhase() { return roundManager.getPhase(); }

	public boolean isBuyPhase()     { return roundManager.getPhase() == RoundManager.Phase.BUY; }
	public boolean isSellPhase()    { return roundManager.getPhase() == RoundManager.Phase.SELL; }
	public boolean isResultsPhase() { return roundManager.getPhase() == RoundManager.Phase.RESULTS; }

	// ============================================================
	// Game loop helpers
	// ============================================================

	public boolean isGameOver() { return roundManager.isGameOver(); }
	public RoundManager.Outcome getOutcome() { return roundManager.getOutcome(); }

	// ============================================================
	// Session control
	// ============================================================

	public void hardReset(boolean wipeKnowledge) {
		crowd.clear();
		roundManager.hardReset(playerShop, rivalShop, wipeKnowledge);
		stockRivalForBuy();
	}

	public void rerollMarketAndReset() {
		marketPlan.reroll();
		updateMarketCountsForCurrentSelection();
		hardReset(true);
	}

	// ============================================================
	// BUY controls
	// ============================================================

	public void incCustomersBy2() {
		if (!isBuyPhase() || isGameOver()) return;
		numCustomers = clampEven(numCustomers + 2);
		updateMarketCountsForCurrentSelection();
	}

	public void decCustomersBy2() {
		if (!isBuyPhase() || isGameOver()) return;
		numCustomers = clampEven(numCustomers - 2);
		updateMarketCountsForCurrentSelection();
	}

	public void buyCatalogSlot(int slot1Based) {
		if (!isBuyPhase() || isGameOver()) return;

		Item item = catalog.getBySlot(slot1Based);
		if (item == null) return;

		buyItem(item);
	}

	public void buyItem(Item item) {
		if (!isBuyPhase() || isGameOver()) return;
		if (item == null) return;

		double cost = getVendorCost(item);
		if (!isFinitePositive(cost)) return;

		if (roundManager.getPlayerCash() + 1e-9 < cost) return;

		roundManager.setPlayerCash(roundManager.getPlayerCash() - cost);
		playerShop.addItems(Collections.singletonList(item));
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
		if (!isBuyPhase() || isGameOver()) return;

		roundManager.beginSellPhase(playerShop, rivalShop);
		crowd.clear();

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
		if (!isResultsPhase() || isGameOver()) return;

		crowd.clear();

		roundManager.advanceAfterResults(playerShop, rivalShop);

		if (roundManager.isGameOver()) return;

		stockRivalForBuy();
	}

	// ============================================================
	// SELL loop
	// ============================================================

	public void update(float delta) {
		if (!isSellPhase() || isGameOver()) return;

		for (CustomerSpawner.VisualCustomer vc : crowd) {
			if (vc == null) continue;

			vc.update(delta);

			if (vc.justArrived()) {
				processArrivedCustomer(vc);
			}
		}

		if (spawner.isSellPhaseOver(crowd)) {
			roundManager.setResultsPhase();
		}
	}

	private void processArrivedCustomer(CustomerSpawner.VisualCustomer vc) {
		if (vc == null || vc.model == null || vc.targetShop == null) return;

		Customer c = vc.model;
		Shop chosenShop = vc.targetShop;

		List<Item> marketOptions = new ArrayList<>();
		marketOptions.addAll(playerShop.getItemsInStock());
		marketOptions.addAll(rivalShop.getItemsInStock());

		Item desired = logic.pickFromList(c, marketOptions);
		if (desired != null && desired.getName() != null) {
			roundManager.incDesiredItem(desired.getName());
		}

		vc.walletBefore = c.getWallet();
		Item bought = chosenShop.sell(c, logic);

		if (bought != null && bought.getName() != null) {
			roundManager.incSoldItem(bought.getName());
		}

		vc.walletAfter = c.getWallet();
		vc.spent = Math.max(0, vc.walletBefore - vc.walletAfter);

		vc.boughtName = (bought == null) ? null : bought.getName();
		vc.purchaseResult = (bought == null) ? "bought nothing" : ("bought " + vc.boughtName);

		c.setLastShopName(chosenShop.getName());
		c.setGotWhatWantedLastTime(desired != null && desired.equals(bought));
	}

	// ============================================================
	// Getters
	// ============================================================

	public Shop getPlayerShop() { return playerShop; }
	public Shop getRivalShop()  { return rivalShop; }

	public List<CustomerSpawner.VisualCustomer> getCrowd() { return crowd; }

	public int getNumCustomers() { return numCustomers; }

	public int getMarketTotalCustomers() { return marketTotalCustomers; }
	public int getMarketTotalPairs() { return marketTotalPairs; }

	public RoundManager getRoundManager() { return roundManager; }

	public List<Item> getCatalogItems() { return catalog.getItems(); }
	public double getVendorCost(Item item) { return catalog.getVendorCost(item); }

	// ============================================================
	// Internals
	// ============================================================

	private void stockRivalForBuy() {
		RivalAI.Result res = rivalAI.stockForBuyPhase(
				rivalShop,
				roundManager.getRivalCash(),
				roundManager.getLastDesiredByItemName(),
				catalog
				);
		roundManager.applyRivalAIResult(res);
	}

	private void updateMarketCountsForCurrentSelection() {
		MarketPlan.MarketCounts c = marketPlan.getCountsForCustomers(numCustomers);
		marketTotalCustomers = c.totalCustomers;
		marketTotalPairs = c.totalPairs;
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

	private boolean isFinitePositive(double v) {
		return !Double.isNaN(v) && !Double.isInfinite(v) && v > 0.0;
	}
}