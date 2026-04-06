package com.mimic.monstermod.entity.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;

public interface IEffectLayer {
    // このエフェクトを描画すべきかどうか（BINDがあるか、等）
    boolean shouldRender(LivingEntity entity);
    // 実際の描画処理
    void render(LivingEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTicks);
}