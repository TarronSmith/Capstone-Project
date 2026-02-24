package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.List;

import com.tarron.marketsim.model.Customer;
import com.tarron.marketsim.model.DecisionLogic;
import com.tarron.marketsim.model.Shop;

/**
 * CustomerSpawner
 *
 * Responsibility:
 * - Creates VisualCustomer instances for a SELL phase.
 * - Assigns each customer a destination shop (targetShop) and movement target (targetX/targetY).
 *
 * Notes:
 * - Handles visual spawning and movement only.
 * - Purchases and memory writeback are handled by MarketEngine when customers arrive.
 *
 * Spawn modes:
 * - spawnFairCustomersFromPlan: deterministic 50/50 split (legacy/demo baseline)
 * - spawnCustomersFromPlanWithShopChoice: customers choose a shop via DecisionLogic.pickShop(...)
 */
public class CustomerSpawner {

	// ============================================================
	// VisualCustomer: simulation model + movement + UI log fields
	// ============================================================

	/**
	 * Represents a single on-screen customer during a SELL phase.
	 *
	 * Fields:
	 * - model/targetShop are the simulation references
	 * - x/y and targetX/targetY define movement
	 * - walletBefore/After/spent/boughtName/purchaseResult are filled in by MarketEngine
	 */
	public static class VisualCustomer {
		public Customer model;
		public Shop targetShop;

		// Filled after arrival by MarketEngine.processArrivedCustomer(...)
		public double walletBefore = 0;
		public double walletAfter = 0;
		public double spent = 0;
		public String boughtName = null;
		public String purchaseResult = null;

		// Movement state
		public float x, y;
		public float targetX, targetY;

		public boolean arrived = false;
		private boolean arrivalConsumed = false;

		public VisualCustomer(Customer model, float x, float y) {
			this.model = model;
			this.x = x;
			this.y = y;
		}

		public VisualCustomer(
				Customer model,
				float x, float y,
				float targetX, float targetY,
				Shop targetShop
				) {
			this.model = model;
			this.x = x;
			this.y = y;
			this.targetX = targetX;
			this.targetY = targetY;
			this.targetShop = targetShop;
		}

		/**
		 * Moves toward (targetX, targetY) at a fixed speed until arrival.
		 */
		public void update(float delta) {
			if (arrived) return;

			float speed = 140f;
			float dx = targetX - x;
			float dy = targetY - y;
			float dist = (float) Math.sqrt(dx * dx + dy * dy);

			float step = speed * delta;

			if (dist <= step) {
				x = targetX;
				y = targetY;
				arrived = true;
				return;
			}

			float nx = dx / dist;
			float ny = dy / dist;

			x += nx * speed * delta;
			y += ny * speed * delta;
		}

		/**
		 * Used by MarketEngine to trigger the purchase logic exactly once.
		 */
		public boolean justArrived() {
			if (arrived && !arrivalConsumed) {
				arrivalConsumed = true;
				return true;
			}
			return false;
		}
	}

	// ============================================================
	// Spawn mode 1: fixed 50/50 split (legacy baseline)
	// ============================================================

	/**
	 * Creates an even-sized crowd where each market plan "pair type" produces:
	 * - one customer forced to Player
	 * - one customer forced to Rival
	 *
	 * This preserves the original demo behavior.
	 */
	public List<VisualCustomer> spawnFairCustomersFromPlan(
			MarketPlan marketPlan,
			int totalCustomers,
			Shop playerShop,
			Shop rivalShop,
			float startX,
			float startY,
			float spacing,
			float playerX,
			float playerY,
			float shopW,
			float shopH,
			float rivalX,
			float rivalY,
			float customerRadius,
			float paddingInside
			) {
		marketPlan.ensureInitialized();

		int evenCustomers = (totalCustomers % 2 == 0) ? totalCustomers : totalCustomers + 1;
		int pairsNeeded = evenCustomers / 2;

		List<String> pairTypes = marketPlan.getPairTypesForCustomers(evenCustomers);

		List<VisualCustomer> crowd = new ArrayList<>();
		int visualIndex = 0;

		for (int i = 0; i < pairsNeeded; i++) {
			String type = pairTypes.get(i);

			Customer cPlayer = marketPlan.makeCustomerFromType(type);
			Customer cRival = marketPlan.makeCustomerFromType(type);

			crowd.add(new VisualCustomer(
					cPlayer,
					startX,
					startY + (visualIndex * spacing),
					(playerX + shopW / 2f),
					(playerY + customerRadius + paddingInside),
					playerShop
					));
			visualIndex++;

			crowd.add(new VisualCustomer(
					cRival,
					startX,
					startY + (visualIndex * spacing),
					(rivalX + shopW / 2f),
					(rivalY + customerRadius + paddingInside),
					rivalShop
					));
			visualIndex++;
		}

		return crowd;
	}

