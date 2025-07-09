package net.mimic.monstermod.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.resources.ResourceLocation;
import net.mimic.monstermod.MonsterMod;
import net.minecraft.network.chat.Component;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;

import java.util.function.Supplier;

public class MimicSwitchC2SPacket {
    public MimicSwitchC2SPacket() {
    }

    public MimicSwitchC2SPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed() && transformation.getTransformedMobId() != null &&
                        transformation.getTransformedMobId().equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"))) {

                    boolean currentOpenState = transformation.isMimicOpen();
                    transformation.setMimicOpen(!currentOpenState);

                    ModMessages.sendToPlayer(new S2CTransformSyncPacket(transformation.isTransformed(), transformation.getTransformedMobId(), transformation.isMimicOpen()), player);

                    player.sendSystemMessage(Component.literal("Mimic state changed to: " + (transformation.isMimicOpen() ? "OPEN" : "CLOSED")));
                }
            });
        });
        return true;
    }
}