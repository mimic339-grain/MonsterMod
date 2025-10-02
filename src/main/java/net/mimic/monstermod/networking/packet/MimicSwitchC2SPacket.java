package net.mimic.monstermod.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.entity.custom.MimicEntity;

import java.util.function.Supplier;

public class MimicSwitchC2SPacket {

    public MimicSwitchC2SPacket() {}
    public MimicSwitchC2SPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public static MimicSwitchC2SPacket decode(FriendlyByteBuf buf) {
        return new MimicSwitchC2SPacket(buf);
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

                PlayerTransformation.MonsterState state = transformation.getMonsterState(mobId);
                if (state == null) return;

                boolean isOpen = state.getFlag("isOpen");
                boolean newOpen = !isOpen;
                state.setFlag("isOpen", newOpen);

                // OPEN / CLOSEアニメーションに変更
                MimicEntity.MimicAnimationState newState = newOpen
                        ? MimicEntity.MimicAnimationState.OPEN
                        : MimicEntity.MimicAnimationState.CLOSE;

                transformation.setAnimationStateAndSync(mobId, newState, player);

                transformation.setMonsterState(mobId, state);
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
