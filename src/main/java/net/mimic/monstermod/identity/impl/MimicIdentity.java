package net.mimic.monstermod.identity.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.PlayerIdentityType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MimicIdentity extends PlayerIdentityType<MimicEntity.MimicAnimationState> {

    public static final ResourceLocation IDENTITY_ID = new ResourceLocation(MonsterMod.MOD_ID, "mimic");
    private final Map<UUID, MimicEntity.MimicAnimationState> lastSentStates = new HashMap<>();

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
    public Vec3 getBoundingBoxDimensions(net.minecraft.world.entity.Pose pose) {
        return new Vec3(0.6f, 0.6f, 0.6f);
    }

    @Override
    public float getEyeHeight(net.minecraft.world.entity.Pose pose) {
        return 0.45f;
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

        // ----- 移動判定とアニメーション更新 -----
        if (player instanceof AbstractClientPlayer abstractPlayer) {
            cachedEntity.updateAnimation(abstractPlayer);
        }

        // ----- 座標・回転は補間なしで直接反映 -----
        cachedEntity.setPosAndRot(player.getX(), player.getY(), player.getZ(), player.yBodyRot, player.getXRot());

        // ----- GeckoLib tick -----
        cachedEntity.tick();

        // ----- 描画 -----
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-player.yBodyRot));
        Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(cachedEntity)
                .render(cachedEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();

        System.out.println("[MimicIdentity] Render player=" + player.getName().getString() +
                " anim=" + cachedEntity.getRenderAnimationState() +
                " pos=(" + cachedEntity.getRenderX() + "," + cachedEntity.getRenderY() + "," + cachedEntity.getRenderZ() + ")");
    }

}
