package com.tarron.marketsim.simulation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tarron.marketsim.model.Shop;

/**
 * RoundManager
 *
 * Responsibilities:
 * - Owns phase state (BUY, SELL, RESULTS) and phase transitions.
 * - Tracks session cash for player and rival.
 * - Tracks per-round desired/sold counts by item name and snapshots them for the next round.
 * - Stores last-round financial summary data for UI display.
 * - Stores RivalAI summary data for HUD/debug.
 * - Tracks round number and determines outcome when the final round completes.
 *
 * Notes:
 * - Counters are keyed by item name to remain compatible with any catalog size.
 * - Revenue is applied to cash when leaving RESULTS (before resetting shop stats).
 * - Player spending is tracked during BUY phase through addSpent(...).
 */
public class RoundManager {

	public enum Phase {BUY, SELL, RESULTS}
	public enum Outcome {NONE, WIN, LOSS, TIE}

	private Phase phase = Phase.BUY;

	private double playerCash;
	private double rivalCash;

	private int roundNumber = 1;   // 1-based
	private int maxRounds;

	private boolean gameOver = false;
	private Outcome outcome = Outcome.NONE;

	// Rival AI summary (HUD/debug)
	private String rivalStrategyLabel = "UNKNOWN";
	private double lastExpectedRevenue = 0.0;
	private final Map<String, Integer> lastRivalPlannedByItemName = new HashMap<>();

	// Current round counters
	private final Map<String, Integer> currentDesiredByItemName = new HashMap<>();
	private final Map<String, Integer> currentSoldByItemName = new HashMap<>();

	// Snapshot from the last completed SELL round
	private final Map<String, Integer> lastDesiredByItemName = new HashMap<>();
	private final Map<String, Integer> lastSoldByItemName = new HashMap<>();

	// Current BUY-round spending before the round is finalized
	private double currentRoundSpent = 0.0;

	// Last completed round financial summary
	private double lastRoundSpent = 0.0;
	private double lastRoundRevenue = 0.0;
	private double lastRoundProfit = 0.0;

	// Last completed round top item summaries
	private String lastTopDesired = null;
	private String lastTopSold = null;

	public RoundManager(double startingCash) {
		this(startingCash, 10);
	}

	public RoundManager(double startingCash, int maxRounds) {
		this.playerCash = startingCash;
		this.rivalCash = startingCash;
		this.maxRounds = Math.max(1, maxRounds);
	}

	// ============================================================
	// Game loop state
	// ============================================================

	public int getRoundNumber() { return roundNumber; }
	public int getMaxRounds() { return maxRounds; }

	public void setMaxRounds(int maxRounds) {
		if (maxRounds < 1) return;
		this.maxRounds = maxRounds;
		if (roundNumber > this.maxRounds) roundNumber = this.maxRounds;
	}

	public boolean isGameOver() { return gameOver; }
	public Outcome getOutcome() { return outcome; }

	// ============================================================
	// Reset
	// ============================================================

	public void hardReset(Shop playerShop, Shop rivalShop, boolean wipeKnowledge) {
		phase = Phase.BUY;

		roundNumber = 1;
		gameOver = false;
		outcome = Outcome.NONE;

		if (playerShop != null) playerShop.resetTurnStats();
		if (rivalShop != null) rivalShop.resetTurnStats();

		currentDesiredByItemName.clear();
		currentSoldByItemName.clear();

		currentRoundSpent = 0.0;
		lastRoundSpent = 0.0;
		lastRoundRevenue = 0.0;
		lastRoundProfit = 0.0;
		lastTopDesired = null;
		lastTopSold = null;

		if (wipeKnowledge) {
			lastDesiredByItemName.clear();
			lastSoldByItemName.clear();

			rivalStrategyLabel = "UNKNOWN";
			lastExpectedRevenue = 0.0;
			lastRivalPlannedByItemName.clear();
		}
	}

	// ============================================================
	// Phase transitions
	// ============================================================

	public void beginSellPhase(Shop playerShop, Shop rivalShop) {
		if (gameOver) return;

		phase = Phase.SELL;

		if (playerShop != null) playerShop.resetTurnStats();
		if (rivalShop != null) rivalShop.resetTurnStats();

		currentDesiredByItemName.clear();
		currentSoldByItemName.clear();
	}

	public void setResultsPhase() {
		if (gameOver) return;
		phase = Phase.RESULTS;
	}

