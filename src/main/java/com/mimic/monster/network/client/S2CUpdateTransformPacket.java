package com.mimic.monster.network.client;

import com.mimic.monster.capability.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class S2CUpdateTransformPacket {
    private final ResourceLocation entityId;
    private final int playerId;
    //PlayerIDと変身先を保持
    public S2CUpdateTransformPacket(int playerId, ResourceLocation entityId) {
        this.playerId = playerId;
        this.entityId = entityId;
    }
    //パケット送信用にデータを書き込む
    public static void encode(S2CUpdateTransformPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.playerId);
        if (packet.entityId != null) {
            buf.writeBoolean(true);
            buf.writeResourceLocation(packet.entityId);
        } else {
            buf.writeBoolean(false);
        }
    }
    //受信側でバイトデータからパケットオブジェクトを復元
    public static S2CUpdateTransformPacket decode(FriendlyByteBuf buf) {
        int playerId = buf.readInt();
        boolean hasEntityId = buf.readBoolean();
        ResourceLocation entityId = hasEntityId ? buf.readResourceLocation() : null;
        return new S2CUpdateTransformPacket(playerId, entityId);
    }
    //受信時に呼ばれて処理
    public static void handle(S2CUpdateTransformPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;
            Entity entity = Minecraft.getInstance().level.getEntity(packet.playerId);
            if (!(entity instanceof Player player)) return;

            player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(cap -> {
                if (packet.entityId != null) {
                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(packet.entityId);
                    cap.setTransformedType(type);
                    cap.setTransformed(true);
                } else {
                    // 変身解除
                    cap.setTransformedType(null);
                    cap.setTransformed(false);
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}