package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class S2CTransformSyncPacket {
    private final UUID playerUUID;
    private final ResourceLocation identityId;
    private final String animationStateName;
    private final int animationTick;
    private final Map<String, Boolean> customFlags;

    // ===== コンストラクタ =====
    public S2CTransformSyncPacket(UUID playerUUID,
                                  ResourceLocation identityId,
                                  String animationStateName,
                                  int animationTick,
                                  Map<String, Boolean> customFlags) {
        this.playerUUID = playerUUID;
        this.identityId = identityId;
        this.animationStateName = animationStateName != null
                ? animationStateName
                : MimicEntity.MimicAnimationState.IDLE.name();
        this.animationTick = animationTick;
        this.customFlags = customFlags != null ? customFlags : new HashMap<>();
    }

    // ===== デシリアライズ =====
    public S2CTransformSyncPacket(FriendlyByteBuf buf) {

        this.playerUUID = buf.readUUID();
        this.identityId = buf.readResourceLocation();
        this.animationStateName = buf.readUtf();
        this.animationTick = buf.readInt();

        int flagsCount = buf.readInt();
        this.customFlags = new HashMap<>();
        for (int i = 0; i < flagsCount; i++) {
            String key = buf.readUtf();
            boolean value = buf.readBoolean();
            customFlags.put(key, value);
        }
    }

    // ===== シリアライズ =====
    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(playerUUID);
        buf.writeResourceLocation(identityId);
        buf.writeUtf(animationStateName);
        buf.writeInt(animationTick);

        buf.writeInt(customFlags.size());
        for (Map.Entry<String, Boolean> entry : customFlags.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeBoolean(entry.getValue());
        }
    }

    // ===== ハンドラ =====
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Player target = mc.level.getPlayerByUUID(playerUUID);
            if (target == null) return;

            MimicEntity.MimicAnimationState animState;
            try {
                animState = MimicEntity.MimicAnimationState.valueOf(animationStateName);
            } catch (IllegalArgumentException e) {
                animState = MimicEntity.MimicAnimationState.IDLE;
            }

            ClientMimicEntity mimicEntity = ClientMimicEntity.getOrCreate(playerUUID);

            int clientTick = mimicEntity.getAnimationTick();
            if (Math.abs(animationTick - clientTick) > 2) {
                mimicEntity.updateFromServer(animState, animationTick, customFlags);
            } else {
                mimicEntity.updateFromServer(animState, clientTick, customFlags);
            }

            MimicEntity.MimicAnimationState finalAnimState = animState;
            target.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                transformation.setTransformed(true);
                transformation.setTransformedMobId(identityId);

                if (identityId != null) {
                    PlayerTransformation.MonsterState state = transformation.getMonsterState(identityId);
                    if (state == null) {
                        state = new PlayerTransformation.MonsterState();
                    }

                    state.animationState = finalAnimState.name();
                    state.animationTick = animationTick;
                    state.customFlags.clear();
                    state.customFlags.putAll(customFlags);
                    transformation.setMonsterState(identityId, state);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }




    // ===== Getter =====
    public UUID getPlayerUUID() { return playerUUID; }
    public ResourceLocation getIdentityId() { return identityId; }
    public String getAnimationStateName() { return animationStateName; }
    public int getAnimationTick() { return animationTick; }
    public Map<String, Boolean> getCustomFlags() { return customFlags; }
}
