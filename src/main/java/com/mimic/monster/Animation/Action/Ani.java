/*
package com.mimic.monster.Animation.Action;

import com.mimic.monster.MonsterMod;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import java.util.List;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.network.NetworkDirection;

public class Ani {
    // 指定したプレイヤー（entity）に対して指定したアニメーション（animations）を再生する
    public static void Animation(Player entity, String animations) {
        // プレイヤーが存在するワールドの参照を取得
        LevelAccessor world = entity.level();

        // クライアント側の処理でAbstractClientPlayerはアニメーションや見た目変更で使う
        if (world.isClientSide() && entity instanceof AbstractClientPlayer player) {
            // プレイヤーのアニメーション情報（ModifierLayer）をResourceLocation(リソースの取得) "mimic:player_animation" で取得する
            //new ResourceLocation("名前空間(MODID)", "キー");
            ModifierLayer<IAnimation> animation = (ModifierLayer)
                    PlayerAnimationAccess.getPlayerAssociatedData(player).get(new ResourceLocation("monstermod", "player_animation"));

            // もしアニメーションレイヤーが存在していれば、
            if (animation != null) {
                // ResourceLocation "mimic:" + animations で定義されるアニメーションを取得し、その結果を新しいキーフレームアニメーションプレイヤーに設定する
                animation.setAnimation(
                        new KeyframeAnimationPlayer(
                                PlayerAnimationRegistry.getAnimation(new ResourceLocation("monstermod", animations))
                        )
                );
            }
        }

        // サーバー側の処理（クライアントへアニメーション変更のパケット送信）
        if (!world.isClientSide() && world instanceof ServerLevel) {
            ServerLevel srvLvl_ = (ServerLevel) world;
            //サーバーに接続しているすべてのクライアントとの通信接続（Connection）リストを取得しています。
            List<Connection> connections = srvLvl_.getServer().getConnection().getConnections();
            //synchronizedで並列処理をして、同時アクセスされないようにする
            synchronized(connections) {
                // 各プレイヤーごとに接続ループ
                for(Connection connection : connections) {
                    //!connection.isConnecting()：まだ接続途中の人は除外して、connection.isConnected()：完全に接続が確立している人のみ対象
                    if (!connection.isConnecting() && connection.isConnected()) {
                        // サーバー側からクライアントへアニメーションを変更するパケットを送信する
                        // ・Component.literal(animations):アニメーション名（"walk"とか）を文字列として送信
                        // ・entity.getId(): 対象プレイヤーのエンティティID
                        // ・Animations.ActiveAniamtion(animations): 有効なアニメーションかどうか（true/false）
                        MonsterMod.PACKET_HANDLER.sendTo(
                                new SetupAnimationsProcedure.AnimationModAnimationMessage(
                                        Component.literal(animations),
                                        entity.getId(),
                                        Animations.ActiveAniamtion(animations)
                                ),
                                connection,//このプレイヤーのクライアントにだけ送信
                                NetworkDirection.PLAY_TO_CLIENT
                        );
                    }
                }
            }
        }
    }
}*/