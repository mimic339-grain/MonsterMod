package net.mimic.monstermod.identity.impl;

import net.mimic.monstermod.identity.PlayerIdentityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * Mimicに変身したプレイヤーの特性を定義するIdentity。
 */
public class MimicIdentity extends PlayerIdentityType {

    public MimicIdentity(ResourceLocation id, Supplier<EntityType<?>> entityTypeSupplier) {
        super(id, entityTypeSupplier);
    }

    @Override
    public Vec3 getBoundingBoxDimensions(Pose pose) {
        // Mimicの具体的なヒットボックスサイズ
        // MimicEntityのサイズ(0.6f, 0.6f)を使用
        return new Vec3(0.6f, 0.6f, 0.6f);
    }

    @Override
    public float getEyeHeight(Pose pose) {
        // Mimicの視点の高さ
        // MimicEntityのeyeHeightは通常Mobに合わせて調整
        return 0.45f; // 例: Mimicのモデルに合わせて調整
    }

    @Override
    public float getStepHeight() {
        // Mimicのステップ高さ
        return 0.6f; // 例えば、プレイヤーと同じ
    }

    @Override
    public void applySpecificAbilities(LivingEntity player) {
        // Mimicに変身した際にプレイヤーに与える特殊能力
        // 例: 特定のブロックに対するインタラクションの変更、飛行能力など
        // 注意: 飛行能力はPlayer#onUpdateAbilities() でクライアントサイドで処理されるため、
        // サーバーサイドでCapabilityのフラグを立てるなどの処理が必要
        // 現状のコードでは飛行能力を直接付与していません。
        // もし飛行を付与したい場合、PlayerTransformationに飛行フラグを追加し、
        // Player#onUpdateAbilities()のMixinでそのフラグを参照して飛行能力を付与する必要があります。
    }

    @Override
    public void removeSpecificAbilities(LivingEntity player) {
        // 変身解除時に特殊能力を解除
        // applySpecificAbilitiesで付与した能力を元に戻す
    }

    // ★変更: プレイヤーごとの動的状態に関するメソッドは削除
    // これらの状態はIPlayerTransformation Capabilityで管理されます。
}