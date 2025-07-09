package net.mimic.monstermod.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.phys.Vec3; // Vec3は念のため維持。他の場所で使う可能性も考慮。

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.PlayerTransformC2SPacket;
import net.mimic.monstermod.util.MonsterKeyBindings;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeEvents {

    private static MimicEntity dummyMimicEntity;

    public static MimicEntity getDummyMimicEntity() {
        return dummyMimicEntity;
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (MonsterKeyBindings.TRANSFORM_KEY.consumeClick()) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                    boolean targetState = !transformation.isTransformed();
                    ModMessages.getChannel().sendToServer(new PlayerTransformC2SPacket(targetState));
                });
            }
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            boolean isMimicForm = transformation.isTransformed()
                    && transformation.getTransformedMobId() != null
                    && transformation.getTransformedMobId().equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"));

            if (isMimicForm) {
                event.setCanceled(true); // 通常のプレイヤー描画をキャンセル

                if (player.level() == null) {
                    return;
                }

                if (dummyMimicEntity == null || dummyMimicEntity.level() != player.level() || dummyMimicEntity.isRemoved()) {
                    dummyMimicEntity = new MimicEntity(ModEntities.MIMIC.get(), player.level());
                }

                // ダミーエンティティの位置と回転をプレイヤーのワールド座標と回転に設定
                // copyPositionはエンティティの多くの状態をコピーするのに役立つ
                dummyMimicEntity.copyPosition(player);

                // プレイヤーの頭と体の回転も正確にコピーする
                dummyMimicEntity.yHeadRot = player.yHeadRot;
                dummyMimicEntity.yBodyRot = player.yBodyRot;

                // Lerp系の回転もコピー (GeckoLibが利用する可能性)
                dummyMimicEntity.yRotO = player.yRotO;
                dummyMimicEntity.xRotO = player.xRotO;

                // setYRotとsetXRotはGeckoLibのアニメーションコントローラーで利用される
                dummyMimicEntity.setYRot(player.getYRot());
                dummyMimicEntity.setXRot(player.getXRot());

                dummyMimicEntity.setDeltaMovement(player.getDeltaMovement());
                dummyMimicEntity.tickCount = player.tickCount; // アニメーション同期のため

                dummyMimicEntity.setOpen(true); // ミミックが変身時に開くアニメーションをトリガー

                Minecraft mc = Minecraft.getInstance();
                EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
                dispatcher.setRenderShadow(false); // 影を非表示にしたい場合

                // ★ここを修正: dispatcher.render() の x, y, z 引数を 0, 0, 0 にする★
                // PoseStackが既に正しい位置に設定されているため、相対座標として0を渡す
                dispatcher.render(
                        dummyMimicEntity,
                        0.0, // X座標オフセット: PoseStackが基準位置を既に設定しているので0で良い
                        0.0, // Y座標オフセット
                        0.0, // Z座標オフセット
                        player.getYRot(), // レンダリング時のY回転はプレイヤーのものを利用
                        event.getPartialTick(), // 部分ティック
                        event.getPoseStack(), // ポーズスタック (変換行列)
                        event.getMultiBufferSource(), // マルチバッファソース
                        event.getPackedLight() // パックされた光源情報
                );
            } else {
                if (dummyMimicEntity != null) {
                    dummyMimicEntity.setOpen(false); // 変身解除時は閉じるアニメーションをトリガー
                }
            }
        });
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                boolean isMimicForm = transformation.isTransformed()
                        && transformation.getTransformedMobId() != null
                        && transformation.getTransformedMobId().equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"));

                if (isMimicForm) {
                    event.setCanceled(true); // 変身中は手を描画しない
                }
            });
        }
    }
}