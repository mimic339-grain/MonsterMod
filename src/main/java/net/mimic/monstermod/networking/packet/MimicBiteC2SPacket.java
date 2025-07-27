package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.MonsterMod; // ★追加: ロガーのため
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation; // ★追加
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.IPlayerIdentity; // IPlayerIdentityをインポート
import net.mimic.monstermod.identity.impl.MimicIdentity; // MimicIdentityをインポート

import java.util.function.Supplier;

public class MimicBiteC2SPacket {
    public MimicBiteC2SPacket() {}

    public MimicBiteC2SPacket(FriendlyByteBuf buf) {}

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
                        if (currentState == MimicEntity.MimicAnimationState.OPEN || currentState == MimicEntity.MimicAnimationState.OPENING) {
                            transformation.setBiting(true); // バイト状態を true に設定
                            transformation.syncToClient(player); // クライアントに同期してアニメーションをトリガー
                            // 噛みつき処理（例：ダメージを与えるなど）をここに追加
                            // MonsterMod.getLogger().debug("{} が噛みついた！", player.getName().getString());
                        } else {
                            MonsterMod.getLogger().debug("{} はMimicだが口が開いていないため噛みつけない。", player.getName().getString());
                        }
                    } else {
                        MonsterMod.getLogger().debug("{} はMimicではないため噛みつけない。", player.getName().getString());
                    }
                } else {
                    MonsterMod.getLogger().debug("{} は変身していないため噛みつけない。", player.getName().getString());
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}