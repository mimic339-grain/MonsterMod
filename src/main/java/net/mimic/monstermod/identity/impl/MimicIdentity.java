package net.mimic.monstermod.identity.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.PlayerIdentityType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MimicIdentity extends PlayerIdentityType<MimicEntity.MimicAnimationState> {

    public static final ResourceLocation IDENTITY_ID =
            new ResourceLocation(MonsterMod.MOD_ID, "mimic");

    public MimicIdentity() {
        super(
                IDENTITY_ID,
                ModEntities.MIMIC::get,
                MimicEntity.MimicAnimationState.class,
                createAnimationMap(),
                MimicEntity.MimicAnimationState.IDLE
        );
    }

    private static Map<String, MimicEntity.MimicAnimationState> createAnimationMap() {
        Map<String, MimicEntity.MimicAnimationState> map = new HashMap<>();
        map.put("WALK", MimicEntity.MimicAnimationState.OPEN);
        map.put("ATTACK", MimicEntity.MimicAnimationState.BITE);
        map.put("IDLE", MimicEntity.MimicAnimationState.IDLE);
        return map;
    }

    @Override
    public void applyAnimation(LivingEntity dummy, MimicEntity.MimicAnimationState state) {
        if (!(dummy instanceof Player player)) return;
        ClientMimicEntity cachedEntity = ClientMimicEntity.getOrCreate(player.getUUID());

        // 非ループアニメは playOnce でリクエスト
        switch (state) {
            case OPEN, CLOSE, BITE -> cachedEntity.playOnce(state);
            default -> cachedEntity.setAnimationState(state);
        }
    }

    @Override
    public Vec3 getBoundingBoxDimensions(net.minecraft.world.entity.Pose pose) {
        return new Vec3(0.6f, 0.6f, 0.6f);
    }

    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose pose) {
        return 0.45f;
    }

    @Override
    public void applySpecificAbilities(LivingEntity player) {}
    @Override
    public void removeSpecificAbilities(LivingEntity player) {}

    /**
     * 微振動防止版の applyAnimationAndRender
     */
    public void applyAnimationAndRender(Player player,
                                        float entityYaw,
                                        float partialTicks,
                                        PoseStack poseStack,
                                        MultiBufferSource buffer,
                                        int packedLight,
                                        MimicEntity.MimicAnimationState baseState) {

        UUID playerId = player.getUUID();
        ClientMimicEntity cachedEntity = ClientMimicEntity.getOrCreate(playerId);

        // 位置・回転・速度同期
        cachedEntity.setPosAndRot(player.getX(), player.getY(), player.getZ(),
                player.yBodyRot, player.getXRot());
        cachedEntity.setDeltaMovement(player.getDeltaMovement());

        // baseState 更新（非ループ再生中は上書きしない）
        if (cachedEntity.isAnimationFinished() || cachedEntity.isLoopAnimation(cachedEntity.getLastRequestedAnimation())) {
            cachedEntity.setAnimationState(baseState);
        }

        // 描画のみ
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-player.yBodyRot));
        Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(cachedEntity)
                .render(cachedEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

}
