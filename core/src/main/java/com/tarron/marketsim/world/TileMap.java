package com.tarron.marketsim.world;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * TileMap
 *
 * Responsibilities:
 * - Stores the world as a tile grid.
 * - Loads tile art from a floor tileset.
 * - Builds the initial outdoor layout used by the market scene.
 * - Renders the map.
 * - Exposes helper methods for tile/world conversion and walkability checks.
 *
 * Notes:
 * - Ground/path tiles are stored in the tile grid.
 * - Vegetation is rendered as a separate decoration layer on top of the ground.
 * - Vegetation in this version is manually cropped from the sheet instead of
 *   being treated as strict 16x16 tiles.
 */
public class TileMap {

	// ============================================================
	// Tile sizing
	// ============================================================

	public static final int SOURCE_TILE_SIZE = 16;
	public static final int RENDER_TILE_SIZE = 32;

	/*
	 * All vegetation is rendered using the same pixel scale.
	 *
	 * This keeps pixel size visually consistent, even when crops have
	 * different dimensions like 16x16, 16x32, 32x32, etc.
	 */
	private static final float DECOR_SCALE = 2f;

	// ============================================================
	// Map size
	// ============================================================

	public static final int MAP_WIDTH = 25;
	public static final int MAP_HEIGHT = 19;

	// ============================================================
	// Tile IDs
	// ============================================================

	public static final int TILE_GRASS = 0;
	public static final int TILE_DIRT_PATH = 1;
	public static final int TILE_STONE_PATH = 2;
	public static final int TILE_BRICK_FLOOR = 3;

	// ============================================================
	// Asset paths
	// ============================================================

	private static final String FLOORS_TILESET_PATH =
			"Pixel Crawler - Free Pack/Environment/Tilesets/Floors_Tiles.png";

	private static final String VEGETATION_TILESET_PATH =
			"Pixel Crawler - Free Pack/Environment/Props/Static/Vegetation.png";

	// ============================================================
	// House anchor tiles
	// ============================================================

	private final int playerHouseTileX = 3;
	private final int playerHouseTileY = 10;

	private final int rivalHouseTileX = 17;
	private final int rivalHouseTileY = 10;

	private final int playerEntranceTileX = 5;
	private final int playerEntranceTileY = 8;

	private final int rivalEntranceTileX = 19;
	private final int rivalEntranceTileY = 8;

	// ============================================================
	// Tile storage
	// ============================================================

	private final int[][] tiles;

	// ============================================================
	// Rendering assets
	// ============================================================

	private Texture floorsTexture;
	private Texture vegetationTexture;

	private TextureRegion grassRegion;
	private TextureRegion dirtPathRegion;
	private TextureRegion stonePathRegion;
	private TextureRegion brickFloorRegion;

	// ============================================================
	// Vegetation regions
	// ============================================================

	/*
	 * Manually cropped vegetation pieces.
	 *
	 * Not all of these are 16x16 tiles because the sheet contains.
	 */
	private TextureRegion bushA;
	private TextureRegion bushB;
	private TextureRegion flowerPinkA;
	private TextureRegion flowerPinkB;
	private TextureRegion tallPlantA;
	private TextureRegion tallPlantB;
	private TextureRegion weedsA;
	private TextureRegion weedsB;
	private TextureRegion stickA;
	private TextureRegion stickB;

	// ============================================================
	// Decoration layer
	// ============================================================

	private static class Decoration {
		int tileX;
		int tileY;
		TextureRegion region;
		float offsetX;
		float offsetY;
		float drawWidth;
		float drawHeight;

		Decoration(int tileX, int tileY, TextureRegion region,
				float offsetX, float offsetY,
				float drawWidth, float drawHeight) {
			this.tileX = tileX;
			this.tileY = tileY;
			this.region = region;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.drawWidth = drawWidth;
			this.drawHeight = drawHeight;
		}
	}

	private final List<Decoration> decorations = new ArrayList<>();

	// ============================================================
	// Construction
	// ============================================================

	public TileMap() {
		tiles = new int[MAP_HEIGHT][MAP_WIDTH];

		loadTilesets();
		buildDefaultMap();
		buildDefaultVegetation();
	}

	// ============================================================
	// Tileset loading
	// ============================================================

	private void loadTilesets() {
		floorsTexture = new Texture(FLOORS_TILESET_PATH);
		vegetationTexture = new Texture(VEGETATION_TILESET_PATH);

		/*
		 * Keep pixel art crisp when scaling.
		 */
		floorsTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		vegetationTexture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

		TextureRegion[][] floorGrid = TextureRegion.split(
				floorsTexture,
				SOURCE_TILE_SIZE,
				SOURCE_TILE_SIZE
				);

		grassRegion = floorGrid[10][1];
		dirtPathRegion = floorGrid[10][6];
		stonePathRegion = floorGrid[0][11];
		brickFloorRegion = floorGrid[0][16];

		loadVegetationRegions();
	}

