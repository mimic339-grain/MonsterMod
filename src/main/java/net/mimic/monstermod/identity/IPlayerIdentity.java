package net.mimic.monstermod.identity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * プレイヤーが変身できる各Mobの「アイデンティティ（性質）」を定義するインターフェース。
 * これはシングルトンとして扱われ、プレイヤーごとの動的な状態は含みません。
 */
public interface IPlayerIdentity {
    ResourceLocation getId();
    Supplier<EntityType<?>> getEntityType();

    // プレイヤーの変身後の物理的プロパティ
    Vec3 getBoundingBoxDimensions(Pose pose);
    float getEyeHeight(Pose pose);
    float getStepHeight();

    // 変身時に特定の能力をプレイヤーに付与する（例: 飛行）
    // このメソッドは、変身時または状態変更時に一度だけ呼び出されるべきです。
    void applySpecificAbilities(LivingEntity player);

    // 変身解除時に付与した能力を解除する
    void removeSpecificAbilities(LivingEntity player);

    // ★変更: プレイヤーごとの動的状態（アニメーション、噛みつき）に関するメソッドは削除
    // これらの状態はIPlayerTransformation Capabilityで管理されます。
}