package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tarron.marketsim.model.Customer;
import com.tarron.marketsim.model.DecisionLogic;
import com.tarron.marketsim.model.Item;
import com.tarron.marketsim.model.Shop;
import com.tarron.marketsim.simulation.CustomerSpawner.PathPoint;
import com.tarron.marketsim.simulation.CustomerSpawner.SpawnSide;

/**
 * MarketEngine
 *
 * Responsibilities:
 * - Owns simulation state for an active session.
 * - Controls phase flow (BUY -> SELL -> RESULTS -> BUY).
 * - Coordinates the simulation subsystems:
 *   - MarketPlan: customer population planning + customer creation
 *   - CustomerSpawner: VisualCustomer spawning + movement targets
 *   - DecisionLogic: shop routing + item choice
 *   - RivalAI: rival stocking decisions during BUY
 *   - RoundManager: cash, round count, counters, snapshots, game-over state
 *
 * SELL flow:
 * - Customer follows a road path to the first shop entrance.
 * - Customer disappears into the shop and waits briefly.
 * - If no purchase occurs, customer follows a road path to the second shop.
 * - Customer then exits along the road toward a screen-edge exit point.
 */
public class MarketEngine {

	// ============================================================
	// Configuration
	// ============================================================

	private static final float SHOPPING_DELAY_SECONDS = 3f;

	/*
	 * Vertical entrance offset used so customers vanish/reappear slightly closer
	 * to the doorway instead of too low near the sidewalk edge.
	 */
	private static final float ENTRANCE_VISUAL_Y_OFFSET = 50f;

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

	// ============================================================
	// SELL-phase routing anchors
	// ============================================================

	private float playerEntranceX;
	private float playerEntranceY;
	private float rivalEntranceX;
	private float rivalEntranceY;

	private float leftExitX;
	private float leftExitY;
	private float rightExitX;
	private float rightExitY;

	private float leftSpawnX;
	private float leftSpawnY;
	private float rightSpawnX;
	private float rightSpawnY;

	private float spawnLaneMinY;
	private float spawnLaneMaxY;

