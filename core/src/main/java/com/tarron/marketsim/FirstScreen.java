package com.tarron.marketsim;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tarron.marketsim.model.Item;
import com.tarron.marketsim.model.Shop;
import com.tarron.marketsim.simulation.CustomerListRenderer;
import com.tarron.marketsim.simulation.CustomerSpawner;
import com.tarron.marketsim.simulation.HudRenderer;
import com.tarron.marketsim.simulation.MarketEngine;
import com.tarron.marketsim.simulation.RoundManager;

/**
 * FirstScreen
 *
 * Purpose:
 * - LibGDX Screen implementation for the MarketSim game.
 * - Owns rendering (shapes + text) and user input mapping.
 * - Delegates all simulation logic (phases, customers, buying/selling, AI stocking)
 *   to MarketEngine.
 *
 * Responsibilities:
 * - Initialize camera/rendering resources and the MarketEngine
 * - Poll input and call engine actions (reset, buy items, start phases)
 * - Render shop rectangles, customers, and HUD/stat text
 */
public class FirstScreen implements Screen {

	// ---------------- Session Settings ----------------
	// Customer count bounds for the session (numCustomers is forced to even)
	private static final int INITIAL_CUSTOMERS = 20;
	private static final int MIN_CUSTOMERS = 2;
	private static final int MAX_CUSTOMERS = 60;

	// Starting budget and legacy costs (MarketEngine/Catalog owns real vendor costs now)
	private static final double STARTING_CASH = 20.0;
	private static final double VENDOR_CHEAP_COST = 2.0;
	private static final double VENDOR_PREMIUM_COST = 5.0;

	// Fixed virtual resolution for orthographic camera
	private static final float SCREEN_W = 800f;
	private static final float SCREEN_H = 600f;

	// Visual-only customer radius for drawing circles (simulation uses its own movement)
	private static final float CUSTOMER_RADIUS = 10f;

	// Shop rectangles (world positions)
	private final float shopW = 220, shopH = 140;
	private final float playerX = 80,  playerY = 260;
	private final float rivalX  = 500, rivalY  = 260;

	// Rendering
	private OrthographicCamera camera;
	private ShapeRenderer shapes;
	private SpriteBatch batch;

	// Fonts for HUD and labels
	private BitmapFont fontMain;
	private BitmapFont fontSmall;
	private BitmapFont fontStats;

	// Simulation "brain"
	private MarketEngine engine;

	// Text rendering helpers (HUD + customer log)
	private HudRenderer hudRenderer;
	private CustomerListRenderer customerListRenderer;

	@Override
	public void show() {
		camera = new OrthographicCamera();
		camera.setToOrtho(false, (int) SCREEN_W, (int) SCREEN_H);

		shapes = new ShapeRenderer();
		batch = new SpriteBatch();

		fontMain = new BitmapFont();
		fontSmall = new BitmapFont();
		fontStats = new BitmapFont();

		// Smaller fonts for dense HUD text
		fontStats.getData().setScale(0.75f);
		fontSmall.getData().setScale(0.65f);

		hudRenderer = new HudRenderer();
		customerListRenderer = new CustomerListRenderer();

		engine = new MarketEngine(
				INITIAL_CUSTOMERS,
				MIN_CUSTOMERS,
				MAX_CUSTOMERS,
				STARTING_CASH,
				VENDOR_CHEAP_COST,
				VENDOR_PREMIUM_COST,
				CUSTOMER_RADIUS
				);

		// One-time initialization for market plan + initial AI stocking
		engine.initAtLaunch();
	}

	@Override
	public void render(float delta) {
		clearScreen();

		camera.update();
		shapes.setProjectionMatrix(camera.combined);
		batch.setProjectionMatrix(camera.combined);

		// Inputs that should work in all phases
		handleGlobalKeys();

		// Phase-specific input mapping
		if (engine.getPhase() == RoundManager.Phase.BUY) {
			handleBuyPhaseKeys();
		} else if (engine.getPhase() == RoundManager.Phase.RESULTS) {
			handleResultsKeys();
		}

		// Updates customer movement + arrival processing during SELL
		engine.update(delta);

		// World pass (shapes + shop labels)
		drawWorld();

		// Text pass (HUD + customer event list)
		batch.begin();
		drawText();
		batch.end();
	}

	// ============================================================
	// INPUT
	// ============================================================

