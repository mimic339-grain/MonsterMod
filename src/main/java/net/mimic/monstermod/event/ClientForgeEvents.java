package net.mimic.monstermod.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.MimicSwitchC2SPacket; // Import the packet

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {

    // クライアント側でレンダリング用のダミーエンティティを保持
    public static MimicEntity dummyMimicEntity; // このフィールドは既に存在

    // ★追加するメソッド★
    public static MimicEntity getDummyMimicEntity() {
        return dummyMimicEntity;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) { // クライアント側でのみ処理
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    ResourceLocation transformedMobId = transformation.getTransformedMobId();

                    if (transformedMobId != null && transformedMobId.equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"))) {
                        // Mimicモデルのレンダリング
                        if (dummyMimicEntity == null) {
                            dummyMimicEntity = new MimicEntity(ModEntities.MIMIC.get(), player.level());
                        }

                        // ダミーエンティティの位置と回転をプレイヤーに合わせる
                        dummyMimicEntity.setPos(player.getX(), player.getY(), player.getZ());
                        dummyMimicEntity.setYRot(player.getYRot());
                        dummyMimicEntity.setXRot(player.getXRot());
                        dummyMimicEntity.yHeadRot = player.yHeadRot;
                        dummyMimicEntity.yBodyRot = player.yBodyRot;
                        dummyMimicEntity.setYBodyRot(player.yBodyRot); // Body rotation sync

                        // ダミーエンティティにプレイヤーの状態を同期
                        dummyMimicEntity.setCurrentAnimationState(transformation.getMimicState());
                        dummyMimicEntity.setBiting(transformation.isBiting());

                        // レンダリング
                        PoseStack poseStack = event.getPoseStack();
                        MultiBufferSource buffer = event.getMultiBufferSource();
                        float partialTicks = event.getPartialTick();
                        int light = event.getPackedLight();

                        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
                        EntityRenderer<MimicEntity> renderer = (EntityRenderer<MimicEntity>) dispatcher.getRenderer(dummyMimicEntity);

                        poseStack.pushPose();

                        // モデルのオフセット調整 (必要に応じて微調整)
                        // 通常、プレイヤーモデルの足元がエンティティのベースになるため、Mimicのベースをプレイヤーの足元に合わせる
                        poseStack.translate(0.0, -1.5, 0.0); // Mimicモデルの高さによって調整が必要

                        renderer.render(dummyMimicEntity, player.getYRot(), partialTicks, poseStack, buffer, light);

                        poseStack.popPose();

                        // 元のプレイヤーレンダリングをキャンセルしてMimicモデルだけを表示
                        event.setCanceled(true);
                    }
                }
            });
        }
    }
}