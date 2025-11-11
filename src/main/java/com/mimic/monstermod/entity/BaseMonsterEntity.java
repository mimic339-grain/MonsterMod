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
 */
public abstract class BaseMonsterEntity extends BaseEntity {

    @Nullable
    private BaseMonsterIdentity identity;

    @Nullable
    protected ModelPart modelRoot;

    public BaseMonsterEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        if (level.isClientSide) ensureModelInitialized();
    }

    @Nullable
    public BaseMonsterIdentity getIdentity() { return identity; }

    public void setIdentity(@Nullable BaseMonsterIdentity identity) {
        this.identity = identity;
        if (identity != null) {
            MonsterMod.LOGGER.info("[BaseMonsterEntity] Identity attached: {}", identity.getId());
            identity.setEntity(this);
            identity.autoInitBoneMap(this);
            ensureModelInitialized();
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

    protected ModelPart createModel() {
        String modelName = this.getType().toShortString().toLowerCase();
        JsonObject json = BaseMonsterIdentity.loadModelJson(modelName);
        if (json != null) {
            ModelBuildResult result = MonsterAnimationUtil.buildModelFromJson(json, 64, 64);
            // modelRootとして top root を保持
            return result.root;
        }
        return new ModelPart(Collections.emptyList(), Collections.emptyMap());
    }

    public void ensureModelInitialized() {
        if (!level().isClientSide) return;
        if (modelRoot == null) {
            modelRoot = createModel();
            if (identity != null) identity.autoInitBoneMap(this);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) return;
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
            identity.autoInitBoneMap(this);
        }
    }

    public void initIdentityBoneMap() {
        if (identity != null) identity.autoInitBoneMap(this);
    }

    public static AttributeSupplier.Builder createDefaultAttributes(
            double health, double speed, double damage, double resistance, double armor, double gravity
    ) {
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
