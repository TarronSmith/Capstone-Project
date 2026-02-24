package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.tarron.marketsim.model.Item;
import com.tarron.marketsim.model.Shop;

/**
 * HudRenderer
 *
 * Responsibility:
 * - Draws the top-of-screen HUD overlay for BUY, SELL, and RESULTS phases.
 *
 * Displays:
 * - Phase + cash
 * - Controls (BUY phase)
 * - Market plan distribution (fixed for current session)
 * - Player/Rival inventory summaries (by catalog item)
 * - Rival AI summary values (strategy + ratios)
 * - Top desired/sold items (current round and last round)
 *
 * Notes:
 * - UI only. Reads RoundManager + Shop state and renders text.
 * - Does not modify simulation state.
 */
public class HudRenderer {

    private final GlyphLayout layout = new GlyphLayout();

    // ============================================================
    // Public draw entry
    // ============================================================

    public void draw(
            SpriteBatch batch,
            BitmapFont fontStats,
            BitmapFont fontSmall,
            RoundManager roundManager,
            Shop playerShop,
            Shop rivalShop,
            Item cheapItem,
            Item premiumItem,
            int numCustomers,
            double vendorCheapCost,
            double vendorPremiumCost,
            int marketCheapCount,
            int marketValueCount,
            int marketConspCount,
            float screenWidth,
            List<Item> catalogItems,
            VendorCostLookup vendorLookup
    ) {
        if (roundManager == null) return;

        if (roundManager.getPhase() == RoundManager.Phase.BUY) {
            drawBuyHud(
                    batch, fontStats, fontSmall,
                    roundManager,
                    playerShop, rivalShop,
                    numCustomers,
                    marketCheapCount, marketValueCount, marketConspCount,
                    screenWidth,
                    catalogItems,
                    vendorLookup
            );
        } else {
            drawSellResultsHud(
                    batch, fontStats, fontSmall,
                    roundManager,
                    playerShop, rivalShop,
                    screenWidth
            );
        }
    }

    // ============================================================
    // BUY HUD
    // ============================================================

    private void drawBuyHud(
            SpriteBatch batch,
            BitmapFont fontStats,
            BitmapFont fontSmall,
            RoundManager rm,
            Shop playerShop,
            Shop rivalShop,
            int numCustomers,
            int marketCheapCount,
            int marketValueCount,
            int marketConspCount,
            float screenWidth,
            List<Item> catalogItems,
            VendorCostLookup vendorLookup
    ) {
        int catalogSize = (catalogItems == null) ? 0 : catalogItems.size();

        // Phase header + cash readout
        fontStats.draw(batch,
                String.format("BUY PHASE  |  Player Cash: $%.2f   Rival Cash: $%.2f   (R = reset, M = new market)",
                        rm.getPlayerCash(), rm.getRivalCash()),
                20, 590);

        // Input legend
        fontSmall.draw(batch,
                String.format("controls: [1-%d] Buy Catalog Slot   [ENTER] Begin Sell   [UP/DOWN] Customers (%d)",
                        Math.max(catalogSize, 1), numCustomers),
                20, 570);

        // Current session's fixed market distribution
        fontSmall.draw(batch,
                String.format("SESSION Market (fixed): Cheap=%d  Value=%d  Conspicuous=%d",
                        marketCheapCount, marketValueCount, marketConspCount),
                20, 552);

        // Inventory summaries (only shows items with qty > 0)
        fontSmall.draw(batch,
                "Player Inventory: " + inventorySummary(playerShop, catalogItems),
                20, 532);

        fontSmall.draw(batch,
                "Rival  Inventory: " + inventorySummary(rivalShop, catalogItems),
                20, 514);

        // Rival AI summary values
        fontSmall.draw(batch,
                String.format("Rival: %s  |  lastDemandPrem=%.2f  lastStockPrem=%.2f",
                        rm.getRivalStrategyLabel(),
                        rm.getLastDemandPremiumRatio(),
                        rm.getLastStockPremiumRatio()),
                20, 496);

        // Last round outcome summaries
        fontSmall.draw(batch,
                "Last Round Top Desired: " + topKSummary(rm.getLastDesiredByItemName(), 5),
                20, 478);

        fontSmall.draw(batch,
                "Last Round Top Sold:    " + topKSummary(rm.getLastSoldByItemName(), 5),
                20, 460);

        // Catalog block (top-right)
        drawCatalogTopRight(batch, fontSmall, screenWidth, catalogItems, vendorLookup);
    }

