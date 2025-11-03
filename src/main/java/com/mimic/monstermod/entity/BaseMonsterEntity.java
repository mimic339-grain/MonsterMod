package com.mimic.monstermod.entity;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * BaseMonsterEntity 完全版（GeoJSONモデル対応）
 *
 * - 描画専用ダミーEntity
 * - BaseMonsterIdentity / AnimationPlayerTemplate 互換
 * - ensureModelInitialized()でGeoJSONモデル自動読み込み
 */
public abstract class BaseMonsterEntity extends BaseEntity {

    @Nullable
    private BaseMonsterIdentity identity;

    // 描画モデル root
    protected ModelPart modelRoot;

    public BaseMonsterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);

        if (level.isClientSide) {
            ensureModelInitialized(); // ★ 起動時に初期化
        }
    }

    /** Identity getter */
    @Nullable
    public BaseMonsterIdentity getIdentity() {
        return identity;
    }

    /** Identity setter */
    public void setIdentity(@Nullable BaseMonsterIdentity identity) {
        this.identity = identity;
        if (identity != null) {
            MonsterMod.LOGGER.info("[BaseMonsterEntity] Identity attached: " + identity.getId());
            identity.setEntity(this);
            identity.autoInitBoneMap(this);
            ensureModelInitialized(); // モデルも確実にロード
        } else {
            MonsterMod.LOGGER.warn("[BaseMonsterEntity] Identity cleared");
        }
    }

    /** 描画用モデル root */
    @Nullable
    public ModelPart getModelRoot() {
        return modelRoot;
    }

    /** クライアント専用: モデル生成（必要ならサブクラスで上書き） */
    protected ModelPart createModel() {
        // Entityクラス名から自動的にモデル名を決定
        String modelName = this.getType().toShortString().toLowerCase();
        ResourceLocation geoLoc = new ResourceLocation(MonsterMod.MOD_ID, "models/" + modelName + ".geo.json");
        MonsterMod.LOGGER.info("[MonsterMod] createModel() -> " + geoLoc);

        ModelPart root = BaseMonsterIdentity.generateModelFromGeoJSON(geoLoc);
        if (root == null) {
            MonsterMod.LOGGER.error("[MonsterMod] Failed to load GeoJSON model: " + geoLoc);
        } else {
            MonsterMod.LOGGER.info("[MonsterMod] Model loaded successfully. Parts: " + root.getAllParts().count());
        }
        return root;
    }

    // -----------------------------
    // Tick
    // -----------------------------
    @Override
    public void tick() {
        super.tick();

        // サーバー側: Capability やクールダウン Tick
        if (!level().isClientSide) {
            IMonsterData data = getMonsterData();
            if (data != null) data.tick();
        }

        // クライアント側: Identity アニメーション Tick
        if (level().isClientSide && identity != null) {
            float delta = 1f / 20f;
            identity.tickClient(delta);
        }
    }

    // -----------------------------
    // Capability 取得
    // -----------------------------
    @Nullable
    public IMonsterData getMonsterData() {
        return CapabilityRegistry.getMonsterData(this);
    }

    // -----------------------------
    // NBT 保存/復元
    // -----------------------------
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (identity != null) tag.put("IdentityData", identity.serializeNBT());

        IMonsterData data = getMonsterData();
        if (data != null) {
            tag.putString("Skill", data.getSkill());
            tag.putInt("SkillTick", data.getSkillTick());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (identity != null && tag.contains("IdentityData")) {
            identity.deserializeNBT(tag.getCompound("IdentityData"));
        }

        IMonsterData data = getMonsterData();
        if (data != null) {
            if (tag.contains("Skill")) data.setSkill(tag.getString("Skill"));
            if (tag.contains("SkillTick")) data.setSkillTick(tag.getInt("SkillTick"));
        }
    }

    // -----------------------------
    // Identity BoneMap 初期化
    // -----------------------------
    public void initIdentityBoneMap() {
        if (identity != null) identity.autoInitBoneMap(this);
    }

    /** クライアント専用: モデルが未生成なら生成 */
    public void ensureModelInitialized() {
        if (!level().isClientSide) return;
        if (modelRoot == null) {
            MonsterMod.LOGGER.info("[MonsterMod] ensureModelInitialized() called for " + this.getType().toShortString());
            modelRoot = createModel();
            if (modelRoot == null) {
                MonsterMod.LOGGER.error("[MonsterMod] ensureModelInitialized: modelRoot is still null!");
            } else {
                MonsterMod.LOGGER.info("[MonsterMod] Model root successfully created with parts: " + modelRoot.getAllParts().count());
            }
        } else {
            MonsterMod.LOGGER.debug("[MonsterMod] ensureModelInitialized() skipped: already initialized");
        }
    }

    // -----------------------------
    // 属性生成ユーティリティ
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

    public static final Attribute GRAVITY = Attributes.ATTACK_KNOCKBACK;
}
