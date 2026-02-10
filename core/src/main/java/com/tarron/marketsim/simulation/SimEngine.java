package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.tarron.marketsim.model.Customer;
import com.tarron.marketsim.model.DecisionLogic;
import com.tarron.marketsim.model.Item;
import com.tarron.marketsim.model.Shop;

/**
 * Console-only simulation runner for debugging.
 *
 * What it does:
 * - Runs multiple scenarios
 * - Logs per-customer: shop, wallet before/after, desired item, bought item
 * - Tracks DESIRED vs SOLD
 * - Runs sanity checks:
 *   * sold totals match shop sold totals
 *   * revenue matches price-weighted sold counts
 *   * customer spending sum matches revenue sum
 *   * inventory deltas match sold totals
 */
public class SimEngine {

    // Config
    private static final long SEED = 1337L;
    private static final boolean VERBOSE_PER_CUSTOMER = true;

    // Items
    private static final Item CHEAP = new Item("CheapItem", 3.0, 1);
    private static final Item PREMIUM = new Item("ExpensiveItem", 8.0, 5);

    // Strategy strings 
    private static final String TYPE_CHEAP = "Cheap Buyer";
    private static final String TYPE_VALUE = "Value Buyer";
    private static final String TYPE_CONSP = "Conspicuous Buyer";

    public static void main(String[] args) {
        System.out.println("=== SimEngine Debug Runner ===");
        System.out.println("Seed = " + SEED);
        System.out.println();

        DecisionLogic logic = new DecisionLogic();
        Random rng = new Random(SEED);

        runScenarioBalancedStock(logic);
        runScenarioPremiumStockout(logic);
        runScenarioAllTooPoor(logic);
        runScenarioRandomMarket(logic, rng, 20);

        System.out.println();
        System.out.println("=== Done ===");
    }

    // ------------------------------------------------------------
    // Scenario 1: Balanced stock and mixed customers
    // ------------------------------------------------------------
    private static void runScenarioBalancedStock(DecisionLogic logic) {
        System.out.println();
        System.out.println("--- Scenario: Balanced Stock + Mixed Customers ---");

        Shop player = newShop("Player Shop");
        Shop rival = newShop("Rival Shop");

        stock(player, CHEAP, 5);
        stock(player, PREMIUM, 3);

        stock(rival, CHEAP, 5);
        stock(rival, PREMIUM, 3);

        List<Customer> customers = Arrays.asList(
                new Customer(TYPE_CHEAP, 4.0),
                new Customer(TYPE_VALUE, 10.0),
                new Customer(TYPE_CONSP, 12.0),
                new Customer(TYPE_VALUE, 6.0),
                new Customer(TYPE_CHEAP, 2.0), // cannot afford anything
                new Customer(TYPE_CONSP, 12.0),
                new Customer(TYPE_VALUE, 10.0),
                new Customer(TYPE_CHEAP, 4.0)
        );

        runSellPhaseAndReport(player, rival, customers, logic);
    }

    // ------------------------------------------------------------
    // Scenario 2: Premium stockout stress test
    // ------------------------------------------------------------
    private static void runScenarioPremiumStockout(DecisionLogic logic) {
        System.out.println();
        System.out.println("--- Scenario: Premium Stockout ---");

        Shop player = newShop("Player Shop");
        Shop rival = newShop("Rival Shop");

        stock(player, CHEAP, 10);
        stock(player, PREMIUM, 1);

        stock(rival, CHEAP, 10);
        stock(rival, PREMIUM, 0);

        List<Customer> customers = Arrays.asList(
                new Customer(TYPE_CONSP, 12.0),
                new Customer(TYPE_CONSP, 12.0),
                new Customer(TYPE_CONSP, 12.0),
                new Customer(TYPE_CONSP, 12.0),
                new Customer(TYPE_VALUE, 10.0),
                new Customer(TYPE_VALUE, 10.0),
                new Customer(TYPE_CHEAP, 4.0),
                new Customer(TYPE_CHEAP, 4.0)
        );

        runSellPhaseAndReport(player, rival, customers, logic);
    }

