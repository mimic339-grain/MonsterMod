package com.mimic.monstermod.animation;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * プレイヤーごとの ModifierLayer を管理するクラス
 */
public class PlayerAnimation {

    // レイヤー識別キー
    public static final ResourceLocation ANIMATION_KEY =
            new ResourceLocation("monstermod", "player_animation");

    /**
     * クライアントセットアップ時に呼び出す
     * プレイヤーに ModifierLayer を登録する
     */
    public static void registerAnimations() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                ANIMATION_KEY,
                2000,              // 優先度：標準より高めに設定
                PlayerAnimation::createLayer
        );
    }

    /**
     * PlayerAnimator がプレイヤーごとに呼び出すレイヤー生成関数
     */
    private static IAnimation createLayer(AbstractClientPlayer player) {

        // 新しい ModifierLayer を作成
        ModifierLayer<IAnimation> layer = new ModifierLayer<>();

        // デフォルト状態では「何も再生しない」を明示する
        layer.setAnimation(null);

        return layer;
    }
}