	// ============================================================
	// Spawn mode 2: customer chooses shop via DecisionLogic
	// ============================================================

	/**
	 * Creates customers from the market plan and lets each one choose a shop.
	 *
	 * The market plan distribution remains the same:
	 * - pairTypes contains one entry per "pair"
	 * - each entry spawns two customers of that type
	 */
	public List<VisualCustomer> spawnCustomersFromPlanWithShopChoice(
			MarketPlan marketPlan,
			DecisionLogic logic,
			int totalCustomers,
			Shop playerShop,
			Shop rivalShop,
			float startX,
			float startY,
			float spacing,
			float playerX,
			float playerY,
			float shopW,
			float shopH,
			float rivalX,
			float rivalY,
			float customerRadius,
			float paddingInside
			) {
		if (marketPlan == null) throw new IllegalArgumentException("marketPlan is null");
		if (logic == null) throw new IllegalArgumentException("logic is null");
		if (playerShop == null) throw new IllegalArgumentException("playerShop is null");
		if (rivalShop == null) throw new IllegalArgumentException("rivalShop is null");

		marketPlan.ensureInitialized();

		int evenCustomers = (totalCustomers % 2 == 0) ? totalCustomers : totalCustomers + 1;

		List<String> pairTypes = marketPlan.getPairTypesForCustomers(evenCustomers);

		List<VisualCustomer> crowd = new ArrayList<>();
		int visualIndex = 0;

		for (String type : pairTypes) {
			Customer c1 = marketPlan.makeCustomerFromType(type);
			Customer c2 = marketPlan.makeCustomerFromType(type);

			visualIndex = addChosenCustomer(
					crowd, logic, c1,
					playerShop, rivalShop,
					startX, startY, spacing, visualIndex,
					playerX, playerY, rivalX, rivalY,
					shopW, shopH, customerRadius, paddingInside
					);

			visualIndex = addChosenCustomer(
					crowd, logic, c2,
					playerShop, rivalShop,
					startX, startY, spacing, visualIndex,
					playerX, playerY, rivalX, rivalY,
					shopW, shopH, customerRadius, paddingInside
					);
		}

		return crowd;
	}

	/**
	 * Assigns a shop, computes a destination point inside that shop, and appends
	 * the VisualCustomer to the crowd list.
	 */
	private int addChosenCustomer(
			List<VisualCustomer> crowd,
			DecisionLogic logic,
			Customer c,
			Shop playerShop,
			Shop rivalShop,
			float startX,
			float startY,
			float spacing,
			int visualIndex,
			float playerX,
			float playerY,
			float rivalX,
			float rivalY,
			float shopW,
			float shopH,
			float customerRadius,
			float paddingInside
			) {
		Shop chosen = logic.pickShop(c, playerShop, rivalShop);

		float targetX;
		float targetY;

		if (chosen == playerShop) {
			targetX = (playerX + shopW / 2f);
			targetY = (playerY + customerRadius + paddingInside);
		} else {
			targetX = (rivalX + shopW / 2f);
			targetY = (rivalY + customerRadius + paddingInside);
		}

		crowd.add(new VisualCustomer(
				c,
				startX,
				startY + (visualIndex * spacing),
				targetX,
				targetY,
				chosen
				));

		return visualIndex + 1;
	}

	// ============================================================
	// Phase helper
	// ============================================================

	/**
	 * SELL phase ends once all customers have arrived at their target point.
	 */
	public boolean isSellPhaseOver(List<VisualCustomer> crowd) {
		if (crowd == null || crowd.isEmpty()) return true;
		for (VisualCustomer vc : crowd) {
			if (!vc.arrived) return false;
		}
		return true;
	}
}