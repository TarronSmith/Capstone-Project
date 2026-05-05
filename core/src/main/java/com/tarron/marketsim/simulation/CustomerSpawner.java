package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.Collections;
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
 * - Assigns each customer a first-choice shop using DecisionLogic.
 * - Assigns a fallback second shop for customers who do not buy at the first.
 * - Spawns customers from the left/right road edges instead of a shared center stack.
 *
 * Notes:
 * - Movement and per-customer UI fields live on VisualCustomer.
 * - Transactions and state transitions are handled by MarketEngine.
 * - This version supports waypoint-based movement instead of a single direct target.
 */
public class CustomerSpawner {

	// ============================================================
	// Sprite selection
	// ============================================================

	private static final int TOTAL_CUSTOMER_SPRITES = 15;

	// ============================================================
	// Spawn sides
	// ============================================================

	public enum SpawnSide {
		LEFT,
		RIGHT
	}

	// ============================================================
	// Path point
	// ============================================================

	/**
	 * Small immutable waypoint container.
	 *
	 * Used to route customers along the road instead of sending them directly
	 * to the final destination in one straight line.
	 */
	public static class PathPoint {
		public final float x;
		public final float y;

		public PathPoint(float x, float y) {
			this.x = x;
			this.y = y;
		}
	}

	// ============================================================
	// VisualCustomer
	// ============================================================

	/**
	 * Bundles:
	 * - Customer model reference
	 * - Shop routing data
	 * - Movement state
	 * - Outdoor visibility state
	 * - UI log fields populated by MarketEngine
	 *
	 * Flow:
	 * - WALK_TO_FIRST_SHOP
	 * - INSIDE_FIRST_SHOP
	 * - WALK_TO_SECOND_SHOP
	 * - INSIDE_SECOND_SHOP
	 * - EXITING
	 * - DONE
	 */
	public static class VisualCustomer {

		public enum State {
			WALK_TO_FIRST_SHOP,
			INSIDE_FIRST_SHOP,
			WALK_TO_SECOND_SHOP,
			INSIDE_SECOND_SHOP,
			EXITING,
			DONE
		}

		// ========================================================
		// Core model + routing
		// ========================================================

		public Customer model;

		public Shop firstShop;
		public Shop secondShop;
		public Shop currentShop;

		public int spriteIndex = 0;

		/*
		 * Spawn origin used when building the first outdoor path.
		 */
		public SpawnSide spawnSide = SpawnSide.LEFT;

		// ========================================================
		// State + visibility
		// ========================================================

		public State state = State.WALK_TO_FIRST_SHOP;

		public boolean visible = true;

		public boolean triedFirstShop = false;
		public boolean triedSecondShop = false;

		public float insideShopTimer = 0f;

		public boolean pendingPurchaseSuccess = false;
		public boolean pendingWasFirstVisit = false;
		public Shop pendingVisitedShop = null;

		// ========================================================
		// UI / logging fields
		// ========================================================

		public double walletBefore = 0.0;
		public double walletAfter = 0.0;
		public double spent = 0.0;
		public String boughtName = null;
		public String purchaseResult = null;

		// ========================================================
		// Movement
		// ========================================================

		public float x;
		public float y;

		public boolean arrived = false;
		private boolean arrivalConsumed = false;

		/*
		 * Waypoint path.
		 *
		 * The customer moves toward the current waypoint until it is reached,
		 * then advances to the next. Arrival is triggered only when the final
		 * waypoint is reached.
		 */
		public final List<PathPoint> pathPoints = new ArrayList<>();
		public int pathIndex = 0;

		public VisualCustomer(Customer model, float x, float y) {
			this.model = model;
			this.x = x;
			this.y = y;
		}

		public VisualCustomer(
				Customer model,
				float x,
				float y,
				Shop firstShop,
				Shop secondShop,
				SpawnSide spawnSide
				) {
			this.model = model;
			this.x = x;
			this.y = y;
			this.firstShop = firstShop;
			this.secondShop = secondShop;
			this.currentShop = firstShop;
			this.state = State.WALK_TO_FIRST_SHOP;
			this.spawnSide = spawnSide;
		}

