package com.mimic.monstermod.geo.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mimic.monstermod.geo.model.monster.YatagarasuModel;
import com.mimic.monstermod.entity.monster.YatagarasuEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class YatagarasuRenderer extends GeoEntityRenderer<YatagarasuEntity> {

    public YatagarasuRenderer(EntityRendererProvider.Context context) {
        super(context, new YatagarasuModel());
        // 影のサイズ（モデルに合わせて調整）
        this.shadowRadius = 1.2f;
    }

    @Override
    public void render(YatagarasuEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        poseStack.pushPose();

        // スキル8 (昇天爆砕) 中は、モデルを空中にスライドさせる
        // getSkillTick() が 20(発動) 以下の時に浮くようにする例
        if ("sky_explosion".equals(entity.getCurrentSkill()) && entity.getSkillTick() <= 20) {
            // 1tick ごとに少しずつ上げるなどの計算も可能。ここでは固定で5ブロック分浮かせる
            poseStack.translate(0, 5.0, 0);
        }
        // ★ConfigがON かつ Entity側が表示許可を出している場合
        com.mimic.monstermod.util.HitboxRenderUtil.renderIfEnabled(entity, poseStack, buffer);
        // ボーン追従ヒットボックス(頭・翼・尻尾など、弱点部位)の回転込み表示
        com.mimic.monstermod.util.HitboxRenderUtil.renderYatagarasuHitboxesIfEnabled(entity, poseStack, buffer);

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }
}
