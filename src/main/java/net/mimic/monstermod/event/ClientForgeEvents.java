package net.mimic.monstermod.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer; // 使用されていないので削除も可能
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.player.PlayerRenderer; // 使用されていないので削除も可能
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
import net.mimic.monstermod.networking.ModMessages; // 使用されていないので削除も可能
import net.mimic.monstermod.networking.packet.MimicSwitchC2SPacket; // 使用されていないので削除も可能

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {

    public static MimicEntity dummyMimicEntity;

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
                        if (dummyMimicEntity == null || dummyMimicEntity.level() != player.level() || dummyMimicEntity.isRemoved()) { // ★改善されたダミーエンティティ初期化
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
                        dummyMimicEntity.setCurrentAnimationState(transformation.getMimicState()); // ★修正: setOpen() の代わりに setCurrentAnimationState() を使用
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
                        // YオフセットはMimicモデルの中心とプレイヤーの足元の調整に重要
                        poseStack.translate(0.0, 0.0, 0.0); // Mimicモデルの高さによって調整が必要

                        renderer.render(dummyMimicEntity, player.getYRot(), partialTicks, poseStack, buffer, light);

                        poseStack.popPose();

                        // 元のプレイヤーレンダリングをキャンセルしてMimicモデルだけを表示
                        event.setCanceled(true);
                    }
                } else { // ★追加: 変身していない場合
                    if (dummyMimicEntity != null) {
                        // 変身解除時、Mimicを閉じるアニメーションをトリガーする意図。
                        // isTransformed() が false の時にここに来るので、
                        // dummyMimicEntityの状態をリセットするのが適切かもしれません。
                        dummyMimicEntity.setCurrentAnimationState(MimicEntity.MimicAnimationState.CLOSED); // ★修正: 変身解除時は完全に閉じた状態に
                        dummyMimicEntity.setBiting(false); // ★修正: バイト状態もリセット
                    }
                }
            });
        }
    }
}