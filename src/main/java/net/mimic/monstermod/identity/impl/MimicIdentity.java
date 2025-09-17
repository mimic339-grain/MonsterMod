package net.mimic.monstermod.identity.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.PlayerEntityCache;
import net.mimic.monstermod.identity.PlayerIdentityType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
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
    public Vec3 getBoundingBoxDimensions(net.minecraft.world.entity.Pose pose) {
        return new Vec3(0.6f, 0.6f, 0.6f);
    }

    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose pose) {
        return 0.45f;
    }

    @Override
    public void applySpecificAbilities(net.minecraft.world.entity.LivingEntity player) {}

    @Override
    public void removeSpecificAbilities(net.minecraft.world.entity.LivingEntity player) {}

    @Override
    public void applyAnimationAndRender(Player player,
                                        float entityYaw,
                                        float partialTicks,
                                        PoseStack poseStack,
                                        MultiBufferSource buffer,
                                        int packedLight,
                                        PlayerTransformation.MonsterState state) {

        UUID playerId = player.getUUID();
        MimicEntity cachedEntity = PlayerEntityCache.getOrCreate(playerId,
                () -> new MimicEntity(ModEntities.MIMIC.get(), player.level()));

        // 位置・移動をコピー
        cachedEntity.setPos(player.getX(), player.getY(), player.getZ());
        cachedEntity.setDeltaMovement(player.getDeltaMovement());

        // transform 後の一度だけ IDLE 再生
        if (state != null && !cachedEntity.isIdlePlayed()) {
            cachedEntity.setAnimationState(MimicEntity.MimicAnimationState.IDLE);
            cachedEntity.setIdlePlayed(true);
        } else if (state != null && state.animationState != null) {
            // 通常のアニメーション状態を反映
            MimicEntity.MimicAnimationState animState;
            try {
                animState = Enum.valueOf(MimicEntity.MimicAnimationState.class, state.animationState);
            } catch (IllegalArgumentException e) {
                animState = MimicEntity.MimicAnimationState.CLOSED; // 安全策
            }
            cachedEntity.setAnimationState(animState);
        }

        // walkAnimation 更新
        float movementSqr = (float) player.getDeltaMovement().lengthSqr();
        if (!cachedEntity.isAnimationLocked() && (movementSqr > 1e-6f || player.walkAnimation.position() > 1e-6f)) {
            cachedEntity.walkAnimation.update(player.walkAnimation.position(), 1.0f);
            cachedEntity.walkAnimation.setSpeed(player.walkAnimation.speed());
        }

        // 回転リセット
        cachedEntity.yBodyRot = 0;
        cachedEntity.setYRot(0);
        cachedEntity.setYHeadRot(0);

        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-player.yBodyRot));

        Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(cachedEntity)
                .render(cachedEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

}