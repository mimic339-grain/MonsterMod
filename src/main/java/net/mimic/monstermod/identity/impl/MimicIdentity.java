package net.mimic.monstermod.identity.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.IPlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.PlayerIdentityType;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MimicIdentity extends PlayerIdentityType<MimicEntity.MimicAnimationState> {

    public static final ResourceLocation IDENTITY_ID = new ResourceLocation(MonsterMod.MOD_ID, "mimic");
    private final Map<UUID, String> lastSentStates = new HashMap<>();

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
            int packedLight
    ) {
        UUID playerId = player.getUUID();
        ClientMimicEntity cachedEntity = ClientMimicEntity.getOrCreate(playerId);

        IPlayerTransformation it = player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).orElse(null);
        if (!(it instanceof PlayerTransformation transformation)) return;

        PlayerTransformation.MonsterState state = transformation.getMonsterState(transformation.getTransformedMobId());
        if (state == null) return;

        // 座標・回転補間
        cachedEntity.setPosAndRotIfChanged(
                player.getX(), player.getY(), player.getZ(),
                player.yBodyRot, player.getXRot()
        );

        // クライアント側アニメーション更新（WASD 判定）
        if (player instanceof AbstractClientPlayer abstractPlayer) {
            cachedEntity.updateAnimation(abstractPlayer);
        }

        // アニメーション変化があればサーバ送信
        String currentAnim = cachedEntity.getRenderAnimationState().name();
        String lastState = lastSentStates.getOrDefault(playerId, "");
        if (!currentAnim.equals(lastState)) {
            lastSentStates.put(playerId, currentAnim);

            if (transformation.isTransformed() && transformation.getTransformedMobId() != null) {
                MonsterMod.CHANNEL.sendToServer(new net.mimic.monstermod.networking.packet.PlayerTransformC2SPacket(
                        true, // 変身状態
                        transformation.getTransformedMobId()
                ));
            }
        }

        // 描画
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-player.yBodyRot));
        Minecraft.getInstance().getEntityRenderDispatcher()
                .getRenderer(cachedEntity)
                .render(cachedEntity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    /** クライアント側で S2C パケットを受信したら呼ぶ */
    public void handleS2CPacket(S2CTransformSyncPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (packet.getTransformedMobId() != null) {
                // MonsterState に反映
                PlayerTransformation.MonsterState state = transformation.getMonsterState(packet.getTransformedMobId());
                state.animationState = packet.getAnimationState();
                state.animationTick = packet.getAnimationTick();  // ここで tick 反映
                transformation.setMonsterState(packet.getTransformedMobId(), state);

                // ClientMimicEntity に反映
                ClientMimicEntity clientEntity = ClientMimicEntity.getOrCreate(mc.player.getUUID());
                clientEntity.updateAnimationFromServer(
                        MimicEntity.MimicAnimationState.valueOf(packet.getAnimationState()),
                        packet.getAnimationTick(),
                        mc.player
                );
            }
        });
    }

}
