package com.mimic.monstermod.block;

import com.mimic.monstermod.bomb.BombExplosion;
import com.mimic.monstermod.bomb.BombInstance;
import com.mimic.monstermod.bomb.BombKind;
import com.mimic.monstermod.bomb.BombTiming;
import com.mimic.monstermod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 設置された大型ボムの中身。
 *
 * 【SavedData ではなく BlockEntity にした理由】
 * 爆発半径の球をその場に置かれている間ずっと、しかも周りの全員に見せたい。
 * 座標だけをサーバーで持つ方式(BombStore)だとクライアントが何も知らないので描けない。
 * BlockEntity なら状態がそのままクライアントへ同期されるので、
 * 誰から見ても「今そこに何秒のボムがあるか」が分かる。
 *
 * 置いた直後はまだ時間が決まっておらず(armed=false)、
 * 右クリックで時間を選んで初めてカウントが始まる。
 */
public class PlacedBombBlockEntity extends BlockEntity {

    private int fuseTicks;
    private int totalTicks;
    private float radius;
    private boolean armed;
    private UUID owner;
    /** どの種類のボムとして爆発するか。見た目は同じでも中身が違う */
    private BombKind kind = BombKind.PLACED;

    public PlacedBombBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLACED_BOMB.get(), pos, state);
    }

    // ---------------- 状態 ----------------

    public boolean isArmed() { return armed; }
    public int getFuseTicks() { return fuseTicks; }
    public float getRadius() { return radius; }
    public UUID getOwner() { return owner; }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    public BombKind getKind() { return kind; }

    public void setKind(BombKind kind) {
        this.kind = kind;
        sync();
    }

    /** 時間を決めてカウントを開始する。長いほど爆発半径が大きい */
    public void startTimer(int seconds) {
        this.totalTicks = Math.max(1, seconds * BombTiming.TICKS_PER_SECOND);
        this.fuseTicks = this.totalTicks;
        this.radius = BombTiming.radiusForFuse(this.totalTicks);
        this.armed = true;
        sync();
    }

    /** 火打ち石での即爆。自爆覚悟の早撃ち */
    public void detonateNow() {
        if (!armed) {
            // まだ時間を決めていない場合も、最低限の威力で爆発させる
            this.totalTicks = 1;
            this.radius = BombTiming.radiusForFuse(30 * BombTiming.TICKS_PER_SECOND);
            this.armed = true;
        }
        this.fuseTicks = 1;
        sync();
    }

    /** 変更をクライアントへ流す。これをしないと球の表示が更新されない */
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ---------------- 進行 ----------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, PlacedBombBlockEntity be) {
        if (!be.armed || !(level instanceof ServerLevel serverLevel)) return;

        be.fuseTicks--;

        Vec3 at = Vec3.atCenterOf(pos);
        if (be.fuseTicks <= 0) {
            // 先にブロックを消してから爆発させる。
            // 残したまま爆発させると、破壊不可の設定のせいでその場に残ってしまう
            level.removeBlock(pos, false);
            BombExplosion.explodeAt(serverLevel, pos,
                    new BombInstance(be.kind, be.owner, 1, be.radius, true));
            return;
        }

        // 音は残りが減るほど詰まっていく。BombInstance と同じ間隔の決め方を使う
        BombInstance view = new BombInstance(be.kind, be.owner, be.fuseTicks, be.radius, true);
        if (be.fuseTicks == 20) {
            BombExplosion.playFinalWarning(level, at, be.radius);
        } else if (shouldBeep(be)) {
            BombExplosion.playBeep(level, at, view);
        }
    }

    /**
     * 残り時間に応じて間隔が詰まる点滅音。
     * 残り10秒を切るまでは鳴らさない(最初から鳴っていると気付かれるし、長く鳴り続けてうるさい)。
     */
    private static boolean shouldBeep(PlacedBombBlockEntity be) {
        if (be.fuseTicks > BombInstance.BEEP_START_TICKS) return false;
        float progress = 1.0F - (float) be.fuseTicks / (float) BombInstance.BEEP_START_TICKS;
        // 最後は毎tick鳴らして「チチチチ」と繋がって聞こえるようにする
        int interval = Math.max(1, Math.round(20 - progress * 19));
        return be.fuseTicks % interval == 0;
    }

    // ---------------- 保存・同期 ----------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("fuse", fuseTicks);
        tag.putInt("total", totalTicks);
        tag.putFloat("radius", radius);
        tag.putBoolean("armed", armed);
        tag.putString("kind", kind.name());
        if (owner != null) tag.putUUID("owner", owner);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        fuseTicks = tag.getInt("fuse");
        totalTicks = Math.max(1, tag.getInt("total"));
        radius = tag.getFloat("radius");
        armed = tag.getBoolean("armed");
        kind = BombKind.byName(tag.getString("kind"));
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    /** 最初にチャンクが届いたときに渡す内容 */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    /** 途中で変わったときにクライアントへ送る内容 */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * 描画の対象範囲。
     * 爆発半径の球はブロックより遥かに大きいので、その分まで広げておかないと
     * ブロック自体が画面外に出た瞬間に球ごと消えてしまう。
     */
    @Override
    public AABB getRenderBoundingBox() {
        double r = Math.max(2.0, radius) + 1.0;
        return new AABB(worldPosition).inflate(r);
    }
}
