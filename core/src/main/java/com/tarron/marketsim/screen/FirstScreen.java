package com.tarron.marketsim.screen;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.tarron.marketsim.MarketRivalGame;
import com.tarron.marketsim.simulation.CustomerSpawner;
import com.tarron.marketsim.simulation.MarketEngine;
import com.tarron.marketsim.simulation.RoundManager;
import com.tarron.marketsim.ui.BuyMenuUI;
import com.tarron.marketsim.ui.RoundSummaryTabUI;
import com.tarron.marketsim.world.TileMap;

/**
 * FirstScreen
 *
 * Responsibilities:
 * - Main gameplay screen.
 * - Handles input mapping and delegates gameplay actions to MarketEngine.
 * - Renders the tile map, shop houses, customers, shop labels, and gameplay UI.
 * - Switches to GameOverScreen when the session ends.
 */
public class FirstScreen implements Screen {

	private static final int INITIAL_CUSTOMERS = 20;
	private static final int MIN_CUSTOMERS = 2;
	private static final int MAX_CUSTOMERS = 60;
	private static final double STARTING_CASH = 20.0;

	private static final float SCREEN_W = 800f;
	private static final float SCREEN_H = 600f;

	private static final float CUSTOMER_RADIUS = 10f;

	private static final String CUSTOMER_SHEET_PATH =
			"sprites/humanoid/Basic Humanoid Sprites 2x.png";

	private static final int CUSTOMER_SHEET_COLS = 5;
	private static final int CUSTOMER_SHEET_ROWS = 3;
	private static final int CUSTOMER_FRAME_W = 32;
	private static final int CUSTOMER_FRAME_H = 32;
	private static final int CUSTOMER_MARGIN_X = 2;
	private static final int CUSTOMER_MARGIN_Y = 2;
	private static final int CUSTOMER_SPACING_X = 4;
	private static final int CUSTOMER_SPACING_Y = 4;

	private static final String PLAYER_SHOP_PATH =
			"Pixel Crawler - Free Pack/Environment/Structures/Custom/House-1.png";

	private static final String RIVAL_SHOP_PATH =
			"Pixel Crawler - Free Pack/Environment/Structures/Custom/House-2.png";

	private static final float HOUSE_DRAW_W = 160f;
	private static final float HOUSE_DRAW_H = 160f;

	private float playerX;
	private float playerY;
	private float rivalX;
	private float rivalY;

	private float playerHouseDrawX;
	private float playerHouseDrawY;
	private float rivalHouseDrawX;
	private float rivalHouseDrawY;

	private OrthographicCamera camera;
	private SpriteBatch batch;

	private BitmapFont fontMain;
	private BitmapFont fontSmall;

	private Texture customerSheet;
	private TextureRegion[] customerSprites;

	private TileMap tileMap;
	private Texture playerHouseTexture;
	private Texture rivalHouseTexture;

	private MarketEngine engine;
	private BuyMenuUI buyMenuUI;
	private RoundSummaryTabUI roundSummaryTabUI;

	private final MarketRivalGame game;

	public FirstScreen(MarketRivalGame game) {
		this.game = game;
	}

