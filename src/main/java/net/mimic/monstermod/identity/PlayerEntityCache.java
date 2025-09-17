package net.mimic.monstermod.identity;

import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PlayerEntityCache {
    private static final Map<UUID, MimicEntity> cache = new ConcurrentHashMap<>();
    private static final Map<UUID, ClientMimicEntity> clientCache = new ConcurrentHashMap<>();

    public static MimicEntity getOrCreate(UUID playerId, Supplier<MimicEntity> factory) {
        return cache.computeIfAbsent(playerId, id -> factory.get());
    }
    // ClientMimicEntity 用
    public static ClientMimicEntity getOrCreateClient(UUID playerId, Supplier<ClientMimicEntity> supplier) {
        return clientCache.computeIfAbsent(playerId, id -> supplier.get());
    }
}
