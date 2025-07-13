package net.mimic.monstermod.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;

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
            ServerPlayer serverPlayer = context.getSender();
            if (serverPlayer == null) return;

            serverPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed() && transformation.getTransformedMobId() != null && transformation.getTransformedMobId().getPath().equals("mimic")) {
                    MimicEntity.MimicAnimationState currentState = transformation.getMimicState();

                    // 現在の状態に応じて次の状態を設定
                    if (currentState == MimicEntity.MimicAnimationState.IDLE || currentState == MimicEntity.MimicAnimationState.CLOSED) {
                        transformation.setMimicState(MimicEntity.MimicAnimationState.OPENING);
                        serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("Mimic is opening!"));
                    } else if (currentState == MimicEntity.MimicAnimationState.OPEN || currentState == MimicEntity.MimicAnimationState.OPENING) {
                        transformation.setMimicState(MimicEntity.MimicAnimationState.CLOSING);
                        serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("Mimic is closing!"));
                    } else if (currentState == MimicEntity.MimicAnimationState.CLOSING) {
                        // CLOSING中に再度押された場合、OPENINGに戻すか、完全に閉じるか
                        // 今回はOPENINGに戻す
                        transformation.setMimicState(MimicEntity.MimicAnimationState.OPENING);
                        serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("Mimic is opening!"));
                    }
                    // 状態が変更されたらクライアントに同期
                    transformation.syncToClient(serverPlayer);
                }
            });
        });
        return true;
    }
}