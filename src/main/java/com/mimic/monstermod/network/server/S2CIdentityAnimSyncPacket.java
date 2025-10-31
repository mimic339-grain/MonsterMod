package com.mimic.monstermod.network.server;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * サーバー -> クライアント アニメーション同期パケット (完全版)
 * - BaseMonsterIdentity の現在PoseとAnimationStateをクライアントに送信
 * - 差分同期済みの float[] 形式の骨変換マップを使用
 */
public class S2CIdentityAnimSyncPacket {

    private final UUID playerUuid;
    private final String animName;
    private final float animTime;
    private final boolean loop;
    private final Map<String, float[]> boneTransforms;

    /**
     * サーバー用コンストラクタ（BaseMonsterIdentity から Pose を取得）
     */
    public S2CIdentityAnimSyncPacket(UUID playerUuid, BaseMonsterIdentity identity) {
        this.playerUuid = playerUuid;
        this.animName = identity.currentState;
        this.animTime = identity.animationPlayer != null ? identity.animationPlayer.getTime() : 0f;
        this.loop = identity.loop;

        // サーバー同期用 Pose を float[] に変換
        this.boneTransforms = identity.getPoseArrayForSync();

        // サーバー側 lastBoneTransforms を更新
        identity.applyServerTransforms(this.boneTransforms);
    }

    /**
     * サーバー用コンストラクタ（直接 Pose Map を渡す場合）
     */
    public S2CIdentityAnimSyncPacket(UUID playerUuid, Map<String, float[]> poseMap) {
        this.playerUuid = playerUuid;
        this.animName = "idle";
        this.animTime = 0f;
        this.loop = true;
        this.boneTransforms = poseMap != null ? poseMap : new HashMap<>();
    }

    /**
     * デコード用コンストラクタ（バッファから復元）
     */
    public S2CIdentityAnimSyncPacket(FriendlyByteBuf buf) {
        this.playerUuid = buf.readUUID();
        this.animName = buf.readUtf(32767);
        this.animTime = buf.readFloat();
        this.loop = buf.readBoolean();
        int size = buf.readInt();
        this.boneTransforms = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf(32767);
            float[] arr = new float[6];
            for (int j = 0; j < 6; j++) arr[j] = buf.readFloat();
            boneTransforms.put(key, arr);
        }
    }

    /**
     * パケット書き込み
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(playerUuid);
        buf.writeUtf(animName);
        buf.writeFloat(animTime);
        buf.writeBoolean(loop);
        buf.writeInt(boneTransforms.size());
        for (var entry : boneTransforms.entrySet()) {
            buf.writeUtf(entry.getKey());
            float[] arr = entry.getValue();
            for (int i = 0; i < 6; i++) buf.writeFloat(i < arr.length ? arr[i] : 0f);
        }
    }

    /**
     * クライアント側受信処理
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ctx.get().getDirection().getReceptionSide().isClient()) return;

            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().level == null) return;
                Player target = Minecraft.getInstance().level.getPlayerByUUID(playerUuid);
                if (target != null) applyToPlayer(target);
            });
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * クライアント側で identity に適用
     */
    private void applyToPlayer(Player player) {
        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {
                    BaseMonsterIdentity identity = trans.getIdentity();
                    if (identity != null) {
                        // AnimationPlayer 補間再生
                        identity.playAnimation(animName, loop, animTime, 0.05f);

                        // BonePose 部分更新
                        identity.applyServerTransforms(boneTransforms);
                    }
                });
    }
}
