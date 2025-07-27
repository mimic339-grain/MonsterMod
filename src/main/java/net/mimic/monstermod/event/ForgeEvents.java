package net.mimic.monstermod.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.identity.impl.MimicIdentity;
import net.mimic.monstermod.entity.custom.MimicEntity; // MimicEntityをインポート

/**
 * Forgeのゲームプレイイベントを処理するクラス。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class ForgeEvents {

    // プレイヤーがティックするたびにCapabilityをチェックし、Mimic固有のロジックを適用
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side.isServer()) {
            Player player = event.player;
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    IPlayerIdentity currentIdentity = transformation.getTransformedIdentity();

                    if (currentIdentity instanceof MimicIdentity) {
                        // Mimic固有のロジックをここに追加
                        // 例: 飛行状態の維持
                        if (transformation.isTransformed() && transformation.getTransformedMobId().equals(MimicIdentity.ID)) {
                            // 飛行能力を付与（ForgeEventでやるのが望ましいが、簡易的にここで）
                            player.getAbilities().mayfly = true;
                            player.onUpdateAbilities(); // アビリティの変更をクライアントに同期
                        }

                        // Mimicが噛みつきアニメーション中でない場合、isBitingをfalseにリセット
                        // クライアント側でアニメーションが終了したらisBitingをfalseにするべきだが、
                        // サーバー側で強制的にリセットするロジック（例：一定時間後）を入れることも可能
                        // 現在はMimicEntityのanimation listenerで管理されるのが理想
                        // 例: if (transformation.isBiting() && !MimicBiteAnimationIsPlaying) { transformation.setBiting(false); }
                    }

                    // ★重要: 動的状態（アニメーション、噛みつき）はCapabilityから直接取得する
                    // if (transformation.getMimicState() == MimicEntity.MimicAnimationState.OPENING) {
                    //     // アニメーションの進行状況に応じて状態を更新するロジック
                    //     // これは主にクライアントサイドのpredicateでGeckoLibが処理すべき
                    //     // サーバーは最終的な状態のみを管理すべき
                    // }
                } else {
                    // 変身していない場合、飛行能力をリセット
                    if (player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false; // 飛行状態もリセット
                        player.onUpdateAbilities();
                    }
                }
            });
        }
    }
}