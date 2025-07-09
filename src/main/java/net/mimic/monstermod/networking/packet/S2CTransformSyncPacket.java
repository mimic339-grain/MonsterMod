package net.mimic.monstermod.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.event.ClientForgeEvents; // ClientForgeEventsをインポート

import java.util.function.Supplier;

public class S2CTransformSyncPacket {
    private final boolean transformed;
    private final ResourceLocation mobId;

    public S2CTransformSyncPacket(boolean transformed, ResourceLocation mobId) {
        this.transformed = transformed;
        this.mobId = mobId;
    }

    public S2CTransformSyncPacket(FriendlyByteBuf buf) {
        this.transformed = buf.readBoolean();
        this.mobId = buf.readBoolean() ? buf.readResourceLocation() : null;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(transformed);
        buf.writeBoolean(mobId != null);
        if (mobId != null) {
            buf.writeResourceLocation(mobId);
        }
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                    transformation.setTransformed(this.transformed);
                    transformation.setTransformedMobId(this.mobId);

                    if (ClientForgeEvents.getDummyMimicEntity() != null) {
                        boolean shouldBeOpen = transformed && mobId != null && mobId.equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"));
                        ClientForgeEvents.getDummyMimicEntity().setOpen(shouldBeOpen);
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }
}