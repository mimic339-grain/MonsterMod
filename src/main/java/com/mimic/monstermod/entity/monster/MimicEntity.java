package com.mimic.monstermod.entity.monster;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

import java.util.Collections;
import java.util.HashMap;

public class MimicEntity extends BaseMonsterEntity {

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BITE =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private BaseMonsterIdentity identity; // Renderer 用 identity

    public MimicEntity(EntityType<? extends BaseMonsterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OPEN, false);
        this.entityData.define(BITE, false);
    }

    // -----------------------------
// Open/Bite 管理
// -----------------------------
    public boolean isOpen() { return this.entityData.get(OPEN); }
    public void setOpen(boolean open) { this.entityData.set(OPEN, open); }
    public boolean isBiting() { return this.entityData.get(BITE); }
    public void setBiting(boolean bite) { this.entityData.set(BITE, bite); }

    // -----------------------------
// Identity 管理
// -----------------------------
    @Override
    public void setIdentity(BaseMonsterIdentity identity) {
        this.identity = identity;
        if (identity != null) {
            identity.setEntity(this);
            identity.autoInitBoneMap(this);
            // level は protected メソッド level() でアクセス
            if (this.level() != null && this.level().isClientSide) ensureModelInitialized();
        }
    }

    public BaseMonsterIdentity getIdentity() {
        return this.identity;
    }

    // -----------------------------
// モデル生成
// -----------------------------
    @Override
    protected ModelPart createModel() {
        ResourceLocation geoJson = new ResourceLocation(MonsterMod.MOD_ID, "models/mimic.geo.json");
        MonsterMod.LOGGER.info("[MimicEntity] Loading model from: {}", geoJson);

        ModelPart loaded = BaseMonsterIdentity.generateModelFromGeoJSON(geoJson);
        if (loaded == null) {
            MonsterMod.LOGGER.error("[MimicEntity] ❌ Failed to load geo.json -> using empty model (fallback)");
            return new ModelPart(Collections.emptyList(), new HashMap<>());
        }

        try {
            var field = ModelPart.class.getDeclaredField("children");
            field.setAccessible(true);
            var children = (HashMap<?, ?>) field.get(loaded);
            MonsterMod.LOGGER.info("[MimicEntity] ✅ Model loaded successfully: {} child bones", children.size());
        } catch (Exception e) {
            MonsterMod.LOGGER.warn("[MimicEntity] Model introspection failed", e);
        }
        return loaded;
    }

    // -----------------------------
// 属性作成ユーティリティ
// -----------------------------
    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(
                200.0D, 0.25D, 4.0D, 0.2D, 2.0D, 1.0D
        );
    }

    // -----------------------------
// NBT 保存/復元
// -----------------------------
    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Open", isOpen());
        tag.putBoolean("Bite", isBiting());
        if (identity != null) {
            tag.putString("mobId", identity.getId() != null ? identity.getId() : "monstermod");
            // getAbilitySlotCount がない場合は abilityCooldowns.length を使う
            tag.putInt("abilitySlotCount", identity.abilityCooldowns != null ? identity.abilityCooldowns.length : 3);
            tag.put("Identity", identity.serializeNBT());
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Open")) setOpen(tag.getBoolean("Open"));
        if (tag.contains("Bite")) setBiting(tag.getBoolean("Bite"));

        if (tag.contains("Identity")) {
            String mobIdStr = tag.contains("mobId") ? tag.getString("mobId") : "monstermod:mimic";
            int slotCount = tag.contains("abilitySlotCount") ? tag.getInt("abilitySlotCount") : 3;
            if (identity == null) identity = new BaseMonsterIdentity(new ResourceLocation(mobIdStr), slotCount);
            identity.deserializeNBT(tag.getCompound("Identity"));
            identity.setEntity(this);
        }
    }

}
