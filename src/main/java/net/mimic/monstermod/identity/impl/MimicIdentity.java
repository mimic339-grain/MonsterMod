package net.mimic.monstermod.identity.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.PlayerIdentityType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MimicIdentity extends PlayerIdentityType<MimicEntity.MimicAnimationState> {

    public static final net.minecraft.resources.ResourceLocation IDENTITY_ID =
            new net.minecraft.resources.ResourceLocation(MonsterMod.MOD_ID, "mimic");

    public MimicIdentity() {
        super(IDENTITY_ID, ModEntities.MIMIC::get, MimicEntity.MimicAnimationState.class,
                createAnimationMap(), MimicEntity.MimicAnimationState.IDLE);
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
    public void applyAnimation(LivingEntity dummy, MimicEntity.MimicAnimationState state) {
        if (!(dummy instanceof Player player)) return;
        UUID playerId = player.getUUID();
        ClientMimicEntity cachedEntity = ClientMimicEntity.getOrCreate(playerId);

        switch (state) {
            case OPEN, CLOSE, BITE -> {
                // 単発アニメは強制再生
                cachedEntity.playOnce(state);
            }
            default -> {
                // ループ系は現在と違う時だけセット
                if (cachedEntity.getAnimationState() != state) {
                    cachedEntity.setAnimationState(state);
                }
            }
        }
    }

    @Override
    public void applySpecificAbilities(LivingEntity player) {}
    @Override
    public void removeSpecificAbilities(LivingEntity player) {}

    public void applyAnimationAndRender(Player player,
                                        float entityYaw,
                                        float partialTicks,
                                        PoseStack poseStack,
                                        MultiBufferSource buffer,
                                        int packedLight,
                                        MimicEntity.MimicAnimationState baseState) {

        UUID playerId = player.getUUID();
        ClientMimicEntity cachedEntity = ClientMimicEntity.getOrCreate(playerId);

        // プレイヤーの位置・回転を反映
        cachedEntity.setDeltaMovement(player.getDeltaMovement());
        cachedEntity.setPosAndRot(player.getX(), player.getY(), player.getZ(),
                player.yBodyRot, player.getXRot());

        // 内部アニメーション状態を更新
        cachedEntity.tick();

        poseStack.pushPose();

        // 平行移動（補間付き）
        poseStack.translate(
                cachedEntity.getInterpolatedX(partialTicks) - cachedEntity.getX(),
                cachedEntity.getInterpolatedY(partialTicks) - cachedEntity.getY(),
                cachedEntity.getInterpolatedZ(partialTicks) - cachedEntity.getZ()
        );

        // Y軸回転のみ適用（頭のピッチはアニメ側で制御）
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                -cachedEntity.getInterpolatedYRot(partialTicks))
        );

        Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(cachedEntity)
                .render(cachedEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }
}
