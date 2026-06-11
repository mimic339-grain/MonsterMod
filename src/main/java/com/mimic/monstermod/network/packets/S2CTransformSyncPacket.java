package com.mimic.monstermod.network.packets;

import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー → 全クライアント: Monster変身状態同期パケット。
 *
 * EFM参考:
 *   - network/server/SPChangePlayerMode.java
 *   - network/server/SPAnimationVariablePacket.java
 *
 * 送信トリガー:
 *   MonsterTransformation.syncToClient() 内で送信。
 *   変身開始・解除・HP変化・頭の向き変化時に呼ぶ。
 *
 * 配置: com/mimic/monstermod/network/packets/S2CTransformSyncPacket.java
 */
public class S2CTransformSyncPacket {

    private final int entityId;
    private final boolean isTransformed;
    private final String monsterType;
    private final float monsterHp;
    private final float monsterMaxHp;
    private final float headYaw;
    private final float headPitch;

    public S2CTransformSyncPacket(int entityId, boolean isTransformed, String monsterType,
                                  float monsterHp, float monsterMaxHp,
                                  float headYaw, float headPitch) {
        this.entityId      = entityId;
        this.isTransformed = isTransformed;
        this.monsterType   = monsterType;
        this.monsterHp     = monsterHp;
        this.monsterMaxHp  = monsterMaxHp;
        this.headYaw       = headYaw;
        this.headPitch     = headPitch;
    }

    // ── シリアライズ ───────────────────────────────────────────────────
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(isTransformed);
        buf.writeUtf(monsterType);
        buf.writeFloat(monsterHp);
        buf.writeFloat(monsterMaxHp);
        buf.writeFloat(headYaw);
        buf.writeFloat(headPitch);
    }

    public static S2CTransformSyncPacket decode(FriendlyByteBuf buf) {
        return new S2CTransformSyncPacket(
                buf.readInt(), buf.readBoolean(), buf.readUtf(),
                buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat()
        );
    }

    // ── クライアント側受信処理 ─────────────────────────────────────────
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Entity entity = mc.level.getEntity(entityId);
            if (!(entity instanceof Player player)) return;

            // 対象プレイヤーのCapabilityを更新 → PlayerRendererMixinが自動的に描画切り替え
            player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(cap -> {
                // cap は既に MonsterTransformation クラスとして認識されます
                cap.deserializeNBT(buildSyncTag());
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private net.minecraft.nbt.CompoundTag buildSyncTag() {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putBoolean("isTransformed", isTransformed);
        tag.putString("monsterType", monsterType);
        tag.putFloat("monsterHp", monsterHp);
        tag.putFloat("monsterMaxHp", monsterMaxHp);
        tag.putFloat("headYaw", headYaw);
        tag.putFloat("headPitch", headPitch);
        return tag;
    }
}