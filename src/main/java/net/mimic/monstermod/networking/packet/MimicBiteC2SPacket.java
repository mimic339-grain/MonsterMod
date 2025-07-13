package net.mimic.monstermod.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;

import java.util.function.Supplier;

public class MimicBiteC2SPacket {
    // ★追加: 引数なしのコンストラクタ
    public MimicBiteC2SPacket() {
        // 送信するデータがないため、コンストラクタ内での処理は不要
    }

    public MimicBiteC2SPacket(FriendlyByteBuf buf) {
        // デコードするデータは特にありません
    }

    public void encode(FriendlyByteBuf buf) {
        // エンコードするデータは特にありません
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                ResourceLocation transformedId = transformation.getTransformedMobId();
                boolean isMimic = transformedId != null
                        && "mimic".equals(transformedId.getPath())
                        && MonsterMod.MOD_ID.equals(transformedId.getNamespace());

                if (transformation.isTransformed() && isMimic) {
                    MimicEntity.MimicAnimationState currentState = transformation.getMimicState();
                    if (currentState == MimicEntity.MimicAnimationState.OPEN || currentState == MimicEntity.MimicAnimationState.OPENING) {
                        transformation.setBiting(true); // バイト状態を true に設定
                        transformation.syncToClient(player); // クライアントに同期してアニメーションをトリガー
                        // 噛みつき処理（例：ダメージを与えるなど）をここに追加
                    }
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}