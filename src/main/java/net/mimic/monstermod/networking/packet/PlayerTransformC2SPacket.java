package net.mimic.monstermod.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.entity.custom.MimicEntity; // MimicEntityをインポート

import java.util.function.Supplier;

public class PlayerTransformC2SPacket {
    private final boolean transform;

    public PlayerTransformC2SPacket(boolean transform) {
        this.transform = transform;
    }

    public PlayerTransformC2SPacket(FriendlyByteBuf buf) {
        this.transform = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.transform);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                transformation.setTransformed(this.transform);

                if (this.transform) {
                    // 仮にMimicに変身するとする
                    transformation.setTransformedMobId(new ResourceLocation(MonsterMod.MOD_ID, "mimic"));
                    // Mimicに変身する場合、初期状態をIDLEに設定
                    transformation.setMimicState(MimicEntity.MimicAnimationState.IDLE);
                    transformation.setBiting(false);
                } else {
                    transformation.setTransformedMobId(null);
                    // 変身解除時、Mimic関連の状態をリセット
                    transformation.setMimicState(MimicEntity.MimicAnimationState.IDLE);
                    transformation.setBiting(false);
                }

                // クライアントへ同期
                ModMessages.sendToPlayer(new S2CTransformSyncPacket(transformation.isTransformed(), transformation.getTransformedMobId(), transformation.getMimicState().name(), transformation.isBiting()), player);
            });
        });
        return true;
    }
}