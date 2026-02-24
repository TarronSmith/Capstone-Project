package com.tarron.marketsim.simulation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tarron.marketsim.model.Shop;

/**
 * RoundManager
 *
 * Purpose:
 * - Owns round/phase state (BUY -> SELL -> RESULTS -> BUY).
 * - Owns cash state for player and rival (resets each round to startingCash).
 * - Tracks per-round market signals:
 *   - "Desired" counts: what customers wanted (independent of stockouts).
 *   - "Sold" counts: what customers actually purchased (stock constrained).
 *
 * Tracking is done in two layers:
 * 1) Legacy demo counters: cheap vs premium (kept for compatibility with older HUD/AI displays).
 * 2) Current counters: per-item counts keyed by item name (drives updated HUD + multi-item RivalAI).
 *
 * Notes:
 * - Desired/Sold maps are snapshot at the end of RESULTS into last* maps so RivalAI can plan the next BUY phase.
 * - RoundManager does not simulate customer behavior; MarketEngine drives increments during SELL.
 */
public class RoundManager {

    // ============================================================
    // Phase
    // ============================================================

    public enum Phase { BUY, SELL, RESULTS }

    private Phase phase = Phase.BUY;

    // ============================================================
    // Cash rules
    // ============================================================

    private final double startingCash;
    private double playerCash;
    private double rivalCash;

    // If true, clears the player's inventory at the start of each BUY phase (after RESULTS).
    private final boolean clearPlayerInventoryEachRound;

    // ============================================================
    // Legacy demo tracking (cheap vs premium)
    // ============================================================

    private int lastDesiredCheap = 0;
    private int lastDesiredPremium = 0;

    private int currentDesiredCheap = 0;
    private int currentDesiredPremium = 0;

    private int currentSoldCheap = 0;
    private int currentSoldPremium = 0;

    // ============================================================
    // Current tracking (per item name)
    // ============================================================

    private final Map<String, Integer> currentDesiredByItemName = new HashMap<>();
    private final Map<String, Integer> currentSoldByItemName = new HashMap<>();

    private final Map<String, Integer> lastDesiredByItemName = new HashMap<>();
    private final Map<String, Integer> lastSoldByItemName = new HashMap<>();

    // ============================================================
    // Rival AI summary (for HUD)
    // ============================================================

    private String rivalStrategyLabel = "UNKNOWN (starter mix)";

    // Legacy ratios retained for HUD continuity.
    private double lastDemandPremiumRatio = 0.0;
    private double lastStockPremiumRatio = 0.0;

    // Legacy planned counts retained for HUD continuity.
    private int lastPlannedCheap = 0;
    private int lastPlannedPremium = 0;

    // Optimistic estimate of revenue implied by rival plan (AI-produced).
    private double lastExpectedRevenue = 0.0;

    // ============================================================
    // Construction
    // ============================================================

    public RoundManager(double startingCash, boolean clearPlayerInventoryEachRound) {
        this.startingCash = startingCash;
        this.clearPlayerInventoryEachRound = clearPlayerInventoryEachRound;

        this.playerCash = startingCash;
        this.rivalCash = startingCash;
    }

    // ============================================================
    // Phase access
    // ============================================================

    public Phase getPhase() { return phase; }

    // ============================================================
    // Cash access
    // ============================================================

    public double getPlayerCash() { return playerCash; }
    public double getRivalCash() { return rivalCash; }

    public void setPlayerCash(double cash) { this.playerCash = cash; }
    public void setRivalCash(double cash) { this.rivalCash = cash; }

    // ============================================================
    // Rival AI summary access
    // ============================================================

    public String getRivalStrategyLabel() { return rivalStrategyLabel; }
    public double getLastDemandPremiumRatio() { return lastDemandPremiumRatio; }
    public double getLastStockPremiumRatio() { return lastStockPremiumRatio; }

    public int getLastPlannedCheap() { return lastPlannedCheap; }
    public int getLastPlannedPremium() { return lastPlannedPremium; }
    public double getLastExpectedRevenue() { return lastExpectedRevenue; }

    // ============================================================
    // Legacy demo counters access
    // ============================================================

    public int getLastDesiredCheap() { return lastDesiredCheap; }
    public int getLastDesiredPremium() { return lastDesiredPremium; }

    public int getCurrentDesiredCheap() { return currentDesiredCheap; }
    public int getCurrentDesiredPremium() { return currentDesiredPremium; }

    public int getCurrentSoldCheap() { return currentSoldCheap; }
    public int getCurrentSoldPremium() { return currentSoldPremium; }

    // ============================================================
    // Per-item counters access (HUD + RivalAI use these)
    // ============================================================

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

    // ============================================================
    // Increment helpers (MarketEngine calls these during SELL)
    // ============================================================

