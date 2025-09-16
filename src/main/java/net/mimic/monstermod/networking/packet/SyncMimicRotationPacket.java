package net.mimic.monstermod.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.custom.MimicEntity;

import java.util.function.Supplier;

public class SyncMimicRotationPacket {

    private final int entityId;
    private final float bodyRot;
    private final float headRot;

    public SyncMimicRotationPacket(int entityId, float bodyRot, float headRot) {
        this.entityId = entityId;
        this.bodyRot = bodyRot;
        this.headRot = headRot;
    }

    public SyncMimicRotationPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.bodyRot = buf.readFloat();
        this.headRot = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeFloat(bodyRot);
        buf.writeFloat(headRot);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            if (player == null) return;

            Entity entity = player.level().getEntity(entityId);
            if (entity instanceof MimicEntity mimic) {
                mimic.setBodyRot(bodyRot);
                mimic.setHeadRot(headRot);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
