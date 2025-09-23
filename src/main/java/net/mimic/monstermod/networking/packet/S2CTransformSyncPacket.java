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
    private final String animationState;
    private final int animationTick;

    public S2CTransformSyncPacket(boolean isTransformed, ResourceLocation transformedMobId,
                                  String animationState, int animationTick) {
        this.isTransformed = isTransformed;
        this.transformedMobId = transformedMobId;
        this.animationState = animationState;
        this.animationTick = animationTick;
    }

    public S2CTransformSyncPacket(FriendlyByteBuf buf) {
        this.isTransformed = buf.readBoolean();
        this.transformedMobId = buf.readNullable(FriendlyByteBuf::readResourceLocation);
        this.animationState = buf.readUtf();
        this.animationTick = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(isTransformed);
        buf.writeNullable(transformedMobId, FriendlyByteBuf::writeResourceLocation);
        buf.writeUtf(animationState);
        buf.writeInt(animationTick);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;

            Minecraft.getInstance().player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION)
                    .ifPresent(transformation -> {
                        transformation.setTransformed(isTransformed);

                        if (isTransformed && transformedMobId != null) {
                            transformation.setTransformedMobId(transformedMobId);

                            // MonsterState に反映
                            PlayerTransformation.MonsterState state = transformation.getMonsterState(transformedMobId);
                            state.animationState = animationState;
                            state.animationTick = animationTick;
                            transformation.setMonsterState(transformedMobId, state);

                            // ClientMimicEntity に反映
                            ClientMimicEntity clientEntity = ClientMimicEntity.getOrCreate(Minecraft.getInstance().player.getUUID());
                            clientEntity.updateAnimationFromServer(
                                    MimicEntity.MimicAnimationState.valueOf(animationState),
                                    animationTick,
                                    Minecraft.getInstance().player
                            );
                        }
                    });
        });
        context.setPacketHandled(true);
        return true;
    }



    public String getAnimationState() { return animationState; }
    public int getAnimationTick() { return animationTick; }
    public ResourceLocation getTransformedMobId() { return transformedMobId; }
}
