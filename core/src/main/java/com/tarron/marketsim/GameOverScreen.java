package com.tarron.marketsim;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tarron.marketsim.simulation.RoundManager;

/**
 * GameOverScreen
 *
 * Responsibilities:
 * - End-of-run screen showing outcome and final cash totals.
 * - Provides a simple "Play Again" button and ENTER shortcut to restart.
 */
public class GameOverScreen implements Screen {

	// ============================================================
	// Inputs (final state from the completed run)
	// ============================================================

	private final MarketRivalGame game;
	private final RoundManager.Outcome outcome;
	private final double playerCash;
	private final double rivalCash;

	// ============================================================
	// Rendering
	// ============================================================

	private OrthographicCamera camera;
	private SpriteBatch batch;
	private ShapeRenderer shapes;
	private BitmapFont font;

	private final GlyphLayout layout = new GlyphLayout();

	// ============================================================
	// Layout (fixed 800x600 world coords)
	// ============================================================

	private static final float WORLD_W = 800f;
	private static final float WORLD_H = 600f;

	private final float btnW = 220f, btnH = 60f;
	private final float btnX = (WORLD_W - btnW) / 2f;
	private final float btnY = 160f;

	public GameOverScreen(
			MarketRivalGame game,
			RoundManager.Outcome outcome,
			double playerCash,
			double rivalCash
			) {
		this.game = game;
		this.outcome = outcome;
		this.playerCash = playerCash;
		this.rivalCash = rivalCash;
	}

	@Override
	public void show() {
		camera = new OrthographicCamera();
		camera.setToOrtho(false, WORLD_W, WORLD_H);

		batch = new SpriteBatch();
		shapes = new ShapeRenderer();

		font = new BitmapFont();
		font.getData().setScale(1.0f);
	}

	@Override
	public void render(float delta) {
		clearScreen();

		camera.update();
		shapes.setProjectionMatrix(camera.combined);
		batch.setProjectionMatrix(camera.combined);

		if (handleRestartInput()) return;

		drawButton();
		drawText();
	}

	// ============================================================
	// Input
	// ============================================================

	private boolean handleRestartInput() {
		if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
			restart();
			return true;
		}

		if (!Gdx.input.justTouched()) return false;

		float worldX = toWorldX(Gdx.input.getX());
		float worldY = toWorldY(Gdx.input.getY());

		if (isInside(worldX, worldY, btnX, btnY, btnW, btnH)) {
			restart();
			return true;
		}

		return false;
	}

	private float toWorldX(float screenX) {
		return screenX * (WORLD_W / Gdx.graphics.getWidth());
	}

	private float toWorldY(float screenY) {
		return (Gdx.graphics.getHeight() - screenY) * (WORLD_H / Gdx.graphics.getHeight());
	}

	private boolean isInside(float x, float y, float rx, float ry, float rw, float rh) {
		return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
	}

	// ============================================================
	// Drawing
	// ============================================================

	private void clearScreen() {
		Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	private void drawButton() {
		shapes.begin(ShapeRenderer.ShapeType.Filled);
		shapes.rect(btnX, btnY, btnW, btnH);
		shapes.end();
	}

	private void drawText() {
		batch.begin();

		String title = outcomeTitle(outcome);
		drawCentered(title, 460f);

		String score = String.format(
				"Final Cash  |  Player: $%.2f   Rival: $%.2f",
				playerCash, rivalCash
				);
		drawCentered(score, 410f);

		String btnText = "Play Again";
		layout.setText(font, btnText);
		font.draw(batch, btnText, btnX + (btnW - layout.width) / 2f, btnY + 38f);

		String hint = "(Press ENTER or click Play Again)";
		drawCentered(hint, 120f);

		batch.end();
	}

	private void drawCentered(String text, float y) {
		layout.setText(font, text);
		font.draw(batch, text, (WORLD_W - layout.width) / 2f, y);
	}

	private String outcomeTitle(RoundManager.Outcome o) {
		if (o == RoundManager.Outcome.WIN) return "YOU WIN";
		if (o == RoundManager.Outcome.LOSS) return "YOU LOSE";
		if (o == RoundManager.Outcome.TIE) return "TIE";
		return "GAME OVER";
	}

	// ============================================================
	// Navigation
	// ============================================================

	private void restart() {
		game.setScreen(new FirstScreen(game));
	}

	// ============================================================
	// Screen lifecycle
	// ============================================================

	@Override public void resize(int width, int height) {}
	@Override public void pause() {}
	@Override public void resume() {}
	@Override public void hide() {}

	@Override
	public void dispose() {
		if (batch != null) batch.dispose();
		if (shapes != null) shapes.dispose();
		if (font != null) font.dispose();
	}
}