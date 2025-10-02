package net.mimic.monstermod.identity.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.PlayerIdentityType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MimicIdentity extends PlayerIdentityType<MimicEntity.MimicAnimationState> {

    public static final ResourceLocation IDENTITY_ID = new ResourceLocation(MonsterMod.MOD_ID, "mimic");
    public static final MimicIdentity INSTANCE = new MimicIdentity();

    public MimicIdentity() {
        super(
                IDENTITY_ID,
                () -> null,
                MimicEntity.MimicAnimationState.class,
                createAnimationMap(),
                MimicEntity.MimicAnimationState.IDLE
        );
    }

    private static Map<String, MimicEntity.MimicAnimationState> createAnimationMap() {
        Map<String, MimicEntity.MimicAnimationState> map = new HashMap<>();
        map.put("WALK", MimicEntity.MimicAnimationState.OPENJUMP);
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
    public void applyAnimationAndRender(
            Player player,
            float entityYaw,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        UUID playerId = player.getUUID();

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (!transformation.isTransformed()) {
                ClientMimicEntity.remove(playerId);
                return;
            }

            ClientMimicEntity mimicEntity = ClientMimicEntity.getOrCreate(playerId);

            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(-player.yBodyRot));

            poseStack.popPose();
        });
    }
}