    private void drawCatalogTopRight(
            SpriteBatch batch,
            BitmapFont fontSmall,
            float screenWidth,
            List<Item> catalogItems,
            VendorCostLookup vendorLookup
    ) {
        if (catalogItems == null || catalogItems.isEmpty() || vendorLookup == null) return;

        float rightX = screenWidth - 20f;
        float y = 570f;

        String header = "Catalog:";
        layout.setText(fontSmall, header);
        fontSmall.draw(batch, header, rightX - layout.width, y);
        y -= 18f;

        // Limits text height so the list stays in the HUD region
        int maxLines = 6;
        for (int i = 0; i < catalogItems.size() && i < maxLines; i++) {
            Item it = catalogItems.get(i);
            double cost = vendorLookup.getVendorCost(it);

            String line = String.format("%d) %s  $%.2f  cost $%.2f",
                    (i + 1),
                    safeName(it),
                    (it == null ? 0.0 : it.getPrice()),
                    cost);

            layout.setText(fontSmall, line);
            fontSmall.draw(batch, line, rightX - layout.width, y);
            y -= 16f;
        }

        if (catalogItems.size() > maxLines) {
            String more = String.format("... (%d more)", catalogItems.size() - maxLines);
            layout.setText(fontSmall, more);
            fontSmall.draw(batch, more, rightX - layout.width, y);
        }
    }

    // ============================================================
    // SELL / RESULTS HUD
    // ============================================================

    private void drawSellResultsHud(
            SpriteBatch batch,
            BitmapFont fontStats,
            BitmapFont fontSmall,
            RoundManager rm,
            Shop playerShop,
            Shop rivalShop,
            float screenW
    ) {
        String statsLine1 = "Player sold=" + safeSold(playerShop)
                + "  revenue=$" + String.format("%.2f", safeRevenue(playerShop));
        String statsLine2 = "Rival  sold=" + safeSold(rivalShop)
                + "  revenue=$" + String.format("%.2f", safeRevenue(rivalShop));

        // Centered revenue summary
        layout.setText(fontStats, statsLine1);
        fontStats.draw(batch, statsLine1, (screenW - layout.width) / 2f, 590);

        layout.setText(fontStats, statsLine2);
        fontStats.draw(batch, statsLine2, (screenW - layout.width) / 2f, 568);

        // Current round summaries (per-item counts)
        fontSmall.draw(batch,
                "Top Desired (this round): " + topKSummary(rm.getCurrentDesiredByItemName(), 6),
                20, 548);

        fontSmall.draw(batch,
                "Top Sold (this round):   " + topKSummary(rm.getCurrentSoldByItemName(), 6),
                20, 530);

        // Phase prompt
        if (rm.getPhase() == RoundManager.Phase.SELL) {
            fontSmall.draw(batch, "SELL running... (wait for all customers to arrive)", 20, 512);
        } else {
            fontSmall.draw(batch, "SELL COMPLETE. Press ENTER for next BUY phase. (R = reset, M = new market)", 20, 512);
        }
    }

    // ============================================================
    // Formatting helpers
    // ============================================================

    private int safeSold(Shop s) {
        return (s == null) ? 0 : s.getItemsSoldThisTurn();
    }

    private double safeRevenue(Shop s) {
        return (s == null) ? 0.0 : s.getRevenueThisTurn();
    }

    private String safeName(Item it) {
        return (it == null || it.getName() == null) ? "(null)" : it.getName();
    }

    /**
     * Returns a compact inventory summary including only in-stock items.
     * Example: "Bargain=2, Quality=1"
     */
    private String inventorySummary(Shop shop, List<Item> catalogItems) {
        if (shop == null || catalogItems == null || catalogItems.isEmpty()) return "(empty)";

        List<String> parts = new ArrayList<>();
        for (Item it : catalogItems) {
            int q = shop.getQuantity(it);
            if (q > 0) parts.add(safeName(it) + "=" + q);
        }
        if (parts.isEmpty()) return "(empty)";
        return String.join(", ", parts);
    }

    /**
     * Returns "name(count)" entries sorted by highest count first.
     * Example: "CheapItem(4), Decent(2), Luxury(1)"
     */
    private String topKSummary(Map<String, Integer> counts, int k) {
        if (counts == null || counts.isEmpty()) return "(none)";

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(
                Comparator.<Map.Entry<String, Integer>>comparingInt(e -> e.getValue() == null ? 0 : e.getValue())
                        .reversed()
                        .thenComparing(e -> e.getKey() == null ? "" : e.getKey())
        );

        StringBuilder sb = new StringBuilder();
        int shown = 0;

        for (Map.Entry<String, Integer> e : entries) {
            if (shown >= k) break;

            String name = (e.getKey() == null) ? "(null)" : e.getKey();
            int val = (e.getValue() == null) ? 0 : e.getValue();
            if (val <= 0) continue;

            if (shown > 0) sb.append(", ");
            sb.append(name).append("(").append(val).append(")");
            shown++;
        }

        return (shown == 0) ? "(none)" : sb.toString();
    }

    // ============================================================
    // Vendor cost lookup hook (engine provides the implementation)
    // ============================================================

    public interface VendorCostLookup {
        double getVendorCost(Item item);
    }
}