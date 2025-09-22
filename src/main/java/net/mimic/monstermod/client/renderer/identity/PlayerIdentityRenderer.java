
package net.mimic.monstermod.client.renderer.identity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.mimic.monstermod.identity.IPlayerIdentity;

/**
 * 変身Identity描画用ユーティリティ（Mixin方針統一版）
 */
public class PlayerIdentityRenderer {

    /**
     * Identity に紐づく描画を呼び出す
     * ★ tick / setAnimation は呼ばない
     */
    public static void render(IPlayerIdentity identity, LivingEntity entity,
                              float entityYaw, float partialTicks,
                              PoseStack poseStack, MultiBufferSource buffer,
                              int packedLight) {

        if (!(entity instanceof Player player)) return;
        if (identity == null) return;

        identity.applyAnimationAndRender(player, entityYaw, partialTicks, poseStack, buffer, packedLight,
                MimicEntity.MimicAnimationState.IDLE);
    }
}
