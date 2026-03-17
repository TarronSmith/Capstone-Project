package com.tarron.marketsim;

import com.badlogic.gdx.Game;
import com.tarron.marketsim.screen.FirstScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class MarketRivalGame extends Game {
    @Override
    public void create() {
        setScreen(new FirstScreen(this));
    }
}