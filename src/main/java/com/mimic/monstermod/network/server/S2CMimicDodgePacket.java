package com.mimic.monstermod.network.server;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CMimicDodgePacket {
    private final int entityId;
    private final double x, y, z;

    public S2CMimicDodgePacket(int entityId, Vec3 pos) {
        this.entityId = entityId;
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
    }

    public S2CMimicDodgePacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {

            var mc = Minecraft.getInstance();
            if (mc.level == null) return;

            var e = mc.level.getEntity(entityId);
            if (e != null) {
                e.setPos(x, y, z);

                // デバッグ
                System.out.println("[S2CMimicDodgePacket] Client moved entity " +
                        entityId + " to " + x + ", " + y + ", " + z);
            }
        });
        ctx.setPacketHandled(true);
        return true;
    }
}
