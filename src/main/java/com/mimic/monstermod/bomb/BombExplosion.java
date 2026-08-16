package com.mimic.monstermod.bomb;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * ボムが爆発したときの処理。
 *
 * 【地形破壊はバニラの爆発に任せる】
 * 自前でブロックを消すと、ドロップ・爆発耐性・他MODのブロックの扱いを全部作り直すことになる。
 * {@code Level#explode} を使えばTNTと同じ挙動がそのまま得られるので、それに乗せている。
 * 種類ごとに壊す/壊さないを {@link BombKind#breaksTerrain()} で切り替える。
 *
 * 【ダメージを自前で出す理由】
 * バニラの爆発ダメージは距離と遮蔽で減るため、
 * 「付けられた本人は必ず死ぬ」という要望を満たせない。
 * そのため本人への処理だけ別に行い、周囲への巻き込みは爆発に任せている。
 */
public final class BombExplosion {

    private BombExplosion() {}

    /** 連鎖ボムが周りのボムを巻き込む範囲の余裕(ブロック) */
    private static final double CHAIN_EXTRA_RANGE = 2.0;
    /** 連鎖で誘爆するまでの間。少しずらすと数珠つなぎに見える */
    private static final int CHAIN_DELAY_TICKS = 4;

    /**
     * エンティティに付いていたボムが爆発した。
     *
     * @param carrier ボムを付けられていた本人
     */
    public static void explodeOn(Entity carrier, BombInstance bomb) {
        if (!(carrier.level() instanceof ServerLevel level)) return;

        Vec3 at = carrier.position().add(0.0, carrier.getBbHeight() * 0.5, 0.0);
        blast(level, at, bomb);

        if (carrier instanceof LivingEntity living) {
            if (bomb.getKind().killsCarrier()) {
                // 仕掛けられた本人は必ず倒れる、という要望どおりの挙動
                living.hurt(level.damageSources().explosion(null, null), Float.MAX_VALUE);
            } else if (bomb.getKind() == BombKind.DUMMY) {
                // 偽物は死なないが、体力の半分ほどを持っていく
                living.hurt(level.damageSources().explosion(null, null),
                        Math.max(1.0F, living.getMaxHealth() * 0.5F));
            }
        }
    }

    /** 座標に仕掛けられていたボムが爆発した(ブロックボム・設置ボム) */
    public static void explodeAt(ServerLevel level, BlockPos pos, BombInstance bomb) {
        blast(level, Vec3.atCenterOf(pos), bomb);
    }

    /** 実際の爆発。地形破壊・巻き込みダメージ・連鎖をここでまとめて行う */
    private static void blast(ServerLevel level, Vec3 at, BombInstance bomb) {
        BombKind kind = bomb.getKind();
        float radius = Math.max(0.5F, bomb.getRadius());

        if (kind == BombKind.DUMMY) {
            // 偽物は地形を壊さず、音と見た目だけ派手にする
            level.explode(null, at.x, at.y, at.z, radius * 0.35F, Level.ExplosionInteraction.NONE);
            hurtNearby(level, at, radius, radius * 1.2F);
            return;
        }

        Level.ExplosionInteraction interaction = kind.breaksTerrain()
                ? Level.ExplosionInteraction.TNT
                : Level.ExplosionInteraction.NONE;
        level.explode(null, at.x, at.y, at.z, radius, interaction);

        if (kind == BombKind.CHAIN) {
            chainNearbyBombs(level, at, radius + CHAIN_EXTRA_RANGE);
        }
    }

    /**
     * 周囲に直接ダメージを与える。
     * バニラの爆発は遮蔽で大きく減衰するので、確実に効かせたいときだけ使う。
     */
    private static void hurtNearby(ServerLevel level, Vec3 at, double radius, float damage) {
        AABB area = new AABB(at.x - radius, at.y - radius, at.z - radius,
                at.x + radius, at.y + radius, at.z + radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e.position().distanceTo(at) <= radius);

        for (LivingEntity target : targets) {
            double dist = target.position().distanceTo(at);
            float falloff = (float) (1.0 - dist / radius);
            target.hurt(level.damageSources().explosion(null, null), damage * falloff);
        }
    }

    /**
     * 範囲内の他のボムを起爆させる(連鎖ボム)。
     * エンティティに付いたものとブロックに仕掛けたものの両方を巻き込む。
     */
    private static void chainNearbyBombs(ServerLevel level, Vec3 at, double range) {
        AABB area = new AABB(at.x - range, at.y - range, at.z - range,
                at.x + range, at.y + range, at.z + range);

        for (Entity e : level.getEntities(null, area)) {
            List<BombInstance> list = BombAttachment.get(e);
            if (list.isEmpty()) continue;
            for (BombInstance b : list) b.detonateIn(CHAIN_DELAY_TICKS);
            BombAttachment.set(e, list);
        }

        BombStore store = BombStore.get(level);
        for (Map.Entry<BlockPos, BombInstance> entry : store.all().entrySet()) {
            if (Vec3.atCenterOf(entry.getKey()).distanceTo(at) > range) continue;
            entry.getValue().detonateIn(CHAIN_DELAY_TICKS);
        }
        store.setDirty();

        level.playSound(null, at.x, at.y, at.z,
                SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 0.7F);
    }

    /** 起爆間近の警告音。誰の足元で鳴っているかが分かるよう、その場から鳴らす */
    public static void playBeep(Level level, Vec3 at, BombInstance bomb) {
        // ピピピという電子音はバニラの音ブロックで作れる。外部素材が要らないのでこれを使う
        level.playSound(null, at.x, at.y, at.z,
                SoundEvents.NOTE_BLOCK_BIT.get(), SoundSource.PLAYERS,
                0.8F, bomb.beepPitch());
    }

    /** 起爆直前の合図。導火線の音に切り替えて「もう手遅れ」を伝える */
    public static void playFinalWarning(Level level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z,
                SoundEvents.CREEPER_PRIMED, SoundSource.PLAYERS, 1.2F, 1.0F);
    }

    /** 仕掛けた本人にだけ分かるよう、対象へメッセージを出す用の判定 */
    public static boolean isOwner(BombInstance bomb, Player player) {
        return bomb.getOwner() != null && bomb.getOwner().equals(player.getUUID());
    }
}
