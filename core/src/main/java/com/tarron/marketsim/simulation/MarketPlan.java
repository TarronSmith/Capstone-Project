package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.tarron.marketsim.model.Customer;

/**
 * MarketPlan
 *
 * Responsibility:
 * - Defines the session-level population mix of customer archetypes.
 * - Produces the sequence of archetype "pair types" used to spawn customers each SELL phase.
 * - Generates customers with wallets sampled from archetype-specific distributions.
 *
 * Notes:
 * - The plan is generated once per reroll and then reused across rounds.
 * - The internal ordering of pair types is shuffled once per session, so the market composition
 *   is stable while still being varied between sessions.
 * - Spawning still uses "pair types" (each pair type produces two customers). Customers can
 *   still choose different shops during the SELL phase based on DecisionLogic.pickShop(...).
 */
public class MarketPlan {

    // Archetype labels (used throughout the project)
    public static final String TYPE_CHEAP = "Cheap Buyer";
    public static final String TYPE_VALUE = "Value Buyer";
    public static final String TYPE_CONSP = "Conspicuous Buyer";

    // Wallet distributions for each archetype (session-deterministic draws)
    private static final double[] CHEAP_WALLETS = { 0.0, 2.0, 3.0, 4.0, 4.0, 5.0 };
    private static final double[] VALUE_WALLETS = { 6.0, 8.0, 9.0, 10.0, 10.0, 12.0 };
    private static final double[] CONSP_WALLETS = { 8.0, 10.0, 12.0, 12.0, 15.0 };

    // Target market mix (remaining share goes to Conspicuous)
    private static final double SHARE_VALUE = 0.40;
    private static final double SHARE_CHEAP = 0.30;

    private final int maxCustomers;

    // Each entry is a pair archetype; spawning uses two customers per entry.
    private final List<String> marketPairPlan = new ArrayList<>();

    private boolean initialized = false;

    // RNG for repeatable wallet sampling and plan shuffling per session
    private long sessionSeed = 0L;
    private Random rng = null;

    public MarketPlan(int maxCustomers) {
        this.maxCustomers = maxCustomers;
    }

    /** Reroll the market plan for a new session (new plan + new deterministic wallet draws). */
    public void reroll() {
        reroll(System.nanoTime());
    }

    /** Reroll with an explicit seed (useful for debugging/replays). */
    public void reroll(long seed) {
        this.sessionSeed = seed;
        this.rng = new Random(sessionSeed);

        marketPairPlan.clear();

        int maxPairs = maxCustomers / 2;

        int valuePairs = (int) Math.round(maxPairs * SHARE_VALUE);
        int cheapPairs = (int) Math.round(maxPairs * SHARE_CHEAP);
        int conspPairs = maxPairs - valuePairs - cheapPairs;

        for (int i = 0; i < cheapPairs; i++) marketPairPlan.add(TYPE_CHEAP);
        for (int i = 0; i < valuePairs; i++) marketPairPlan.add(TYPE_VALUE);
        for (int i = 0; i < conspPairs; i++) marketPairPlan.add(TYPE_CONSP);

        Collections.shuffle(marketPairPlan, rng);

        initialized = true;
    }

    /** Ensure the market is initialized at least once. */
    public void ensureInitialized() {
        if (!initialized) reroll();
    }

    /**
     * Returns the archetype "pair types" needed for totalCustomers.
     * totalCustomers is forced to even and clamped to maxCustomers.
     */
    public List<String> getPairTypesForCustomers(int totalCustomers) {
        ensureInitialized();

        int evenCustomers = (totalCustomers % 2 == 0) ? totalCustomers : totalCustomers + 1;
        evenCustomers = Math.min(evenCustomers, maxCustomers);

        int pairsNeeded = evenCustomers / 2;
        pairsNeeded = Math.min(pairsNeeded, marketPairPlan.size());

        List<String> slice = new ArrayList<>(pairsNeeded);
        for (int i = 0; i < pairsNeeded; i++) {
            slice.add(marketPairPlan.get(i));
        }
        return slice;
    }

    /**
     * Counts how many customers of each type will appear (counts customers, not pairs).
     */
    public MarketCounts getCountsForCustomers(int totalCustomers) {
        ensureInitialized();

        int evenCustomers = (totalCustomers % 2 == 0) ? totalCustomers : totalCustomers + 1;
        evenCustomers = Math.min(evenCustomers, maxCustomers);

        int pairsNeeded = evenCustomers / 2;
        pairsNeeded = Math.min(pairsNeeded, marketPairPlan.size());

        int cheap = 0, value = 0, consp = 0;
        for (int i = 0; i < pairsNeeded; i++) {
            String type = marketPairPlan.get(i);
            if (TYPE_CHEAP.equals(type)) cheap += 2;
            else if (TYPE_VALUE.equals(type)) value += 2;
            else consp += 2;
        }
        return new MarketCounts(cheap, value, consp);
    }

    /**
     * Creates a Customer using:
     * - the archetype type string (Cheap/Value/Conspicuous)
     * - a wallet sampled from that archetype's wallet distribution
     */
    public Customer makeCustomerFromType(String type) {
        ensureInitialized();

        double wallet;
        if (TYPE_CHEAP.equals(type)) wallet = pickFrom(CHEAP_WALLETS);
        else if (TYPE_VALUE.equals(type)) wallet = pickFrom(VALUE_WALLETS);
        else wallet = pickFrom(CONSP_WALLETS);

        return new Customer(type, wallet);
    }

    /** Exposes the session seed for debugging/replays. */
    public long getSessionSeed() {
        ensureInitialized();
        return sessionSeed;
    }

    // ============================================================
    // Internals
    // ============================================================

    private double pickFrom(double[] options) {
        return options[rng.nextInt(options.length)];
    }

    /** Simple value object for counts. */
    public static class MarketCounts {
        public final int cheapCount;
        public final int valueCount;
        public final int conspCount;

        public MarketCounts(int cheapCount, int valueCount, int conspCount) {
            this.cheapCount = cheapCount;
            this.valueCount = valueCount;
            this.conspCount = conspCount;
        }
    }
}