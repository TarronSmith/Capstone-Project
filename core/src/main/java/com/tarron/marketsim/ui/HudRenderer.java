package com.tarron.marketsim.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.tarron.marketsim.model.Item;
import com.tarron.marketsim.model.Shop;
import com.tarron.marketsim.simulation.RoundManager;

/**
 * HudRenderer
 *
 * Responsibilities:
 * - Renders the top HUD overlay during BUY, SELL, and RESULTS phases.
 *
 * Displays:
 * - Phase, round, cash
 * - Controls (BUY, RESULTS)
 * - Session market summary (total customers, total pairs)
 * - Player/Rival inventory summary
 * - Rival AI summary
 * - Top desired/sold (this round and last round)
 *
 * Notes:
 * - Read-only UI renderer. No simulation state changes.
 */
public class HudRenderer {

	private final GlyphLayout layout = new GlyphLayout();

	// ============================================================
	// Entry
	// ============================================================

	public void draw(
			SpriteBatch batch,
			BitmapFont fontStats,
			BitmapFont fontSmall,
			RoundManager rm,
			Shop playerShop,
			Shop rivalShop,
			int numCustomers,
			int marketTotalCustomers,
			int marketTotalPairs,
			float screenWidth,
			List<Item> catalogItems,
			VendorCostLookup vendorLookup
			) {
		if (batch == null || fontStats == null || fontSmall == null || rm == null) return;

		switch (rm.getPhase()) {
		case BUY:
			drawBuyHud(
					batch, fontStats, fontSmall,
					rm,
					playerShop, rivalShop,
					numCustomers,
					marketTotalCustomers, marketTotalPairs,
					screenWidth,
					catalogItems,
					vendorLookup
					);
			break;

		case SELL:
		case RESULTS:
		default:
			drawSellResultsHud(
					batch, fontStats, fontSmall,
					rm,
					playerShop, rivalShop,
					screenWidth
					);
			break;
		}
	}

	// ============================================================
	// BUY
	// ============================================================

	private void drawBuyHud(
			SpriteBatch batch,
			BitmapFont fontStats,
			BitmapFont fontSmall,
			RoundManager rm,
			Shop playerShop,
			Shop rivalShop,
			int numCustomers,
			int marketTotalCustomers,
			int marketTotalPairs,
			float screenWidth,
			List<Item> catalogItems,
			VendorCostLookup vendorLookup
			) {
		int catalogSize = (catalogItems == null) ? 0 : catalogItems.size();

		fontStats.draw(batch,
				String.format("BUY  |  Round %d / %d  |  Player: $%.2f   Rival: $%.2f   (R = reset, M = new market)",
						rm.getRoundNumber(), rm.getMaxRounds(),
						rm.getPlayerCash(), rm.getRivalCash()),
				20, 590);

		fontSmall.draw(batch,
				String.format("controls: [1-%d] Buy Item   [ENTER] Begin Sell   [UP/DOWN] Customers (%d)",
						Math.max(catalogSize, 1), numCustomers),
				20, 570);

		fontSmall.draw(batch,
				String.format("SESSION Market: totalCustomers=%d  totalPairs=%d",
						marketTotalCustomers, marketTotalPairs),
				20, 552);

		fontSmall.draw(batch,
				"Player Inventory: " + inventorySummary(playerShop, catalogItems),
				20, 532);

		fontSmall.draw(batch,
				"Rival  Inventory: " + inventorySummary(rivalShop, catalogItems),
				20, 514);

		fontSmall.draw(batch,
				String.format("RivalAI: %s  |  expectedRevenue=%.2f",
						safe(rm.getRivalStrategyLabel()),
						rm.getLastExpectedRevenue()),
				20, 496);

		fontSmall.draw(batch,
				"Last Round Top Desired: " + topKSummary(rm.getLastDesiredByItemName(), 5),
				20, 478);

		fontSmall.draw(batch,
				"Last Round Top Sold:    " + topKSummary(rm.getLastSoldByItemName(), 5),
				20, 460);

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

		int maxLines = 7;

		for (int i = 0; i < catalogItems.size() && i < maxLines; i++) {
			Item it = catalogItems.get(i);
			double cost = vendorLookup.getVendorCost(it);

			String line = String.format("%d) %s  $%.2f  cost $%.2f",
					(i + 1),
					safeName(it),
					(it == null ? 0.0 : it.getPrice()),
					(isFinite(cost) ? cost : 0.0));

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
	// SELL / RESULTS
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
		fontStats.draw(batch,
				String.format("%s  |  Round %d / %d  |  Player: $%.2f   Rival: $%.2f",
						rm.getPhase().name(),
						rm.getRoundNumber(), rm.getMaxRounds(),
						rm.getPlayerCash(), rm.getRivalCash()),
				20, 590);

		String statsLine1 = "Player sold=" + safeSold(playerShop)
		+ "  revenue=$" + String.format("%.2f", safeRevenue(playerShop));
		String statsLine2 = "Rival  sold=" + safeSold(rivalShop)
		+ "  revenue=$" + String.format("%.2f", safeRevenue(rivalShop));

		layout.setText(fontSmall, statsLine1);
		fontSmall.draw(batch, statsLine1, (screenW - layout.width) / 2f, 568);

		layout.setText(fontSmall, statsLine2);
		fontSmall.draw(batch, statsLine2, (screenW - layout.width) / 2f, 550);

		fontSmall.draw(batch,
				"Top Desired (this round): " + topKSummary(rm.getCurrentDesiredByItemName(), 6),
				20, 530);

		fontSmall.draw(batch,
				"Top Sold (this round):   " + topKSummary(rm.getCurrentSoldByItemName(), 6),
				20, 512);

		if (rm.getPhase() == RoundManager.Phase.SELL) {
			fontSmall.draw(batch, "SELL running... (wait for all customers to arrive)", 20, 494);
		} else {
			fontSmall.draw(batch, "RESULTS: Press ENTER for next BUY. (R = reset, M = new market)", 20, 494);
		}
	}

	// ============================================================
	// Formatting
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

	private String safe(String s) {
		return (s == null || s.isBlank()) ? "UNKNOWN" : s;
	}

	private boolean isFinite(double v) {
		return !Double.isNaN(v) && !Double.isInfinite(v);
	}

	private String inventorySummary(Shop shop, List<Item> catalogItems) {
		if (shop == null || catalogItems == null || catalogItems.isEmpty()) return "(empty)";

		List<String> parts = new ArrayList<>();
		for (Item it : catalogItems) {
			int q = shop.getQuantity(it);
			if (q > 0) parts.add(safeName(it) + "=" + q);
		}
		return parts.isEmpty() ? "(empty)" : String.join(", ", parts);
	}

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
	// Vendor cost lookup hook
	// ============================================================

	public interface VendorCostLookup {
		double getVendorCost(Item item);
	}
}