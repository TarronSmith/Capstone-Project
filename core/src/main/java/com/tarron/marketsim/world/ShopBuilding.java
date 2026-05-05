package com.tarron.marketsim.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * ShopBuilding
 *
 * Responsibilities:
 * - Builds a tile-based shop house using the Pixel Crawler building assets.
 * - Anchors the building to a tile coordinate from TileMap.
 * - Layers wall, roof, door, and window regions into one composed structure.
 */
public class ShopBuilding {

	// ============================================================
	// Building footprint
	// ============================================================

	/*
	 * House footprint in rendered tile units.
	 *
	 * Layout:
	 * - width  = 5 tiles
	 * - height = 4 tiles
	 *
	 * Visual plan:
	 * row 3 -> roof
	 * row 2 -> roof
	 * row 1 -> wall with door / windows
	 * row 0 -> wall base
	 */
	private static final int WIDTH_TILES = 5;
	private static final int HEIGHT_TILES = 4;

	private static final int TILE_SIZE = TileMap.RENDER_TILE_SIZE;

	// ============================================================
	// Asset paths
	// ============================================================

	private static final String WALLS_PATH =
			"Pixel Crawler - Free Pack/Environment/Structures/Buildings/Walls.png";

	private static final String ROOFS_PATH =
			"Pixel Crawler - Free Pack/Environment/Structures/Buildings/Roofs.png";

	private static final String PROPS_PATH =
			"Pixel Crawler - Free Pack/Environment/Structures/Buildings/Props.png";

	// ============================================================
	// Anchor
	// ============================================================

	/*
	 * Bottom-left tile of the building.
	 */
	private final int tileX;
	private final int tileY;

	// ============================================================
	// Textures
	// ============================================================

	private Texture wallsTexture;
	private Texture roofsTexture;
	private Texture propsTexture;

	// ============================================================
	// Regions
	// ============================================================

	/*
	 * Base building pieces
	 */
	private TextureRegion wallRegion;
	private TextureRegion roofRegion;

	/*
	 * Overlay props
	 */
	private TextureRegion doorRegion;
	private TextureRegion windowLeftRegion;
	private TextureRegion windowRightRegion;

	// ============================================================
	// Construction
	// ============================================================

	public ShopBuilding(int tileX, int tileY) {
		this.tileX = tileX;
		this.tileY = tileY;

		loadTextures();
		buildRegions();
	}

	// ============================================================
	// Asset loading
	// ============================================================

	private void loadTextures() {
		wallsTexture = new Texture(WALLS_PATH);
		roofsTexture = new Texture(ROOFS_PATH);
		propsTexture = new Texture(PROPS_PATH);
	}

	/**
	 * Builds the regions used by this house.
	 *
	 * All tiles in these sheets are based on a 16x16 source grid.
	 *
	 * The wall/roof tiles are selected from the tile grid.
	 * The door and windows are extracted manually as larger prop regions.
	 */
	private void buildRegions() {
		TextureRegion[][] wallGrid = TextureRegion.split(wallsTexture, 16, 16);
		TextureRegion[][] roofGrid = TextureRegion.split(roofsTexture, 16, 16);

		/*
		 * Wall choice:
		 * Uses the plaster wall section from the middle set of the Walls sheet.
		 */
		wallRegion = wallGrid[0][8];

		/*
		 * Roof choice:
		 * Uses a regular brown roof shingle tile from Roofs.png.
		 */
		roofRegion = roofGrid[1][1];

		/*
		 * Prop overlays from Props.png
		 */
		doorRegion = new TextureRegion(propsTexture, 8 * 16, 0 * 16, 2 * 16, 2 * 16);

		windowLeftRegion = new TextureRegion(propsTexture, 2 * 16, 6 * 16, 2 * 16, 2 * 16);
		windowRightRegion = new TextureRegion(propsTexture, 8 * 16, 6 * 16, 2 * 16, 2 * 16);
	}

	// ============================================================
	// Rendering
	// ============================================================

	public void render(SpriteBatch batch) {
		if (batch == null) return;

		drawBaseHouse(batch);
		drawOverlays(batch);
	}

	/**
	 * Draws the repeating wall/roof structure.
	 */
	private void drawBaseHouse(SpriteBatch batch) {
		for (int localY = 0; localY < HEIGHT_TILES; localY++) {
			for (int localX = 0; localX < WIDTH_TILES; localX++) {
				TextureRegion region = (localY >= 2) ? roofRegion : wallRegion;

				float worldX = worldXFor(localX);
				float worldY = worldYFor(localY);

				batch.draw(region, worldX, worldY, TILE_SIZE, TILE_SIZE);
			}
		}
	}

	/**
	 * Draws doors and windows on top of the base house tiles.
	 *
	 * Overlay positions:
	 * - door centered across 2 tiles on the front wall
	 * - left and right windows above the wall base
	 */
	private void drawOverlays(SpriteBatch batch) {
		/*
		 * Centered 2x2 tile door.
		 *
		 * Starts at local tile x = 2, y = 0 so the top of the door rises into
		 * the wall row above it.
		 */
		float doorX = worldXFor(2);
		float doorY = worldYFor(0);

		batch.draw(
				doorRegion,
				doorX,
				doorY,
				2 * TILE_SIZE,
				2 * TILE_SIZE
				);

		/*
		 * Two windows positioned on the front wall.
		 *
		 * These sit one row above the base so the house face does not look empty.
		 */
		float leftWindowX = worldXFor(0);
		float leftWindowY = worldYFor(0);

		float rightWindowX = worldXFor(3);
		float rightWindowY = worldYFor(0);

		batch.draw(
				windowLeftRegion,
				leftWindowX,
				leftWindowY,
				2 * TILE_SIZE,
				2 * TILE_SIZE
				);

		batch.draw(
				windowRightRegion,
				rightWindowX,
				rightWindowY,
				2 * TILE_SIZE,
				2 * TILE_SIZE
				);
	}

	// ============================================================
	// Entrance helpers
	// ============================================================

	/**
	 * Entrance tile is the tile directly in front of the centered door.
	 */
	public int getEntranceTileX() {
		return tileX + 2;
	}

	public int getEntranceTileY() {
		return tileY - 1;
	}

	// ============================================================
	// World helpers
	// ============================================================

	private float worldXFor(int localTileX) {
		return (tileX + localTileX) * TILE_SIZE;
	}

	private float worldYFor(int localTileY) {
		return (tileY + localTileY) * TILE_SIZE;
	}

	// ============================================================
	// Cleanup
	// ============================================================

	public void dispose() {
		if (wallsTexture != null) wallsTexture.dispose();
		if (roofsTexture != null) roofsTexture.dispose();
		if (propsTexture != null) propsTexture.dispose();
	}
}