package com.tarron.marketsim.simulation;

import java.util.ArrayList;
import java.util.List;

import com.tarron.marketsim.model.Customer;
import com.tarron.marketsim.model.Shop;

/**
 * Spawns and updates visual customers. Keeps spawning logic out of FirstScreen.
 */
public class CustomerSpawner {

    public static class VisualCustomer {
        public Customer model;
        public Shop targetShop;

        public double walletBefore = 0;
        public double walletAfter = 0;
        public double spent = 0;
        public String boughtName = null;

        public float x, y;
        public float targetX, targetY;

        public boolean arrived = false;
        private boolean arrivalConsumed = false;

        public String purchaseResult = null;

        public VisualCustomer(Customer model, float x, float y,
                              float targetX, float targetY,
                              Shop targetShop) {
            this.model = model;
            this.x = x;
            this.y = y;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetShop = targetShop;
        }

        public void update(float delta) {
            if (arrived) return;

            float speed = 140f;
            float dx = targetX - x;
            float dy = targetY - y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            float step = speed * delta;

            if (dist <= step) {
                x = targetX;
                y = targetY;
                arrived = true;
                return;
            }

            float nx = dx / dist;
            float ny = dy / dist;

            x += nx * speed * delta;
            y += ny * speed * delta;
        }

        public boolean justArrived() {
            if (arrived && !arrivalConsumed) {
                arrivalConsumed = true;
                return true;
            }
            return false;
        }
    }

    public List<VisualCustomer> spawnFairCustomersFromPlan(
            MarketPlan marketPlan,
            int totalCustomers,
            Shop playerShop,
            Shop rivalShop,
            float startX,
            float startY,
            float spacing,
            float playerX,
            float playerY,
            float shopW,
            float shopH,
            float rivalX,
            float rivalY,
            float customerRadius,
            float paddingInside
    ) {
        marketPlan.ensureInitialized();

        int evenCustomers = (totalCustomers % 2 == 0) ? totalCustomers : totalCustomers + 1;
        int pairsNeeded = evenCustomers / 2;

        List<String> pairTypes = marketPlan.getPairTypesForCustomers(evenCustomers);

        List<VisualCustomer> crowd = new ArrayList<>();
        int visualIndex = 0;

        for (int i = 0; i < pairsNeeded; i++) {
            String type = pairTypes.get(i);

            Customer cPlayer = marketPlan.makeCustomerFromType(type);
            Customer cRival = marketPlan.makeCustomerFromType(type);

            // Player
            crowd.add(new VisualCustomer(
                    cPlayer,
                    startX,
                    startY + (visualIndex * spacing),
                    (playerX + shopW / 2f),
                    (playerY + customerRadius + paddingInside),
                    playerShop
            ));
            visualIndex++;

            // Rival
            crowd.add(new VisualCustomer(
                    cRival,
                    startX,
                    startY + (visualIndex * spacing),
                    (rivalX + shopW / 2f),
                    (rivalY + customerRadius + paddingInside),
                    rivalShop
            ));
            visualIndex++;
        }

        return crowd;
    }

    public boolean isSellPhaseOver(List<VisualCustomer> crowd) {
        if (crowd == null || crowd.isEmpty()) return true;
        for (VisualCustomer vc : crowd) {
            if (!vc.arrived) return false;
        }
        return true;
    }
}
