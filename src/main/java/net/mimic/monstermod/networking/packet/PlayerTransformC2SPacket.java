package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.networking.PlayerTransformHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayerTransformC2SPacket {

    private final boolean transform;
    private final ResourceLocation identityId;

    public PlayerTransformC2SPacket(boolean transform, ResourceLocation identityId) {
        this.transform = transform;
        this.identityId = identityId;
    }

    public PlayerTransformC2SPacket(FriendlyByteBuf buf) {
        this.transform = buf.readBoolean();
        this.identityId = buf.readNullable(FriendlyByteBuf::readResourceLocation);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.transform);
        buf.writeNullable(this.identityId, FriendlyByteBuf::writeResourceLocation);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            // 変身処理は PlayerTransformHandler に移譲
            PlayerTransformHandler.handleTransformRequest(player, transform, identityId);
        });
        context.setPacketHandled(true);
        return true;
    }
}
