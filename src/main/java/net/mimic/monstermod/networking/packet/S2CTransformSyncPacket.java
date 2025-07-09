package net.mimic.monstermod.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.event.ClientForgeEvents;
import java.util.function.Supplier;

public class S2CTransformSyncPacket {
    private final boolean isTransformed;
    private final ResourceLocation transformedMobId;
    private final boolean isMimicOpen;

    public S2CTransformSyncPacket(boolean isTransformed, ResourceLocation transformedMobId, boolean isMimicOpen) {
        this.isTransformed = isTransformed;
        this.transformedMobId = transformedMobId;
        this.isMimicOpen = isMimicOpen;
    }

    public S2CTransformSyncPacket(FriendlyByteBuf buf) {
        this.isTransformed = buf.readBoolean();
        this.transformedMobId = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.isMimicOpen = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isTransformed);
        buf.writeBoolean(this.transformedMobId != null);
        if (this.transformedMobId != null) {
            buf.writeResourceLocation(this.transformedMobId);
        }
        buf.writeBoolean(this.isMimicOpen);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                    transformation.setTransformed(this.isTransformed);
                    transformation.setTransformedMobId(this.transformedMobId);
                    transformation.setMimicOpen(this.isMimicOpen);

                    if (ClientForgeEvents.getDummyMimicEntity() != null) {
                        boolean shouldBeOpen = isTransformed && transformedMobId != null &&
                                transformedMobId.equals(new ResourceLocation("monstermod", "mimic")) && isMimicOpen;
                        ClientForgeEvents.getDummyMimicEntity().setOpen(shouldBeOpen);
                    }
                });
            }
        });
        return true;
    }
}