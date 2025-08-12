/*
package com.mimic.monster.Animation.Action;

import com.mimic.monster.MonsterMod;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationFactory;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkEvent;

// Forgeに「イベントを自動で登録するクラスです」と伝えるアノテーション
@EventBusSubscriber(
        modid = "monstermod",
        bus = Bus.MOD,//mod読み込み時に反応、Bus.Forgeではイベント時に反応
        value = {Dist.CLIENT}//	クライアント側のみ読み込み
)
//登録・再生・パケット処理をまとめたもの
public class SetupAnimationsProcedure {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        //プレイヤーごとにアニメーションのデータを登録"MODID"、"登録ID"、1000:優先度（通常は大きな影響なし）registerPlayerAnimations は実際にアニメーションデータを生成するメソッド
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(new ResourceLocation("monstermod", "player_animation"), 1000, SetupAnimationsProcedure::registerPlayerAnimations);
    }
    //IAnimation=アニメーションのインターフェス（制御や再生）、ModifierLayerはアニメーションを切り替えたり、上書きできるレイヤー
    private static IAnimation registerPlayerAnimations(AbstractClientPlayer player) {
        return new ModifierLayer();
    }

    //雛形でイベントが来てないときでも処理したいときに書く（今はなにもない）
    public static void execute() {
        execute((Event)null);
    }
    //雛形でイベントが来たときの処理をかく（今はなにもない）
    private static void execute(@Nullable Event event) {
    }

    @EventBusSubscriber(
            bus = Bus.MOD
    )
    //アニメーション再生をクライアントに指示するための「パケット」処理をまとめたもの（アニメーションの送受信）
    public static class AnimationModAnimationMessage {
        Component animation;//再生するアニメーション
        int target;//エンティティID
        boolean override;//再生中にoverride可能かどうか

        public AnimationModAnimationMessage(Component animation, int target, boolean override) {
            this.animation = animation;
            this.target = target;
            this.override = override;
        }
        //bufferは送られてきた情報を読み解くためのもの（受信用）
        public AnimationModAnimationMessage(FriendlyByteBuf buffer) {
            this.animation = buffer.readComponent();
            this.target = buffer.readInt();
            this.override = buffer.readBoolean();
        }
        //パケットを送信するためにデータを書き込む（送信用）
        public static void buffer(AnimationModAnimationMessage message, FriendlyByteBuf buffer) {
            buffer.writeComponent(message.animation);
            buffer.writeInt(message.target);
            buffer.writeBoolean(message.override);
        }
        //AnimationModAnimationMessageで受け取ったアニメーションの再生処理
        public static void handler(AnimationModAnimationMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            //ネットで誰から送られたかなどの情報取得
            NetworkEvent.Context context = contextSupplier.get();
            //メインスレッドの処理をラムダ式で簡潔に
            context.enqueueWork(() -> {
                //ワールド情報の取得
                Level level = Minecraft.getInstance().level;
                //プレイヤー、アニメーションなどの情報取得
                if (level.getEntity(message.target) instanceof AbstractClientPlayer player) {
                    ModifierLayer<IAnimation> animation = (ModifierLayer<IAnimation>)
                            PlayerAnimationAccess.getPlayerAssociatedData(player)
                                    .get(new ResourceLocation("monstermod", "player_animation"));
                    //もしアニメーションが正しく取得していれば、上書きオッケで再生されてないときに
                    if (animation != null && (message.override || !animation.isActive())) {
                        //アニメーションレイヤーに新しいアニメーションをセットする
                        animation.setAnimation(
                                new KeyframeAnimationPlayer(
                                        //登録されたアニメーションを取得してKEYFRAMEで再生準備
                                        PlayerAnimationRegistry.getAnimation(new ResourceLocation("monstermod", message.animation.getString()))
                                )
                        );
                    }
                }
            });
            //この処理完了したぜメッセージをFORGEに送る
            context.setPacketHandled(true);
        }
        //MOD起動時にFORGEに登録
        @SubscribeEvent
        // FMLCommonSetupEvent 発生時に以下の処理を行わせる
        public static void registerMessage(FMLCommonSetupEvent event) {
            MonsterMod.addNetworkMessage(
                    AnimationModAnimationMessage.class,
                    AnimationModAnimationMessage::buffer,
                    AnimationModAnimationMessage::new,
                    AnimationModAnimationMessage::handler);
        }
    }
}
*/