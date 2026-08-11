package com.mimic.monstermod.util;

import com.mimic.monstermod.entity.hitbox.BonePoseResolver;
import com.mimic.monstermod.entity.hitbox.BoneRigData;
import com.mimic.monstermod.entity.hitbox.YatagarasuHitboxProfile;
import com.mimic.monstermod.entity.monster.YatagarasuEntity;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IPlayerData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

public class HitboxRenderUtil {
    public static void renderIfEnabled(Entity entity, PoseStack poseStack, MultiBufferSource buffer) {
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;

        localPlayer.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
            if (cap.hasState(IPlayerData.STATE_SHOW_SKILL_LEAD)) {

                boolean isTarget = (entity == localPlayer)
                        || (entity instanceof com.mimic.monstermod.entity.BaseEntity)
                        || (entity instanceof com.mimic.monstermod.entity.obj.OnibiEntity);

                if (!isTarget) return;

                float r = cap.getLeadR();
                float g = cap.getLeadG();
                float b = cap.getLeadB();
                float thickness = cap.getLeadThickness(); // ★ 太さを取得

                net.minecraft.world.phys.AABB box = entity.getBoundingBox().move(-entity.getX(), -entity.getY(), -entity.getZ());
                com.mojang.blaze3d.vertex.VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

                // ★ ループを回して「太さ」を表現
                // 0.002ずつ判定箱を膨らませて重ね描きする
                int passes = Math.max(1, (int)thickness);
                for (int i = 0; i < passes; i++) {
                    float offset = i * 0.002f;
                    LevelRenderer.renderLineBox(poseStack, consumer, box.inflate(offset), r, g, b, 1.0F);
                }
            }
        });
    }

    /**
     * Yatagarasuのボーン追従ヒットボックス(頭・翼・尻尾など)を、回転込みの
     * 実際の箱(OBB)として描画する。通常のAABBと違い回転しているため
     * LevelRenderer.renderLineBoxは使えず、12本の辺を個別に描画する。
     * ON/OFF・色・太さは既存の「当たり判定表示」設定をそのまま共有する。
     */
    public static void renderYatagarasuHitboxesIfEnabled(YatagarasuEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;

        BoneRigData rig = YatagarasuEntity.getBoneRig();
        if (!rig.isLoaded()) return;

        localPlayer.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
            if (!cap.hasState(IPlayerData.STATE_SHOW_SKILL_LEAD)) return;

            float r = cap.getLeadR();
            float g = cap.getLeadG();
            float b = cap.getLeadB();
            VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

            String animation = entity.getCurrentAnimation();
            if (animation == null || animation.isEmpty()) return;
            double elapsedSeconds = entity.getCurrentAnimationElapsedSeconds();

            for (YatagarasuHitboxProfile.PartConfig part : YatagarasuHitboxProfile.PARTS) {
                Vector3f[] corners = BonePoseResolver.resolveWorldCorners(rig, part.boneName(), animation, elapsedSeconds, entity);
                if (corners == null) continue;

                // entityの位置基準(poseStackは既にentity位置へ平行移動済み)に変換
                Vector3f[] local = new Vector3f[8];
                for (int i = 0; i < 8; i++) {
                    local[i] = new Vector3f(
                            (float) (corners[i].x - entity.getX()),
                            (float) (corners[i].y - entity.getY()),
                            (float) (corners[i].z - entity.getZ())
                    );
                }
                drawObbEdges(poseStack, consumer, local, r, g, b);
            }
        });
    }

    // 頂点の並びはBonePoseResolver.resolveWorldCornersと対応させる:
    // 0:(x,y,z) 1:(x+w,y,z) 2:(x,y+h,z) 3:(x+w,y+h,z) 4:(x,y,z+d) 5:(x+w,y,z+d) 6:(x,y+h,z+d) 7:(x+w,y+h,z+d)
    private static final int[][] OBB_EDGES = {
            {0, 1}, {0, 2}, {1, 3}, {2, 3}, // 手前の面
            {4, 5}, {4, 6}, {5, 7}, {6, 7}, // 奥の面
            {0, 4}, {1, 5}, {2, 6}, {3, 7}  // 手前と奥をつなぐ辺
    };

    private static void drawObbEdges(PoseStack poseStack, VertexConsumer consumer, Vector3f[] corners, float r, float g, float b) {
        var pose = poseStack.last().pose();
        for (int[] edge : OBB_EDGES) {
            Vector3f a = corners[edge[0]];
            Vector3f bVert = corners[edge[1]];
            Vector3f normal = new Vector3f(bVert).sub(a).normalize();
            consumer.vertex(pose, a.x, a.y, a.z).color(r, g, b, 1.0F).normal(normal.x, normal.y, normal.z).endVertex();
            consumer.vertex(pose, bVert.x, bVert.y, bVert.z).color(r, g, b, 1.0F).normal(normal.x, normal.y, normal.z).endVertex();
        }
    }
}