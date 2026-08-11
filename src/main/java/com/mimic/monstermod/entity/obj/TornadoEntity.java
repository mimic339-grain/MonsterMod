package com.mimic.monstermod.entity.obj;

import com.mimic.monstermod.particle.Tornado.TornadoParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class TornadoEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_SIZE = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_LIFE = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_IS_CENTER = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.BOOLEAN);

    private LivingEntity owner;
    private float damage;

    public TornadoEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_SIZE, 3.0f);
        this.entityData.define(DATA_LIFE, 200);
        this.entityData.define(DATA_IS_CENTER, false);
    }

    public void setProperties(LivingEntity owner, int life, float size, float damage, boolean isCenter) {
        this.owner = owner;
        this.damage = damage;
        this.entityData.set(DATA_SIZE, size);
        this.entityData.set(DATA_LIFE, life);
        this.entityData.set(DATA_IS_CENTER, isCenter);
    }

    @Override
    public void tick() {
        super.tick();

        float currentSize = this.entityData.get(DATA_SIZE);
        int currentLife = this.entityData.get(DATA_LIFE);
        boolean currentIsCenter = this.entityData.get(DATA_IS_CENTER);

        // --- クライアント側：密度の高い円柱の壁を構築 ---
        if (this.level().isClientSide) {
            spawnTornadoParticles(currentSize);
            return;
        }

        // --- サーバー側：ロジック ---
        // (以前はこのガードが無く、移動処理とダメージ判定がクライアント側でも
        //  実行されてしまっていた。BigTornadoEntityと同じくサーバー限定にする)
        if (this.tickCount > currentLife || (owner != null && !owner.isAlive())) {
            this.discard();
            return;
        }

        // 1. 移動：ターゲットがいれば追従、いなければ静止
        if (!currentIsCenter) {
            Player target = this.level().getNearestPlayer(this, 20.0D);
            if (target != null && target != owner) {
                Vec3 dir = target.position().subtract(this.position()).normalize().scale(0.25);
                this.setDeltaMovement(dir.x, 0, dir.z);
            }
        }
        this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());

        // 2. ダメージ判定（円柱範囲）
        applyStormEffect(currentSize);
    }

    private void spawnTornadoParticles(float radius) {
        int streamCount = 120;

        if (this.tickCount == 1 || (this.tickCount > 1 && this.tickCount % 180 == 0)) {
            for (int i = 0; i < streamCount; i++) {
                double startAngle = (Math.PI * 2 / streamCount) * i;
                this.level().addParticle(new TornadoParticleOptions(1.0f),
                        this.getX(), this.getY(), this.getZ(),
                        startAngle, radius, 0);
            }
        }
    }
    private void applyStormEffect(float radius) {
        float height = 30.0f;
        AABB hitArea = this.getBoundingBox().inflate(radius + 7.0, height + 10.0, radius + 7.0);
        List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitArea, e -> e != owner && e.isAlive());

        for (LivingEntity target : targets) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double dy = target.getY() - this.getY();
            double horizontalDist = Math.sqrt(dx * dx + dz * dz);

            float currentRadiusAtHeight = calculateRadiusAtHeight((float)dy);

            if (horizontalDist <= currentRadiusAtHeight + 2.5) {
                // 1. ダメージ処理
                if (this.tickCount % 5 == 0) {
                    target.hurt(this.damageSources().magic(), 0.5f);
                }

                Vec3 currentVec = target.getDeltaMovement();

                // 2. フェーズ分岐
                if (dy < height - 2.0f) {
                    // 【フェーズA: 吸い込み・回転・上昇】
                    Vec3 toCenter = new Vec3(dx, 0, dz).normalize();

                    // a) 吸い込み
                    double pullFactor = 0.15;
                    Vec3 pull = toCenter.scale(-pullFactor);

                    // b) 回転
                    double rotateSpeed = 0.5;
                    Vec3 tangent = new Vec3(-dz, 0, dx).normalize().scale(rotateSpeed);

                    // c) 上昇 (ここを強化)
                    // 重力を完全に無視して、一定の速度で確実に上へ運ぶ
                    double liftSpeed = 0.5;

                    // 合成: XZは慣性を少し混ぜて滑らかに、Yは強制
                    target.setDeltaMovement(
                            currentVec.x * 0.5 + (pull.x + tangent.x),
                            liftSpeed,
                            currentVec.z * 0.5 + (pull.z + tangent.z)
                    );

                    if (target instanceof Player player) {
                        player.setYRot(player.getYRot() + 12.0f);
                    }

                } else {
                    // 【フェーズB & C: 頂上での滞留と超強力放出】

                    // 1. 頂上での超高速回転（放出前の溜め）
                    double topRotateSpeed = 1.2; // 回転をさらに速く
                    target.setDeltaMovement(
                            (-dz / horizontalDist) * topRotateSpeed,
                            0.02, // ほとんど上昇せず、その場にとどまって回る
                            (dx / horizontalDist) * topRotateSpeed
                    );

                    // 2. 放出ロジック
                    // 20チックに1回だとタイミングが合わない場合があるため、
                    // ターゲットごとに「ちょうど良い高さ」に来た瞬間に弾き飛ばす判定を強めます
                    if (target.tickCount % 20 == 0) {
                        // 【フェーズC: 超強力バースト放出】

                        // 外側へ向かうベクトルを大幅に強化 (2.2 -> 5)
                        // さらに y 方向も 1.0 -> 1.5 に強化して高く飛ばす
                        Vec3 launch = new Vec3(dx, 0, dz).normalize().scale(5).add(0, 1.5, 0);

                        target.setDeltaMovement(launch);

                        // クライアント側へ移動を即時同期させる
                        target.hurtMarked = true;

                        // 確実に竜巻の判定から引き剥がすための強制テレポート
                        // 竜巻の中心から少し離れた空中に飛ばす
                        target.teleportTo(target.getX() + launch.x, this.getY() + height + 2.0, target.getZ() + launch.z);
                    }
                }

                target.hurtMarked = true;
                target.fallDistance = 0;
            }
        }
    }
    public float getScale() {
        return this.entityData.get(DATA_SIZE);
    }
    /**
     * パーティクルの計算式と同期させて、高さに応じた現在の半径を取得
     */
    private float calculateRadiusAtHeight(float h) {
        float tornadoHeight = 30.0f;
        if (h < 0) return 3.0f;
        if (h > tornadoHeight) return 15.0f;

        float hProgress = h / tornadoHeight;
        return 3.0f + ((float) Math.pow(hProgress, 4.0) * 12.0f);
    }

    @Override public Packet<ClientGamePacketListener> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) { }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { }
}