package net.mimic.monstermod.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.capability.PlayerTransformation.MonsterState;
import net.mimic.monstermod.entity.custom.MimicEntity;

import java.util.function.Supplier;

public class MimicSwitchC2SPacket {
    private boolean open; // 単純な open/close トグル用

    public MimicSwitchC2SPacket() {}
    public MimicSwitchC2SPacket(boolean open) { this.open = open; }

    public MimicSwitchC2SPacket(FriendlyByteBuf buf) { this.open = buf.readBoolean(); }
    public void encode(FriendlyByteBuf buf) { buf.writeBoolean(this.open); }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (!transformation.isTransformed()) return;

                MonsterState state = transformation.getMonsterState(transformation.getTransformedMobId());
                if (state == null) return;

                boolean newOpen = !state.getFlag("isOpen"); // トグル
                state.setFlag("isOpen", newOpen);
                state.animationState = newOpen ? "OPENING" : "CLOSING";

                transformation.setMonsterState(transformation.getTransformedMobId(), state);
                transformation.syncToClient(player);

                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.getEntitiesOfClass(MimicEntity.class, player.getBoundingBox().inflate(32.0D))
                            .forEach(mimic -> {
                                if (mimic.isLinkedTo(player)) {
                                    mimic.requestSwitchAnimation(newOpen);
                                }
                            });
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
