package com.mimic.monstermod.npc;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NPC化したエンティティの状態を毎tick強制する。
 *
 * 【他MODのMobにも効く理由】
 * ここで使っているのは Mob#setNoAi / Entity#setNoGravity / Entity#setInvulnerable /
 * LivingEntity#setYHeadRot といった共通メソッドだけで、特定のクラスに依存しない。
 * そのため他MODが追加したMobであっても、そのMobのクラスを知らなくてもNPC化できる。
 *
 * 「毎tick強制する」方式にしているのは、他MODのAIやtick処理が
 * 位置・速度・向きを書き戻してくることがあるため。1回設定するだけでは戻されてしまう。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class NpcTickEvents {

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        if (entity instanceof Player) return; // プレイヤーは対象外

        NpcSettings s = NpcSettings.load(entity);
        if (s == null) return;

        applyStatic(entity, s);
        applyPerTick(entity, s);
    }

    /** 一度設定すれば済むもの(ただし他MODが書き戻す場合に備えて毎tick確認する) */
    private static void applyStatic(LivingEntity entity, NpcSettings s) {
        if (entity instanceof Mob mob && s.immobile() && !mob.isNoAi()) {
            mob.setNoAi(true);
        }
        if (entity.isNoGravity() != s.noGravity()) {
            entity.setNoGravity(s.noGravity());
        }
        if (entity.isInvulnerable() != s.invulnerable()) {
            entity.setInvulnerable(s.invulnerable());
        }
    }

    /** 毎tick打ち消す必要があるもの(移動・落下・水流・炎・向き) */
    private static void applyPerTick(LivingEntity entity, NpcSettings s) {
        if (s.fireProof() && entity.isOnFire()) {
            entity.clearFire();
            entity.setRemainingFireTicks(0);
        }

        if (s.immobile()) {
            // 水流やノックバック、他MODのAIによる移動を打ち消す
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hurtMarked = false;
            entity.fallDistance = 0;

            // 押し出されて位置がずれた場合は元の位置へ戻す
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

    /** 最寄りのプレイヤーへ首(と体)を向ける。AI無効でも向きだけは動かせる */
    private static void lookAtNearestPlayer(LivingEntity entity) {
        Player target = entity.level().getNearestPlayer(entity, 16.0D);
        if (target == null) return;

        double dx = target.getX() - entity.getX();
        double dz = target.getZ() - entity.getZ();
        float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;

        entity.setYRot(yaw);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);

        double dy = (target.getEyeY()) - entity.getEyeY();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        entity.setXRot((float) (-(Math.atan2(dy, horiz) * (180.0 / Math.PI))));
    }

    /** 他所からNPC設定を適用する入口(ツールから使う) */
    public static void applyNow(Entity entity, NpcSettings s) {
        NpcSettings.save(entity, s);
        if (entity instanceof Mob mob && s.immobile()) mob.setNoAi(true);
        entity.setNoGravity(s.noGravity());
        entity.setInvulnerable(s.invulnerable());
        if (s.fireProof()) entity.clearFire();
    }

    /** NPC化を解除して元の挙動に戻す */
    public static void release(Entity entity) {
        NpcSettings.clear(entity);
        if (entity instanceof Mob mob) mob.setNoAi(false);
        entity.setNoGravity(false);
        entity.setInvulnerable(false);
    }
}