    // --- Legacy demo increments ---
    public void incDesiredCheap() { currentDesiredCheap++; }
    public void incDesiredPremium() { currentDesiredPremium++; }
    public void incSoldCheap() { currentSoldCheap++; }
    public void incSoldPremium() { currentSoldPremium++; }

    // --- Per-item increments ---
    public void incDesiredItem(String itemName) {
        if (itemName == null) return;
        currentDesiredByItemName.put(itemName, currentDesiredByItemName.getOrDefault(itemName, 0) + 1);
    }

    public void incSoldItem(String itemName) {
        if (itemName == null) return;
        currentSoldByItemName.put(itemName, currentSoldByItemName.getOrDefault(itemName, 0) + 1);
    }

    // ============================================================
    // Phase transitions
    // ============================================================

    /**
     * BUY -> SELL transition.
     * Resets per-turn stats and clears current round counters.
     */
    public void beginSellPhase(Shop playerShop, Shop rivalShop) {
        playerShop.resetTurnStats();
        rivalShop.resetTurnStats();

        // Reset legacy counters.
        currentDesiredCheap = 0;
        currentDesiredPremium = 0;
        currentSoldCheap = 0;
        currentSoldPremium = 0;

        // Reset per-item counters.
        currentDesiredByItemName.clear();
        currentSoldByItemName.clear();

        phase = Phase.SELL;
    }

    /**
     * SELL -> RESULTS transition.
     * MarketEngine triggers this when all customers have arrived.
     */
    public void setResultsPhase() {
        phase = Phase.RESULTS;
    }

    /**
     * RESULTS -> BUY transition.
     *
     * Responsibilities:
     * - Snapshots "current" desired/sold into "last" for the next round's RivalAI planning.
     * - Resets cash back to startingCash.
     * - Applies inventory policy for the next round.
     */
    public void startNextBuyPhase(Shop playerShop, Shop rivalShop) {
        // Snapshot legacy desired.
        lastDesiredCheap = currentDesiredCheap;
        lastDesiredPremium = currentDesiredPremium;

        int total = lastDesiredCheap + lastDesiredPremium;
        lastDemandPremiumRatio = (total == 0) ? 0.0 : (lastDesiredPremium / (double) total);

        // Snapshot per-item maps.
        lastDesiredByItemName.clear();
        lastDesiredByItemName.putAll(currentDesiredByItemName);

        lastSoldByItemName.clear();
        lastSoldByItemName.putAll(currentSoldByItemName);

        // Reset cash for the new BUY phase.
        playerCash = startingCash;
        rivalCash = startingCash;

        // Inventory policy for the new round.
        if (clearPlayerInventoryEachRound) {
            playerShop.clearInventory();
        }
        rivalShop.clearInventory();

        phase = Phase.BUY;
    }

    // ============================================================
    // Rival AI integration
    // ============================================================

    /**
     * Copies RivalAI result fields into RoundManager so HUD can display them.
     * RivalAI is responsible for actually stocking rivalShop.
     */
    public void applyRivalAIResult(RivalAI.Result res) {
        if (res == null) return;

        rivalCash = res.remainingCash;
        rivalStrategyLabel = res.strategyLabel;

        lastDemandPremiumRatio = res.demandPremiumRatio;
        lastStockPremiumRatio = res.stockPremiumRatio;

        lastPlannedCheap = res.plannedCheap;
        lastPlannedPremium = res.plannedPremium;

        lastExpectedRevenue = res.expectedRevenue;
    }

    // ============================================================
    // Reset
    // ============================================================

    /**
     * Hard reset for restarting a session.
     *
     * wipeKnowledge:
     * - true: clears last-round knowledge (demand history, AI label, ratios)
     * - false: keeps last-round maps/counters intact (useful for some debugging flows)
     */
    public void hardReset(Shop playerShop, Shop rivalShop, boolean wipeKnowledge) {
        playerShop.resetTurnStats();
        rivalShop.resetTurnStats();

        playerShop.clearInventory();
        rivalShop.clearInventory();

        playerCash = startingCash;
        rivalCash = startingCash;

        // Clear current round counters.
        currentDesiredCheap = 0;
        currentDesiredPremium = 0;
        currentSoldCheap = 0;
        currentSoldPremium = 0;

        currentDesiredByItemName.clear();
        currentSoldByItemName.clear();

        if (wipeKnowledge) {
            // Clear last-round legacy counters.
            lastDesiredCheap = 0;
            lastDesiredPremium = 0;

            // Clear last-round per-item maps.
            lastDesiredByItemName.clear();
            lastSoldByItemName.clear();

            // Clear rival AI display fields.
            lastDemandPremiumRatio = 0.0;
            lastStockPremiumRatio = 0.0;

            lastPlannedCheap = 0;
            lastPlannedPremium = 0;
            lastExpectedRevenue = 0.0;

            rivalStrategyLabel = "UNKNOWN (starter mix)";
        }

        phase = Phase.BUY;
    }
}