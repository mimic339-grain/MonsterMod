package com.mimic.monstermod.Math;

import java.util.HashMap;
import java.util.Map;

public class Cooldown {

    private final Map<String, Long> lastUseMap = new HashMap<>();
    private final long cooldownMillis;

    public Cooldown(long cooldownMillis) {
        this.cooldownMillis = cooldownMillis;
    }

    public boolean canUse(String id) {
        long now = System.currentTimeMillis();
        if (!lastUseMap.containsKey(id)) return true;
        return now - lastUseMap.get(id) >= cooldownMillis;
    }

    public void use(String id) {
        lastUseMap.put(id, System.currentTimeMillis());
    }

    public long getRemaining(String id) {
        if (!lastUseMap.containsKey(id)) return 0;
        long remaining = cooldownMillis - (System.currentTimeMillis() - lastUseMap.get(id));
        return Math.max(remaining, 0);
    }
}