	/**
	 * Manually crops vegetation pieces from the vegetation sheet.
	 *
	 * Coordinates are in source pixels.
	 */
	private void loadVegetationRegions() {
		bushA = new TextureRegion(vegetationTexture, 2, 5, 29, 27);
		bushB = new TextureRegion(vegetationTexture, 5, 32, 36, 32);

		flowerPinkA = new TextureRegion(vegetationTexture, 195, 161, 11, 31);
		flowerPinkB = new TextureRegion(vegetationTexture, 212, 165, 9, 27);

		tallPlantA = new TextureRegion(vegetationTexture, 112, 170, 16, 22);
		tallPlantB = new TextureRegion(vegetationTexture, 128, 166, 16, 26);

		weedsA = new TextureRegion(vegetationTexture, 80, 147, 31, 25);
		weedsB = new TextureRegion(vegetationTexture, 64, 144, 16, 16);

		stickA = new TextureRegion(vegetationTexture, 243, 2, 9, 11);
		stickB = new TextureRegion(vegetationTexture, 293, 3, 9, 11);
	}

	// ============================================================
	// Map building
	// ============================================================

	private void buildDefaultMap() {
		fill(TILE_GRASS);

		drawHorizontalLine(0, 24, 8, TILE_BRICK_FLOOR);
		drawHorizontalLine(0, 24, 7, TILE_BRICK_FLOOR);

		fillRect(5, 8, 1, 2, TILE_BRICK_FLOOR);
		fillRect(19, 8, 1, 2, TILE_BRICK_FLOOR);
	}

	/**
	 * Hand-places vegetation.
	 *
	 * Focus:
	 * - bushes
	 * - flowers
	 * - plants
	 *
	 * No randomness is used.
	 *
	 * Sizes are no longer hardcoded to 32x32 for everything.
	 * They are derived from the cropped source size using DECOR_SCALE.
	 */
	private void buildDefaultVegetation() {
		decorations.clear();

		/*
		 * Left side, behind / around player shop
		 */
		addDecorationScaled(0, 9, bushA);
		addDecorationScaled(9, 9, bushB);

		addDecorationScaled(0, 12, weedsA);
		addDecorationScaled(1, 12, weedsB);

		addDecorationScaled(8, 10, flowerPinkA);
		addDecorationScaled(9, 10, flowerPinkB);

		addDecorationScaled(15, 10, tallPlantA);
		addDecorationScaled(16, 10, tallPlantB);

		addDecorationScaled(3, 10, stickA);
		addDecorationScaled(16, 15, stickB);
	}

	// ============================================================
	// Rendering
	// ============================================================

	public void render(SpriteBatch batch) {
		if (batch == null) return;

		/*
		 * 1. Ground layer
		 */
		for (int y = 0; y < MAP_HEIGHT; y++) {
			for (int x = 0; x < MAP_WIDTH; x++) {
				TextureRegion region = regionForTileId(tiles[y][x]);
				if (region == null) continue;

				batch.draw(
						region,
						worldXForTile(x),
						worldYForTile(y),
						RENDER_TILE_SIZE,
						RENDER_TILE_SIZE
						);
			}
		}

		/*
		 * 2. Vegetation / decoration layer
		 */
		for (Decoration d : decorations) {
			if (d == null || d.region == null) continue;

			batch.draw(
					d.region,
					worldXForTile(d.tileX) + d.offsetX,
					worldYForTile(d.tileY) + d.offsetY,
					d.drawWidth,
					d.drawHeight
					);
		}
	}

	private TextureRegion regionForTileId(int tileId) {
		switch (tileId) {
		case TILE_DIRT_PATH:
			return dirtPathRegion;
		case TILE_STONE_PATH:
			return stonePathRegion;
		case TILE_BRICK_FLOOR:
			return brickFloorRegion;
		case TILE_GRASS:
		default:
			return grassRegion;
		}
	}

	// ============================================================
	// Decoration helpers
	// ============================================================

	/**
	 * Adds a decoration with explicit draw size.
	 *
	 * Old, but can be used for custom scaling.
	 */
	private void addDecoration(int tileX, int tileY, TextureRegion region,
			float offsetX, float offsetY,
			float drawWidth, float drawHeight) {
		if (region == null) return;

		decorations.add(new Decoration(
				tileX,
				tileY,
				region,
				offsetX,
				offsetY,
				drawWidth,
				drawHeight
				));
	}

	/**
	 * Adds a decoration using a consistent pixel-art scale.
	 */
	private void addDecorationScaled(int tileX, int tileY, TextureRegion region) {
		addDecorationScaled(tileX, tileY, region, 0f, 0f);
	}

	/**
	 * Adds a decoration using a consistent pixel-art scale with optional offsets.
	 */
	private void addDecorationScaled(int tileX, int tileY, TextureRegion region,
			float offsetX, float offsetY) {
		if (region == null) return;

		float drawWidth = region.getRegionWidth() * DECOR_SCALE;
		float drawHeight = region.getRegionHeight() * DECOR_SCALE;

		decorations.add(new Decoration(
				tileX,
				tileY,
				region,
				offsetX,
				offsetY,
				drawWidth,
				drawHeight
				));
	}

