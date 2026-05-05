package com.tarron.marketsim.ui;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.tarron.marketsim.model.Item;
import com.tarron.marketsim.simulation.MarketEngine;

/**
 * BuyMenuUI
 *
 * Responsibilities:
 * - Handles BUY-phase order/inventory menu input.
 * - Renders menu panels using UI sprites.
 * - Renders menu text using the pixel font sprite sheet.
 * - Provides per-item BUY buttons in the order menu.
 */
public class BuyMenuUI {

	private static final String PANEL_PATH = "ui/Panels/Panel6.png";
	private static final String ICONS_PATH = "ui/Basic_Menu_Icons.png";
	private static final String FONT_PATH = "ui/pxfntfree9-8x10.png";

	private static final int FONT_CHAR_W = 8;
	private static final int FONT_CHAR_H = 10;
	private static final int FONT_COLS = 10;

	private static final String FONT_CHARS =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
					"abcdefghijklmnopqrstuvwxyz" +
					"0123456789.$";

	public enum MenuState {
		CLOSED,
		ORDER_OPEN,
		INVENTORY_OPEN
	}

	private MenuState state = MenuState.CLOSED;

	private final Rectangle orderButtonBounds;
	private final Rectangle inventoryButtonBounds;
	private final Rectangle orderPanelBounds;
	private final Rectangle inventoryPanelBounds;

	private static final float PANEL_X = 500f;
	private static final float TAB_Y = 0f;
	private static final float TAB_H = 34f;
	private static final float PANEL_Y = TAB_Y + TAB_H + 6f;
	private static final float PANEL_W = 290f;
	private static final float PANEL_H = 190f;

	private static final float TAB_W = 140f;
	private static final float TAB_GAP = 10f;

	private static final float ROW_HEIGHT = 20f;
	private static final float ROW_TOP_PADDING = 54f;
	private static final float ROW_SIDE_PADDING = 18f;

	private static final float BUY_BUTTON_W = 46f;
	private static final float BUY_BUTTON_H = 18f;
	private static final float BUY_BUTTON_RIGHT_PADDING = 8f;

	private static final float FONT_SCALE = 1.4f;
	private static final float TITLE_FONT_SCALE = 1.6f;
	private static final float ROW_FONT_SCALE = 1.2f;
	private static final float PRICE_FONT_SCALE = 1.0f;
	private static final float BUY_FONT_SCALE = 1.0f;

	/*
	 * Order panel column positions.
	 *
	 * Buy column = vendor cost paid by the player.
	 * Sell column = customer-facing sale price.
	 */
	private static final float ORDER_NAME_X_OFFSET = 14f;
	private static final float ORDER_BUY_PRICE_X_OFFSET = 120f;
	private static final float ORDER_SELL_PRICE_X_OFFSET = 170f;

	private final Texture panelTexture;
	private final Texture iconsTexture;
	private final Texture fontTexture;

	private final TextureRegion fullPanelRegion;
	private final TextureRegion tabRegion;

