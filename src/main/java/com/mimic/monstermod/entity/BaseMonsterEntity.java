package com.mimic.monstermod.entity;

import com.mimic.monstermod.network.server.S2CMonsterSyncPacket;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;

import java.util.List;

import static software.bernie.geckolib.util.ClientUtils.getLevel;

/**
 * 完全版 BaseMonsterEntity
 * - GeoEntity 対応
 * - IMONSTERDATA 連動（Skill / SkillTick）
 * - 状態変化時のみクライアント同期
 * - AnimationController は GeckoLib 標準
 * - 各モンスターは decideAnimation() を override 可能
 */
public abstract class BaseMonsterEntity extends BaseEntity implements GeoEntity {

    // ==============================
    // GeckoLib Animation
    // ==============================
    protected final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    public static EntityDataAccessor<String> ANIMATION_NAME;

    // ==============================
    // サーバー同期用状態
    // ==============================
    private String currentAnim = "idle";

    // クライアント依存フラグ（WASD 判定）
    public boolean playerActiveMove = false;

    public BaseMonsterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    // -----------------------------
    // SynchedEntityData 初期化
    // -----------------------------
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        if (ANIMATION_NAME == null) {
            ANIMATION_NAME = SynchedEntityData.defineId(this.getClass(), EntityDataSerializers.STRING);
        }
        this.entityData.define(ANIMATION_NAME, "idle");
    }

    // -----------------------------
    // Capability / MonsterData
    // -----------------------------
    public IMonsterData getMonsterData() {
        return CapabilityRegistry.getMonsterData(this);
    }

    // -----------------------------
    // GeckoLib Controller
    // -----------------------------
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    // -----------------------------
    // クライアント WASD 判定フラグ操作
    // -----------------------------
    public void setPlayerActiveMove(boolean moving) {
        this.playerActiveMove = moving;
    }

    public boolean isPlayerActivelyMoving() {
        return this.playerActiveMove;
    }

    /**
     * AnimationController 用の基本 predicate
     */
    protected <T extends GeoEntity> PlayState predicate(AnimationState<T> state) {
        String animName = decideAnimation();
        state.getController().setAnimation(RawAnimation.begin().then(animName, Animation.LoopType.LOOP));
        return PlayState.CONTINUE;
    }

    /**
     * decideAnimation() は各派生クラスで override 可能
     */
    protected String decideAnimation() {
        IMonsterData data = getMonsterData();
        if (data != null && data.getSkill() != null && !data.getSkill().isEmpty()) {
            return "animation.monster." + data.getSkill();
        }
        return "animation.monster.idle";
    }

    public String getAnimation() {
        return this.entityData.get(ANIMATION_NAME);
    }

    // -----------------------------
    // Tick / SkillTick 管理（サーバー側）
    // -----------------------------
    @Override
    public void tick() {
        super.tick();

        Level lvl = getLevel();
        if (lvl == null) return;

        // サーバー専用処理
        if (!lvl.isClientSide) {
            IMonsterData data = getMonsterData();
            if (data != null) {
                data.tick();

                String nextAnim = decideAnimation();

                // アニメーションが変化した時のみクライアント同期
                if (!nextAnim.equals(currentAnim)) {
                    currentAnim = nextAnim;
                    syncToClients(data);
                }
            }
        }
    }

    // -----------------------------
    // クライアント同期
    // -----------------------------
    private void syncToClients(IMonsterData data) {
        Level lvl = getLevel();
        if (!(lvl instanceof ServerLevel serverLevel)) return;

        List<ServerPlayer> players = serverLevel.players();
        for (ServerPlayer player : players) {
            S2CMonsterSyncPacket packet = new S2CMonsterSyncPacket(
                    getId(), currentAnim, data.getSkill()
            );
            packet.sendToPlayer(player);
        }
    }

    // -----------------------------
    // NBT 保存 / 読み込み
    // -----------------------------
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AnimationName", getAnimation());
        IMonsterData data = getMonsterData();
        if (data != null) {
            tag.putString("Skill", data.getSkill());
            tag.putInt("SkillTick", data.getSkillTick());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("AnimationName")) this.entityData.set(ANIMATION_NAME, tag.getString("AnimationName"));
        IMonsterData data = getMonsterData();
        if (data != null) {
            if (tag.contains("Skill")) data.setSkill(tag.getString("Skill"));
            if (tag.contains("SkillTick")) data.setSkillTick(tag.getInt("SkillTick"));
        }
    }

    // -----------------------------
    // 属性作成
    // -----------------------------
    public static AttributeSupplier.Builder createDefaultAttributes(
            double health, double speed, double damage, double resistance, double armor, double gravity) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, resistance)
                .add(BaseMonsterEntity.GRAVITY, gravity);
    }

    // 独自GRAVITY属性
    public static final Attribute GRAVITY = Attributes.ATTACK_KNOCKBACK;
}
