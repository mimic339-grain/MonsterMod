package com.mimic.monstermod.boss;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.MonsterTransformation;
import com.mimic.monstermod.identity.BaseIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_BossBarStylePacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ボスに変身したプレイヤーのHPを、周囲のプレイヤー全員にボスバーとして見せる。
 *
 * 【なぜバニラのボスバーを使うのか】
 * 大人数で1体を囲んで戦うので、全員が同じ情報を同じ場所で見られる必要がある。
 * バニラの {@link ServerBossEvent} を使うと、
 *   ・誰に見せるかの管理(addPlayer/removePlayer)
 *   ・切断時の後始末
 *   ・複数のバーが出たときの縦の積み方
 * をバニラ側が面倒を見てくれる。見た目だけを差し替えれば済むので、
 * 独自のパケットでHPを配るより堅い。
 *
 * 【見た目の差し替え】
 * バニラのボスバーは色と形しか選べないので、枠のデザインだけは
 * {@link S2C_BossBarStylePacket} で別に送り、クライアント側の
 * {@link com.mimic.monstermod.client.BossBarRenderer} が自前で描いている。
 *
 * 【表示条件】
 * 変身中かつ {@link BaseIdentity#isBoss()} が true の間だけ。
 * 見えるのは同じディメンションの {@link #RANGE} ブロック以内にいるプレイヤー。
 * 変身したプレイヤー本人にも見える(本人も残りHPを把握したいため)。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public final class MonsterBossBars {

    private MonsterBossBars() {}

    /** バーが見える距離(ブロック)。ここを変えれば表示範囲が変わる */
    public static final double RANGE = 50.0;

    /** 見せる相手を計算し直す間隔(tick)。毎tick全プレイヤーを走査するのは無駄なので間引く */
    private static final int REFRESH_INTERVAL = 5;

    /** 変身しているプレイヤーのUUID → そのプレイヤーのボスバー */
    private static final Map<UUID, Bar> BARS = new HashMap<>();

    /** ボスバー1本ぶんの情報。枠デザインは作り直しの判定にも使うので持っておく */
    private static final class Bar {
        final ServerBossEvent event;
        BossBarStyle style;

        Bar(ServerBossEvent event, BossBarStyle style) {
            this.event = event;
            this.style = style;
        }
    }

    /**
     * 毎tick、変身しているかを見てバーを作る/消す/更新する。
     * 呼び出し元: Forgeのプレイヤーtick。判定は軽いので専用のイベントにはしていない。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer boss)) return;

        BaseIdentity identity = currentBossIdentity(boss);
        if (identity == null) {
            remove(boss.getUUID());
            return;
        }

        Bar bar = BARS.get(boss.getUUID());
        if (bar == null || bar.style != identity.getBossBarStyle()) {
            // 初回、または変身先が変わって枠デザインが変わったとき
            remove(boss.getUUID());
            bar = new Bar(
                    new ServerBossEvent(identity.getDisplayName(),
                            BossEvent.BossBarColor.WHITE,
                            BossEvent.BossBarOverlay.PROGRESS),
                    identity.getBossBarStyle());
            BARS.put(boss.getUUID(), bar);
        }

        // HPはバニラの実HPを使う。
        // 変身中は最大HP属性そのものが変身先の値になっているので、この2つで正しい残量になる。
        float max = boss.getMaxHealth();
        bar.event.setProgress(max <= 0 ? 0.0F : Math.max(0.0F, Math.min(1.0F, boss.getHealth() / max)));

        if (boss.tickCount % REFRESH_INTERVAL == 0) {
            refreshViewers(boss, bar);
        }
    }

    /**
     * 変身中でボス扱いの Identity を返す。そうでなければ null。
     * 死亡中はバーを出しっぱなしにしないよう除外する。
     *
     * 【LazyOptional#map を使わない理由】
     * Forge の LazyOptional#map は中で Optional.of() を呼んでいるため、
     * 渡した関数が null を返した瞬間に NullPointerException で落ちる。
     * ここは「ボスでなければ null」を返したいので、
     * resolve() で中身を取り出してから普通に null 判定する。
     */
    private static BaseIdentity currentBossIdentity(ServerPlayer player) {
        if (!player.isAlive()) return null;

        MonsterTransformation trans = player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION)
                .resolve().orElse(null);
        if (trans == null || !trans.isTransformed()) return null;

        BaseIdentity identity = trans.getIdentity();
        return (identity != null && identity.isBoss()) ? identity : null;
    }

    /**
     * 半径 {@link #RANGE} 以内のプレイヤーだけがバーを見られるようにする。
     *
     * 新しく見えるようになったプレイヤーにだけ枠デザインを送る。
     * 毎回送ると人数ぶん無駄なパケットが飛ぶので、追加した瞬間だけにしている。
     */
    private static void refreshViewers(ServerPlayer boss, Bar bar) {
        double rangeSqr = RANGE * RANGE;

        Set<ServerPlayer> want = new HashSet<>();
        for (ServerPlayer other : boss.serverLevel().players()) {
            if (other.distanceToSqr(boss) <= rangeSqr) want.add(other);
        }

        // 離れた人からは消す(getPlayers()を回しながら消すとぶつかるのでコピーしてから)
        for (ServerPlayer viewer : new ArrayList<>(bar.event.getPlayers())) {
            if (!want.contains(viewer)) bar.event.removePlayer(viewer);
        }

        // 近づいた人には見せる。枠デザインはこのタイミングで一度だけ送る
        List<ServerPlayer> current = new ArrayList<>(bar.event.getPlayers());
        for (ServerPlayer viewer : want) {
            if (current.contains(viewer)) continue;
            bar.event.addPlayer(viewer);
            ModMessages.sendToPlayer(
                    new S2C_BossBarStylePacket(bar.event.getId(), bar.style.ordinal()), viewer);
        }
    }

    /** バーを消す。見ていた全員から取り下げてから捨てる */
    private static void remove(UUID bossId) {
        Bar bar = BARS.remove(bossId);
        if (bar != null) bar.event.removeAllPlayers();
    }

    /**
     * 退出したプレイヤーの後始末。
     * 自分のバーを消すのに加えて、他人のバーの視聴者からも外す。
     * 外し忘れると切断済みのプレイヤーを掴んだままになる。
     */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        remove(player.getUUID());
        for (Bar bar : BARS.values()) {
            bar.event.removePlayer(player);
        }
    }
}
