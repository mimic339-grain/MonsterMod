package com.mimic.monstermod.bomb;

import com.mimic.monstermod.MonsterMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 仕掛けられたボムのカウントダウンを進める場所。
 *
 * 【エンティティとブロックで進め方が違う理由】
 * エンティティに付いたボムは、その本人が動いている間だけ進めればよいので
 * LivingTickEvent に乗せるのが一番自然で軽い。
 * ブロックに仕掛けたボムは誰も見ていなくても進む必要があるので、
 * ワールドのtickでまとめて処理する。
 *
 * 音は残りが減るほど間隔が詰まり、音程も上がる({@link BombInstance#shouldBeep()})。
 * 最後の1秒で導火線の音に切り替える。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class BombTicker {

    /** 導火線の音に切り替える残り時間 */
    private static final int FINAL_WARNING_TICKS = 20;

    // ---------------- エンティティに付いたボム ----------------

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        List<BombInstance> bombs = BombAttachment.get(entity);
        if (bombs.isEmpty()) return;

        List<BombInstance> exploded = new ArrayList<>();
        Iterator<BombInstance> it = bombs.iterator();
        while (it.hasNext()) {
            BombInstance bomb = it.next();
            if (bomb.tickDown()) {
                exploded.add(bomb);
                it.remove();
            } else {
                announce(entity, bomb);
            }
        }

        // 爆発したぶんを取り除いてから起爆する。
        // 先に爆発させると、連鎖で同じボムを二重に処理してしまう
        BombAttachment.set(entity, bombs);
        for (BombInstance bomb : exploded) BombExplosion.explodeOn(entity, bomb);
    }

    /** 音による予告。付けられた本人にも周囲にも同じように聞こえる */
    private static void announce(Entity carrier, BombInstance bomb) {
        Vec3 at = carrier.position().add(0.0, carrier.getBbHeight() * 0.5, 0.0);
        if (bomb.getFuseTicks() == FINAL_WARNING_TICKS) {
            BombExplosion.playFinalWarning(carrier.level(), at);
        } else if (bomb.shouldBeep()) {
            BombExplosion.playBeep(carrier.level(), at, bomb);
        }
    }

    // ---------------- ブロックに仕掛けたボム ----------------

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;

        BombStore store = BombStore.get(level);
        if (store.all().isEmpty()) return;

        List<BlockPos> toExplode = new ArrayList<>();

        for (Map.Entry<BlockPos, BombInstance> entry : store.all().entrySet()) {
            BombInstance bomb = entry.getValue();
            BlockPos pos = entry.getKey();

            // 読み込まれていないチャンクのボムは進めない(音も鳴らせないため)
            if (!level.isLoaded(pos)) continue;

            if (bomb.tickDown()) {
                toExplode.add(pos);
                continue;
            }
            Vec3 at = Vec3.atCenterOf(pos);
            if (bomb.getFuseTicks() == FINAL_WARNING_TICKS) {
                BombExplosion.playFinalWarning(level, at);
            } else if (bomb.shouldBeep()) {
                BombExplosion.playBeep(level, at, bomb);
            }
        }

        for (BlockPos pos : toExplode) {
            BombInstance bomb = store.remove(pos);
            if (bomb != null) BombExplosion.explodeAt(level, pos, bomb);
        }
        if (!toExplode.isEmpty()) store.setDirty();
    }
}