    // ------------------------------------------------------------
    // Scenario 3: Everyone too poor
    // ------------------------------------------------------------
    private static void runScenarioAllTooPoor(DecisionLogic logic) {
        System.out.println();
        System.out.println("--- Scenario: Everyone Too Poor ---");

        Shop player = newShop("Player Shop");
        Shop rival = newShop("Rival Shop");

        stock(player, CHEAP, 10);
        stock(player, PREMIUM, 10);

        stock(rival, CHEAP, 10);
        stock(rival, PREMIUM, 10);

        List<Customer> customers = Arrays.asList(
                new Customer(TYPE_CHEAP, 0.0),
                new Customer(TYPE_VALUE, 1.0),
                new Customer(TYPE_CONSP, 2.0),
                new Customer(TYPE_VALUE, 2.5),
                new Customer(TYPE_CHEAP, 2.9)
        );

        runSellPhaseAndReport(player, rival, customers, logic);
    }

    // ------------------------------------------------------------
    // Scenario 4: Random market (repeatable with seed)
    // ------------------------------------------------------------
    private static void runScenarioRandomMarket(DecisionLogic logic, Random rng, int numCustomers) {
        System.out.println();
        System.out.println("--- Scenario: Random Market (" + numCustomers + " customers) ---");

        Shop player = newShop("Player Shop");
        Shop rival = newShop("Rival Shop");

        stock(player, CHEAP, 8);
        stock(player, PREMIUM, 2);

        stock(rival, CHEAP, 4);
        stock(rival, PREMIUM, 5);

        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < numCustomers; i++) {
            customers.add(randomCustomer(rng));
        }

