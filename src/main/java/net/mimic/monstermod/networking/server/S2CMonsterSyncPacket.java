package net.mimic.monstermod.networking.server;

import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.variable.CapabilityRegistry;
import net.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー → クライアント
 * モンスターの状態変化を全クライアントへ同期する
 */
public class S2CMonsterSyncPacket {

    private final int entityId;
    private final String animation;
    private final String skill;
    private final int skillTick;

    public S2CMonsterSyncPacket(int entityId, String animation, String skill, int skillTick) {
        this.entityId = entityId;
        this.animation = animation;
        this.skill = skill;
        this.skillTick = skillTick;
    }

    // ----------------------------
    // 書き込み
    // ----------------------------
    public static void encode(S2CMonsterSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeUtf(msg.animation);
        buf.writeUtf(msg.skill);
        buf.writeInt(msg.skillTick);
    }

    // ----------------------------
    // 読み込み
    // ----------------------------
    public static S2CMonsterSyncPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readInt();
        String animation = buf.readUtf();
        String skill = buf.readUtf();
        int skillTick = buf.readInt();
        return new S2CMonsterSyncPacket(entityId, animation, skill, skillTick);
    }

    // ----------------------------
    // クライアント側処理
    // ----------------------------
    public static void handle(S2CMonsterSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Entity entity = mc.level.getEntity(msg.entityId);
            if (entity instanceof BaseMonsterEntity monster) {

                // 🎬 Animation名を直接同期（setAnimationStateなどは不要）
                monster.getEntityData().set(BaseMonsterEntity.ANIMATION_NAME, msg.animation);

                // Skill関連も必要なら更新
                IMonsterData data = CapabilityRegistry.getMonsterData(monster);
                if (data != null) {
                    data.setSkill(msg.skill);
                    data.setSkillTick(msg.skillTick);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // ----------------------------
    // サーバー → 特定プレイヤー
    // ----------------------------
    public void sendToPlayer(ServerPlayer player) {
        ModMessages.sendToPlayer(this, player);
    }
}