package net.mimic.monstermod.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;

import java.util.function.Supplier;

public class MimicBiteC2SPacket {
    public MimicBiteC2SPacket() {
    }

    public MimicBiteC2SPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer serverPlayer = context.getSender();
            if (serverPlayer == null) return;

            serverPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed() && transformation.getTransformedMobId() != null && transformation.getTransformedMobId().getPath().equals("mimic")) {
                    MimicEntity.MimicAnimationState currentState = transformation.getMimicState();
                    // OPEN または OPENING 状態でのみバイトを許可
                    if (currentState == MimicEntity.MimicAnimationState.OPEN || currentState == MimicEntity.MimicAnimationState.OPENING) {
                        transformation.setBiting(true);
                        transformation.syncToClient(serverPlayer); // クライアントに同期してアニメーションをトリガー
                    }
                }
            });
        });
        return true;
    }
}