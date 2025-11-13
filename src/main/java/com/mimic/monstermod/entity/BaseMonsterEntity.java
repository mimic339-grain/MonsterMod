package com.mimic.monstermod.entity;

import com.google.gson.JsonObject;
import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.util.MonsterAnimationUtil;
import com.mimic.monstermod.util.MonsterAnimationUtil.ModelBuildResult;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * BaseMonsterEntity — 完全版 YSMMOD準拠
 * 修正版：Identity 内で ensureModelInitialized を呼ばず、無限再帰防止
 */
public abstract class BaseMonsterEntity extends BaseEntity {

    @Nullable
    private BaseMonsterIdentity identity;

    @Nullable
    protected ModelPart modelRoot;

    // BoneMap 初期化済みフラグ
    private boolean boneMapInitialized = false;

    public BaseMonsterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        // client ならモデルだけ先に生成しておく
        if (level.isClientSide) {
            modelRoot = createModel();
        }
    }

    @Nullable
    public BaseMonsterIdentity getIdentity() { return identity; }

    public void setIdentity(@Nullable BaseMonsterIdentity identity) {
        this.identity = identity;
        if (identity != null) {
            MonsterMod.LOGGER.info("[BaseMonsterEntity] Identity attached: {}", identity.getId());
            identity.setEntity(this);
        }
    }

    @Nullable
    protected abstract BaseMonsterIdentity createIdentityInstance();

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (level().isClientSide && identity == null) {
            BaseMonsterIdentity auto = createIdentityInstance();
            if (auto != null) setIdentity(auto);
        }
    }

    @Nullable
    public ModelPart getModelRoot() { return modelRoot; }

    /**
     * モデル作成（JSONがあれば読み込み、なければ空モデル）
     */
    protected ModelPart createModel() {
        String modelName = this.getType().toShortString().toLowerCase();
        JsonObject json = BaseMonsterIdentity.loadModelJson(modelName);
        if (json != null) {
            ModelBuildResult result = MonsterAnimationUtil.buildModelFromJson(json, 64, 64);
            if (result != null && result.root != null) {
                return result.root;
            }
        }
        return new ModelPart(Collections.emptyList(), Collections.emptyMap());
    }

    /**
     * モデル & BoneMap の初期化（Identity 側で ensureModelInitialized を呼ばない）
     */
    public void ensureModelInitialized() {
        if (!level().isClientSide) return;

        // モデル生成
        if (modelRoot == null) {
            modelRoot = createModel();
        }

        // BoneMap 初期化（Identity があれば一度だけ）
        if (!boneMapInitialized && identity != null) {
            identity.autoInitBoneMap(this); // ここで再帰しない
            boneMapInitialized = true;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) return;

        // Identity に tick を任せる
        if (identity != null) identity.tickClient(1f / 20f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (identity != null) tag.put("IdentityData", identity.serializeNBT());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);

        if (identity == null) identity = createIdentityInstance();
        if (identity != null && tag.contains("IdentityData")) {
            identity.deserializeNBT(tag.getCompound("IdentityData"));
        }
    }

    /**
     * デフォルトの属性設定
     */
    public static AttributeSupplier.Builder createDefaultAttributes(
            double health, double speed, double damage, double resistance, double armor, double gravity
    ) {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, health)
                .add(Attributes.MOVEMENT_SPEED, speed)
                .add(Attributes.ATTACK_DAMAGE, damage)
                .add(Attributes.ARMOR, armor)
                .add(Attributes.KNOCKBACK_RESISTANCE, resistance)
                .add(GRAVITY, gravity);
    }

    public static final Attribute GRAVITY = Attributes.ATTACK_KNOCKBACK;
}
