package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class PlayerTransformC2SPacket {

    private final boolean transform;
    private final ResourceLocation identityId;

    public PlayerTransformC2SPacket(boolean transform, ResourceLocation identityId) {
        this.transform = transform;
        this.identityId = identityId;
    }

    public PlayerTransformC2SPacket(FriendlyByteBuf buf) {
        this.transform = buf.readBoolean();
        this.identityId = buf.readNullable(FriendlyByteBuf::readResourceLocation);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(transform);
        buf.writeNullable(identityId, FriendlyByteBuf::writeResourceLocation);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(it -> {
                if (!(it instanceof PlayerTransformation transformation)) return;

                transformation.setTransformed(transform);

                if (transform && identityId != null) {
                    transformation.setTransformedMobId(identityId);

                    // 既存 Entity を再利用
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

                    // MonsterState 初回生成
                    PlayerTransformation.MonsterState state = transformation.getMonsterState(identityId);
                    if (state == null) {
                        state = new PlayerTransformation.MonsterState();
                        state.animationState = "IDLE";
                        transformation.setMonsterState(identityId, state);
                    }
                } else {
                    // 変身解除
                    Entity entity = transformation.getTransformedEntity();
                    if (entity != null) {
                        entity.remove(Entity.RemovalReason.DISCARDED);
                        transformation.setTransformedEntity(null);
                    }
                    transformation.setTransformedMobId(null);
                }

                transformation.syncToClient(player);
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