        runSellPhaseAndReport(player, rival, customers, logic);
    }

    // ============================================================
    // Core runner: SELL phase with logs and sanity checks
    // ============================================================
    private static void runSellPhaseAndReport(Shop player, Shop rival, List<Customer> customers, DecisionLogic logic) {

        player.resetTurnStats();
        rival.resetTurnStats();

        int desiredCheap = 0;
        int desiredPremium = 0;

        int soldCheap = 0;
        int soldPremium = 0;

        double sumCustomerSpent = 0.0;

        // Inventory snapshots
        int pStartCheap = player.getQuantity(CHEAP);
        int pStartPremium = player.getQuantity(PREMIUM);
        int rStartCheap = rival.getQuantity(CHEAP);
        int rStartPremium = rival.getQuantity(PREMIUM);

        System.out.println("Start Inventory:");
        printInventory(player);
        printInventory(rival);
        System.out.println();

        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);

            Shop chosenShop = (i % 2 == 0) ? player : rival;

            // DESIRED (ignores inventory)
            Item desired = logic.iPick(c, CHEAP, PREMIUM);
            if (desired != null) {
                if (sameItem(desired, CHEAP)) desiredCheap++;
                else if (sameItem(desired, PREMIUM)) desiredPremium++;
            }

            // SALE (affected by inventory)
            double walletBefore = getWallet(c);
            Item bought = chosenShop.sell(c, CHEAP, PREMIUM, logic);
            double walletAfter = getWallet(c);

            double spent = walletBefore - walletAfter;
            if (spent < 0) spent = 0.0;
            sumCustomerSpent += spent;

            if (bought != null) {
                if (sameItem(bought, CHEAP)) soldCheap++;
                else if (sameItem(bought, PREMIUM)) soldPremium++;
            }

            if (VERBOSE_PER_CUSTOMER) {
                System.out.println(
                        "C" + i
                        + " shop=" + chosenShop.getName()
                        + " walletBefore=$" + fmt2(walletBefore)
                        + " walletAfter=$" + fmt2(walletAfter)
                        + " spent=$" + fmt2(spent)
                        + " desired=" + (desired == null ? "none" : desired.getName())
                        + " bought=" + (bought == null ? "none" : bought.getName())
                );
            }
        }

        System.out.println();
        System.out.println("Results:");
        printShopStats(player);
        printShopStats(rival);

        System.out.println();
        System.out.println("Desired vs Sold:");
        System.out.println("DESIRED cheap=" + desiredCheap + " premium=" + desiredPremium
                + " totalDesired=" + (desiredCheap + desiredPremium)
                + " totalCustomers=" + customers.size());
        System.out.println("SOLD    cheap=" + soldCheap + " premium=" + soldPremium
                + " totalSold=" + (soldCheap + soldPremium));

        // ---------------- SANITY CHECKS ----------------
        // Note: desiredTotal can be less than customers if iPick returns null (too poor).
        int desiredTotal = desiredCheap + desiredPremium;
        if (desiredTotal > customers.size()) {
            throw new IllegalStateException("SANITY FAIL: desiredTotal > customers (double count).");
        }

        int soldTotal = soldCheap + soldPremium;
        int shopSoldTotal = player.getItemsSoldThisTurn() + rival.getItemsSoldThisTurn();
        assertEquals("SANITY FAIL: sold totals mismatch", soldTotal, shopSoldTotal);

        double expectedRevenue = soldCheap * CHEAP.getPrice() + soldPremium * PREMIUM.getPrice();
        double actualRevenue = player.getRevenueThisTurn() + rival.getRevenueThisTurn();
        assertApprox("SANITY FAIL: revenue mismatch", expectedRevenue, actualRevenue, 0.0001);

        assertApprox("SANITY FAIL: customer spending sum mismatch revenue", sumCustomerSpent, actualRevenue, 0.0001);

        // Inventory delta checks
        int pEndCheap = player.getQuantity(CHEAP);
        int pEndPremium = player.getQuantity(PREMIUM);
        int rEndCheap = rival.getQuantity(CHEAP);
        int rEndPremium = rival.getQuantity(PREMIUM);

        int pSoldCheap = pStartCheap - pEndCheap;
        int pSoldPremium = pStartPremium - pEndPremium;
        int rSoldCheap = rStartCheap - rEndCheap;
        int rSoldPremium = rStartPremium - rEndPremium;

        if (pSoldCheap < 0 || pSoldPremium < 0 || rSoldCheap < 0 || rSoldPremium < 0) {
            throw new IllegalStateException("SANITY FAIL: inventory increased during sell phase.");
        }

        int invSoldTotal = pSoldCheap + pSoldPremium + rSoldCheap + rSoldPremium;
        assertEquals("SANITY FAIL: inventory delta sold mismatch", invSoldTotal, soldTotal);

        System.out.println();
        System.out.println("Sanity checks PASS");
    }

    // ============================================================
    // Helpers
    // ============================================================

    private static Shop newShop(String name) {
        Shop s = new Shop();
        s.setName(name);
        return s;
    }

    private static void stock(Shop shop, Item item, int qty) {
        if (qty <= 0) return;
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < qty; i++) items.add(item);
        shop.addItems(items);
    }

    private static void printInventory(Shop shop) {
        System.out.println(shop.getName()
                + " cheap=" + shop.getQuantity(CHEAP)
                + " premium=" + shop.getQuantity(PREMIUM));
    }

    private static void printShopStats(Shop shop) {
        System.out.println(shop.getName()
                + " soldThisTurn=" + shop.getItemsSoldThisTurn()
                + " revenueThisTurn=$" + fmt2(shop.getRevenueThisTurn())
                + " remainingCheap=" + shop.getQuantity(CHEAP)
                + " remainingPremium=" + shop.getQuantity(PREMIUM));
    }

    private static double getWallet(Customer c) {
        return c.getWallet();
    }

    private static Customer randomCustomer(Random rng) {
        double roll = rng.nextDouble();

        if (roll < 0.30) {
            double[] budgets = { 0.0, 2.0, 4.0, 4.0, 5.0 };
            return new Customer(TYPE_CHEAP, budgets[rng.nextInt(budgets.length)]);
        } else if (roll < 0.70) {
            double[] budgets = { 6.0, 8.0, 10.0, 10.0, 12.0 };
            return new Customer(TYPE_VALUE, budgets[rng.nextInt(budgets.length)]);
        } else {
            double[] budgets = { 8.0, 10.0, 12.0, 12.0, 15.0 };
            return new Customer(TYPE_CONSP, budgets[rng.nextInt(budgets.length)]);
        }
    }

    private static boolean sameItem(Item a, Item b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        String an = a.getName();
        String bn = b.getName();
        return an != null && an.equals(bn);
    }

    private static void assertEquals(String msg, int expected, int actual) {
        if (expected != actual) {
            throw new IllegalStateException(msg + " expected=" + expected + " actual=" + actual);
        }
    }

    private static void assertApprox(String msg, double expected, double actual, double eps) {
        if (Math.abs(expected - actual) > eps) {
            throw new IllegalStateException(msg + " expected=" + fmt2(expected) + " actual=" + fmt2(actual));
        }
    }

    private static String fmt2(double v) {
        return String.format("%.2f", v);
    }
}
