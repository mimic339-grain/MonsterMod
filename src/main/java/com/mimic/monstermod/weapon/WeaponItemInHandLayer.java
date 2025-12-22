package com.mimic.monstermod.weapon;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.variable.CapabilityRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WeaponItemInHandLayer<T extends LivingEntity, M extends EntityModel<T> & ArmedModel>
        extends RenderLayer<T, M> {

    private final ItemInHandRenderer itemInHandRenderer;

    public WeaponItemInHandLayer(RenderLayerParent<T, M> parent,
                                 ItemInHandRenderer renderer) {
        super(parent);
        this.itemInHandRenderer = renderer;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        boolean rightMainArm = entity.getMainArm() == HumanoidArm.RIGHT;

        ItemStack mainHand = getWeaponStack(entity);
        ItemStack offHand  = ItemStack.EMPTY;

        ItemStack left  = rightMainArm ? offHand  : mainHand;
        ItemStack right = rightMainArm ? mainHand : offHand;

        if (left.isEmpty() && right.isEmpty()) return;

        poseStack.pushPose();

        if (this.getParentModel().young) {
            poseStack.translate(0.0F, 0.75F, 0.0F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }

        renderArmWithItem(entity, right,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                HumanoidArm.RIGHT, poseStack, buffer, packedLight);

        renderArmWithItem(entity, left,
                ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                HumanoidArm.LEFT, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    protected void renderArmWithItem(
            LivingEntity entity,
            ItemStack stack,
            ItemDisplayContext context,
            HumanoidArm arm,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        if (stack.isEmpty()) return;

        poseStack.pushPose();

        this.getParentModel().translateToHand(arm, poseStack);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        boolean left = arm == HumanoidArm.LEFT;
        poseStack.translate((left ? -1 : 1) / 16.0F, 0.125F, -0.625F);

        this.itemInHandRenderer.renderItem(
                entity, stack, context, left,
                poseStack, buffer, packedLight
        );

        poseStack.popPose();
    }

    private ItemStack getWeaponStack(LivingEntity entity) {
        if (!(entity instanceof net.minecraft.world.entity.player.Player player))
            return ItemStack.EMPTY;

        return player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                .filter(h -> h.isActive() && !h.isSheathed())
                .map(HunterTransformation::getWeaponSlot)
                .orElse(ItemStack.EMPTY);
    }
}