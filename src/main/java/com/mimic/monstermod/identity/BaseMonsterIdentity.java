package com.mimic.monstermod.identity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;

/**
 * 共通処理をまとめたベース
 */
public abstract class BaseMonsterIdentity {

    protected final String id;

    protected BaseMonsterIdentity(String id) {
        this.id = id;
    }

    /** Identity ID */
    public String getId() {
        return id;
    }

    /** BoundingBoxのサイズ（必要ならオーバーライド） */
    public Vec3 getBoundingBoxDimensions(Pose pose) {
        return new Vec3(0.6f, 1.8f, 0.6f);
    }

    /** EyeHeight（必要ならオーバーライド） */
    public float getEyeHeight(Pose pose) {
        return 1.62f;
    }

    /** 描画共通処理 */
    public void applyAnimationAndRender(
            Player player,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (!transformation.isTransformed()) return;

            Level level = player.getCommandSenderWorld();
            BaseMonsterEntity entity = transformation.getTransformedEntity(level);
            if (entity == null) return;

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-player.yBodyRot));

            Minecraft.getInstance().getEntityRenderDispatcher()
                    .getRenderer(entity)
                    .render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

            poseStack.popPose();
        });
    }
}
