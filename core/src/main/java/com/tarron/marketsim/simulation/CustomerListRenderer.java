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
 * Responsibility:
 * - Displays a per-customer activity list beneath each shop.
 *
 * Behavior:
 * - Customers are grouped by the shop they selected (targetShop).
 * - Each entry shows the transaction outcome once the customer arrives.
 * - While SELL is running, entries show "walking".
 * - During BUY phase, entries show "idle".
 *
 * Notes:
 * - UI only. Reads VisualCustomer state.
 * - Does not modify simulation data.
 */
public class CustomerListRenderer {

	private final GlyphLayout layout = new GlyphLayout();

	// ============================================================
	// Public draw entry
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

		// Separate customers by destination shop
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

		// Positioning relative to shop rectangles
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
	// Line construction
	// ============================================================

	/**
	 * Builds a single display line for a customer.
	 *
	 * States:
	 * BUY phase     -> idle
	 * SELL moving   -> walking
	 * SELL arrived  -> transaction summary
	 */
	private String buildLine(RoundManager rm, int index, CustomerSpawner.VisualCustomer vc) {
		if (vc == null) {
			return "C" + index + ": null";
		}

		// No transaction yet
		if (vc.purchaseResult == null) {
			boolean inBuy = (rm != null && rm.getPhase() == RoundManager.Phase.BUY);
			return inBuy
					? ("C" + index + ": idle...")
							: ("C" + index + ": walking...");
		}

		// Transaction completed
		String itemLabel = (vc.boughtName == null) ? "no buy" : vc.boughtName;

		return String.format(
				"C%d: Initial=$%.2f  Price=$%.2f  Final=$%.2f  (%s)",
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
	 * Limits output to avoid UI overflow.
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

		int count = (lines == null)
				? 0
						: Math.min(lines.size(), maxLinesToShow);

		for (int i = 0; i < count; i++) {
			font.draw(batch, lines.get(i), leftX, topY - i * lineStep);
		}
	}
}