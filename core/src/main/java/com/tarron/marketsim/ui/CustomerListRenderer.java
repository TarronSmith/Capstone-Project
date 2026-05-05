package com.tarron.marketsim.ui;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.tarron.marketsim.model.Shop;
import com.tarron.marketsim.simulation.CustomerSpawner;
import com.tarron.marketsim.simulation.RoundManager;

/**
 * CustomerListRenderer
 *
 * Responsibilities:
 * - Renders per-customer status lines beneath each shop during BUY/SELL/RESULTS.
 *
 * Rules:
 * - Customers are grouped by current shop context.
 * - BUY phase: "idle..."
 * - SELL phase: state-specific movement / inside-shop / exit text
 * - After the full flow finishes: prints wallet before/after and purchase result
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

				Shop listShop = shopContextForList(vc);

				if (listShop == playerShop) {
					playerLines.add(line);
				} else if (listShop == rivalShop) {
					rivalLines.add(line);
				} else {
					playerLines.add(line);
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
	// Grouping helpers
	// ============================================================

	private Shop shopContextForList(CustomerSpawner.VisualCustomer vc) {
		if (vc == null) return null;
		if (vc.currentShop != null) return vc.currentShop;
		if (vc.firstShop != null) return vc.firstShop;
		return null;
	}

	// ============================================================
	// Line formatting
	// ============================================================

	private String buildLine(RoundManager rm, int index, CustomerSpawner.VisualCustomer vc) {
		if (vc == null) return "C" + index + ": null";

		boolean inBuy = (rm != null && rm.getPhase() == RoundManager.Phase.BUY);

		if (inBuy) {
			return "C" + index + ": idle...";
		}

		switch (vc.state) {
		case WALK_TO_FIRST_SHOP:
			return "C" + index + ": walking to first shop...";

		case INSIDE_FIRST_SHOP:
			return "C" + index + ": inside first shop...";

		case WALK_TO_SECOND_SHOP:
			return "C" + index + ": walking to second shop...";

		case INSIDE_SECOND_SHOP:
			return "C" + index + ": inside second shop...";

		case EXITING:
			return "C" + index + ": leaving...";

		case DONE:
			return buildFinishedLine(index, vc);

		default:
			return "C" + index + ": active...";
		}
	}

	private String buildFinishedLine(int index, CustomerSpawner.VisualCustomer vc) {
		String resultLabel = (vc.purchaseResult == null || vc.purchaseResult.isBlank())
				? "no result"
						: vc.purchaseResult;

		return String.format(
				"C%d: Initial=$%.2f  Spent=$%.2f  Final=$%.2f  (%s)",
				index,
				vc.walletBefore,
				vc.spent,
				vc.walletAfter,
				resultLabel
				);
	}

	// ============================================================
	// Rendering helpers
	// ============================================================

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