	/**
	 * Advances from RESULTS to either:
	 * - game over (after final round), or
	 * - next BUY round
	 *
	 * Order:
	 * 1) Capture and apply revenue
	 * 2) Snapshot current maps to last maps
	 * 3) Compute last-round summary values
	 * 4) Clear current maps
	 * 5) Reset shop stats
	 * 6) End game or increment round and return to BUY
	 */
	public void advanceAfterResults(Shop playerShop, Shop rivalShop) {
		if (gameOver) return;
		if (phase != Phase.RESULTS) return;

		// 1) Capture and transfer revenue into cash
		double playerRevenue = 0.0;
		double rivalRevenue = 0.0;

		if (playerShop != null) {
			playerRevenue = playerShop.getRevenueThisTurn();
			playerCash += playerRevenue;
		}

		if (rivalShop != null) {
			rivalRevenue = rivalShop.getRevenueThisTurn();
			rivalCash += rivalRevenue;
		}

		// 2) Snapshot current counters
		lastDesiredByItemName.clear();
		lastDesiredByItemName.putAll(currentDesiredByItemName);

		lastSoldByItemName.clear();
		lastSoldByItemName.putAll(currentSoldByItemName);

		// 3) Compute last-round summary values
		lastRoundSpent = currentRoundSpent;
		lastRoundRevenue = playerRevenue;
		lastRoundProfit = lastRoundRevenue - lastRoundSpent;

		lastTopDesired = findTopItem(lastDesiredByItemName);
		lastTopSold = findTopItem(lastSoldByItemName);

		// 4) Clear current counters and spending accumulator
		currentDesiredByItemName.clear();
		currentSoldByItemName.clear();
		currentRoundSpent = 0.0;

		// 5) Reset shop stats
		if (playerShop != null) playerShop.resetTurnStats();
		if (rivalShop != null)  rivalShop.resetTurnStats();

		// 6) Final round check
		if (roundNumber >= maxRounds) {
			gameOver = true;
			outcome = computeOutcome(playerCash, rivalCash);
			return;
		}

		// 7) Next BUY
		roundNumber++;
		phase = Phase.BUY;
	}

	private Outcome computeOutcome(double playerCash, double rivalCash) {
		double diff = playerCash - rivalCash;
		if (Math.abs(diff) < 1e-9) return Outcome.TIE;
		return (diff > 0) ? Outcome.WIN : Outcome.LOSS;
	}

	// ============================================================
	// Counting
	// ============================================================

	public void incDesiredItem(String itemName) {
		if (itemName == null) return;
		currentDesiredByItemName.put(itemName, currentDesiredByItemName.getOrDefault(itemName, 0) + 1);
	}

	public void incSoldItem(String itemName) {
		if (itemName == null) return;
		currentSoldByItemName.put(itemName, currentSoldByItemName.getOrDefault(itemName, 0) + 1);
	}

	/**
	 * Adds player spending during the BUY phase.
	 *
	 * This value is stored separately from cash because cash changes immediately
	 * when items are ordered, while last-round summaries are finalized after SELL.
	 */
	public void addSpent(double amount) {
		if (amount <= 0.0) return;
		if (Double.isNaN(amount) || Double.isInfinite(amount)) return;

		currentRoundSpent += amount;
	}

	// ============================================================
	// Summary helpers
	// ============================================================

	private String findTopItem(Map<String, Integer> map) {
		if (map == null || map.isEmpty()) return null;

		String bestName = null;
		int bestCount = 0;

		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			if (entry == null || entry.getKey() == null || entry.getValue() == null) continue;

			int count = entry.getValue();

			if (count > bestCount) {
				bestCount = count;
				bestName = entry.getKey();
			}
		}

		return bestName;
	}

	// ============================================================
	// Rival AI integration
	// ============================================================

	public void applyRivalAIResult(RivalAI.Result res) {
		if (res == null) return;

		rivalCash = res.remainingCash;
		rivalStrategyLabel = (res.strategyLabel == null) ? "UNKNOWN" : res.strategyLabel;
		lastExpectedRevenue = res.expectedRevenue;

		lastRivalPlannedByItemName.clear();
		if (res.plannedByItemName != null) {
			lastRivalPlannedByItemName.putAll(res.plannedByItemName);
		}
	}

	// ============================================================
	// Getters / setters
	// ============================================================

	public Phase getPhase() { return phase; }

	public double getPlayerCash() { return playerCash; }
	public void setPlayerCash(double v) { playerCash = v; }

	public double getRivalCash() { return rivalCash; }
	public void setRivalCash(double v) { rivalCash = v; }

	public String getRivalStrategyLabel() { return rivalStrategyLabel; }
	public double getLastExpectedRevenue() { return lastExpectedRevenue; }

	public double getCurrentRoundSpent() { return currentRoundSpent; }

	public double getLastRoundSpent() { return lastRoundSpent; }
	public double getLastRoundRevenue() { return lastRoundRevenue; }
	public double getLastRoundProfit() { return lastRoundProfit; }

	public String getLastTopDesired() { return lastTopDesired; }
	public String getLastTopSold() { return lastTopSold; }

	public Map<String, Integer> getCurrentDesiredByItemName() {
		return Collections.unmodifiableMap(currentDesiredByItemName);
	}

	public Map<String, Integer> getCurrentSoldByItemName() {
		return Collections.unmodifiableMap(currentSoldByItemName);
	}

	public Map<String, Integer> getLastDesiredByItemName() {
		return Collections.unmodifiableMap(lastDesiredByItemName);
	}

	public Map<String, Integer> getLastSoldByItemName() {
		return Collections.unmodifiableMap(lastSoldByItemName);
	}

	public Map<String, Integer> getLastRivalPlannedByItemName() {
		return Collections.unmodifiableMap(lastRivalPlannedByItemName);
	}
}