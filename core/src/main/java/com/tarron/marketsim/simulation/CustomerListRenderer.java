package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.tarron.marketsim.model.Shop;

/**
 * CustomerListRenderer
 *
 * Responsibilities:
 * - Renders per-customer status lines beneath each shop during BUY/SELL/RESULTS.
 *
 * Rules:
 * - Customers are grouped by targetShop.
 * - BUY phase: "idle..."
 * - SELL phase (not arrived): "walking..."
 * - After arrival: prints wallet before/after and purchase result.
 *
 * Notes:
 * - UI only; reads VisualCustomer state and RoundManager phase.
 * - Does not modify simulation state.
 */
public class CustomerListRenderer {

	private final GlyphLayout layout = new GlyphLayout();

	// ============================================================
	// Draw
	// ============================================================

	public void draw(
			SpriteBatch batch,
			BitmapFont font,
			RoundManager roundManager,
			List<CustomerSpawner.VisualCustomer> crowd,
			Shop playerShop,
			Shop rivalShop,
			float playerX,
			float playerY,
			float rivalX,
			float rivalY
			) {
		if (batch == null || font == null) return;

		List<String> playerLines = new ArrayList<>();
		List<String> rivalLines = new ArrayList<>();

		if (crowd != null) {
			for (int i = 0; i < crowd.size(); i++) {
				CustomerSpawner.VisualCustomer vc = crowd.get(i);
				String line = buildLine(roundManager, i, vc);

				if (vc != null && vc.targetShop == playerShop) {
					playerLines.add(line);
				} else {
					rivalLines.add(line);
				}
			}
		}

		float listLeftPadding = 10f;
		float listTopOffset = 25f;
		int maxLines = 12;

		drawShopCustomerList(
				font, layout,
				playerX + listLeftPadding,
				playerY - listTopOffset,
				playerLines,
				maxLines,
				batch
				);

		drawShopCustomerList(
				font, layout,
				rivalX + listLeftPadding,
				rivalY - listTopOffset,
				rivalLines,
				maxLines,
				batch
				);
	}

	// ============================================================
	// Line formatting
	// ============================================================

	/**
	 * Formats one line of UI text for a customer entry.
	 */
	private String buildLine(RoundManager rm, int index, CustomerSpawner.VisualCustomer vc) {
		if (vc == null) return "C" + index + ": null";

		if (vc.purchaseResult == null) {
			boolean inBuy = (rm != null && rm.getPhase() == RoundManager.Phase.BUY);
			return inBuy
					? ("C" + index + ": idle...")
							: ("C" + index + ": walking...");
		}

		String itemLabel = (vc.boughtName == null) ? "no buy" : vc.boughtName;

		return String.format(
				"C%d: Initial=$%.2f  Spent=$%.2f  Final=$%.2f  (%s)",
				index,
				vc.walletBefore,
				vc.spent,
				vc.walletAfter,
				itemLabel
				);
	}

	// ============================================================
	// Rendering helpers
	// ============================================================

	/**
	 * Draws a vertical list of customer lines under a shop.
	 * Limits output to avoid overflowing the UI region.
	 */
	private void drawShopCustomerList(
			BitmapFont font,
			GlyphLayout layout,
			float leftX,
			float topY,
			List<String> lines,
			int maxLinesToShow,
			SpriteBatch batch
			) {
		float lineStep = 14f;

		int count = (lines == null) ? 0 : Math.min(lines.size(), maxLinesToShow);

		for (int i = 0; i < count; i++) {
			font.draw(batch, lines.get(i), leftX, topY - i * lineStep);
		}
	}
}