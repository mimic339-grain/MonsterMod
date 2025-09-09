package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.MonsterMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.capability.PlayerTransformation.MonsterState;

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
                if (!transformation.isTransformed()) {
                    MonsterMod.getLogger().debug("{} は変身していないため状態を切り替えられない。", player.getName().getString());
                    return;
                }

                ResourceLocation mobId = transformation.getTransformedMobId();
                if (mobId == null) return;

                // MonsterState を取得
                MonsterState state = transformation.getMonsterState(mobId);

                // Mimic 専用フラグを切り替える
                boolean isOpen = state.getFlag("isOpen");
                state.setFlag("isOpen", !isOpen);

                // animationState も併せて更新
                if (!isOpen) {
                    state.animationState = "OPENING";
                } else {
                    state.animationState = "CLOSING";
                }

                // Capability に保存
                transformation.setMonsterState(mobId, state);

                // クライアントへ同期
                transformation.syncToClient(player);

                MonsterMod.getLogger().debug("{} の {} 状態を {} に変更。isOpen={}",
                        player.getName().getString(), mobId.getPath(), state.animationState, !isOpen);
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