		/**
		 * Moves along the current waypoint path.
		 *
		 * Road-following:
		 * - Movement is directed toward the current waypoint.
		 * - When a waypoint is reached, the next waypoint becomes active.
		 * - Arrival is triggered only when the final waypoint is reached.
		 *
		 * Separation:
		 * - Visible customers slightly repel each other at short range.
		 * - Horizontal path segments suppress vertical repel so customers do not
		 *   drift down onto grass while spacing out.
		 */
		public void update(float delta, List<VisualCustomer> crowd) {
			if (state == State.DONE) return;
			if (state == State.INSIDE_FIRST_SHOP || state == State.INSIDE_SECOND_SHOP) return;
			if (arrived) return;
			if (pathPoints.isEmpty()) return;
			if (pathIndex < 0 || pathIndex >= pathPoints.size()) return;

			final float speed = 140f;

			/*
			 * Increased slightly so customers start spacing out a bit earlier.
			 */
			final float separationRadius = 26f;
			final float pathWeight = 1.5f;
			final float separationWeight = 0.95f;

			PathPoint target = pathPoints.get(pathIndex);

			float toTargetX = target.x - x;
			float toTargetY = target.y - y;
			float distToTarget = (float) Math.sqrt(toTargetX * toTargetX + toTargetY * toTargetY);

			float step = speed * delta;

			/*
			 * Snap to waypoint when close enough.
			 *
			 * Final waypoint:
			 * - marks the customer as arrived for the current state
			 *
			 * Intermediate waypoint:
			 * - advances to the next waypoint and continues next frame
			 */
			if (distToTarget <= step || distToTarget <= 0.0001f) {
				x = target.x;
				y = target.y;

				if (pathIndex >= pathPoints.size() - 1) {
					arrived = true;
				} else {
					pathIndex++;
				}
				return;
			}

			float desiredX = toTargetX / distToTarget;
			float desiredY = toTargetY / distToTarget;

			/*
			 * Local separation from nearby visible customers.
			 */
			float repelX = 0f;
			float repelY = 0f;

			if (crowd != null) {
				for (VisualCustomer other : crowd) {
					if (other == null || other == this) continue;
					if (!other.visible) continue;
					if (other.state == State.DONE) continue;

					float dx = x - other.x;
					float dy = y - other.y;
					float dist = (float) Math.sqrt(dx * dx + dy * dy);

					if (dist <= 0.0001f) continue;
					if (dist >= separationRadius) continue;

					float strength = (separationRadius - dist) / separationRadius;
					repelX += (dx / dist) * strength;
					repelY += (dy / dist) * strength;
				}
			}

			/*
			 * Keep customers tighter to road lanes.
			 *
			 * Horizontal path segments:
			 * - suppress vertical repel so customers do not get pushed onto grass
			 *
			 * Vertical path segments:
			 * - keep full repel so they can still separate while queuing into a shop
			 */
			boolean mostlyHorizontalSegment = Math.abs(toTargetX) > Math.abs(toTargetY);

			float adjustedRepelX = repelX;
			float adjustedRepelY = mostlyHorizontalSegment ? 0f : repelY;

			float moveX = desiredX * pathWeight + adjustedRepelX * separationWeight;
			float moveY = desiredY * pathWeight + adjustedRepelY * separationWeight;

			float moveLen = (float) Math.sqrt(moveX * moveX + moveY * moveY);

			if (moveLen <= 0.0001f) {
				moveX = desiredX;
				moveY = desiredY;
				moveLen = 1f;
			}

			moveX /= moveLen;
			moveY /= moveLen;

			x += moveX * step;
			y += moveY * step;
		}

		/**
		 * Advances the inside-shop timer when the customer is hidden in a shop.
		 */
		public void updateInsideTimer(float delta) {
			if (state != State.INSIDE_FIRST_SHOP && state != State.INSIDE_SECOND_SHOP) return;
			if (insideShopTimer > 0f) insideShopTimer -= delta;
		}

		/**
		 * True when the current inside-shop delay has completed.
		 */
		public boolean insideTimerFinished() {
			return insideShopTimer <= 0f;
		}

		/**
		 * Replaces the current waypoint path.
		 *
		 * Arrival is re-armed so MarketEngine can react once the final waypoint
		 * of the new path is reached.
		 */
		public void setPath(List<PathPoint> newPath) {
			pathPoints.clear();
			if (newPath != null) {
				pathPoints.addAll(newPath);
			}
			pathIndex = 0;
			arrived = false;
			arrivalConsumed = false;
		}

