package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.tarron.marketsim.model.Customer;
import com.tarron.marketsim.model.DecisionLogic;
import com.tarron.marketsim.model.Shop;

/**
 * CustomerSpawner
 *
 * Responsibilities:
 * - Builds the SELL-phase crowd as VisualCustomer objects.
 * - Assigns each customer a target shop using DecisionLogic.
 * - Assigns movement targets inside the chosen shop rectangle.
 *
 * Notes:
 * - Movement and per-customer UI fields live on VisualCustomer.
 * - Transactions and customer memory updates are handled by MarketEngine on arrival.
 */
public class CustomerSpawner {

	// ============================================================
	// VisualCustomer
	// ============================================================

	/**
	 * Bundles:
	 * - Customer model reference
	 * - Movement state
	 * - Target shop selection
	 * - UI log fields populated after arrival
	 */
	public static class VisualCustomer {
		public Customer model;
		public Shop targetShop;

		// Set by MarketEngine after arrival
		public double walletBefore = 0.0;
		public double walletAfter = 0.0;
		public double spent = 0.0;
		public String boughtName = null;
		public String purchaseResult = null;

		// Movement
		public float x, y;
		public float targetX, targetY;

		public boolean arrived = false;
		private boolean arrivalConsumed = false;

		public VisualCustomer(Customer model, float x, float y) {
			this.model = model;
			this.x = x;
			this.y = y;
		}

		public VisualCustomer(Customer model, float x, float y, float targetX, float targetY, Shop targetShop) {
			this.model = model;
			this.x = x;
			this.y = y;
			this.targetX = targetX;
			this.targetY = targetY;
			this.targetShop = targetShop;
		}

		/** Moves toward (targetX, targetY) at a fixed speed until arrival. */
		public void update(float delta) {
			if (arrived) return;

			final float speed = 140f;

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

			x += nx * step;
			y += ny * step;
		}

		/** True exactly once when the customer transitions into the arrived state. */
		public boolean justArrived() {
			if (arrived && !arrivalConsumed) {
				arrivalConsumed = true;
				return true;
			}
			return false;
		}
	}

	// ============================================================
	// Spawn
	// ============================================================

	/**
	 * Spawns customers and assigns each one a target shop using DecisionLogic.pickShop(...).
	 * Movement targets land inside the chosen shop rectangle.
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
		List<Integer> pairSlots = marketPlan.getPairSlotsForCustomers(evenCustomers);

		List<VisualCustomer> crowd = new ArrayList<>();
		int visualIndex = 0;

		for (int ignoredSlot : pairSlots) {
			Customer c1 = marketPlan.makeCustomer();
			Customer c2 = marketPlan.makeCustomer();

			visualIndex = addCustomer(
					crowd, logic, c1,
					playerShop, rivalShop,
					startX, startY, spacing, visualIndex,
					playerX, playerY, rivalX, rivalY,
					shopW, shopH,
					customerRadius, paddingInside
					);

			visualIndex = addCustomer(
					crowd, logic, c2,
					playerShop, rivalShop,
					startX, startY, spacing, visualIndex,
					playerX, playerY, rivalX, rivalY,
					shopW, shopH,
					customerRadius, paddingInside
					);
		}

		return crowd;
	}

	private int addCustomer(
			List<VisualCustomer> crowd,
			DecisionLogic logic,
			Customer customer,
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
		if (crowd == null) throw new IllegalArgumentException("crowd is null");
		if (customer == null) return visualIndex;

		Shop chosen = logic.pickShop(customer, playerShop, rivalShop);
		if (chosen == null) chosen = playerShop;

		float shopLeftX = (chosen == playerShop) ? playerX : rivalX;
		float shopBottomY = (chosen == playerShop) ? playerY : rivalY;

		float minX = shopLeftX + paddingInside + customerRadius;
		float maxX = shopLeftX + shopW - paddingInside - customerRadius;

		float minY = shopBottomY + paddingInside + customerRadius;
		float maxY = shopBottomY + shopH - paddingInside - customerRadius;

		float targetX = clamp(randf(minX, maxX), minX, maxX);
		float targetY = clamp(randf(minY, maxY), minY, maxY);

		crowd.add(new VisualCustomer(
				customer,
				startX,
				startY + (visualIndex * spacing),
				targetX,
				targetY,
				chosen
				));

		return visualIndex + 1;
	}

	// ============================================================
	// Phase completion
	// ============================================================

	/** SELL phase ends once all customers have arrived at their target points. */
	public boolean isSellPhaseOver(List<VisualCustomer> crowd) {
		if (crowd == null || crowd.isEmpty()) return true;
		for (VisualCustomer vc : crowd) {
			if (vc != null && !vc.arrived) return false;
		}
		return true;
	}

	// ============================================================
	// Small utilities
	// ============================================================

	private static float randf(float min, float max) {
		if (max <= min) return min;
		return (float) ThreadLocalRandom.current().nextDouble(min, max);
	}

	private static float clamp(float v, float lo, float hi) {
		if (v < lo) return lo;
		if (v > hi) return hi;
		return v;
	}
}