package com.mimic.monstermod.util;

import com.mimic.monstermod.entity.hitbox.BoneHitboxRegistry;
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

            String animation = entity.getActiveAnimation();
            if (animation == null || animation.isEmpty()) return;
            double elapsedSeconds = entity.getCurrentAnimationElapsedSeconds();

            for (YatagarasuHitboxProfile.PartConfig part : YatagarasuHitboxProfile.PARTS) {
                // poseStackは既にエンティティ位置へ平行移動済みなので、原点基準で計算する
                Vector3f[] local = BonePoseResolver.resolveLocalCorners(
                        rig, part.boneName(), animation, elapsedSeconds, entity.getYRot());
                if (local == null) continue;
                drawObbEdges(poseStack, consumer, local, r, g, b);
            }
        });
    }

    /**
     * モンスターに変身したプレイヤーの部位当たり判定を描画する。
     * PlayerRendererMixinから呼ばれ、実体のモンスターと同じ見え方になる。
     */
    public static void renderTransformedPlayerHitboxes(net.minecraft.world.entity.player.Player player,
                                                       PoseStack poseStack, MultiBufferSource buffer) {
        var localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;

        localPlayer.getCapability(CapabilityRegistry.PLAYER_CAPABILITY).ifPresent(cap -> {
            if (!cap.hasState(IPlayerData.STATE_SHOW_SKILL_LEAD)) return;

            var transformation = player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).orElse(null);
            if (transformation == null || !transformation.isTransformed()) return;

            BoneHitboxRegistry.Rig rig = BoneHitboxRegistry.get(transformation.getMobId());
            if (rig == null || !rig.rigData().isLoaded()) return;

            var proxy = transformation.getEntity(player.level());
            if (proxy == null) return;

            String animation = proxy.getActiveAnimation();
            if (animation == null || animation.isEmpty()) return;
            double elapsed = proxy.getAnimationElapsedSeconds(rig.rigData());

            float r = cap.getLeadR();
            float g = cap.getLeadG();
            float b = cap.getLeadB();
            VertexConsumer consumer = buffer.getBuffer(RenderType.lines());

            for (var part : rig.parts()) {
                Vector3f[] local = BonePoseResolver.resolveLocalCorners(
                        rig.rigData(), part.boneName(), animation, elapsed, player.getYRot());
                if (local == null) continue;
                drawObbEdges(poseStack, consumer, local, r, g, b);
            }
        });
    }

    private static void drawObbEdges(PoseStack poseStack, VertexConsumer consumer, Vector3f[] corners, float r, float g, float b) {
        var pose = poseStack.last().pose();
        for (int[] edge : BonePoseResolver.EDGES) {
            Vector3f a = corners[edge[0]];
            Vector3f bVert = corners[edge[1]];
            Vector3f normal = new Vector3f(bVert).sub(a).normalize();
            consumer.vertex(pose, a.x, a.y, a.z).color(r, g, b, 1.0F).normal(normal.x, normal.y, normal.z).endVertex();
            consumer.vertex(pose, bVert.x, bVert.y, bVert.z).color(r, g, b, 1.0F).normal(normal.x, normal.y, normal.z).endVertex();
        }
    }
}