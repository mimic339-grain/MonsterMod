package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PlayerTransformC2SPacket {
    private final boolean transform;
    private final ResourceLocation identityId;
    private final String requestedAnimation;
    private final Map<String, Boolean> customFlags;

    public PlayerTransformC2SPacket(boolean transform,
                                    ResourceLocation identityId,
                                    String requestedAnimation,
                                    Map<String, Boolean> customFlags) {
        this.transform = transform;
        this.identityId = identityId;
        this.requestedAnimation = requestedAnimation;
        this.customFlags = customFlags != null ? customFlags : new HashMap<>();
    }

    public PlayerTransformC2SPacket(FriendlyByteBuf buf) {
        this.transform = buf.readBoolean();
        this.identityId = buf.readNullable(FriendlyByteBuf::readResourceLocation);
        this.requestedAnimation = buf.readBoolean() ? buf.readUtf(32767) : null;

        int flagCount = buf.readInt();
        this.customFlags = new HashMap<>();
        for (int i = 0; i < flagCount; i++) {
            String key = buf.readUtf();
            boolean value = buf.readBoolean();
            customFlags.put(key, value);
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(transform);
        buf.writeNullable(identityId, FriendlyByteBuf::writeResourceLocation);

        if (requestedAnimation != null) {
            buf.writeBoolean(true);
            buf.writeUtf(requestedAnimation);
        } else {
            buf.writeBoolean(false);
        }

        buf.writeInt(customFlags.size());
        for (Map.Entry<String, Boolean> entry : customFlags.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeBoolean(entry.getValue());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                MimicEntity.MimicAnimationState currentState = transformation.getAnimationState(identityId);
                int currentTick = transformation.getAnimationTick(identityId);

                // 同期が必要か判定
                boolean shouldSync = transformation.shouldSync(identityId, transform, currentState, currentTick);
                if (!shouldSync) {
                    MonsterMod.getLogger().trace("[PlayerTransformC2SPacket.handle] Sync skipped");
                    return;
                }

                transformation.markSynced(identityId, transform, currentState);
                transformation.setTransformed(transform);

                if (transform && identityId != null) {
                    transformation.setTransformedMobId(identityId);

                    Entity entity = transformation.getTransformedEntity();
                    if (entity == null) {
                        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(identityId);
                        if (type != null) {
                            entity = type.create(player.level());
                            if (entity != null) {
                                entity.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                                player.level().addFreshEntity(entity);
                                transformation.setTransformedEntity(entity);
                            }
                        }
                    }

                    PlayerTransformation.MonsterState state = transformation.getMonsterState(identityId);
                    if (state == null) state = new PlayerTransformation.MonsterState();

                    state.animationState = requestedAnimation != null ? requestedAnimation : "IDLE";
                    // ★ tick はサーバー側で維持し、リセットしない

                    state.customFlags.clear();
                    state.customFlags.putAll(customFlags);
                    transformation.setMonsterState(identityId, state);

                    PlayerTransformationProvider.sendToAllPlayers(
                            new S2CTransformSyncPacket(
                                    player.getUUID(),
                                    transformation.getTransformedMobId(),
                                    transformation.getAnimationState(transformation.getTransformedMobId()).name(),
                                    transformation.getAnimationTick(transformation.getTransformedMobId()),
                                    state.customFlags
                            )
                    );
                } else {
                    Entity entity = transformation.getTransformedEntity();
                    if (entity != null) {
                        entity.remove(Entity.RemovalReason.DISCARDED);
                        transformation.setTransformedEntity(null);
                    }
                    if (transformation.getTransformedMobId() != null) {
                        transformation.setMonsterState(transformation.getTransformedMobId(), null);
                    }
                    transformation.setTransformedMobId(null);

                    PlayerTransformationProvider.sendToAllPlayers(
                            new S2CTransformSyncPacket(
                                    player.getUUID(),
                                    null,
                                    null,
                                    0,
                                    new HashMap<>()
                            )
                    );
                }
            });
        });

        context.setPacketHandled(true);
    }

    public boolean isTransform() { return transform; }
    public ResourceLocation getIdentityId() { return identityId; }
    public String getRequestedAnimation() { return requestedAnimation; }
    public Map<String, Boolean> getCustomFlags() { return customFlags; }
}