		/**
		 * Shows the customer at a world position.
		 */
		public void showAt(float worldX, float worldY) {
			this.x = worldX;
			this.y = worldY;
			this.visible = true;
			this.arrived = false;
			this.arrivalConsumed = false;
		}

		/**
		 * Hides the customer while preserving current world position.
		 */
		public void hide() {
			this.visible = false;
		}

		/**
		 * True exactly once when the customer reaches the final waypoint of the
		 * active path.
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
	// Spawn
	// ============================================================

	/**
	 * Spawns customers and assigns:
	 * - firstShop using DecisionLogic.pickShop(...)
	 * - secondShop as the fallback shop
	 *
	 * Initial outdoor positions come from the left/right edge of the road.
	 * Path routing is assigned later by MarketEngine.
	 */
	public List<VisualCustomer> spawnCustomersFromPlanWithShopChoice(
			MarketPlan marketPlan,
			DecisionLogic logic,
			int totalCustomers,
			Shop playerShop,
			Shop rivalShop,
			float leftSpawnX,
			float leftSpawnY,
			float rightSpawnX,
			float rightSpawnY,
			float spawnLaneMinY,
			float spawnLaneMaxY,
			float playerEntranceX,
			float playerEntranceY,
			float rivalEntranceX,
			float rivalEntranceY
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
					crowd,
					logic,
					c1,
					playerShop,
					rivalShop,
					visualIndex,
					leftSpawnX,
					leftSpawnY,
					rightSpawnX,
					rightSpawnY,
					spawnLaneMinY,
					spawnLaneMaxY
					);

			visualIndex = addCustomer(
					crowd,
					logic,
					c2,
					playerShop,
					rivalShop,
					visualIndex,
					leftSpawnX,
					leftSpawnY,
					rightSpawnX,
					rightSpawnY,
					spawnLaneMinY,
					spawnLaneMaxY
					);
		}

		Collections.shuffle(crowd);
		return crowd;
	}

	private int addCustomer(
			List<VisualCustomer> crowd,
			DecisionLogic logic,
			Customer customer,
			Shop playerShop,
			Shop rivalShop,
			int visualIndex,
			float leftSpawnX,
			float leftSpawnY,
			float rightSpawnX,
			float rightSpawnY,
			float spawnLaneMinY,
			float spawnLaneMaxY
			) {
		if (crowd == null) throw new IllegalArgumentException("crowd is null");
		if (customer == null) return visualIndex;

		Shop chosen = logic.pickShop(customer, playerShop, rivalShop);
		if (chosen == null) chosen = playerShop;

		Shop firstShop = chosen;
		Shop secondShop = (chosen == playerShop) ? rivalShop : playerShop;

		boolean spawnLeft = (visualIndex % 2 == 0);
		SpawnSide spawnSide = spawnLeft ? SpawnSide.LEFT : SpawnSide.RIGHT;

		float spawnX = spawnLeft ? leftSpawnX : rightSpawnX;
		float spawnBaseY = spawnLeft ? leftSpawnY : rightSpawnY;
		float spawnY = clamp(randf(spawnLaneMinY, spawnLaneMaxY), spawnLaneMinY, spawnLaneMaxY);

		if (spawnBaseY < spawnLaneMinY || spawnBaseY > spawnLaneMaxY) {
			spawnBaseY = spawnY;
		}

		VisualCustomer vc = new VisualCustomer(
				customer,
				spawnX,
				spawnY,
				firstShop,
				secondShop,
				spawnSide
				);

		vc.spriteIndex = randInt(0, TOTAL_CUSTOMER_SPRITES - 1);

		crowd.add(vc);
		return visualIndex + 1;
	}

	// ============================================================
	// Phase completion
	// ============================================================

	/**
	 * SELL phase ends once every customer reaches the DONE state.
	 */
	public boolean isSellPhaseOver(List<VisualCustomer> crowd) {
		if (crowd == null || crowd.isEmpty()) return true;

		for (VisualCustomer vc : crowd) {
			if (vc != null && vc.state != VisualCustomer.State.DONE) {
				return false;
			}
		}
		return true;
	}

	// ============================================================
	// Small utilities
	// ============================================================

	private static int randInt(int minInclusive, int maxInclusive) {
		if (maxInclusive <= minInclusive) return minInclusive;
		return ThreadLocalRandom.current().nextInt(minInclusive, maxInclusive + 1);
	}

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