package net.mimic.monstermod.identity;

import net.mimic.monstermod.entity.custom.MimicEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PlayerEntityCache {
    private static final Map<UUID, MimicEntity> cache = new ConcurrentHashMap<>();

    public static MimicEntity getOrCreate(UUID playerId, Supplier<MimicEntity> factory) {
        return cache.computeIfAbsent(playerId, id -> factory.get());
    }
}
