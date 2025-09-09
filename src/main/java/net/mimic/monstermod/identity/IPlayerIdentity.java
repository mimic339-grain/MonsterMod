package net.mimic.monstermod.identity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

//プレイヤーが変身できる各Mobの「アイデンティティ（性質）」を定義するインターフェース。
public interface IPlayerIdentity {

    ResourceLocation getId();

    // プレイヤーの変身後の物理的プロパティ
    Vec3 getBoundingBoxDimensions(Pose pose);
    float getEyeHeight(Pose pose);
    float getStepHeight();

    void applySpecificAbilities(LivingEntity player);
    void removeSpecificAbilities(LivingEntity player);

    /** このIdentityに対応するMonsterのID */
    String getMonsterId();

    /** プレイヤー専用のダミーEntityを生成 */
    LivingEntity createDummy(Level level);

    /** ダミーEntityにアニメーションや状態を適用 */
    void applyAnimation(LivingEntity dummy, net.mimic.monstermod.capability.PlayerTransformation.MonsterState state);
}
