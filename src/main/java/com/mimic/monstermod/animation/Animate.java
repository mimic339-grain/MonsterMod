package com.mimic.monstermod.animation;


import com.mimic.monstermod.network.server.S2CPlayAnimationPacket;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class Animate {

    // 💡 関数名を Animation から play に変更して一貫性を高めています (元のコードに基づき、public static void play(Player entity, String animations) とします)
    public static void play(Player entity, String animations) {
        LevelAccessor world = entity.level();
        // 1. クライアント側での処理 (プレイヤー自身のアニメーション再生)
        if (world.isClientSide() && entity instanceof AbstractClientPlayer player) {
            // Mod ID "monstermod" に紐づいたアニメーションレイヤーを取得
            ModifierLayer<IAnimation> animation = (ModifierLayer)PlayerAnimationAccess.getPlayerAssociatedData(player).get(new ResourceLocation("monstermod", "player_animation"));
            if (animation != null) {
                animation.setAnimation(new KeyframeAnimationPlayer(PlayerAnimationRegistry.getAnimation(new ResourceLocation("monstermod", animations))));
            }
        }

        // 2. サーバー側での処理 (他のクライアントへの同期)
        if (!world.isClientSide() && world instanceof ServerLevel) {
            // 💡 新しい Animations.java クラスを使用して上書き判定を取得
            boolean override = Animations.ActiveAniamtion(animations);
            S2CPlayAnimationPacket.sendToAll(entity, animations, override);
        }
    }
}