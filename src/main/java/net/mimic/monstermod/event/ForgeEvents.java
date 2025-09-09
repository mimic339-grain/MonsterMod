package net.mimic.monstermod.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.identity.impl.MimicIdentity;


/**
 * Forgeのゲームプレイイベントを処理するクラス。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class ForgeEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.side.isServer()) {
            Player player = event.player;
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    IPlayerIdentity currentIdentity = transformation.getTransformedIdentity();

                    if (currentIdentity instanceof MimicIdentity) {
                        // Mimic固有のロジック
                        if (transformation.getTransformedMobId().equals(MimicIdentity.IDENTITY_ID)) {
                            // 飛行能力を付与
                            player.getAbilities().mayfly = true;
                            player.onUpdateAbilities(); // アビリティ変更をクライアントに同期
                        }

                        // ここで噛みつきアニメーションやその他動的状態の管理も可能
                        // 例: if (transformation.isBiting() && !MimicBiteAnimationIsPlaying) { transformation.setBiting(false); }
                    }

                } else {
                    // 変身解除時に飛行能力をリセット
                    if (player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                    }
                }
            });
        }
    }

    //ノックバック耐性を変身中適用
    @SubscribeEvent
    public void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof Player player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isNoKnockback()) {
                    event.setCanceled(true); // ノックバック無効
                }
            });
        }
    }

}
