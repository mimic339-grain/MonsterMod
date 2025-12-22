package com.mimic.monstermod.animation;

import com.mimic.monstermod.weapon.WeaponAnimator;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Player / Weapon アニメーション同期司令塔
 * ★ Client ONLY
 */
public class HunterAnimationController {

    private static final ResourceLocation PLAYER_LAYER_ID =
            new ResourceLocation("monstermod", "player_animation");

    /**
     * S2C packet から唯一呼ばれる入口
     */
    public static void play(Player entity, String animationId, boolean override) {
        if (!(entity instanceof AbstractClientPlayer player)) return;

        // ① Player animation
        playPlayerAnimation(player, animationId, override);

        // ② Weapon animation
        WeaponAnimator.playWeaponAnimation(player, animationId);
    }

    /* ============================= */

    private static void playPlayerAnimation(AbstractClientPlayer player,
                                            String animationId,
                                            boolean override) {

        ModifierLayer<IAnimation> layer =
                (ModifierLayer<IAnimation>) PlayerAnimationAccess
                        .getPlayerAssociatedData(player)
                        .get(PLAYER_LAYER_ID);

        if (layer == null) return;

        if (!layer.isActive() || override) {
            var anim = PlayerAnimationRegistry.getAnimation(
                    new ResourceLocation("monstermod", animationId)
            );

            if (anim != null) {
                layer.setAnimation(new KeyframeAnimationPlayer(anim));
            }
        }
    }
}