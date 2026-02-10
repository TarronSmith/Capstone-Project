package com.tarron.marketsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.tarron.marketsim.model.DecisionLogic;
import com.tarron.marketsim.model.Item;
import com.tarron.marketsim.model.Shop;
import com.tarron.marketsim.simulation.CustomerSpawner;
import com.tarron.marketsim.simulation.MarketPlan;
import com.tarron.marketsim.simulation.RivalAI;
import com.tarron.marketsim.simulation.RoundManager;

public class FirstScreen implements Screen {

    // ---------------- DEMO SETTINGS ----------------
    private int numCustomers = 20; // must be even for perfect split
    private static final int MIN_CUSTOMERS = 2;
    private static final int MAX_CUSTOMERS = 60;
    // ----------------------------------------------

    // ---- Phase + round rules ----
    private static final double STARTING_CASH = 20.0;
    private static final double VENDOR_CHEAP_COST = 2.0;
    private static final double VENDOR_PREMIUM_COST = 5.0;

    private OrthographicCamera camera;
    private ShapeRenderer shapes;
    private SpriteBatch batch;

    private BitmapFont fontMain;
    private BitmapFont fontSmall;
    private BitmapFont fontStats;

    // Model objects
    private Shop playerShop;
    private Shop rivalShop;

    private DecisionLogic logic;

    private Item cheapLowQ;
    private Item priceyHighQ;

    private static final float CUSTOMER_RADIUS = 10f;

    // Shop positions on screen
    private float shopW = 220, shopH = 140;
    private float playerX = 80, playerY = 260;
    private float rivalX = 500, rivalY = 260;

    // Helper classes
    private MarketPlan marketPlan;
    private RivalAI rivalAI;
    private RoundManager roundManager;
    private CustomerSpawner spawner;

    // Visual customers
    private final List<CustomerSpawner.VisualCustomer> crowd = new ArrayList<>();

