package com.tarron.marketsim.screen;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tarron.marketsim.MarketRivalGame;
import com.tarron.marketsim.model.Shop;
import com.tarron.marketsim.simulation.CustomerSpawner;
import com.tarron.marketsim.simulation.MarketEngine;
import com.tarron.marketsim.simulation.RoundManager;
import com.tarron.marketsim.ui.CustomerListRenderer;
import com.tarron.marketsim.ui.HudRenderer;

/**
 * FirstScreen
 *
 * Responsibilities:
 * - Main gameplay screen (BUY / SELL / RESULTS).
 * - Handles input mapping and delegates actions to MarketEngine.
 * - Renders shops, customers, and HUD.
 * - Switches to GameOverScreen when the session ends.
 */
public class FirstScreen implements Screen {

	// ============================================================
	// Session settings
	// ============================================================

	private static final int INITIAL_CUSTOMERS = 20;
	private static final int MIN_CUSTOMERS = 2;
	private static final int MAX_CUSTOMERS = 60;
	private static final double STARTING_CASH = 20.0;

	// ============================================================
	// Viewport (fixed virtual resolution)
	// ============================================================

	private static final float SCREEN_W = 800f;
	private static final float SCREEN_H = 600f;

	// Visual-only customer radius
	private static final float CUSTOMER_RADIUS = 10f;

	// ============================================================
	// Sprite sheet settings
	// ============================================================

	/*
	 * This path is relative to src/main/resources.
	 *
	 * Example actual file location:
	 * src/main/resources/sprites/humanoid/Basic Humanoid Sprites 2x.png
	 */
	private static final String CUSTOMER_SHEET_PATH =
			"sprites/humanoid/Basic Humanoid Sprites 2x.png";

	/*
	 * Sheet layout for the humanoid pack you showed:
	 * - 5 columns
	 * - 3 rows
	 * - each sprite is 32x32 in the 2x sheet
	 * - about 4 px gap between sprites
	 * - about 2 px margin around the outside
	 *
	 * If the slicing looks slightly off in-game, these are the values to tweak.
	 */
	private static final int CUSTOMER_SHEET_COLS = 5;
	private static final int CUSTOMER_SHEET_ROWS = 3;
	private static final int CUSTOMER_FRAME_W = 32;
	private static final int CUSTOMER_FRAME_H = 32;
	private static final int CUSTOMER_MARGIN_X = 2;
	private static final int CUSTOMER_MARGIN_Y = 2;
	private static final int CUSTOMER_SPACING_X = 4;
	private static final int CUSTOMER_SPACING_Y = 4;

	// ============================================================
	// Layout
	// ============================================================

	private final float shopW = 220, shopH = 140;
	private final float playerX = 80,  playerY = 260;
	private final float rivalX  = 500, rivalY  = 260;

	// ============================================================
	// Rendering
	// ============================================================

	private OrthographicCamera camera;
	private ShapeRenderer shapes;
	private SpriteBatch batch;
	private BitmapFont fontMain;
	private BitmapFont fontSmall;
	private BitmapFont fontStats;

	/*
	 * Customer sprite rendering:
	 * - customerSheet holds the full sprite sheet image
	 * - customerSprites holds the extracted individual sprites from that sheet
	 *
	 * Each customer will store one spriteIndex, and FirstScreen will draw that
	 * specific sprite every frame.
	 */
	private Texture customerSheet;
	private TextureRegion[] customerSprites;

	// ============================================================
	// Simulation + UI helpers
	// ============================================================

	private MarketEngine engine;
	private HudRenderer hudRenderer;
	private CustomerListRenderer customerListRenderer;

	// Screen owner (screen switching)
	private final MarketRivalGame game;

	public FirstScreen(MarketRivalGame game) {
		this.game = game;
	}

	@Override
	public void show() {
		camera = new OrthographicCamera();
		camera.setToOrtho(false, (int) SCREEN_W, (int) SCREEN_H);

		shapes = new ShapeRenderer();
		batch = new SpriteBatch();

		fontMain = new BitmapFont();
		fontSmall = new BitmapFont();
		fontStats = new BitmapFont();

		fontStats.getData().setScale(0.75f);
		fontSmall.getData().setScale(0.65f);

		hudRenderer = new HudRenderer();
		customerListRenderer = new CustomerListRenderer();

		/*
		 * Load the full customer sprite sheet once at screen startup,
		 * then slice it into individual TextureRegion sprites.
		 */
		customerSheet = new Texture(CUSTOMER_SHEET_PATH);
		customerSprites = buildCustomerSprites(customerSheet);

		engine = new MarketEngine(
				INITIAL_CUSTOMERS,
				MIN_CUSTOMERS,
				MAX_CUSTOMERS,
				STARTING_CASH,
				CUSTOMER_RADIUS
				);

		engine.initAtLaunch();
	}

	@Override
	public void render(float delta) {
		clearScreen();

		camera.update();
		shapes.setProjectionMatrix(camera.combined);
		batch.setProjectionMatrix(camera.combined);

		handleGlobalKeys();

		if (engine.getPhase() == RoundManager.Phase.BUY) {
			handleBuyPhaseKeys();
		} else if (engine.getPhase() == RoundManager.Phase.RESULTS) {
			handleResultsKeys();
		}

		engine.update(delta);

		if (engine.isGameOver()) {
			game.setScreen(new GameOverScreen(
					game,
					engine.getOutcome(),
					engine.getRoundManager().getPlayerCash(),
					engine.getRoundManager().getRivalCash()
					));
			dispose();
			return;
		}

		drawWorld();

		batch.begin();
		drawText();
		batch.end();
	}

