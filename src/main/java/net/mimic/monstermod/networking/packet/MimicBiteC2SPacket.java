package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.MonsterMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.capability.PlayerTransformation.MonsterState;

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
                if (!transformation.isTransformed()) {
                    MonsterMod.getLogger().debug("{} は変身していないためBITEできない。", player.getName().getString());
                    return;
                }

                // 変身先が Mimic か確認
                MonsterState state = transformation.getMonsterState(transformation.getTransformedMobId());
                if (state == null) return;

                // BITE アニメーション状態に更新
                state.animationState = "BITE";

                // 必要なら他のフラグ（例: クールダウン）もここで操作可能
                transformation.setMonsterState(transformation.getTransformedMobId(), state);

                // クライアントへ同期
                transformation.syncToClient(player);

                MonsterMod.getLogger().debug("{} が BITE アニメーションを再生", player.getName().getString());
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
