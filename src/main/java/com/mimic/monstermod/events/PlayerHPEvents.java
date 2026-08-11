package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.command.ModGameRules;
import com.mimic.monstermod.util.MonsterTransformUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class PlayerHPEvents {

    /**
     * 変身中のHPスナップショットを毎tick更新する。
     *
     * 【設計】変身中の「今のHP」はバニラの player.getHealth() を唯一の正とする。
     * バニラが自動でクライアントへ同期するため、表示も自前同期なしで常に正しくなる。
     * Capabilityが持つHPマップは「別のIdentityへ切り替えた際に元のHPへ戻すための控え」
     * であって、表示や判定に使う値ではない。
     *
     * 【以前の不具合】LivingHurtEvent / LivingHealEvent の中で player.setHealth() を
     * 呼んでいたため、その直後にバニラが再度ダメージを適用して二重に減っていた。
     * さらに表示(MonsterHpOverlay)は控えの値だけを見ており、その控えはダメージ時に
     * クライアントへ同期されないため、
     * 「表示は300のまま変わらないのに実HPは減っていて0で死ぬ」状態になっていた。
     */
    @SubscribeEvent
    public static void onPlayerTickHpSnapshot(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            if (trans.isTransformed()) {
                trans.setIdentityHP(trans.getHpKey(), player.getHealth());
            } else {
                trans.setHumanHP(player.getHealth());
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MonsterTransformUtil.saveAllToNBT(sp);
            MonsterMod.LOGGER.info("[LogoutSave] {} のデータを保存しました。", sp.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            boolean keepTransform = player.level().getGameRules().getBoolean(ModGameRules.RULE_DEATH_KEEP_TRANSFORM);
            boolean resetHp = player.level().getGameRules().getBoolean(ModGameRules.RULE_DEATH_RESET_HP);

            if (!keepTransform) {
                trans.stopTransformation(player);
            }

            // 【重要】PlayerEvent.Clone の copyCaps は deserializeNBT を通すため、
            // この時点では isTransformed が true でも identity は null になっている。
            // 以前はここで trans.getIdentity().getId() を無防備に呼んでおり、
            // NullPointerException でリスポーン処理が中断して復活できなくなっていた。
            // HPのキーは identity ではなく getHpKey() から引けるので null 安全。
            String hpKey = trans.getHpKey();

            if (resetHp) {
                MonsterTransformUtil.resetPlayerHP(player);
                MonsterTransformUtil.resetIdentityHP(player);
            } else {
                if (keepTransform && trans.isTransformed()) {
                    double currentHP = MonsterTransformUtil.getIdentityHP(player, hpKey);
                    if (currentHP <= 0) {
                        MonsterTransformUtil.setIdentityHP(player, hpKey, MonsterTransformUtil.getIdentityMaxHP(player));
                    }
                } else if (MonsterTransformUtil.getPlayerHP(player) <= 0) {
                    MonsterTransformUtil.resetPlayerHP(player);
                }
            }

            // 属性の適用(この中で trans.onLoad が走り Entity/Identity が再生成される)
            MonsterTransformUtil.applyFullTransformation(player, trans);

            double finalHP = trans.isTransformed()
                    ? MonsterTransformUtil.getIdentityHP(player, hpKey)
                    : MonsterTransformUtil.getPlayerHP(player);

            // 復活直後にHPが0だと即死してリスポーンを繰り返すため、必ず正の値にする
            if (finalHP <= 0) finalHP = player.getMaxHealth();
            player.setHealth((float) finalHP);

            if (player instanceof ServerPlayer sp) {
                // ★ 既存の syncToClient を使用。内部で S2CTransformSyncPacket が飛ぶ
                trans.syncToClient(sp);
                MonsterTransformUtil.saveAllToNBT(player);
            }
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player oldPlayer = event.getOriginal();
        Player newPlayer = event.getEntity();
        oldPlayer.reviveCaps();
        try {
            CapabilityRegistry.copyCaps(oldPlayer, newPlayer);
        } finally {
            // reviveCaps したら必ず invalidateCaps で元に戻すこと。
            // 戻し忘れると古いプレイヤーのCapabilityが生き続けてリークになる。
            // またコピー中に例外が出てもリスポーンを止めないよう finally で行う。
            oldPlayer.invalidateCaps();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        // 1. 変身状態・HPはMonsterTransformation Capability(ICapabilitySerializable)により
        //    Forgeの標準プレイヤーロード処理で既に復元済み。Attributeスナップショットのみ別途復元する。
        MonsterTransformUtil.loadAllFromNBT(sp);

        sp.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
            // 2. deserializeNBTで復元済みのフラグ(isTransformed/mobId)を基に
            //    Entity/Identityインスタンスを再構築する(ID: null 解消)
            trans.onLoad(sp);

            // 3. ロードされたデータに基づき属性を再適用
            MonsterTransformUtil.applyFullTransformation(sp, trans);

            // 4. クライアント全体へ同期
            trans.syncToClient(sp);
            trans.syncToAllClients(sp);

            MonsterMod.LOGGER.info("[LoginLoad] {} transformation restored: {}", sp.getName().getString(), trans.isTransformed());
        });
    }
}