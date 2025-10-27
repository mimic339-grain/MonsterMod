package com.mimic.monstermod.network.server;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CMonsterSyncPacket {

    private final int entityId;
    private final String animation;
    private final String skill;

    public S2CMonsterSyncPacket(int entityId, String animation, String skill) {
        this.entityId = entityId;
        this.animation = animation;
        this.skill = skill;
    }

    public static void encode(S2CMonsterSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeUtf(msg.animation);
        buf.writeUtf(msg.skill);
    }

    public static S2CMonsterSyncPacket decode(FriendlyByteBuf buf) {
        return new S2CMonsterSyncPacket(
                buf.readInt(),
                buf.readUtf(),
                buf.readUtf()
        );
    }

    public static void handle(S2CMonsterSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Entity entity = mc.level.getEntity(msg.entityId);
            if (entity instanceof BaseMonsterEntity monster) {
                // 安全に同期：必ず setAnimation() を呼ぶ
                monster.setAnimation(msg.animation);

                IMonsterData data = CapabilityRegistry.getMonsterData(monster);
                if (data != null) {
                    data.setSkill(msg.skill);
                    // skillTick は送信しない設計
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public void sendToPlayer(ServerPlayer player) {
        ModMessages.sendToPlayer(this, player);
    }
}
