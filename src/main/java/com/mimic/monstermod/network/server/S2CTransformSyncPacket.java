package com.mimic.monstermod.network.server;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**

 * サーバー → クライアント 変身状態同期パケット 完全版（Entity汎用化）
 * * クライアントで Identity を復元
 * * 描画用 BaseMonsterEntity を生成して紐付け
 * * AnimationPlayer と BonePose を適用
 * * 安全性チェックと例外処理あり
 */
public class S2CTransformSyncPacket {

    private final UUID playerId;
    private final CompoundTag nbt;
    private final String animName;
    private final float animTime;
    private final boolean loop;
    private final Map<String, float[]> boneTransforms;

    public S2CTransformSyncPacket(UUID playerId, CompoundTag nbt, String animName, float animTime, boolean loop, Map<String, float[]> boneTransforms) {
        this.playerId = playerId;
        this.nbt = nbt;
        this.animName = animName;
        this.animTime = animTime;
        this.loop = loop;
        this.boneTransforms = boneTransforms != null ? boneTransforms : new HashMap<>();
    }

    /**

     * クライアント側受信処理
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ctx.get().getDirection().getReceptionSide().isClient()) return;

            Minecraft.getInstance().execute(() -> {
                try {
                    if (Minecraft.getInstance().level == null) return;
                    Player player = Minecraft.getInstance().level.getPlayerByUUID(playerId);
                    if (player == null) return;

                    player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                            .ifPresent(transformation -> {
                                try {
                                    // Identity 復元
                                    transformation.deserializeNBT(player, nbt);
                                    BaseMonsterIdentity identity = transformation.getIdentity();
                                    if (identity == null) return;

                                    // 描画用 Entity 生成（汎用）
                                    if (transformation.getEntity() == null) {
                                        Level world = player.level();
                                        Class<? extends BaseMonsterEntity> clazz = identity.getEntityClass();
                                        if (clazz != null) {
                                            BaseMonsterEntity entity = clazz.getConstructor(Level.class).newInstance(world);

                                            // 座標・回転コピー（ゲッター使用）
                                            entity.moveTo(player.getX(), player.getY(), player.getZ(),
                                                    player.getYRot(), player.getXRot());

                                            identity.setEntity(entity);
                                            transformation.attachEntity(entity);

                                            MonsterMod.LOGGER.info("[S2CTransformSyncPacket] Generated client-side entity ({}) for player {}", clazz.getSimpleName(), player.getName().getString());
                                        } else {
                                            MonsterMod.LOGGER.warn("[S2CTransformSyncPacket] Identity {} has no entity class", identity.getId());
                                        }
                                    }

                                    // Animation / Pose 適用
                                    identity.playAnimation(animName, loop, animTime, 0.05f);
                                    identity.applyServerTransforms(boneTransforms);

                                } catch (Exception e) {
                                    MonsterMod.LOGGER.error("[S2CTransformSyncPacket] Identity復元/描画更新で例外発生", e);
                                }
                            });
                } catch (Exception e) {
                    MonsterMod.LOGGER.error("[S2CTransformSyncPacket] クライアント処理で例外発生", e);
                }
            });

        });
        ctx.get().setPacketHandled(true);
    }
}
