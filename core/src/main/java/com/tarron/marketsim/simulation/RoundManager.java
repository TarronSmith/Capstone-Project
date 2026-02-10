package com.tarron.marketsim.simulation;

import com.tarron.marketsim.model.Shop;

public class RoundManager {

    public enum Phase { BUY, SELL, RESULTS }

    private Phase phase = Phase.BUY;

    private final double startingCash;

    private double playerCash;
    private double rivalCash;

    // DEMAND TRACKING (DESIRED not SOLD)
    private int lastDesiredCheap = 0;
    private int lastDesiredPremium = 0;

    private int currentDesiredCheap = 0;
    private int currentDesiredPremium = 0;

    private int currentSoldCheap = 0;
    private int currentSoldPremium = 0;

    // Rival AI display info
    private String rivalStrategyLabel = "UNKNOWN (starter mix)";

    // From last round desired
    private double lastDemandPremiumRatio = 0.0;

    // From what the rival actually stocked in the BUY phase
    private double lastStockPremiumRatio = 0.0;

    // Optional: show the plan explicitly
    private int lastPlannedCheap = 0;
    private int lastPlannedPremium = 0;
    private double lastExpectedRevenue = 0.0;

    // Policy toggles
    private final boolean clearPlayerInventoryEachRound;

    public RoundManager(double startingCash, boolean clearPlayerInventoryEachRound) {
        this.startingCash = startingCash;
        this.clearPlayerInventoryEachRound = clearPlayerInventoryEachRound;

        this.playerCash = startingCash;
        this.rivalCash = startingCash;
    }

    public Phase getPhase() { return phase; }

    public double getPlayerCash() { return playerCash; }
    public double getRivalCash() { return rivalCash; }

    public void setPlayerCash(double cash) { this.playerCash = cash; }
    public void setRivalCash(double cash) { this.rivalCash = cash; }

    public String getRivalStrategyLabel() { return rivalStrategyLabel; }
    public double getLastDemandPremiumRatio() { return lastDemandPremiumRatio; }
    public double getLastStockPremiumRatio() { return lastStockPremiumRatio; }

    public int getLastPlannedCheap() { return lastPlannedCheap; }
    public int getLastPlannedPremium() { return lastPlannedPremium; }
    public double getLastExpectedRevenue() { return lastExpectedRevenue; }

    public int getLastDesiredCheap() { return lastDesiredCheap; }
    public int getLastDesiredPremium() { return lastDesiredPremium; }

    public int getCurrentDesiredCheap() { return currentDesiredCheap; }
    public int getCurrentDesiredPremium() { return currentDesiredPremium; }

    public int getCurrentSoldCheap() { return currentSoldCheap; }
    public int getCurrentSoldPremium() { return currentSoldPremium; }

    public void incDesiredCheap() { currentDesiredCheap++; }
    public void incDesiredPremium() { currentDesiredPremium++; }

    public void incSoldCheap() { currentSoldCheap++; }
    public void incSoldPremium() { currentSoldPremium++; }

    /** Called when entering SELL phase. */
    public void beginSellPhase(Shop playerShop, Shop rivalShop) {
        playerShop.resetTurnStats();
        rivalShop.resetTurnStats();

        currentDesiredCheap = 0;
        currentDesiredPremium = 0;
        currentSoldCheap = 0;
        currentSoldPremium = 0;

        phase = Phase.SELL;
    }

    public void setResultsPhase() {
        phase = Phase.RESULTS;
    }

    /**
     * Called when leaving RESULTS -> next BUY.
     * Stores last DESIRED, resets cash, and optionally clears player inventory each round.
     */
    public void startNextBuyPhase(Shop playerShop, Shop rivalShop) {
        lastDesiredCheap = currentDesiredCheap;
        lastDesiredPremium = currentDesiredPremium;

        int total = lastDesiredCheap + lastDesiredPremium;
        lastDemandPremiumRatio = (total == 0) ? 0.0 : (lastDesiredPremium / (double) total);

        // Reset cash each round
        playerCash = startingCash;
        rivalCash = startingCash;

        // Inventory policy
        if (clearPlayerInventoryEachRound) {
            playerShop.clearInventory();
        }

        // Rival inventory should be restocked by RivalAI
        rivalShop.clearInventory();

        phase = Phase.BUY;
    }

    /** Apply RivalAI decision to RoundManager fields */
    public void applyRivalAIResult(RivalAI.Result res) {
        if (res == null) return;

        this.rivalCash = res.remainingCash;
        this.rivalStrategyLabel = res.strategyLabel;

        this.lastDemandPremiumRatio = res.demandPremiumRatio;
        this.lastStockPremiumRatio = res.stockPremiumRatio;

        this.lastPlannedCheap = res.plannedCheap;
        this.lastPlannedPremium = res.plannedPremium;
        this.lastExpectedRevenue = res.expectedRevenue;
    }

    /** Hard reset: wipe knowledge and reset cash/inventory. */
    public void hardReset(Shop playerShop, Shop rivalShop, boolean wipeKnowledge) {
        playerShop.resetTurnStats();
        rivalShop.resetTurnStats();

        playerShop.clearInventory();
        rivalShop.clearInventory();

        playerCash = startingCash;
        rivalCash = startingCash;

        currentDesiredCheap = 0;
        currentDesiredPremium = 0;
        currentSoldCheap = 0;
        currentSoldPremium = 0;

        if (wipeKnowledge) {
            lastDesiredCheap = 0;
            lastDesiredPremium = 0;

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
