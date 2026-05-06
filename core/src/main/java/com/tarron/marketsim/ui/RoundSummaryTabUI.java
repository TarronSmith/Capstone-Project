package com.tarron.marketsim.ui;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.tarron.marketsim.simulation.MarketEngine;

/**
 * RoundSummaryTabUI
 *
 * Responsibilities:
 * - Renders a top-center round summary tab.
 * - Shows phase, round number, player cash, and rival cash while collapsed.
 * - Expands into a summary panel when clicked.
 * - Displays last-round summary information.
 */
public class RoundSummaryTabUI {

	private static final String PANEL_PATH = "ui/Panels/Panel6.png";
	private static final String FONT_PATH = "ui/pxfntfree9-8x10.png";

	private static final int FONT_CHAR_W = 8;
	private static final int FONT_CHAR_H = 10;
	private static final int FONT_COLS = 10;

	private static final String FONT_CHARS =
			"ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
			"abcdefghijklmnopqrstuvwxyz" +
			"0123456789.$";

	private static final float SCREEN_W = 800f;
	private static final float SCREEN_H = 600f;

	private static final float TAB_W = 320f;
	private static final float TAB_H = 34f;
	private static final float TAB_X = (SCREEN_W - TAB_W) / 2f;
	private static final float TAB_Y = SCREEN_H - TAB_H;

	private static final float PANEL_W = 330f;
	private static final float PANEL_H = 210f;
	private static final float PANEL_X = (SCREEN_W - PANEL_W) / 2f;
	private static final float PANEL_Y = TAB_Y - PANEL_H - 6f;

	private static final float TITLE_SCALE = 1.25f;
	private static final float TEXT_SCALE = 1.1f;
	private static final float SMALL_TEXT_SCALE = 1.0f;

	private static final float LINE_HEIGHT = 18f;
	private static final float SOLD_LINE_HEIGHT = 16f;
	private static final float SOLD_TEXT_MAX_WIDTH = PANEL_W - 48f;

	private boolean expanded = false;

	private final Rectangle tabBounds;
	private final Rectangle panelBounds;

	private final Texture panelTexture;
	private final Texture fontTexture;

	private final TextureRegion fullPanelRegion;
	private final TextureRegion tabRegion;

	public RoundSummaryTabUI() {
		panelTexture = new Texture(Gdx.files.internal(PANEL_PATH));
		panelTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

		fontTexture = new Texture(Gdx.files.internal(FONT_PATH));
		fontTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

		fullPanelRegion = new TextureRegion(panelTexture);

		tabRegion = new TextureRegion(
				panelTexture,
				0,
				0,
				panelTexture.getWidth(),
				Math.min(42, panelTexture.getHeight())
				);

		tabRegion.flip(false, true);

		tabBounds = new Rectangle(TAB_X, TAB_Y, TAB_W, TAB_H);
		panelBounds = new Rectangle(PANEL_X, PANEL_Y, PANEL_W, PANEL_H);
	}

	public void handleClick(float mouseX, float mouseY) {
		if (tabBounds.contains(mouseX, mouseY)) {
			expanded = !expanded;
			return;
		}

		if (expanded && panelBounds.contains(mouseX, mouseY)) {
			return;
		}

		if (expanded) {
			expanded = false;
		}
	}

	public void render(SpriteBatch batch, BitmapFont unusedFont, MarketEngine engine) {
		if (batch == null || engine == null) return;

		if (expanded) {
			drawExpandedPanel(batch, engine);
		}

		drawCollapsedTab(batch, engine);
	}

	private void drawCollapsedTab(SpriteBatch batch, MarketEngine engine) {
		batch.draw(tabRegion, tabBounds.x, tabBounds.y, tabBounds.width, tabBounds.height);

		String text =
				getDisplayPhase(engine) +
				" R" + getCurrentRound(engine) +
				" P$" + formatMoney(getPlayerCash(engine)) +
				" V$" + formatMoney(getRivalCash(engine));

		drawPixelText(batch, text, tabBounds.x + 18f, tabBounds.y + 9f, TITLE_SCALE);
	}

	private void drawExpandedPanel(SpriteBatch batch, MarketEngine engine) {
		batch.draw(fullPanelRegion, panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height);

		float x = panelBounds.x + 18f;
		float y = panelBounds.y + panelBounds.height - 32f;

		drawPixelText(batch, "ROUND SUMMARY", x, y, TITLE_SCALE);

		y -= LINE_HEIGHT + 6f;

		drawPixelText(batch, "Top Desired: " + getLastTopDesired(engine), x, y, TEXT_SCALE);
		y -= LINE_HEIGHT;

		drawPixelText(batch, "Top Sold: " + getLastTopSold(engine), x, y, TEXT_SCALE);
		y -= LINE_HEIGHT;

		drawPixelText(batch, "Spent: $" + formatMoney(getLastSpent(engine)), x, y, TEXT_SCALE);
		y -= LINE_HEIGHT;

		drawPixelText(batch, "Revenue: $" + formatMoney(getLastRevenue(engine)), x, y, TEXT_SCALE);
		y -= LINE_HEIGHT;

		drawPixelText(batch, "Profit: $" + formatMoney(getLastProfit(engine)), x, y, TEXT_SCALE);
		y -= LINE_HEIGHT + 4f;

		drawPixelText(batch, "Items Sold:", x, y, TEXT_SCALE);
		y -= LINE_HEIGHT;

		List<String> lines = getLastSoldItemsLines(engine);

		if (lines.isEmpty()) {
			drawPixelText(batch, "None", x + 12f, y, SMALL_TEXT_SCALE);
			return;
		}

		for (int i = 0; i < lines.size(); i++) {
			drawPixelText(batch, lines.get(i), x + 12f, y - (i * SOLD_LINE_HEIGHT), SMALL_TEXT_SCALE);
		}
	}

