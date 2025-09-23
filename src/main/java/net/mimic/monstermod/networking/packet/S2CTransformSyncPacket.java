package net.mimic.monstermod.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.capability.PlayerTransformation;

import java.util.function.Supplier;

public class S2CTransformSyncPacket {
    private final boolean isTransformed;
    private final ResourceLocation transformedMobId;
    private final String animationState;

    public S2CTransformSyncPacket(boolean isTransformed, ResourceLocation transformedMobId, String animationState) {
        this.isTransformed = isTransformed;
        this.transformedMobId = transformedMobId;
        this.animationState = animationState;
    }

    public S2CTransformSyncPacket(FriendlyByteBuf buf) {
        this.isTransformed = buf.readBoolean();
        this.transformedMobId = buf.readNullable(FriendlyByteBuf::readResourceLocation);
        this.animationState = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isTransformed);
        buf.writeNullable(this.transformedMobId, FriendlyByteBuf::writeResourceLocation);
        buf.writeUtf(this.animationState);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                    transformation.setTransformed(this.isTransformed);
                    transformation.setTransformedMobId(this.transformedMobId);

                    // ===== デバッグログ追加 =====
                    System.out.println("[S2CTransformSyncPacket] handle | Player=" +
                            Minecraft.getInstance().player.getName().getString() +
                            " MobId=" + this.transformedMobId +
                            " AnimationStateReceived=" + this.animationState);

                    if (this.transformedMobId != null && this.animationState != null) {
                        PlayerTransformation.MonsterState state = new PlayerTransformation.MonsterState();
                        state.animationState = this.animationState;
                        transformation.setMonsterState(this.transformedMobId, state);
                    }
                });
            }
        });
        context.setPacketHandled(true);
        return true;
    }


}
