package com.mimic.monstermod.npc;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NPC化したエンティティの挙動を強制する。
 *
 * 【他MODのMobにも効く理由】
 * 使っているのは Mob#setNoAi / Entity#setNoGravity / setInvulnerable /
 * LivingEntity#setYHeadRot / LivingChangeTargetEvent といった共通の仕組みだけで、
 * 特定のクラスに依存しない。そのため他MODのMobでもクラスを知らずにNPC化できる。
 *
 * 【毎tick強制する理由】
 * 他MODのAIやtick処理が位置・速度・向き・ターゲットを書き戻すことがあるため、
 * 一度設定するだけでは元に戻されてしまう。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class NpcTickEvents {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity instanceof Player) return;

        NpcSettings s = NpcSettings.load(entity);
        if (s == null) return;

        applyStatic(entity, s);
        applyPerTick(entity, s);
    }

    /**
     * 攻撃対象の選定を制御する。
     * ここを押さえることで「襲ってこないNPC」「殴られたら反撃するNPC」を作り分けられる。
     */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        NpcSettings s = NpcSettings.load(entity);
        if (s == null) return;

        LivingEntity target = event.getNewTarget();
        if (!(target instanceof Player)) return; // プレイヤー以外への敵対はそのまま

        if (s.attacksPlayers()) return; // 自分から襲うNPCなら何もしない

        // 反撃が許可されていて、実際にその相手から攻撃されている場合のみ許可する
        if (s.allowRetaliation() && entity.getLastHurtByMob() == target) return;

        event.setNewTarget(null);
    }

    /** 一度設定すれば済むもの(書き戻される場合に備えて毎tick確認する) */
    private static void applyStatic(LivingEntity entity, NpcSettings s) {
        if (entity instanceof Mob mob) {
            if (s.isFixed()) {
                // 固定NPCはAIごと止める
                if (!mob.isNoAi()) mob.setNoAi(true);
            } else {
                // 徘徊NPCは元のAI(追跡・攻撃を含む)を捨てて、こちらの徘徊AIに差し替える。
                // ターゲットを消すだけでは追いかけ回す挙動が残るため
                if (mob.isNoAi()) mob.setNoAi(false);
                NpcAiUtil.applyWanderAi(mob);
            }
        }
        // 落下・押し出しを防ぐのは固定NPCのみ
        boolean wantNoGravity = s.isFixed();
        if (entity.isNoGravity() != wantNoGravity) entity.setNoGravity(wantNoGravity);

        if (entity.isInvulnerable() != s.invulnerable()) entity.setInvulnerable(s.invulnerable());
    }

    /** 毎tick打ち消す必要があるもの */
    private static void applyPerTick(LivingEntity entity, NpcSettings s) {
        // 襲わない設定のNPCが敵対状態のまま残らないようにする
        if (!s.attacksPlayers() && entity instanceof Mob mob && mob.getTarget() instanceof Player p) {
            boolean retaliating = s.allowRetaliation() && entity.getLastHurtByMob() == p;
            if (!retaliating) mob.setTarget(null);
        }

        if (s.isFixed()) {
            // 水流・ノックバック・他MODのAIによる移動を打ち消す
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hurtMarked = false;
            entity.fallDistance = 0;

            double dx = entity.getX() - s.anchorX();
            double dy = entity.getY() - s.anchorY();
            double dz = entity.getZ() - s.anchorZ();
            if (dx * dx + dy * dy + dz * dz > 1.0E-4) {
                entity.teleportTo(s.anchorX(), s.anchorY(), s.anchorZ());
            }
        }

        switch (s.lookMode()) {
            case LOOK_PLAYER -> lookAtNearestPlayer(entity);
            case FIXED -> {
                entity.setYRot(s.fixedYaw());
                entity.setYHeadRot(s.fixedYaw());
                entity.setYBodyRot(s.fixedYaw());
                entity.setXRot(0);
            }
            case FREE -> { }
        }
    }

    /** 最寄りのプレイヤーへ首と体を向ける。AIを止めていても向きだけは動かせる */
    private static void lookAtNearestPlayer(LivingEntity entity) {
        Player target = entity.level().getNearestPlayer(entity, 16.0D);
        if (target == null) return;

        double dx = target.getX() - entity.getX();
        double dz = target.getZ() - entity.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;

        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);

        double dy = target.getEyeY() - entity.getEyeY();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        entity.setXRot((float) (-(Math.atan2(dy, horiz) * (180.0 / Math.PI))));
    }

    /** NPC設定を適用する(ツールから使う) */
    public static void applyNow(Entity entity, NpcSettings s) {
        NpcSettings.save(entity, s);
        if (entity instanceof Mob mob) {
            mob.setNoAi(s.isFixed());
            if (!s.isFixed()) NpcAiUtil.applyWanderAi(mob);
            if (!s.attacksPlayers()) mob.setTarget(null);
        }
        entity.setNoGravity(s.isFixed());
        entity.setInvulnerable(s.invulnerable());
    }

    /** NPC化を解除して元の挙動へ戻す */
    public static void release(Entity entity) {
        NpcSettings.clear(entity);
        if (entity instanceof Mob mob) {
            mob.setNoAi(false);
            NpcAiUtil.clearMark(mob);
        }
        entity.setNoGravity(false);
        entity.setInvulnerable(false);
    }
}
