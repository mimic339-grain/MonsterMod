package net.mimic.monstermod.capability;

import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PlayerTransformation {

    private boolean transformed = false;
    private ResourceLocation transformedMobId;
    private BaseMonsterEntity<?> transformedEntity;

    // サーバ側EntityのIDを保持（クライアント側で取得するため）
    private int transformedEntityId = -1;

    private final Map<ResourceLocation, MonsterState> states = new HashMap<>();

    @Nullable
    private BaseMonsterEntity<?> clientTransformedEntity;

    @Nullable
    public BaseMonsterEntity<?> getClientTransformedEntity() { return clientTransformedEntity; }

    public void setClientTransformedEntity(@Nullable BaseMonsterEntity<?> entity) { this.clientTransformedEntity = entity; }

    public boolean isTransformed() { return transformed; }
    public void setTransformed(boolean transformed) { this.transformed = transformed; }

    public ResourceLocation getTransformedMobId() { return transformedMobId; }
    public void setTransformedMobId(ResourceLocation id) { this.transformedMobId = id; }

    @Nullable
    public BaseMonsterEntity<?> getTransformedEntity() { return transformedEntity; }
    public void setTransformedEntity(@Nullable BaseMonsterEntity<?> entity) { this.transformedEntity = entity; }

    public int getTransformedEntityId() { return transformedEntityId; }
    public void setTransformedEntityId(int id) { this.transformedEntityId = id; }

    public MonsterState getMonsterState(ResourceLocation mobId) { return states.get(mobId); }
    public void setMonsterState(ResourceLocation mobId, MonsterState state) { states.put(mobId, state); }

    public void syncToClient(ServerPlayer player) {
        if (player != null) {
            ModMessages.sendToPlayer(
                    new S2CTransformSyncPacket(transformed, transformedMobId, transformedEntityId),
                    player
            );
        }
    }

    public static class MonsterState {
        public String animationState = "IDLE";
        private final Map<String, Boolean> flags = new HashMap<>();

        public boolean getFlag(String key) { return flags.getOrDefault(key, false); }
        public void setFlag(String key, boolean value) { flags.put(key, value); }
    }
}
