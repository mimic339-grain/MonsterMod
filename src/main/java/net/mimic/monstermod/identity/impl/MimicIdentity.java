package net.mimic.monstermod.identity.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mimic.monstermod.MonsterMod;
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
    private final Map<UUID, MimicEntity.MimicAnimationState> lastSentStates = new HashMap<>();

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
    public void applySpecificAbilities(LivingEntity player) { }

    @Override
    public void removeSpecificAbilities(LivingEntity player) { }

    @Override
    public void applyAnimation(LivingEntity entity, MimicEntity.MimicAnimationState state) {
        if (!(entity instanceof Player player)) return;

        UUID playerId = player.getUUID();
        MimicEntity cachedEntity = PlayerEntityCache.getOrCreate(
                playerId,
                () -> new MimicEntity(ModEntities.MIMIC.get(), player.level())
        );

        if (cachedEntity.getAnimationState() != state) {
            System.out.println("[MimicIdentity] applyAnimation: " + state + " to player=" + player.getName().getString());
            cachedEntity.setAnimationState(state);
        }
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

        boolean isMoving = player == Minecraft.getInstance().player &&
                (Minecraft.getInstance().player.zza != 0 || Minecraft.getInstance().player.xxa != 0);

        MimicEntity.MimicAnimationState lastState = lastSentStates.getOrDefault(playerId, MimicEntity.MimicAnimationState.IDLE);
        MimicEntity.MimicAnimationState targetState = isMoving
                ? (cachedEntity.isOpen() ? MimicEntity.MimicAnimationState.OPENJUMP : MimicEntity.MimicAnimationState.CLOSEJUMP)
                : (cachedEntity.isOpen() ? MimicEntity.MimicAnimationState.OPEN_IDLE : MimicEntity.MimicAnimationState.IDLE);

        // 状態が変わった場合のみセット
        if (lastState != targetState) {
            cachedEntity.setAnimationState(targetState);
            lastSentStates.put(playerId, targetState);
            System.out.println("[MimicIdentity] setAnimation: " + targetState + " for player=" + player.getName().getString() +
                    " (moving=" + isMoving + ")");
        }

        // Y座標補間（ジャンプ中はアニメ任せ）
        double renderY = cachedEntity.getRenderY();
        if (!(targetState == MimicEntity.MimicAnimationState.CLOSEJUMP || targetState == MimicEntity.MimicAnimationState.OPENJUMP)) {
            renderY += (player.getY() - renderY) * 0.5;
        }

        // 回転補間
        float renderYRot = cachedEntity.getRenderYRot() + (player.yBodyRot - cachedEntity.getRenderYRot()) * 0.5f;
        float renderXRot = cachedEntity.getRenderXRot() + (player.getXRot() - cachedEntity.getRenderXRot()) * 0.5f;

        cachedEntity.setPosAndRot(player.getX(), renderY, player.getZ(), renderYRot, renderXRot);

        // GeckoLib tick（座標更新後）
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
