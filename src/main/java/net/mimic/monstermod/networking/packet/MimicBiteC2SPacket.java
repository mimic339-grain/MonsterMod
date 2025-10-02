package net.mimic.monstermod.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.entity.custom.MimicEntity;

import java.util.function.Supplier;

public class MimicBiteC2SPacket {

    public MimicBiteC2SPacket() {}
    public MimicBiteC2SPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static MimicBiteC2SPacket decode(FriendlyByteBuf buf) {
        return new MimicBiteC2SPacket(buf);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (!transformation.isTransformed()) return;

                ResourceLocation mobId = transformation.getTransformedMobId();
                if (mobId == null) return;

                // アニメーション変更 & クライアント同期
                transformation.setAnimationStateAndSync(mobId, MimicEntity.MimicAnimationState.BITE, player);
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
