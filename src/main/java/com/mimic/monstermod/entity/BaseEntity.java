package com.mimic.monstermod.entity;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CMonsterSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;

import java.util.HashSet;
import java.util.Set;

public abstract class BaseEntity extends Mob implements GeoEntity {

    protected final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    // ==== SynchedEntityData (バニラの自動同期) ====
    private static final EntityDataAccessor<String> CURRENT_ANIMATION =
            SynchedEntityData.defineId(BaseEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> CURRENT_SKILL =
            SynchedEntityData.defineId(BaseEntity.class, EntityDataSerializers.STRING);

    // ==== 内部状態管理 (Capabilityを使わないフラグ管理) ====
    private final Set<String> activeStates = new HashSet<>();
    private int skillTick = 0; // スキルの残り時間などを保持
    private boolean initialSynced = false;
    @Override
    public float getEyeHeight(Pose pose) {
        return this.getCustomEyeHeight(this.getDimensions(pose));
    }

    // 子クラスでオーバーライドするためのメソッドを作成
    protected float getCustomEyeHeight(EntityDimensions dimensions) {
        return dimensions.height * 0.85f; // デフォルトの目線
    }
    protected EntityDimensions getCustomDimensions() {
        return EntityDimensions.fixed(0.6f, 1.8f); // デフォルト
    }
    // 寸法設定
    protected EntityDimensions monsterDimensions = EntityDimensions.fixed(0.6f, 1.8f);
    protected float monsterEyeHeight = 1.62f;

    public BaseEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    // --- 状態管理メソッド ---
    public boolean hasState(String stateKey) {
        return activeStates.contains(stateKey);
    }

    public void setState(String stateKey, boolean active) {
        if (active) {
            activeStates.add(stateKey);
        } else {
            activeStates.remove(stateKey);
        }
        // サーバー側で状態が変わった際、必要ならNBT保存対象になる
    }

    // --- スキルTick管理 ---
    public void setSkillTick(int tick) {
        this.skillTick = tick;
    }

    public int getSkillTick() {
        return this.skillTick;
    }

    // --- 同期データ定義 ---
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CURRENT_ANIMATION, "");
        this.entityData.define(CURRENT_SKILL, "");
    }

    public void setCurrentAnimation(String name) {
        this.entityData.set(CURRENT_ANIMATION, name == null ? "" : name);
    }

    public String getCurrentAnimation() {
        return this.entityData.get(CURRENT_ANIMATION);
    }

    // ================================================================
    // ボーン追従ヒットボックス用のアニメーション追跡
    // ================================================================
    // 「今どのアニメーションが何秒再生されているか」を保持する。
    // SynchedEntityDataではなくローカルに持つ理由:
    //   resolveAnimationName()が同期済みの状態から決定的に導出されるため、
    //   クライアント・サーバーがそれぞれ独立に同じ値を計算できる(同期不要)。
    private String activeAnim = "";
    private int activeAnimAnchorTick = 0;

    /**
     * 今再生されるべきアニメーション名。部位当たり判定を持つモンスターはこれをオーバーライドし、
     * 描画側(GeckoLibのpredicate)もこれを使うこと。両者が別々に名前を決めると
     * 見た目と当たり判定がズレる。
     */
    public String resolveAnimationName() {
        return "";
    }

    /**
     * アニメーション名の切り替わりを検出して経過時間の基準を更新する。
     * 通常のエンティティはtick()から、変身プレイヤーの見た目用プロキシ(ワールドに
     * 存在せずtick()が呼ばれない)は外部から毎tick呼ぶ必要がある。
     */
    public void updateActiveAnimation() {
        String resolved = resolveAnimationName();
        if (!resolved.equals(activeAnim)) {
            activeAnim = resolved;
            activeAnimAnchorTick = this.tickCount;
        }
    }

    public String getActiveAnimation() {
        return activeAnim;
    }

    /** 現在のアニメーションが再生され始めてから何秒経過したか(ループなら周回込み) */
    public double getAnimationElapsedSeconds(com.mimic.monstermod.entity.hitbox.BoneRigData rig) {
        return getAnimationElapsedSeconds(rig, 0f);
    }

    /**
     * partialTick込みの経過秒。
     *
     * 【なぜpartialTickが要るか】
     * GeckoLibはエンティティのアニメーション時刻に tickCount + partialTick を使い
     * (GeoModel#handleAnimations)、毎フレーム滑らかに姿勢を更新する。
     * 一方こちらが整数tickだけで計算すると1tickに1回しか動かないため、
     * 描画されるモデルと最大1tick分ずれて見える。
     * 描画(デバッグ表示)では必ずpartialTickを渡してモデルと一致させること。
     *
     * 当たり判定の実体は毎tick更新のままでよい(バニラのエンティティ判定も
     * tick単位で、描画だけが補間される。これはMinecraftとして正常な挙動)。
     */
    public double getAnimationElapsedSeconds(com.mimic.monstermod.entity.hitbox.BoneRigData rig, float partialTick) {
        if (activeAnim.isEmpty()) return 0.0;

        double elapsed = Math.max(0.0, (this.tickCount - activeAnimAnchorTick) + partialTick) / 20.0;
        double length = rig.getAnimationLength(activeAnim);
        if (length > 0 && rig.isLooping(activeAnim)) {
            elapsed = elapsed % length;
        }
        return elapsed;
    }

    public void setCurrentSkill(String name) {
        this.entityData.set(CURRENT_SKILL, name == null ? "" : name);
    }

    public String getCurrentSkill() {
        return this.entityData.get(CURRENT_SKILL);
    }

    // --- 基本パラメータ ---
    // --- 移動状態 ---
    private boolean playerActiveMove = false;
    public void setPlayerActiveMove(boolean moving) { this.playerActiveMove = moving; }
    public boolean isPlayerActivelyMoving() { return this.playerActiveMove; }

    // --- GeckoLib ---
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
    @Override
    public abstract void registerControllers(software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar controllers);

    // --- Tick処理 ---
    @Override
    public void tick() {
        super.tick();

        // 初回スポーン時の同期
        if (!level().isClientSide && !initialSynced) {
            initialSynced = true;
            syncMonsterState();
        }

        // スキルTickのカウントダウン（IMonsterData.tick()の代わり）
        if (!level().isClientSide) {
            if (this.skillTick > 0) {
                this.skillTick--;
            }
        }
    }
    // デフォルトでON。特定のEntityだけ消したいならそのEntityでfalseを返すようoverrideする
    public boolean shouldShowHitbox() {
        return true;
    }
    public void syncMonsterState() {
        if (level().isClientSide || !(level() instanceof ServerLevel)) return;

        // IMonsterDataを介さず、自身のフィールドから直接送る
        ModMessages.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                new S2CMonsterSyncPacket(getId(), getCurrentAnimation(), getCurrentSkill()));
    }

    // ==== NBT保存・読込 (IMonsterDataの保存処理をここに統合) ====
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("AnimationName", getCurrentAnimation());
        tag.putString("SkillName", getCurrentSkill());
        tag.putInt("SkillTick", this.skillTick);

        // activeStatesをNBTに書き出し
        ListTag statesList = new ListTag();
        for (String state : activeStates) {
            statesList.add(StringTag.valueOf(state));
        }
        tag.put("ActiveStates", statesList);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setCurrentAnimation(tag.getString("AnimationName"));
        setCurrentSkill(tag.getString("SkillName"));
        this.skillTick = tag.getInt("SkillTick");

        // activeStatesをNBTから読み込み
        this.activeStates.clear();
        if (tag.contains("ActiveStates", Tag.TAG_LIST)) {
            ListTag statesList = tag.getList("ActiveStates", Tag.TAG_STRING);
            for (int i = 0; i < statesList.size(); i++) {
                activeStates.add(statesList.getString(i));
            }
        }
    }

    // ==== 属性生成 ====
    public static AttributeSupplier.Builder createDefaultAttributes(
            double health, double speed, double damage, double resistance, double armor) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, resistance);
    }

    public float getStepHeightValue() {
        return 1.0f;
    }
}