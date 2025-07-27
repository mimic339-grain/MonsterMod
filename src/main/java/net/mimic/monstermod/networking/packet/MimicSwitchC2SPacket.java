package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.MonsterMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.identity.impl.MimicIdentity;

import java.util.function.Supplier;

public class MimicSwitchC2SPacket {
    public MimicSwitchC2SPacket() {}

    public MimicSwitchC2SPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    // ★変更: transformedMobIdとMimicStateをCapabilityから直接取得
                    ResourceLocation transformedMobId = transformation.getTransformedMobId();

                    // 変身先がMimicの場合のみ処理
                    if (transformedMobId != null && transformedMobId.equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"))) {
                        MimicEntity.MimicAnimationState currentState = transformation.getMimicState();

                        // 現在の状態に応じて次の状態を設定
                        if (currentState == MimicEntity.MimicAnimationState.IDLE || currentState == MimicEntity.MimicAnimationState.CLOSED) {
                            transformation.setMimicState(MimicEntity.MimicAnimationState.OPENING);
                        } else if (currentState == MimicEntity.MimicAnimationState.OPEN || currentState == MimicEntity.MimicAnimationState.OPENING) {
                            transformation.setMimicState(MimicEntity.MimicAnimationState.CLOSING);
                        } else if (currentState == MimicEntity.MimicAnimationState.CLOSING) {
                            transformation.setMimicState(MimicEntity.MimicAnimationState.OPENING); // 閉じるアニメ中に再度押すと開くアニメに
                        }
                        transformation.syncToClient(player); // 状態が変更されたらクライアントに同期
                        MonsterMod.getLogger().debug("{} のMimic状態を {} に変更。", player.getName().getString(), transformation.getMimicState().name());
                    } else {
                        MonsterMod.getLogger().debug("{} はMimicではないため状態を切り替えられない。", player.getName().getString());
                    }
                } else {
                    MonsterMod.getLogger().debug("{} は変身していないため状態を切り替えられない。", player.getName().getString());
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}