	/*
	 * Shared road lane used for waypoint routing.
	 *
	 * This is where customers move horizontally before going up into a shop or
	 * leaving toward an edge.
	 */
	private float routeLaneY;

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
		this.roundManager = new RoundManager(startingCash, 3);
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
		roundManager.addSpent(cost);
		playerShop.addItems(Collections.singletonList(item));
	}

	// ============================================================
	// Phase transitions
	// ============================================================

	/**
	 * Begins SELL using side-of-road spawn anchors, entrance targets, and exit targets.
	 */
	public void beginSellPhase(
			float leftSpawnX,
			float leftSpawnY,
			float rightSpawnX,
			float rightSpawnY,
			float spawnLaneMinY,
			float spawnLaneMaxY,
			float playerEntranceX,
			float playerEntranceY,
			float rivalEntranceX,
			float rivalEntranceY,
			float leftExitX,
			float leftExitY,
			float rightExitX,
			float rightExitY
			) {
		if (!isBuyPhase() || isGameOver()) return;

		this.leftSpawnX = leftSpawnX;
		this.leftSpawnY = leftSpawnY;
		this.rightSpawnX = rightSpawnX;
		this.rightSpawnY = rightSpawnY;
		this.spawnLaneMinY = spawnLaneMinY;
		this.spawnLaneMaxY = spawnLaneMaxY;

		this.playerEntranceX = playerEntranceX;
		this.playerEntranceY = playerEntranceY;
		this.rivalEntranceX = rivalEntranceX;
		this.rivalEntranceY = rivalEntranceY;

		this.leftExitX = leftExitX;
		this.leftExitY = leftExitY;
		this.rightExitX = rightExitX;
		this.rightExitY = rightExitY;

		this.routeLaneY = (leftExitY + rightExitY) * 0.55f;

		roundManager.beginSellPhase(playerShop, rivalShop);
		crowd.clear();

		crowd.addAll(spawner.spawnCustomersFromPlanWithShopChoice(
				marketPlan,
				logic,
				numCustomers,
				playerShop,
				rivalShop,
				leftSpawnX,
				leftSpawnY,
				rightSpawnX,
				rightSpawnY,
				spawnLaneMinY,
				spawnLaneMaxY,
				playerEntranceX,
				playerEntranceY,
				rivalEntranceX,
				rivalEntranceY
				));

		/*
		 * Assign the first outdoor path immediately after spawning.
		 */
		for (CustomerSpawner.VisualCustomer vc : crowd) {
			if (vc == null) continue;
			vc.setPath(buildPathToFirstShop(vc));
		}
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

			vc.update(delta, crowd);
			vc.updateInsideTimer(delta);

			if (vc.justArrived()) {
				processArrival(vc);
			}

			if ((vc.state == CustomerSpawner.VisualCustomer.State.INSIDE_FIRST_SHOP
					|| vc.state == CustomerSpawner.VisualCustomer.State.INSIDE_SECOND_SHOP)
					&& vc.insideTimerFinished()) {
				finishInsideShopDelay(vc);
			}
		}

		if (spawner.isSellPhaseOver(crowd)) {
			roundManager.setResultsPhase();
		}
	}

	/**
	 * Processes a single arrival event based on the customer's current state.
	 */
	private void processArrival(CustomerSpawner.VisualCustomer vc) {
		if (vc == null) return;

		switch (vc.state) {
		case WALK_TO_FIRST_SHOP:
			enterFirstShop(vc);
			break;

		case WALK_TO_SECOND_SHOP:
			enterSecondShop(vc);
			break;

		case EXITING:
			finishCustomer(vc);
			break;

		case INSIDE_FIRST_SHOP:
		case INSIDE_SECOND_SHOP:
		case DONE:
		default:
			break;
		}
	}

	// ============================================================
	// Shop visit flow
	// ============================================================

	private void enterFirstShop(CustomerSpawner.VisualCustomer vc) {
		if (vc == null || vc.model == null || vc.firstShop == null) return;

		vc.currentShop = vc.firstShop;
		vc.state = CustomerSpawner.VisualCustomer.State.INSIDE_FIRST_SHOP;
		vc.hide();
		vc.triedFirstShop = true;

		prepareShopVisit(vc, vc.firstShop, true);
	}

	private void enterSecondShop(CustomerSpawner.VisualCustomer vc) {
		if (vc == null || vc.model == null || vc.secondShop == null) return;

		vc.currentShop = vc.secondShop;
		vc.state = CustomerSpawner.VisualCustomer.State.INSIDE_SECOND_SHOP;
		vc.hide();
		vc.triedSecondShop = true;

		prepareShopVisit(vc, vc.secondShop, false);
	}

	/**
	 * Prepares one shop visit and starts the shopping delay.
	 */
	private void prepareShopVisit(
			CustomerSpawner.VisualCustomer vc,
			Shop visitedShop,
			boolean firstVisit
			) {
		if (vc == null || vc.model == null || visitedShop == null) return;

		Customer c = vc.model;

		List<Item> marketOptions = new ArrayList<>();
		marketOptions.addAll(playerShop.getItemsInStock());
		marketOptions.addAll(rivalShop.getItemsInStock());

		Item desired = logic.pickFromList(c, marketOptions);

		if (firstVisit && desired != null && desired.getName() != null) {
			roundManager.incDesiredItem(desired.getName());
		}

		vc.walletBefore = c.getWallet();

		Item bought = visitedShop.sell(c, logic);

		if (bought != null && bought.getName() != null) {
			roundManager.incSoldItem(bought.getName());
		}

		vc.walletAfter = c.getWallet();
		vc.spent = Math.max(0.0, vc.walletBefore - vc.walletAfter);
		vc.boughtName = (bought == null) ? null : bought.getName();

		c.setLastShopName(visitedShop.getName());
		c.setGotWhatWantedLastTime(desired != null && desired.equals(bought));

		vc.pendingVisitedShop = visitedShop;
		vc.pendingWasFirstVisit = firstVisit;
		vc.pendingPurchaseSuccess = (bought != null);
		vc.insideShopTimer = SHOPPING_DELAY_SECONDS;

		if (bought != null) {
			vc.purchaseResult = firstVisit
					? "bought " + vc.boughtName + " at first shop"
							: "bought " + vc.boughtName + " at second shop";
		} else {
			vc.purchaseResult = firstVisit
					? "no purchase at first shop"
							: "bought nothing after both shops";
		}
	}

	private void finishInsideShopDelay(CustomerSpawner.VisualCustomer vc) {
		if (vc == null || vc.pendingVisitedShop == null) return;

		Shop visitedShop = vc.pendingVisitedShop;
		boolean firstVisit = vc.pendingWasFirstVisit;
		boolean purchaseSucceeded = vc.pendingPurchaseSuccess;

		vc.insideShopTimer = 0f;
		vc.pendingVisitedShop = null;

		if (purchaseSucceeded) {
			sendCustomerToExit(vc, visitedShop);
			return;
		}

		if (firstVisit) {
			sendCustomerToSecondShop(vc, visitedShop);
			return;
		}

		sendCustomerToExit(vc, visitedShop);
	}

	/**
	 * Reappears the customer at the current shop entrance and routes them to the
	 * second shop entrance along the road.
	 */
	private void sendCustomerToSecondShop(
			CustomerSpawner.VisualCustomer vc,
			Shop fromShop
			) {
		if (vc == null || fromShop == null || vc.secondShop == null) return;

		float fromEntranceX = entranceXFor(fromShop);
		float fromEntranceY = entranceYFor(fromShop) + ENTRANCE_VISUAL_Y_OFFSET;

		vc.showAt(fromEntranceX, fromEntranceY);
		vc.currentShop = vc.secondShop;
		vc.state = CustomerSpawner.VisualCustomer.State.WALK_TO_SECOND_SHOP;
		vc.setPath(buildPathBetweenShops(fromShop, vc.secondShop));
	}

	/**
	 * Reappears the customer at the current shop entrance and routes them to a
	 * map exit point along the road.
	 *
	 * Exit side:
	 * - player shop -> left exit
	 * - rival shop  -> right exit
	 */
	private void sendCustomerToExit(
			CustomerSpawner.VisualCustomer vc,
			Shop fromShop
			) {
		if (vc == null || fromShop == null) return;

		float fromEntranceX = entranceXFor(fromShop);
		float fromEntranceY = entranceYFor(fromShop) + ENTRANCE_VISUAL_Y_OFFSET;

		vc.showAt(fromEntranceX, fromEntranceY);
		vc.state = CustomerSpawner.VisualCustomer.State.EXITING;
		vc.setPath(buildPathToExit(fromShop));
	}

	private void finishCustomer(CustomerSpawner.VisualCustomer vc) {
		if (vc == null) return;

		vc.visible = false;
		vc.state = CustomerSpawner.VisualCustomer.State.DONE;
	}

	// ============================================================
	// Path builders
	// ============================================================

	/**
	 * Build path:
	 * spawn side -> road edge -> road point below chosen shop -> entrance
	 */
	private List<PathPoint> buildPathToFirstShop(CustomerSpawner.VisualCustomer vc) {
		List<PathPoint> path = new ArrayList<>();
		if (vc == null || vc.firstShop == null) return path;

		float entranceX = entranceXFor(vc.firstShop);
		float entranceY = entranceYFor(vc.firstShop) + ENTRANCE_VISUAL_Y_OFFSET;

		if (vc.spawnSide == SpawnSide.LEFT) {
			path.add(new PathPoint(leftSpawnX, vc.y));
			path.add(new PathPoint(0f, routeLaneY));
		} else {
			path.add(new PathPoint(rightSpawnX, vc.y));
			path.add(new PathPoint(rightExitX - 40f, routeLaneY));
		}

		path.add(new PathPoint(entranceX, routeLaneY));
		path.add(new PathPoint(entranceX, entranceY));

		return path;
	}

	/**
	 * Build path:
	 * current entrance -> road below current shop -> road below other shop -> other entrance
	 */
	private List<PathPoint> buildPathBetweenShops(Shop fromShop, Shop toShop) {
		List<PathPoint> path = new ArrayList<>();
		if (fromShop == null || toShop == null) return path;

		float fromEntranceX = entranceXFor(fromShop);
		float fromEntranceY = entranceYFor(fromShop) + ENTRANCE_VISUAL_Y_OFFSET;

		float toEntranceX = entranceXFor(toShop);
		float toEntranceY = entranceYFor(toShop) + ENTRANCE_VISUAL_Y_OFFSET;

		path.add(new PathPoint(fromEntranceX, fromEntranceY));
		path.add(new PathPoint(fromEntranceX, routeLaneY));
		path.add(new PathPoint(toEntranceX, routeLaneY));
		path.add(new PathPoint(toEntranceX, toEntranceY));

		return path;
	}

	/**
	 * Build path:
	 * current entrance -> road below shop -> side exit
	 */
	private List<PathPoint> buildPathToExit(Shop fromShop) {
		List<PathPoint> path = new ArrayList<>();
		if (fromShop == null) return path;

		float fromEntranceX = entranceXFor(fromShop);
		float fromEntranceY = entranceYFor(fromShop) + ENTRANCE_VISUAL_Y_OFFSET;

		float exitX = (fromShop == playerShop) ? leftExitX : rightExitX;

		path.add(new PathPoint(fromEntranceX, fromEntranceY));

		// Move vertically back down to the road lane.
		path.add(new PathPoint(fromEntranceX, routeLaneY));

		// Stay on the road lane all the way to the screen edge.
		// Using routeLaneY here prevents the final diagonal movement.
		path.add(new PathPoint(exitX, routeLaneY));

		return path;
	}

	// ============================================================
	// Route helpers
	// ============================================================

	private float entranceXFor(Shop shop) {
		if (shop == playerShop) return playerEntranceX;
		if (shop == rivalShop) return rivalEntranceX;
		return playerEntranceX;
	}

	private float entranceYFor(Shop shop) {
		if (shop == playerShop) return playerEntranceY;
		if (shop == rivalShop) return rivalEntranceY;
		return playerEntranceY;
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