	public BuyMenuUI() {
		panelTexture = new Texture(Gdx.files.internal(PANEL_PATH));
		panelTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

		iconsTexture = new Texture(Gdx.files.internal(ICONS_PATH));
		iconsTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

		fontTexture = new Texture(Gdx.files.internal(FONT_PATH));
		fontTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

		fullPanelRegion = new TextureRegion(panelTexture, 0, 0, panelTexture.getWidth(), panelTexture.getHeight());
		tabRegion = new TextureRegion(panelTexture, 0, 0, panelTexture.getWidth(), Math.min(42, panelTexture.getHeight()));

		orderButtonBounds = new Rectangle(PANEL_X, TAB_Y, TAB_W, TAB_H);
		inventoryButtonBounds = new Rectangle(PANEL_X + TAB_W + TAB_GAP, TAB_Y, TAB_W, TAB_H);

		orderPanelBounds = new Rectangle(PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
		inventoryPanelBounds = new Rectangle(PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
	}

	// ============================================================
	// Input
	// ============================================================

	public void handleClick(float mouseX, float mouseY, MarketEngine engine) {
		if (engine == null) return;

		if (orderButtonBounds.contains(mouseX, mouseY)) {
			state = (state == MenuState.ORDER_OPEN) ? MenuState.CLOSED : MenuState.ORDER_OPEN;
			return;
		}

		if (inventoryButtonBounds.contains(mouseX, mouseY)) {
			state = (state == MenuState.INVENTORY_OPEN) ? MenuState.CLOSED : MenuState.INVENTORY_OPEN;
			return;
		}

		if (state == MenuState.ORDER_OPEN) {
			List<Item> catalogItems = engine.getCatalogItems();

			for (int i = 0; i < catalogItems.size(); i++) {
				if (getBuyButtonBounds(i).contains(mouseX, mouseY)) {
					engine.buyCatalogSlot(i + 1);
					return;
				}
			}
		}
	}

	// ============================================================
	// Rendering
	// ============================================================

	public void render(SpriteBatch batch, BitmapFont unusedFont, MarketEngine engine) {
		if (batch == null || engine == null) return;

		if (state == MenuState.ORDER_OPEN) {
			drawOrderPanel(batch, engine);
		} else if (state == MenuState.INVENTORY_OPEN) {
			drawInventoryPanel(batch, engine);
		}

		drawButtons(batch);
	}

	private void drawButtons(SpriteBatch batch) {
		batch.draw(tabRegion, orderButtonBounds.x, orderButtonBounds.y, orderButtonBounds.width, orderButtonBounds.height);
		batch.draw(tabRegion, inventoryButtonBounds.x, inventoryButtonBounds.y, inventoryButtonBounds.width, inventoryButtonBounds.height);

		drawPixelText(batch, "ORDER", orderButtonBounds.x + 34f, orderButtonBounds.y + 8f, FONT_SCALE);
		drawPixelText(batch, "INVENTORY", inventoryButtonBounds.x + 12f, inventoryButtonBounds.y + 8f, FONT_SCALE);
	}

	private void drawOrderPanel(SpriteBatch batch, MarketEngine engine) {
		batch.draw(fullPanelRegion, orderPanelBounds.x, orderPanelBounds.y, orderPanelBounds.width, orderPanelBounds.height);

		drawPixelText(
				batch,
				"ORDER",
				orderPanelBounds.x + 18f,
				orderPanelBounds.y + orderPanelBounds.height - 30f,
				TITLE_FONT_SCALE
				);

		List<Item> items = engine.getCatalogItems();

		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			if (item == null) continue;

			float textY = rowTextY(orderPanelBounds, i);

			String itemName = shortItemName(item.getName());
			double buyCost = engine.getVendorCost(item);
			double sellPrice = item.getPrice();

			String nameText = (i + 1) + " " + itemName;
			String buyText = "B$" + String.format(Locale.US, "%.2f", buyCost);
			String sellText = "S$" + String.format(Locale.US, "%.2f", sellPrice);

			drawPixelText(batch, nameText, orderPanelBounds.x + ORDER_NAME_X_OFFSET, textY, ROW_FONT_SCALE);
			drawPixelText(batch, buyText, orderPanelBounds.x + ORDER_BUY_PRICE_X_OFFSET, textY + 1f, PRICE_FONT_SCALE);
			drawPixelText(batch, sellText, orderPanelBounds.x + ORDER_SELL_PRICE_X_OFFSET, textY + 1f, PRICE_FONT_SCALE);

			Rectangle buyButton = getBuyButtonBounds(i);

			batch.draw(tabRegion, buyButton.x, buyButton.y, buyButton.width, buyButton.height);
			drawPixelText(batch, "BUY", buyButton.x + 6f, buyButton.y + 3f, BUY_FONT_SCALE);
		}
	}

	private void drawInventoryPanel(SpriteBatch batch, MarketEngine engine) {
		batch.draw(fullPanelRegion, inventoryPanelBounds.x, inventoryPanelBounds.y, inventoryPanelBounds.width, inventoryPanelBounds.height);

		drawPixelText(
				batch,
				"INVENTORY",
				inventoryPanelBounds.x + 18f,
				inventoryPanelBounds.y + inventoryPanelBounds.height - 30f,
				TITLE_FONT_SCALE
				);

		Map<String, Integer> counts = buildInventoryCounts(engine);
		float y = inventoryPanelBounds.y + inventoryPanelBounds.height - ROW_TOP_PADDING;

		if (counts.isEmpty()) {
			drawPixelText(batch, "No items purchased", inventoryPanelBounds.x + ROW_SIDE_PADDING, y, FONT_SCALE);
			return;
		}

		int line = 0;
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			String rowText = shortItemName(entry.getKey()) + " x" + entry.getValue();
			drawPixelText(batch, rowText, inventoryPanelBounds.x + ROW_SIDE_PADDING, y - line * ROW_HEIGHT, FONT_SCALE);
			line++;
		}
	}

