package net.mimic.monstermod.identity.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.PlayerEntityCache;
import net.mimic.monstermod.identity.PlayerIdentityType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MimicIdentity extends PlayerIdentityType<MimicEntity.MimicAnimationState> {

    public static final ResourceLocation IDENTITY_ID = new ResourceLocation(MonsterMod.MOD_ID, "mimic");

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
    public Vec3 getBoundingBoxDimensions(Pose pose) {
        return new Vec3(0.6f, 0.6f, 0.6f);
    }

    @Override
    public float getEyeHeight(Pose pose) {
        return 0.45f;
    }

    @Override
    public void applySpecificAbilities(LivingEntity player) {
        // 追加能力があればここで適用
    }

    @Override
    public void removeSpecificAbilities(LivingEntity player) {
        // 能力解除処理
    }

    @Override
    public void applyAnimation(LivingEntity entity, MimicEntity.MimicAnimationState state) {
        if (!(entity instanceof Player player)) return;

        UUID playerId = player.getUUID();
        MimicEntity cachedEntity = PlayerEntityCache.getOrCreate(
                playerId,
                () -> new MimicEntity(ModEntities.MIMIC.get(), player.level())
        );

        // アニメーション状態を直接反映
        cachedEntity.setAnimationState(state);
    }

    @Override
    public void applyAnimationAndRender(
            Player player,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            MimicEntity.MimicAnimationState baseState
    ) {
        UUID playerId = player.getUUID();
        ClientMimicEntity cachedEntity = ClientMimicEntity.getOrCreate(playerId);

        // プレイヤーが移動中か判定
        boolean isMoving = player.getDeltaMovement().lengthSqr() > 1e-6f;

        // 基本状態保持（OPEN / CLOSE）
        boolean wasOpen = baseState == MimicEntity.MimicAnimationState.OPEN
                || baseState == MimicEntity.MimicAnimationState.OPENJUMP;
        boolean wasClose = baseState == MimicEntity.MimicAnimationState.CLOSE
                || baseState == MimicEntity.MimicAnimationState.CLOSEJUMP;

        // 目標ステート決定（現在の cachedEntity の状態をデフォルトにする）
        MimicEntity.MimicAnimationState targetState = cachedEntity.getAnimationState();

        if (!cachedEntity.isAnimationLocked()) {
            if (isMoving) {
                if (wasOpen) {
                    targetState = MimicEntity.MimicAnimationState.OPENJUMP;
                } else if (wasClose) {
                    targetState = MimicEntity.MimicAnimationState.CLOSEJUMP;
                } else {
                    targetState = baseState;
                }
            } else {
                MimicEntity.MimicAnimationState current = cachedEntity.getAnimationState();
                if (current == MimicEntity.MimicAnimationState.OPENJUMP) {
                    targetState = MimicEntity.MimicAnimationState.OPEN;
                } else if (current == MimicEntity.MimicAnimationState.CLOSEJUMP) {
                    targetState = MimicEntity.MimicAnimationState.CLOSE;
                } else {
                    targetState = baseState;
                }
            }
        }

        // 変化時のみセット（毎フレームセットするとアニメがリセットされる）
        if (cachedEntity.getAnimationState() != targetState) {
            cachedEntity.setAnimationState(targetState);
        }

        // 位置・回転はプレイヤーに合わせる
        cachedEntity.setPos(player.getX(), player.getY(), player.getZ());
        cachedEntity.setDeltaMovement(player.getDeltaMovement());
        cachedEntity.yBodyRot = 0;
        cachedEntity.setYRot(0);
        cachedEntity.setYHeadRot(0);

        // walkAnimation 更新
        if (isMoving) {
            cachedEntity.walkAnimation.update(player.walkAnimation.position(), 1.0f);
            cachedEntity.walkAnimation.setSpeed(player.walkAnimation.speed());
        }

        // tick で AnimationController を進める
        cachedEntity.tick();

        // 描画
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-player.yBodyRot));
        Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(cachedEntity)
                .render(cachedEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }
}
