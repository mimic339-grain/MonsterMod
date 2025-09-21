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
        ClientMimicEntity cached = ClientMimicEntity.getOrCreate(playerId);

        boolean isMoving = player == Minecraft.getInstance().player &&
                (player.zza != 0 || player.xxa != 0);

        MimicEntity.MimicAnimationState targetState;
        if (isMoving) {
            targetState = cached.isOpen() ? MimicEntity.MimicAnimationState.OPENJUMP : MimicEntity.MimicAnimationState.CLOSEJUMP;
        } else {
            targetState = cached.isOpen() ? MimicEntity.MimicAnimationState.OPEN_IDLE : MimicEntity.MimicAnimationState.IDLE;
        }

        // ✅ 前回と違う場合のみアニメーションを更新
        if (cached.getAnimationState() != targetState) {
            cached.setAnimationState(targetState);
            System.out.println("[MimicIdentity] Player=" + player.getName().getString() + " animation changed to " + targetState);
        }

        // 座標補間
        double renderY = cached.getRenderY();
        double dy = player.getY() - renderY;
        if (Math.abs(dy) > 0.01 || targetState == MimicEntity.MimicAnimationState.CLOSEJUMP || targetState == MimicEntity.MimicAnimationState.OPENJUMP) {
            renderY += dy * 0.5;
        }

        double renderX = cached.getRenderX() + (player.getX() - cached.getRenderX()) * 0.5;
        double renderZ = cached.getRenderZ() + (player.getZ() - cached.getRenderZ()) * 0.5;
        float renderYRot = cached.getRenderYRot() + (player.yBodyRot - cached.getRenderYRot()) * 0.5f;
        float renderXRot = cached.getRenderXRot() + (player.getXRot() - cached.getRenderXRot()) * 0.5f;

        cached.setPosAndRot(renderX, renderY, renderZ, renderYRot, renderXRot);

        cached.tick();

        System.out.println("[MimicIdentity] applyAnimationAndRender tick completed: pos=("
                + cached.getRenderX() + "," + cached.getRenderY() + "," + cached.getRenderZ() + ") rot=("
                + cached.getRenderYRot() + "," + cached.getRenderXRot() + ") state=" + cached.getAnimationState()
        );

        // 描画
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-player.yBodyRot));
        Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(cached)
                .render(cached, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

}
