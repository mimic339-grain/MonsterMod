
package net.mimic.monstermod.identity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

//プレイヤーが変身できる各Mobの「アイデンティティ（性質）」を定義するインターフェース。
public interface IPlayerIdentity {

    ResourceLocation getId();

    // applyAnimationAndRender を ResourceLocation/MimicAnimationState 統一
    void applyAnimationAndRender(Player player,
                                 float entityYaw,
                                 float partialTicks,
                                 PoseStack poseStack,
                                 MultiBufferSource buffer,
                                 int packedLight,
                                 MimicEntity.MimicAnimationState state);
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
}
