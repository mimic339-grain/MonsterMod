package com.mimic.monstermod.entity.monster;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.util.MonsterAnimationUtil;
import com.mimic.monstermod.util.MonsterAnimationUtil.ModelBuildResult;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.HashMap;

/**
 * MimicEntity - 完全版 YSMMOD準拠
 */
public class MimicEntity extends BaseMonsterEntity {

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BITE =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    public MimicEntity(EntityType<? extends BaseMonsterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OPEN, false);
        this.entityData.define(BITE, false);
    }

    public boolean isOpen() { return entityData.get(OPEN); }
    public void setOpen(boolean open) { entityData.set(OPEN, open); }
    public boolean isBiting() { return entityData.get(BITE); }
    public void setBiting(boolean bite) { entityData.set(BITE, bite); }

    @Override
    public BaseMonsterIdentity createIdentityInstance() {
        return new BaseMonsterIdentity(new ResourceLocation(MonsterMod.MOD_ID, "mimic"), 3);
    }

    @Override
    protected ModelPart createModel() {
        String modelId = "mimic";
        ModelBuildResult result = MonsterAnimationUtil.buildModelFromJson(BaseMonsterIdentity.loadModelJson(modelId), 64, 64);
        if (result == null || result.root == null) {
            MonsterMod.LOGGER.error("[MimicEntity] ❌ Failed to load model, using empty fallback.");
            return new ModelPart(Collections.emptyList(), new HashMap<>());
        }
        // modelRoot に直接セットして BoneMap を同期
        this.modelRoot = result.root;
        if (getIdentity() != null) getIdentity().setNamedPartsFromModelMap(result.namedParts);
        return modelRoot;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(
                200.0D, 0.25D, 4.0D, 0.2D, 2.0D, 1.0D
        );
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Open", isOpen());
        tag.putBoolean("Bite", isBiting());

        BaseMonsterIdentity id = getIdentity();
        if (id != null) tag.put("Identity", id.serializeNBT());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Open")) setOpen(tag.getBoolean("Open"));
        if (tag.contains("Bite")) setBiting(tag.getBoolean("Bite"));

        BaseMonsterIdentity id = getIdentity();
        if (tag.contains("Identity")) {
            if (id == null) id = createIdentityInstance();
            id.deserializeNBT(tag.getCompound("Identity"));
            setIdentity(id);
        }
        // BoneMap を必ず同期
        if (getIdentity() != null) getIdentity().autoInitBoneMap(this);
    }
}
