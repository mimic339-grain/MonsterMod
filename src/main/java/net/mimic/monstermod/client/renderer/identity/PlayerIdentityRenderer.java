package net.mimic.monstermod.client.renderer.identity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.MonsterMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/**
 * 変身Identity描画用ユーティリティ（Mixin方針統一版）
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class PlayerIdentityRenderer {

    /**
     * Identity に紐づく描画を呼び出す
     */
    public static void render(IPlayerIdentity identity, LivingEntity entity,
                              float entityYaw, float partialTicks,
                              PoseStack poseStack, MultiBufferSource buffer,
                              int packedLight) {

        if (identity == null) {
            MonsterMod.getLogger().warn("PlayerIdentityRenderer.render が null の identity で呼び出されました。");
            return;
        }

        if (!(entity instanceof Player player)) {
            MonsterMod.getLogger().warn("PlayerIdentityRenderer.render が Player 以外で呼び出されました: {}", entity);
            return;
        }

        // 変身状態を取得
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (!transformation.isTransformed()) return;

            // ★ MonsterState ではなく MimicAnimationState を取得
            MimicEntity.MimicAnimationState animState = transformation.getAnimationState(transformation.getTransformedMobId());

            // Identity 側でアニメーション反映＋描画
            identity.applyAnimationAndRender(player, entityYaw, partialTicks, poseStack, buffer, packedLight, animState);
        });
    }
}