	// ============================================================
	// Pixel font rendering
	// ============================================================

	private void drawPixelText(SpriteBatch batch, String text, float x, float y, float scale) {
		if (text == null || text.isEmpty()) return;

		float cursorX = x;
		float drawW = FONT_CHAR_W * scale;
		float drawH = FONT_CHAR_H * scale;

		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);

			if (ch == ' ') {
				cursorX += drawW;
				continue;
			}

			TextureRegion region = regionForChar(ch);

			if (region != null) {
				batch.draw(region, cursorX, y, drawW, drawH);
			}

			cursorX += drawW;
		}
	}

	private TextureRegion regionForChar(char ch) {
		int index = FONT_CHARS.indexOf(ch);
		if (index < 0) return null;

		int col = index % FONT_COLS;
		int row = index / FONT_COLS;

		return new TextureRegion(
				fontTexture,
				col * FONT_CHAR_W,
				row * FONT_CHAR_H,
				FONT_CHAR_W,
				FONT_CHAR_H
				);
	}

	// ============================================================
	// Inventory helpers
	// ============================================================

	private Map<String, Integer> buildInventoryCounts(MarketEngine engine) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		if (engine == null || engine.getPlayerShop() == null) return counts;

		List<Item> catalogItems = engine.getCatalogItems();
		if (catalogItems == null) return counts;

		for (Item item : catalogItems) {
			if (item == null || item.getName() == null) continue;

			int quantity = engine.getPlayerShop().getQuantity(item);

			if (quantity > 0) {
				counts.put(item.getName(), quantity);
			}
		}

		return counts;
	}

	/**
	 * Returns a compact UI-facing item name.
	 *
	 * The model can keep descriptive internal item names, while the menu can
	 * display shorter labels that fit inside the order panel.
	 */
	private String shortItemName(String name) {
		if (name == null) return "";

		if (name.endsWith("Item")) {
			return name.substring(0, name.length() - "Item".length());
		}

		return name;
	}

	// ============================================================
	// Row layout helpers
	// ============================================================

	private Rectangle getBuyButtonBounds(int index) {
		float rowY = rowTextY(orderPanelBounds, index);

		return new Rectangle(
				orderPanelBounds.x + orderPanelBounds.width - BUY_BUTTON_W - BUY_BUTTON_RIGHT_PADDING,
				rowY - 4f,
				BUY_BUTTON_W,
				BUY_BUTTON_H
				);
	}

	private float rowTextY(Rectangle panelBounds, int index) {
		return panelBounds.y + panelBounds.height - ROW_TOP_PADDING - (index * ROW_HEIGHT);
	}

	// ============================================================
	// Accessors
	// ============================================================

	public MenuState getState() {
		return state;
	}

	public void close() {
		state = MenuState.CLOSED;
	}

	public void dispose() {
		if (panelTexture != null) panelTexture.dispose();
		if (iconsTexture != null) iconsTexture.dispose();
		if (fontTexture != null) fontTexture.dispose();
	}
}