	private String getDisplayPhase(MarketEngine engine) {
		Object rm = engine.getRoundManager();
		String phase = tryString(rm, "getPhase");

		if (phase == null || phase.isBlank()) {
			return "BUY";
		}

		if (phase.equals("SELL") || phase.equals("RESULTS")) {
			return "SELL";
		}

		return "BUY";
	}

	private int getCurrentRound(MarketEngine engine) {
		Object rm = engine.getRoundManager();
		Integer r = tryInt(rm, "getRoundNumber");
		return r == null ? 1 : r;
	}

	private double getPlayerCash(MarketEngine engine) {
		Object rm = engine.getRoundManager();
		Double v = tryDouble(rm, "getPlayerCash");
		return v == null ? 0.0 : v;
	}

	private double getRivalCash(MarketEngine engine) {
		Object rm = engine.getRoundManager();
		Double v = tryDouble(rm, "getRivalCash");
		return v == null ? 0.0 : v;
	}

	private String getLastTopDesired(MarketEngine engine) {
		Object rm = engine.getRoundManager();
		String v = tryString(rm, "getLastTopDesired");
		return v == null || v.isBlank() ? "None" : shortItemName(v);
	}

	private String getLastTopSold(MarketEngine engine) {
		Object rm = engine.getRoundManager();
		String v = tryString(rm, "getLastTopSold");
		return v == null || v.isBlank() ? "None" : shortItemName(v);
	}

	private double getLastSpent(MarketEngine engine) {
		Object rm = engine.getRoundManager();
		Double v = tryDouble(rm, "getLastRoundSpent");
		return v == null ? 0.0 : v;
	}

	private double getLastRevenue(MarketEngine engine) {
		Object rm = engine.getRoundManager();
		Double v = tryDouble(rm, "getLastRoundRevenue");
		return v == null ? 0.0 : v;
	}

	private double getLastProfit(MarketEngine engine) {
		Object rm = engine.getRoundManager();
		Double v = tryDouble(rm, "getLastRoundProfit");
		return v == null ? 0.0 : v;
	}

	private List<String> getLastSoldItemsLines(MarketEngine engine) {
		Object rm = engine.getRoundManager();

		Object map =
				tryObject(rm, "getLastSoldByItemName") != null ? tryObject(rm, "getLastSoldByItemName") :
				tryObject(rm, "getLastSoldCounts");

		List<String> lines = new ArrayList<>();
		if (!(map instanceof Map<?, ?>)) return lines;

		StringBuilder current = new StringBuilder();

		for (Map.Entry<?, ?> e : ((Map<?, ?>) map).entrySet()) {
			if (e.getKey() == null || e.getValue() == null) continue;

			String part = shortItemName(e.getKey().toString()) + "x" + e.getValue();

			String test = current.length() == 0 ? part : current + " " + part;

			if (estimateWidth(test) > SOLD_TEXT_MAX_WIDTH && current.length() > 0) {
				lines.add(current.toString());
				current = new StringBuilder(part);
			} else {
				if (current.length() > 0) current.append(" ");
				current.append(part);
			}
		}

		if (current.length() > 0) lines.add(current.toString());

		return lines;
	}

	private String shortItemName(String name) {
		if (name == null) return "";

		if (name.endsWith("Item")) {
			return name.substring(0, name.length() - "Item".length());
		}

		return name;
	}

	private float estimateWidth(String text) {
		if (text == null) return 0f;
		return text.length() * FONT_CHAR_W * SMALL_TEXT_SCALE;
	}

	private String formatMoney(double v) {
		return String.format(java.util.Locale.US, "%.2f", v);
	}

	private void drawPixelText(SpriteBatch batch, String text, float x, float y, float scale) {
		if (text == null || text.isEmpty()) return;

		float cursor = x;
		float w = FONT_CHAR_W * scale;
		float h = FONT_CHAR_H * scale;

		for (char c : text.toCharArray()) {
			if (c == ' ') {
				cursor += w;
				continue;
			}

			TextureRegion r = regionForChar(c);

			if (r != null) {
				batch.draw(r, cursor, y, w, h);
			}

			cursor += w;
		}
	}

	private TextureRegion regionForChar(char ch) {
		int i = FONT_CHARS.indexOf(ch);
		if (i < 0) return null;

		int col = i % FONT_COLS;
		int row = i / FONT_COLS;

		return new TextureRegion(
				fontTexture,
				col * FONT_CHAR_W,
				row * FONT_CHAR_H,
				FONT_CHAR_W,
				FONT_CHAR_H
				);
	}

	private Object tryObject(Object t, String m) {
		if (t == null || m == null) return null;

		try {
			Method method = t.getClass().getMethod(m);
			return method.invoke(t);
		} catch (Exception e) {
			return null;
		}
	}

	private String tryString(Object t, String m) {
		Object v = tryObject(t, m);
		return v == null ? null : v.toString();
	}

	private Integer tryInt(Object t, String m) {
		Object v = tryObject(t, m);
		return v instanceof Number ? ((Number) v).intValue() : null;
	}

	private Double tryDouble(Object t, String m) {
		Object v = tryObject(t, m);
		return v instanceof Number ? ((Number) v).doubleValue() : null;
	}

	public void dispose() {
		if (panelTexture != null) panelTexture.dispose();
		if (fontTexture != null) fontTexture.dispose();
	}
}