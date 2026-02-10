package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tarron.marketsim.model.Customer;

/**
 * MarketPlan creates a fixed (session) distribution of customer "pair types".
 * Each entry represents a PAIR type: one customer goes to Player, one to Rival.
 *
 * It supports:
 * - rerolling the market (new plan)
 * - getting counts for a given numCustomers
 * - getting the pair types needed for a given numCustomers
 */
public class MarketPlan {

    public static final String TYPE_CHEAP = "Cheap Buyer";
    public static final String TYPE_VALUE = "Value Buyer";
    public static final String TYPE_CONSP = "Conspicuous Buyer";

    // Wallets
    public static final double WALLET_CHEAP = 4.0;
    public static final double WALLET_VALUE = 10.0;
    public static final double WALLET_CONSP = 12.0;

    private final int maxCustomers;
    private final List<String> marketPairPlan = new ArrayList<>();
    private boolean initialized = false;

    public MarketPlan(int maxCustomers) {
        this.maxCustomers = maxCustomers;
    }

    /** Reroll the market plan for a new session. */
    public void reroll() {
        marketPairPlan.clear();

        // Plan is built for the maximum possible customers (pairs)
        int maxPairs = maxCustomers / 2;

        int valuePairs = (int) Math.round(maxPairs * 0.40);
        int cheapPairs = (int) Math.round(maxPairs * 0.30);
        int conspPairs = maxPairs - valuePairs - cheapPairs;

        for (int i = 0; i < cheapPairs; i++) marketPairPlan.add(TYPE_CHEAP);
        for (int i = 0; i < valuePairs; i++) marketPairPlan.add(TYPE_VALUE);
        for (int i = 0; i < conspPairs; i++) marketPairPlan.add(TYPE_CONSP);

        Collections.shuffle(marketPairPlan);
        initialized = true;
    }

    /** Ensure the market is initialized at least once. */
    public void ensureInitialized() {
        if (!initialized) {
            reroll();
        }
    }

    /**
     * Returns the list of pair types needed for totalCustomers.
     * totalCustomers will be forced to even.
     */
    public List<String> getPairTypesForCustomers(int totalCustomers) {
        ensureInitialized();

        int evenCustomers = (totalCustomers % 2 == 0) ? totalCustomers : totalCustomers + 1;
        int pairsNeeded = evenCustomers / 2;

        // Return a copy slice so callers can't mutate our plan
        List<String> slice = new ArrayList<>();
        for (int i = 0; i < pairsNeeded; i++) {
            slice.add(marketPairPlan.get(i));
        }
        return slice;
    }

    /** Counts how many customers of each type will appear (not pairs, actual customers). */
    public MarketCounts getCountsForCustomers(int totalCustomers) {
        ensureInitialized();

        int evenCustomers = (totalCustomers % 2 == 0) ? totalCustomers : totalCustomers + 1;
        int pairsNeeded = evenCustomers / 2;

        int cheap = 0, value = 0, consp = 0;
        for (int i = 0; i < pairsNeeded; i++) {
            String type = marketPairPlan.get(i);
            if (TYPE_CHEAP.equals(type)) cheap += 2;
            else if (TYPE_VALUE.equals(type)) value += 2;
            else consp += 2;
        }
        return new MarketCounts(cheap, value, consp);
    }

    public Customer makeCustomerFromType(String type) {
        if (TYPE_CHEAP.equals(type)) return new Customer(TYPE_CHEAP, WALLET_CHEAP);
        if (TYPE_VALUE.equals(type)) return new Customer(TYPE_VALUE, WALLET_VALUE);
        return new Customer(TYPE_CONSP, WALLET_CONSP);
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
