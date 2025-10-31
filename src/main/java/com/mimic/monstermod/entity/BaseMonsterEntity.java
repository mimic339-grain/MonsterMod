package com.mimic.monstermod.entity;

import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mimic.monstermod.variable.entity.IMonsterData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.client.model.geom.ModelPart;
import org.jetbrains.annotations.Nullable;

/**
 * 完全版 BaseMonsterEntity
 * - BaseMonsterIdentity / AnimationPlayerTemplate (YSM方式) 互換
 * - 描画用 ModelPart root を提供
 */
public abstract class BaseMonsterEntity extends BaseEntity {

    @Nullable
    private BaseMonsterIdentity identity;

    // 描画モデルの root (Blockbench 等で定義)
    protected ModelPart modelRoot;

    public BaseMonsterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);

        // Client 側でモデル生成
        if (level.isClientSide) {
            this.modelRoot = createModel();
        }
    }

    /** Identity 用 getter */
    @Nullable
    public BaseMonsterIdentity getIdentity() {
        return identity;
    }

    /** Identity セット */
    public void setIdentity(@Nullable BaseMonsterIdentity identity) {
        this.identity = identity;
        if (identity != null) identity.autoInitBoneMap(this);
    }

    /** 描画用モデル root */
    @Nullable
    public ModelPart getModelRoot() {
        return modelRoot;
    }

    /** クライアント専用: モデル生成 */
    protected abstract ModelPart createModel();

    // -----------------------------
    // Tick
    // -----------------------------
    @Override
    public void tick() {
        super.tick();

        // サーバー側: CapabilityやクールダウンTick
        if (!level().isClientSide) {
            IMonsterData data = getMonsterData();
            if (data != null) data.tick();
        }

        // クライアント側: IdentityアニメーションTick
        if (level().isClientSide && identity != null) {
            float delta = 1f / 20f; // 1tick=1/20秒
            identity.tickClient(delta);
        }
    }

    // -----------------------------
    // Capability取得
    // -----------------------------
    @Nullable
    public IMonsterData getMonsterData() {
        return CapabilityRegistry.getMonsterData(this);
    }

    // -----------------------------
    // NBT保存/復元
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
