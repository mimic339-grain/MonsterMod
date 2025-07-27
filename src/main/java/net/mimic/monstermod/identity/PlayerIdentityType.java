package net.mimic.monstermod.identity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * IPlayerIdentityの抽象基底クラス。
 * 共通の実装を提供します。
 */
public abstract class PlayerIdentityType implements IPlayerIdentity {
    private final ResourceLocation id;
    private final Supplier<EntityType<?>> entityTypeSupplier;

    public PlayerIdentityType(ResourceLocation id, Supplier<EntityType<?>> entityTypeSupplier) {
        this.id = id;
        this.entityTypeSupplier = entityTypeSupplier;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public Supplier<EntityType<?>> getEntityType() {
        return entityTypeSupplier;
    }

    // デフォルトの実装。必要に応じてオーバーライドしてください。
    @Override
    public Vec3 getBoundingBoxDimensions(Pose pose) {
        EntityType<?> type = entityTypeSupplier.get();
        if (type != null) {
            EntityDimensions dimensions = type.getDimensions();
            return new Vec3(dimensions.width, dimensions.height, dimensions.width); // 幅はz方向にも適用
        }
        return new Vec3(0.6f, 1.8f, 0.6f); // デフォルトのプレイヤーサイズ
    }

    @Override
    public float getEyeHeight(Pose pose) {
        EntityType<?> type = entityTypeSupplier.get();
        if (type != null) {
            return type.getDimensions().eyeHeight;
        }
        return 1.62f; // デフォルトのプレイヤーの視点高さ
    }

    @Override
    public float getStepHeight() {
        return 0.6f; // デフォルトのプレイヤーのステップ高さ
    }

    // ★変更: プレイヤーごとの動的状態に関するフィールドとメソッドは削除
    // これらの状態はIPlayerTransformation Capabilityで管理されます。
}