	// ============================================================
	// Input
	// ============================================================

	private void handleGlobalKeys() {
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.R)) {
			engine.hardReset(true);
		}

		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.M)) {
			engine.rerollMarketAndReset();
		}
	}

	private void handleBuyPhaseKeys() {
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) {
			engine.incCustomersBy2();
		}
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
			engine.decCustomersBy2();
		}

		// Catalog purchase mapping: 1..9 -> slot number (1-based).
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

	private void handleResultsKeys() {
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
			engine.endResultsAndStartNextBuy();
		}
	}

	private void beginSellPhase() {
		float startX = 380;
		float startY = 60;
		float spacing = 22;

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
	// Drawing
	// ============================================================

	private void clearScreen() {
		Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	private void drawWorld() {
		Shop playerShop = engine.getPlayerShop();
		Shop rivalShop = engine.getRivalShop();
		List<CustomerSpawner.VisualCustomer> crowd = engine.getCrowd();

		/*
		 * Keep the shops as simple rectangles for now.
		 * We are only replacing the customer circles with randomized sprites.
		 */
		shapes.begin(ShapeRenderer.ShapeType.Filled);

		shapes.rect(playerX, playerY, shopW, shopH);
		shapes.rect(rivalX, rivalY, shopW, shopH);

		shapes.end();

		batch.begin();

		/*
		 * Draw each customer using the sprite assigned at spawn time.
		 *
		 * Important:
		 * - vc.x / vc.y represent the customer center
		 * - batch.draw(...) uses bottom-left coordinates
		 * - so we subtract CUSTOMER_RADIUS to center the sprite on the old circle position
		 *
		 * Fallback behavior:
		 * - if sprite data is missing or index is invalid, draw the full sheet's first sprite if available
		 * - if customerSprites failed to load entirely, the customer simply won't render
		 */
		for (CustomerSpawner.VisualCustomer vc : crowd) {
			if (vc == null) continue;

			TextureRegion region = getCustomerSpriteFor(vc);

			if (region != null) {
				batch.draw(
						region,
						vc.x - CUSTOMER_RADIUS,
						vc.y - CUSTOMER_RADIUS,
						CUSTOMER_RADIUS * 2f,
						CUSTOMER_RADIUS * 2f
						);
			}
		}

		fontMain.draw(batch, playerShop.getName(), playerX, playerY + shopH + 20);
		fontMain.draw(batch, rivalShop.getName(), rivalX, rivalY + shopH + 20);

		batch.end();
	}

	private void drawText() {
		RoundManager rm = engine.getRoundManager();
		Shop playerShop = engine.getPlayerShop();
		Shop rivalShop = engine.getRivalShop();
		List<CustomerSpawner.VisualCustomer> crowd = engine.getCrowd();

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
				engine.getNumCustomers(),
				engine.getMarketTotalCustomers(),
				engine.getMarketTotalPairs(),
				SCREEN_W,
				engine.getCatalogItems(),
				engine::getVendorCost
				);
	}

	// ============================================================
	// Sprite helpers
	// ============================================================

	/**
	 * Builds a flat array of individual customer sprites from the humanoid sheet.
	 *
	 * This is manual slicing because the sheet has visible padding/gaps between characters,
	 * so TextureRegion.split(...) is less reliable here than explicit coordinates.
	 */
	private TextureRegion[] buildCustomerSprites(Texture sheet) {
		if (sheet == null) return new TextureRegion[0];

		TextureRegion[] out = new TextureRegion[CUSTOMER_SHEET_COLS * CUSTOMER_SHEET_ROWS];
		int index = 0;

		for (int row = 0; row < CUSTOMER_SHEET_ROWS; row++) {
			for (int col = 0; col < CUSTOMER_SHEET_COLS; col++) {
				int x = CUSTOMER_MARGIN_X + col * (CUSTOMER_FRAME_W + CUSTOMER_SPACING_X);
				int y = CUSTOMER_MARGIN_Y + row * (CUSTOMER_FRAME_H + CUSTOMER_SPACING_Y);

				out[index++] = new TextureRegion(
						sheet,
						x,
						y,
						CUSTOMER_FRAME_W,
						CUSTOMER_FRAME_H
						);
			}
		}

		return out;
	}

	/**
	 * Returns the customer's assigned sprite.
	 *
	 * This assumes VisualCustomer has a public int field named spriteIndex.
	 * That field should be assigned once when the customer is created,
	 * so the same customer keeps the same appearance for the whole SELL phase.
	 */
	private TextureRegion getCustomerSpriteFor(CustomerSpawner.VisualCustomer vc) {
		if (vc == null) return null;
		if (customerSprites == null || customerSprites.length == 0) return null;

		int idx = vc.spriteIndex;

		if (idx < 0 || idx >= customerSprites.length) {
			idx = 0;
		}

		return customerSprites[idx];
	}

	// ============================================================
	// Screen lifecycle
	// ============================================================

	@Override
	public void resize(int width, int height) {
		if (width <= 0 || height <= 0) return;
	}

	@Override public void pause() {}
	@Override public void resume() {}
	@Override public void hide() {}

	@Override
	public void dispose() {
		if (shapes != null) shapes.dispose();
		if (batch != null) batch.dispose();
		if (fontMain != null) fontMain.dispose();
		if (fontSmall != null) fontSmall.dispose();
		if (fontStats != null) fontStats.dispose();

		// Dispose the loaded sprite sheet texture.
		if (customerSheet != null) customerSheet.dispose();
	}
}