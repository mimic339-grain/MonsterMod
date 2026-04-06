package com.mimic.monstermod.effect;

import com.mimic.monstermod.entity.layer.ChainLayer;
import com.mimic.monstermod.entity.layer.IEffectLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class EffectRenderManager {
    private static final List<IEffectLayer> LAYERS = new ArrayList<>();

    static {
        // ★ここでLayerを登録する！
        LAYERS.add(new ChainLayer());
        // 今後、LAYERS.add(new StunLayer()); とかを追加するだけでOK
    }

    // 全てのレイヤーを一括で描画する
    public static void renderAll(LivingEntity entity, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, float partialTicks) {
        for (IEffectLayer layer : LAYERS) {
            if (layer.shouldRender(entity)) {
                layer.render(entity, poseStack, bufferSource, packedLight, partialTicks);
            }
        }
    }
}