	// ============================================================
	// Map queries
	// ============================================================

	public int getTile(int tileX, int tileY) {
		if (!inBounds(tileX, tileY)) return TILE_GRASS;
		return tiles[tileY][tileX];
	}

	public void setTile(int tileX, int tileY, int tileId) {
		if (!inBounds(tileX, tileY)) return;
		tiles[tileY][tileX] = tileId;
	}

	public boolean inBounds(int tileX, int tileY) {
		return tileX >= 0 && tileX < MAP_WIDTH && tileY >= 0 && tileY < MAP_HEIGHT;
	}

	/**
	 * Walkability is intentionally strict in this version:
	 * only road/path tiles are walkable.
	 *
	 * Vegetation is visual only for now and does not block movement.
	 */
	public boolean isWalkable(int tileX, int tileY) {
		if (!inBounds(tileX, tileY)) return false;

		int tile = tiles[tileY][tileX];
		return tile == TILE_DIRT_PATH || tile == TILE_STONE_PATH || tile == TILE_BRICK_FLOOR;
	}

	// ============================================================
	// Tile/world conversion
	// ============================================================

	public float worldXForTile(int tileX) {
		return tileX * RENDER_TILE_SIZE;
	}

	public float worldYForTile(int tileY) {
		return tileY * RENDER_TILE_SIZE;
	}

	public float worldCenterXForTile(int tileX) {
		return worldXForTile(tileX) + (RENDER_TILE_SIZE / 2f);
	}

	public float worldCenterYForTile(int tileY) {
		return worldYForTile(tileY) + (RENDER_TILE_SIZE / 2f);
	}

	public int tileXForWorld(float worldX) {
		return (int) (worldX / RENDER_TILE_SIZE);
	}

	public int tileYForWorld(float worldY) {
		return (int) (worldY / RENDER_TILE_SIZE);
	}

	// ============================================================
	// House anchors / entrances
	// ============================================================

	public int getPlayerHouseTileX() {
		return playerHouseTileX;
	}

	public int getPlayerHouseTileY() {
		return playerHouseTileY;
	}

	public int getRivalHouseTileX() {
		return rivalHouseTileX;
	}

	public int getRivalHouseTileY() {
		return rivalHouseTileY;
	}

	public int getPlayerEntranceTileX() {
		return playerEntranceTileX;
	}

	public int getPlayerEntranceTileY() {
		return playerEntranceTileY;
	}

	public int getRivalEntranceTileX() {
		return rivalEntranceTileX;
	}

	public int getRivalEntranceTileY() {
		return rivalEntranceTileY;
	}

	public float getPlayerHouseWorldX() {
		return worldXForTile(playerHouseTileX);
	}

	public float getPlayerHouseWorldY() {
		return worldYForTile(playerHouseTileY);
	}

	public float getRivalHouseWorldX() {
		return worldXForTile(rivalHouseTileX);
	}

	public float getRivalHouseWorldY() {
		return worldYForTile(rivalHouseTileY);
	}

	public float getPlayerEntranceWorldCenterX() {
		return worldCenterXForTile(playerEntranceTileX);
	}

	public float getPlayerEntranceWorldCenterY() {
		return worldCenterYForTile(playerEntranceTileY);
	}

	public float getRivalEntranceWorldCenterX() {
		return worldCenterXForTile(rivalEntranceTileX);
	}

	public float getRivalEntranceWorldCenterY() {
		return worldCenterYForTile(rivalEntranceTileY);
	}

	// ============================================================
	// Small map-building utilities
	// ============================================================

	private void fill(int tileId) {
		for (int y = 0; y < MAP_HEIGHT; y++) {
			for (int x = 0; x < MAP_WIDTH; x++) {
				tiles[y][x] = tileId;
			}
		}
	}

	private void fillRect(int startX, int startY, int width, int height, int tileId) {
		for (int y = startY; y < startY + height; y++) {
			for (int x = startX; x < startX + width; x++) {
				setTile(x, y, tileId);
			}
		}
	}

	private void drawHorizontalLine(int startX, int endX, int y, int tileId) {
		int lo = Math.min(startX, endX);
		int hi = Math.max(startX, endX);

		for (int x = lo; x <= hi; x++) {
			setTile(x, y, tileId);
		}
	}

	private void drawVerticalLine(int x, int startY, int endY, int tileId) {
		int lo = Math.min(startY, endY);
		int hi = Math.max(startY, endY);

		for (int y = lo; y <= hi; y++) {
			setTile(x, y, tileId);
		}
	}

	// ============================================================
	// Cleanup
	// ============================================================

	public void dispose() {
		if (floorsTexture != null) floorsTexture.dispose();
		if (vegetationTexture != null) vegetationTexture.dispose();
	}
}