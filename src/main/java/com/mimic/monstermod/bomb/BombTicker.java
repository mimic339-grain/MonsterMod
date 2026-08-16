package com.mimic.monstermod.bomb;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.identity.bomber.BomberIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_BlockBombMarksPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
    /** 仕掛けた場所をボマーへ送る間隔。数が少ないので一覧をそのまま送る */
    private static final int MARK_SYNC_INTERVAL = 20;
    /** 前回この人に目印を送ったか。ボマーでなくなったとき、消す指示を1回だけ送るために持つ */
    private static final String TAG_HAD_MARKS = "monstermod_bomb_marks";

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
            BombExplosion.playFinalWarning(carrier.level(), at, bomb.getRadius());
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
        if (store.all().isEmpty()) {
            sendMarksToBombers(level, store); // 全部消えたことも伝える必要がある
            return;
        }

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
                BombExplosion.playFinalWarning(level, at, bomb.getRadius());
            } else if (bomb.shouldBeep()) {
                BombExplosion.playBeep(level, at, bomb);
            }
        }

        for (BlockPos pos : toExplode) {
            BombInstance bomb = store.remove(pos);
            if (bomb != null) BombExplosion.explodeAt(level, pos, bomb);
        }
        if (!toExplode.isEmpty()) store.setDirty();

        sendMarksToBombers(level, store);
    }

    /**
     * 仕掛けた場所をボマーにだけ知らせる。
     *
     * 見た目が普通のブロックのままなので、仕掛けた側も忘れると自分で踏む。
     * かといって全員に送ると罠にならないので、送る相手をここで絞っている。
     * 数が少ないので、差分ではなく一覧をそのまま定期的に送っている。
     */
    private static void sendMarksToBombers(ServerLevel level, BombStore store) {
        store.tickRevealed();
        if (level.getGameTime() % MARK_SYNC_INTERVAL != 0) return;

        List<BlockPos> secret = new ArrayList<>(store.all().keySet());
        List<BlockPos> revealed = new ArrayList<>(store.revealed().keySet());

        // 全員が対象なのは「踏まれた証拠」だけ。仕掛けの位置はボマーにしか送らない
        for (ServerPlayer player : level.players()) {
            boolean bomber = BomberIdentity.of(player) != null;
            boolean hadMarks = player.getPersistentData().getBoolean(TAG_HAD_MARKS);
            if (!bomber && !hadMarks && revealed.isEmpty()) continue;

            ModMessages.sendToPlayer(new S2C_BlockBombMarksPacket(
                    bomber ? secret : List.of(), revealed), player);
            player.getPersistentData().putBoolean(TAG_HAD_MARKS, bomber || !revealed.isEmpty());
        }
    }
}