	@Override
	public void show() {
		camera = new OrthographicCamera();
		camera.setToOrtho(false, (int) SCREEN_W, (int) SCREEN_H);

		batch = new SpriteBatch();

		fontMain = new BitmapFont();
		fontMain.getData().setScale(1.1f);

		fontSmall = new BitmapFont();
		fontSmall.getData().setScale(0.65f);

		buyMenuUI = new BuyMenuUI();
		roundSummaryTabUI = new RoundSummaryTabUI();

		tileMap = new TileMap();

		playerX = tileMap.getPlayerHouseWorldX();
		playerY = tileMap.getPlayerHouseWorldY();

		rivalX = tileMap.getRivalHouseWorldX();
		rivalY = tileMap.getRivalHouseWorldY();

		playerHouseDrawX = playerX;
		playerHouseDrawY = playerY - 8f;

		rivalHouseDrawX = rivalX;
		rivalHouseDrawY = rivalY - 8f;

		playerHouseTexture = new Texture(PLAYER_SHOP_PATH);
		rivalHouseTexture = new Texture(RIVAL_SHOP_PATH);

		playerHouseTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		rivalHouseTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

		customerSheet = new Texture(CUSTOMER_SHEET_PATH);
		customerSheet.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
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
		batch.setProjectionMatrix(camera.combined);

		handleGlobalKeys();

		if (engine.getPhase() == RoundManager.Phase.BUY) {
			handleMouseInput();
			handleBuyPhaseKeys();
		} else if (engine.getPhase() == RoundManager.Phase.RESULTS) {
			handleMouseInput();
			handleResultsKeys();
		} else {
			handleMouseInput();
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
		drawUI();
		batch.end();
	}

	private void handleGlobalKeys() {
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.R)) {
			engine.hardReset(true);
		}

		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.M)) {
			engine.rerollMarketAndReset();
		}
	}

	private void handleMouseInput() {
		if (!Gdx.input.justTouched()) return;

		com.badlogic.gdx.math.Vector3 mouse = new com.badlogic.gdx.math.Vector3(
				Gdx.input.getX(),
				Gdx.input.getY(),
				0f
				);

		camera.unproject(mouse);

		if (roundSummaryTabUI != null) {
			roundSummaryTabUI.handleClick(mouse.x, mouse.y);
		}

		if (buyMenuUI != null && engine.getPhase() == RoundManager.Phase.BUY) {
			buyMenuUI.handleClick(mouse.x, mouse.y, engine);
		}
	}

	private void handleBuyPhaseKeys() {
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) {
			engine.incCustomersBy2();
		}
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
			engine.decCustomersBy2();
		}

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
		float playerEntranceX = tileMap.getPlayerEntranceWorldCenterX();
		float playerEntranceY = tileMap.getPlayerEntranceWorldCenterY();

		float rivalEntranceX = tileMap.getRivalEntranceWorldCenterX();
		float rivalEntranceY = tileMap.getRivalEntranceWorldCenterY();

		float leftSpawnX = -20f;
		float leftSpawnY = tileMap.worldCenterYForTile(8);

		float rightSpawnX = (TileMap.MAP_WIDTH * TileMap.RENDER_TILE_SIZE) + 20f;
		float rightSpawnY = tileMap.worldCenterYForTile(8);

		float spawnLaneMinY = tileMap.worldCenterYForTile(7);
		float spawnLaneMaxY = tileMap.worldCenterYForTile(8);

		float leftExitX = -40f;
		float leftExitY = tileMap.worldCenterYForTile(7);

		float rightExitX = (TileMap.MAP_WIDTH * TileMap.RENDER_TILE_SIZE) + 40f;
		float rightExitY = tileMap.worldCenterYForTile(7);

		engine.beginSellPhase(
				leftSpawnX,
				leftSpawnY,
				rightSpawnX,
				rightSpawnY,
				spawnLaneMinY,
				spawnLaneMaxY,
				playerEntranceX,
				playerEntranceY,
				rivalEntranceX,
				rivalEntranceY,
				leftExitX,
				leftExitY,
				rightExitX,
				rightExitY
				);
	}

	private void clearScreen() {
		Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	private void drawWorld() {
		List<CustomerSpawner.VisualCustomer> crowd = engine.getCrowd();

		batch.begin();

		if (tileMap != null) {
			tileMap.render(batch);
		}

		if (playerHouseTexture != null) {
			batch.draw(playerHouseTexture, playerHouseDrawX, playerHouseDrawY, HOUSE_DRAW_W, HOUSE_DRAW_H);
		}

		if (rivalHouseTexture != null) {
			batch.draw(rivalHouseTexture, rivalHouseDrawX, rivalHouseDrawY, HOUSE_DRAW_W, HOUSE_DRAW_H);
		}

		if (fontMain != null) {
			fontMain.draw(
					batch,
					"Player Shop",
					playerHouseDrawX + 18f,
					playerHouseDrawY + HOUSE_DRAW_H + 16f
					);

			fontMain.draw(
					batch,
					"Rival Shop",
					rivalHouseDrawX + 24f,
					rivalHouseDrawY + HOUSE_DRAW_H + 16f
					);
		}

		for (CustomerSpawner.VisualCustomer vc : crowd) {
			if (vc == null) continue;
			if (!vc.visible) continue;
			if (vc.state == CustomerSpawner.VisualCustomer.State.DONE) continue;

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

		batch.end();
	}

	private void drawUI() {
		if (roundSummaryTabUI != null) {
			roundSummaryTabUI.render(batch, fontSmall, engine);
		}

		if (buyMenuUI != null && engine.getPhase() == RoundManager.Phase.BUY) {
			buyMenuUI.render(batch, fontSmall, engine);
		}
	}

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

	private TextureRegion getCustomerSpriteFor(CustomerSpawner.VisualCustomer vc) {
		if (vc == null) return null;
		if (customerSprites == null || customerSprites.length == 0) return null;

		int idx = vc.spriteIndex;

		if (idx < 0 || idx >= customerSprites.length) {
			idx = 0;
		}

		return customerSprites[idx];
	}

	@Override
	public void resize(int width, int height) {
		if (width <= 0 || height <= 0) return;
	}

	@Override public void pause() {}
	@Override public void resume() {}
	@Override public void hide() {}

	@Override
	public void dispose() {
		if (batch != null) batch.dispose();
		if (fontMain != null) fontMain.dispose();
		if (fontSmall != null) fontSmall.dispose();
		if (customerSheet != null) customerSheet.dispose();
		if (playerHouseTexture != null) playerHouseTexture.dispose();
		if (rivalHouseTexture != null) rivalHouseTexture.dispose();
		if (tileMap != null) tileMap.dispose();
		if (buyMenuUI != null) buyMenuUI.dispose();
		if (roundSummaryTabUI != null) roundSummaryTabUI.dispose();
	}
}