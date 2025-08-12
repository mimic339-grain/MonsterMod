package com.mimic.monster.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * プレイヤーの変身状態を管理するCapabilityの中身
 */
public class TransformCapability {

    // 変身先エンティティタイプ
    private EntityType<?> transformedType;
    // 変身中かどうか
    private boolean transformed;
    //元のステータス
    private double originalMaxHealth;
    private double originalAttackDamage;
    private double originalWidth;
    private double originalHeight;

    // Getter / Setter

    public EntityType<?> getTransformedType() {
        return transformedType;
    }

    public void setTransformedType(EntityType<?> transformedType) {
        this.transformedType = transformedType;
    }

    public boolean isTransformed() {
        return transformed;
    }

    public void setTransformed(boolean transformed) {
        this.transformed = transformed;
    }

    public double getOriginalMaxHealth() {
        return originalMaxHealth;
    }

    public void setOriginalMaxHealth(double originalMaxHealth) {
        this.originalMaxHealth = originalMaxHealth;
    }

    public double getOriginalAttackDamage() {
        return originalAttackDamage;
    }

    public void setOriginalAttackDamage(double originalAttackDamage) {
        this.originalAttackDamage = originalAttackDamage;
    }

    public double getOriginalWidth() {
        return originalWidth;
    }

    public void setOriginalWidth(double originalWidth) {
        this.originalWidth = originalWidth;
    }

    public double getOriginalHeight() {
        return originalHeight;
    }

    public void setOriginalHeight(double originalHeight) {
        this.originalHeight = originalHeight;
    }

    /**
     * Capabilityの状態をNBTに保存する
     * @param tag 保存先CompoundTag
     */
    public void saveNBTData(CompoundTag tag) {
        tag.putBoolean("Transformed", transformed);
        tag.putDouble("OriginalMaxHealth", originalMaxHealth);
        tag.putDouble("OriginalAttackDamage", originalAttackDamage);
        tag.putDouble("OriginalWidth", originalWidth);
        tag.putDouble("OriginalHeight", originalHeight);

        if (transformedType != null) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(transformedType);
            if (id != null) {
                tag.putString("TransformedType", id.toString());
            }
        }
    }

    /**
     * NBTからCapabilityの状態を読み込む
     * @param tag 読み込み元CompoundTag
     */
    public void loadNBTData(CompoundTag tag) {
        transformed = tag.getBoolean("Transformed");
        originalMaxHealth = tag.getDouble("OriginalMaxHealth");
        originalAttackDamage = tag.getDouble("OriginalAttackDamage");
        originalWidth = tag.getDouble("OriginalWidth");
        originalHeight = tag.getDouble("OriginalHeight");

        if (tag.contains("TransformedType")) {
            ResourceLocation id = new ResourceLocation(tag.getString("TransformedType"));
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            transformedType = type;
        }
    }

    /**
     * Capabilityの状態をリセットし、変身解除状態に戻す
     */
    public void reset() {
        this.transformed = false;
        this.transformedType = null;
    }
}