    // Cached market counts for current numCustomers (for display)
    private int marketCheapCount = 0;
    private int marketValueCount = 0;
    private int marketConspCount = 0;

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);

        shapes = new ShapeRenderer();
        batch = new SpriteBatch();

        fontMain = new BitmapFont();
        fontSmall = new BitmapFont();
        fontStats = new BitmapFont();
        fontStats.getData().setScale(0.75f);
        fontSmall.getData().setScale(0.65f);

        cheapLowQ = new Item("CheapItem", 3.0, 1);
        priceyHighQ = new Item("ExpensiveItem", 8.0, 5);
        logic = new DecisionLogic();

        playerShop = new Shop();
        playerShop.setName("Player Shop");

        rivalShop = new Shop();
        rivalShop.setName("Rival Shop");

        marketPlan = new MarketPlan(MAX_CUSTOMERS);
        rivalAI = new RivalAI(VENDOR_CHEAP_COST, VENDOR_PREMIUM_COST);
        roundManager = new RoundManager(STARTING_CASH, true);
        spawner = new CustomerSpawner();

        // Create market once at launch
        marketPlan.reroll();
        updateMarketCountsForCurrentSelection();

        // Hard reset into BUY phase (wipeKnowledge = true)
        hardReset(true);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        // R = reset (does NOT reroll market)
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.R)) {
            hardReset(true); // wipe knowledge
        }

        // M = new market (reroll market + wipe knowledge)
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.M)) {
            marketPlan.reroll();
            updateMarketCountsForCurrentSelection();
            hardReset(true);
        }

        // ---------------- BUY PHASE CONTROLS ----------------
        if (roundManager.getPhase() == RoundManager.Phase.BUY) {

            // Adjust customer count (keep even)
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) {
                numCustomers = Math.min(MAX_CUSTOMERS, numCustomers + 2);
                updateMarketCountsForCurrentSelection();
            }
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
                numCustomers = Math.max(MIN_CUSTOMERS, numCustomers - 2);
                updateMarketCountsForCurrentSelection();
            }

            // Buy cheap
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_1)) {
                if (roundManager.getPlayerCash() >= VENDOR_CHEAP_COST) {
                    roundManager.setPlayerCash(roundManager.getPlayerCash() - VENDOR_CHEAP_COST);
                    playerShop.addItems(Arrays.asList(cheapLowQ));
                }
            }

            // Buy premium
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_2)) {
                if (roundManager.getPlayerCash() >= VENDOR_PREMIUM_COST) {
                    roundManager.setPlayerCash(roundManager.getPlayerCash() - VENDOR_PREMIUM_COST);
                    playerShop.addItems(Arrays.asList(priceyHighQ));
                }
            }

            // Begin sell
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
                beginSellPhase();
            }
        }

        // ---------------- SELL PHASE UPDATE ----------------
        if (roundManager.getPhase() == RoundManager.Phase.SELL) {
            for (CustomerSpawner.VisualCustomer vc : crowd) {
                vc.update(delta);

                if (vc.justArrived()) {
                    // Desired (independent of stock)
                    Item desired = logic.iPick(vc.model, cheapLowQ, priceyHighQ);
                    if (desired != null) {
                        if (sameItem(desired, cheapLowQ)) roundManager.incDesiredCheap();
                        else if (sameItem(desired, priceyHighQ)) roundManager.incDesiredPremium();
                    }

                    // Actually buy (affected by stock)
                    vc.walletBefore = vc.model.getWallet();

                    Item bought = vc.targetShop.sell(vc.model, cheapLowQ, priceyHighQ, logic);
                    if (bought != null) {
                        if (sameItem(bought, cheapLowQ)) roundManager.incSoldCheap();
                        else if (sameItem(bought, priceyHighQ)) roundManager.incSoldPremium();
                    }

                    vc.walletAfter = vc.model.getWallet();
                    vc.spent = Math.max(0, vc.walletBefore - vc.walletAfter);

                    if (bought == null) {
                        vc.boughtName = null;
                        vc.purchaseResult = "bought nothing";
                    } else {
                        vc.boughtName = bought.getName();
                        vc.purchaseResult = "bought " + vc.boughtName;
                    }
                }
            }

            // When all arrived, move to RESULTS
            if (spawner.isSellPhaseOver(crowd)) {
                roundManager.setResultsPhase();
            }
        }

        // ---------------- RESULTS CONTROLS ----------------
        if (roundManager.getPhase() == RoundManager.Phase.RESULTS) {
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
                endResultsAndStartNextBuy();
            }
        }

        // Draw shops + customers
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(playerX, playerY, shopW, shopH);
        shapes.rect(rivalX, rivalY, shopW, shopH);
        for (CustomerSpawner.VisualCustomer vc : crowd) {
            shapes.circle(vc.x, vc.y, CUSTOMER_RADIUS);
        }
        shapes.end();

        // Text
        batch.begin();

        fontMain.draw(batch, playerShop.getName(), playerX, playerY + shopH + 20);
        fontMain.draw(batch, rivalShop.getName(), rivalX, rivalY + shopH + 20);

        drawCustomerLists();
        drawTopOverlay();

        batch.end();
    }

    private void beginSellPhase() {
        roundManager.beginSellPhase(playerShop, rivalShop);

        crowd.clear();

        // Same spawn parameters
        float startX = 380;
        float startY = 60;
        float spacing = 22;
        float paddingInside = 12f;

        crowd.addAll(spawner.spawnFairCustomersFromPlan(
                marketPlan,
                numCustomers,
                playerShop,
                rivalShop,
                startX,
                startY,
                spacing,
                playerX,
                playerY,
                shopW,
                shopH,
                rivalX,
                rivalY,
                CUSTOMER_RADIUS,
                paddingInside
        ));
    }

    private void endResultsAndStartNextBuy() {
        // Store last desired + reset cash + clear inventories per policy
        roundManager.startNextBuyPhase(playerShop, rivalShop);

        // Clear customers
        crowd.clear();

        // Rival stocks now, so BUY phase display shows real inventory
        RivalAI.Result res = rivalAI.stockForBuyPhase(
                rivalShop,
                roundManager.getRivalCash(),
                roundManager.getLastDesiredCheap(),
                roundManager.getLastDesiredPremium(),
                cheapLowQ,
                priceyHighQ
        );

        // IMPORTANT NOTE: one call updates rival cash + labels + ratios + plan
        roundManager.applyRivalAIResult(res);
    }

    private void hardReset(boolean wipeKnowledge) {
        crowd.clear();
        roundManager.hardReset(playerShop, rivalShop, wipeKnowledge);

        // Rival stocks immediately for BUY display
        RivalAI.Result res = rivalAI.stockForBuyPhase(
                rivalShop,
                roundManager.getRivalCash(),
                roundManager.getLastDesiredCheap(),
                roundManager.getLastDesiredPremium(),
                cheapLowQ,
                priceyHighQ
        );

        // IMPORTANT NOTE: one call updates rival cash + labels + ratios + plan
        roundManager.applyRivalAIResult(res);
    }

    private void updateMarketCountsForCurrentSelection() {
        MarketPlan.MarketCounts c = marketPlan.getCountsForCustomers(numCustomers);
        marketCheapCount = c.cheapCount;
        marketValueCount = c.valueCount;
        marketConspCount = c.conspCount;
    }

    private void drawCustomerLists() {
        GlyphLayout layout = new GlyphLayout();

        List<String> playerLines = new ArrayList<>();
        List<String> rivalLines = new ArrayList<>();

        for (int i = 0; i < crowd.size(); i++) {
            CustomerSpawner.VisualCustomer vc = crowd.get(i);

            String line;
            if (vc.purchaseResult == null) {
                line = (roundManager.getPhase() == RoundManager.Phase.BUY)
                        ? ("C" + i + ": idle...")
                        : ("C" + i + ": walking...");
            } else {
                String itemLabel = (vc.boughtName == null) ? "no buy" : vc.boughtName;
                line = String.format(
                        "C%d: Initial=$%.2f  Price=$%.2f  Final=$%.2f  (%s)",
                        i, vc.walletBefore, vc.spent, vc.walletAfter, itemLabel
                );
            }

            if (vc.targetShop == playerShop) playerLines.add(line);
            else rivalLines.add(line);
        }

        float listLeftPadding = 10f;
        float listTopOffset = 25f;
        int maxLines = 12;

        drawShopCustomerList(fontSmall, layout, playerX + listLeftPadding, playerY - listTopOffset, playerLines, maxLines);
        drawShopCustomerList(fontSmall, layout, rivalX + listLeftPadding, rivalY - listTopOffset, rivalLines, maxLines);
    }

    private void drawTopOverlay() {
        if (roundManager.getPhase() == RoundManager.Phase.BUY) {
            fontStats.draw(batch,
                    String.format("BUY PHASE  |  Player Cash: $%.2f   Rival Cash: $%.2f   (R = reset, M = new market)",
                            roundManager.getPlayerCash(), roundManager.getRivalCash()),
                    20, 590);

            fontSmall.draw(batch,
                    String.format("Controls: [1] Buy Cheap ($%.2f)   [2] Buy Premium ($%.2f)   [ENTER] Begin Sell   [UP/DOWN] Customers (%d)",
                            VENDOR_CHEAP_COST, VENDOR_PREMIUM_COST, numCustomers),
                    20, 570);

            fontSmall.draw(batch,
                    String.format("SESSION Market (fixed): Cheap=%d  Value=%d  Conspicuous=%d",
                            marketCheapCount, marketValueCount, marketConspCount),
                    20, 552);

            fontSmall.draw(batch,
                    String.format("Player Inventory: Cheap=%d  Premium=%d",
                            playerShop.getQuantity(cheapLowQ),
                            playerShop.getQuantity(priceyHighQ)),
                    20, 532);

            fontSmall.draw(batch,
                    String.format("Rival  Inventory: Cheap=%d  Premium=%d",
                            rivalShop.getQuantity(cheapLowQ),
                            rivalShop.getQuantity(priceyHighQ)),
                    20, 514);

            String desiredLabel = (roundManager.getLastDesiredCheap() + roundManager.getLastDesiredPremium() == 0)
                    ? "(none yet)"
                    : String.format("Cheap=%d  Premium=%d",
                            roundManager.getLastDesiredCheap(),
                            roundManager.getLastDesiredPremium());

            fontSmall.draw(batch,
                    String.format("Last Round DESIRED: %s | Rival: %s demandPrem=%.2f stockPrem=%.2f plan(C=%d,P=%d)",
                            desiredLabel,
                            roundManager.getRivalStrategyLabel(),
                            roundManager.getLastDemandPremiumRatio(),
                            roundManager.getLastStockPremiumRatio(),
                            roundManager.getLastPlannedCheap(),
                            roundManager.getLastPlannedPremium()),
                    20, 496);

        } else {
            float screenW = 800f;

            String statsLine1 = "Player sold=" + playerShop.getItemsSoldThisTurn()
                    + "  revenue=$" + String.format("%.2f", playerShop.getRevenueThisTurn());
            String statsLine2 = "Rival  sold=" + rivalShop.getItemsSoldThisTurn()
                    + "  revenue=$" + String.format("%.2f", rivalShop.getRevenueThisTurn());

            GlyphLayout statsLayout = new GlyphLayout();

            statsLayout.setText(fontStats, statsLine1);
            fontStats.draw(batch, statsLine1, (screenW - statsLayout.width) / 2f, 590);

            statsLayout.setText(fontStats, statsLine2);
            fontStats.draw(batch, statsLine2, (screenW - statsLayout.width) / 2f, 568);

            fontSmall.draw(batch,
                    String.format("Market DESIRED: Cheap=%d  Premium=%d  |  ACTUAL SOLD: Cheap=%d  Premium=%d",
                            roundManager.getCurrentDesiredCheap(), roundManager.getCurrentDesiredPremium(),
                            roundManager.getCurrentSoldCheap(), roundManager.getCurrentSoldPremium()),
                    20, 548);

            if (roundManager.getPhase() == RoundManager.Phase.SELL) {
                fontSmall.draw(batch, "SELL running... (wait for all customers to arrive)", 20, 530);
            } else {
                fontSmall.draw(batch, "SELL COMPLETE. Press ENTER for next BUY phase. (R = reset, M = new market)", 20, 530);
            }
        }
    }

    private void drawShopCustomerList(BitmapFont font, GlyphLayout layout,
                                      float leftX, float topY,
                                      List<String> lines,
                                      int maxLinesToShow) {

        float lineStep = 14f;
        int count = Math.min(lines.size(), maxLinesToShow);

        for (int i = 0; i < count; i++) {
            font.draw(batch, lines.get(i), leftX, topY - i * lineStep);
        }
    }

    // This compares by name as a fallback
    private boolean sameItem(Item a, Item b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.getName() != null && a.getName().equals(b.getName());
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
        shapes.dispose();
        batch.dispose();
        fontMain.dispose();
        fontSmall.dispose();
        fontStats.dispose();
    }
}
