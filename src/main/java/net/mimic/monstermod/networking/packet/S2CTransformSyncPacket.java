package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CTransformSyncPacket {
    private final boolean isTransformed;
    private final ResourceLocation transformedMobId;
    private final String animationStateName;
    private final int animationTick;

    public S2CTransformSyncPacket(boolean isTransformed, ResourceLocation transformedMobId, String animationStateName, int animationTick) {
        this.isTransformed = isTransformed;
        this.transformedMobId = transformedMobId;
        this.animationStateName = animationStateName != null ? animationStateName : MimicEntity.MimicAnimationState.IDLE.name();
        this.animationTick = animationTick;
    }

    public S2CTransformSyncPacket(FriendlyByteBuf buf) {
        this.isTransformed = buf.readBoolean();
        this.transformedMobId = buf.readNullable(FriendlyByteBuf::readResourceLocation);
        this.animationStateName = buf.readUtf();
        this.animationTick = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isTransformed);
        buf.writeNullable(transformedMobId, FriendlyByteBuf::writeResourceLocation);
        buf.writeUtf(animationStateName);
        buf.writeInt(animationTick);
    }

    public static S2CTransformSyncPacket decode(FriendlyByteBuf buf) {
        return new S2CTransformSyncPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            mc.player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                transformation.setTransformed(isTransformed);

                if (!isTransformed) {
                    // ===== 解除処理 =====
                    ResourceLocation currentMobId = transformation.getTransformedMobId();
                    if (currentMobId != null) {
                        transformation.setMonsterState(currentMobId, null);
                    }
                    transformation.setTransformedMobId(null);

                    // ClientMimicEntity を削除
                    ClientMimicEntity.remove(mc.player.getUUID());

                    System.out.printf("[S2CTransformSyncPacket] %s の変身解除\n", mc.player.getName().getString());
                } else {
                    // ===== 変身適用 =====
                    transformation.setTransformedMobId(transformedMobId);

                    if (transformedMobId != null && animationStateName != null) {
                        PlayerTransformation.MonsterState state = transformation.getMonsterState(transformedMobId);
                        if (state == null) {
                            state = new PlayerTransformation.MonsterState();
                        }
                        state.animationState = animationStateName;
                        state.animationTick = animationTick;
                        transformation.setMonsterState(transformedMobId, state);

                        System.out.printf("[S2CTransformSyncPacket] %s が %s に変身 | Animation=%s Tick=%d\n",
                                mc.player.getName().getString(),
                                transformedMobId.getPath(),
                                animationStateName,
                                animationTick
                        );
                    }
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
