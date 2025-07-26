package net.mimic.monstermod.network.morph;

import net.mimic.monstermod.common.capabilities.IMorphCapability;
import net.mimic.monstermod.common.capabilities.MorphCapabilityAttacher;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class MorphSyncPacket {
    private final int playerId;
    @Nullable
    private final String morphEntityTypeId;

    public MorphSyncPacket(int playerId, @Nullable String morphEntityTypeId) {
        this.playerId = playerId;
        this.morphEntityTypeId = morphEntityTypeId;
    }

    public static void encode(MorphSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.playerId);
        buf.writeBoolean(msg.morphEntityTypeId != null);
        if (msg.morphEntityTypeId != null) {
            buf.writeUtf(msg.morphEntityTypeId);
        }
    }

    public MorphSyncPacket(FriendlyByteBuf buf) {
        this.playerId = buf.readInt();
        boolean hasMorph = buf.readBoolean();
        this.morphEntityTypeId = hasMorph ? buf.readUtf() : null;
    }

    public static void handle(MorphSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                Player player = (Player) mc.level.getEntity(msg.playerId);
                if (player != null) {
                    player.getCapability(MorphCapabilityAttacher.MORPH_CAPABILITY).ifPresent(cap -> {
                        cap.setMorphEntityTypeId(msg.morphEntityTypeId);
                    });
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}