	/**
	 * Global hotkeys across all phases.
	 * - R: hard reset simulation state (optionally wipes knowledge)
	 * - M: reroll market distribution then reset
	 */
	private void handleGlobalKeys() {
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.R)) {
			engine.hardReset(true);
		}

		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.M)) {
			engine.rerollMarketAndReset();
		}
	}

	/**
	 * BUY phase:
	 * - Up/Down: change customer count (in steps of 2)
	 * - 1..9: buy catalog slot (slots outside catalog size are ignored)
	 * - Enter: start SELL phase
	 */
	private void handleBuyPhaseKeys() {
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) {
			engine.incCustomersBy2();
		}
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
			engine.decCustomersBy2();
		}

		// Catalog purchase mapping: number keys -> catalog slot (1-based)
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_1)) engine.buyCatalogSlot(1);
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_2)) engine.buyCatalogSlot(2);
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_3)) engine.buyCatalogSlot(3);
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_4)) engine.buyCatalogSlot(4);
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_5)) engine.buyCatalogSlot(5);
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_6)) engine.buyCatalogSlot(6);
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_7)) engine.buyCatalogSlot(7);
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_8)) engine.buyCatalogSlot(8);
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_9)) engine.buyCatalogSlot(9);

		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
			beginSellPhase();
		}
	}

	/**
	 * RESULTS phase:
	 * - Enter: advance to next BUY phase (snapshots last-round stats + restocks rival)
	 */
	private void handleResultsKeys() {
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
			engine.endResultsAndStartNextBuy();
		}
	}

	/**
	 * Starts SELL phase and spawns customers into the world.
	 * Spawn positions are UI concerns; simulation logic stays in MarketEngine.
	 */
	private void beginSellPhase() {
		// Spawn band for customers (visual layout)
		float startX = 380;
		float startY = 60;
		float spacing = 22;

		// Keeps customers from clipping into shop rectangles
		float paddingInside = 12f;

		engine.beginSellPhase(
				startX,
				startY,
				spacing,
				playerX,
				playerY,
				shopW,
				shopH,
				rivalX,
				rivalY,
				paddingInside
				);
	}

	// ============================================================
	// DRAWING
	// ============================================================

	private void clearScreen() {
		// Dark background for readability
		Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	/**
	 * World pass:
	 * - Draw shop rectangles and customer circles
	 * - Draw shop name labels
	 */
	private void drawWorld() {
		Shop playerShop = engine.getPlayerShop();
		Shop rivalShop = engine.getRivalShop();
		List<CustomerSpawner.VisualCustomer> crowd = engine.getCrowd();

		shapes.begin(ShapeRenderer.ShapeType.Filled);

		// Shop bodies
		shapes.rect(playerX, playerY, shopW, shopH);
		shapes.rect(rivalX, rivalY, shopW, shopH);

		// Customer bodies
		for (CustomerSpawner.VisualCustomer vc : crowd) {
			shapes.circle(vc.x, vc.y, CUSTOMER_RADIUS);
		}

		shapes.end();

		// Shop titles (text)
		batch.begin();
		fontMain.draw(batch, playerShop.getName(), playerX, playerY + shopH + 20);
		fontMain.draw(batch, rivalShop.getName(), rivalX, rivalY + shopH + 20);
		batch.end();
	}

	/**
	 * Text pass:
	 * - Customer purchase log (per-customer outcome lines)
	 * - HUD summary (phase, cash, inventory, last round stats, catalog list)
	 */
	private void drawText() {
		RoundManager rm = engine.getRoundManager();
		Shop playerShop = engine.getPlayerShop();
		Shop rivalShop = engine.getRivalShop();
		List<CustomerSpawner.VisualCustomer> crowd = engine.getCrowd();

		// Legacy references (kept while HUD still shows cheap/premium lines)
		Item cheap = engine.getCheapItem();
		Item premium = engine.getPremiumItem();

		customerListRenderer.draw(
				batch,
				fontSmall,
				rm,
				crowd,
				playerShop,
				rivalShop,
				playerX,
				playerY,
				rivalX,
				rivalY
				);

		hudRenderer.draw(
				batch,
				fontStats,
				fontSmall,
				rm,
				playerShop,
				rivalShop,
				cheap,
				premium,
				engine.getNumCustomers(),
				engine.getVendorCheapCost(),
				engine.getVendorPremiumCost(),
				engine.getMarketCheapCount(),
				engine.getMarketValueCount(),
				engine.getMarketConspCount(),
				SCREEN_W,
				engine.getCatalogItems(),
				engine::getVendorCost
				);
	}

	// ============================================================
	// Screen lifecycle
	// ============================================================

	@Override
	public void resize(int width, int height) {
		// Screen is fixed-resolution for now; input guard prevents invalid sizes.
		if (width <= 0 || height <= 0) return;
	}

	@Override public void pause() {}
	@Override public void resume() {}
	@Override public void hide() {}

	@Override
	public void dispose() {
		shapes.dispose();
		batch.dispose();
		fontMain.dispose();
		fontSmall.dispose();
		fontStats.dispose();
	}
}