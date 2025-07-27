package net.mimic.monstermod.client.renderer.identity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.player.Player;
import net.mimic.monstermod.entity.ModEntities;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.impl.MimicIdentity;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Mimicに変身したプレイヤーを描画するためのレンダラー。
 * ダミーのMimicEntityを使用してGeckolibのレンダリングを行います。
 */
public class MimicPlayerRenderer implements IPlayerIdentityRenderer<MimicIdentity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private MimicEntity dummyMimicEntity; // 各プレイヤーのレンダリングに共有されるダミーエンティティ

    @Override
    public void render(MimicIdentity identity, Player player, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // ★ClientForgeEvents.java にあったダミーエンティティの初期化ロジックをここに移動
        // ただし、ClientForgeEventsのonRenderPlayerが呼ばれるたびにダミーが作られるため、
        // キャッシュの概念を導入。player.level()が一致しない場合は再生成。
        if (dummyMimicEntity == null || dummyMimicEntity.level() != player.level()) {
            dummyMimicEntity = new MimicEntity(ModEntities.MIMIC.get(), player.level());
            LOGGER.debug("New dummyMimicEntity created for MimicPlayerRenderer.");
        }

        // ダミーエンティティの位置と回転をプレイヤーに合わせる
        dummyMimicEntity.setPos(player.getX(), player.getY(), player.getZ());
        dummyMimicEntity.setYRot(player.getYRot());
        dummyMimicEntity.setXRot(player.getXRot());
        dummyMimicEntity.yHeadRot = player.yHeadRot;
        dummyMimicEntity.yBodyRot = player.yBodyRot;
        dummyMimicEntity.setYBodyRot(player.yBodyRot); // Body rotation sync
        dummyMimicEntity.setDeltaMovement(player.getDeltaMovement()); // 移動状態も同期

        // ダミーエンティティにIdentityの状態を同期
        dummyMimicEntity.setCurrentAnimationState(identity.getMimicAnimationState());
        dummyMimicEntity.setBiting(identity.isBiting());

        // レンダリング
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        @SuppressWarnings("unchecked")
        EntityRenderer<MimicEntity> renderer = (EntityRenderer<MimicEntity>) dispatcher.getRenderer(dummyMimicEntity);

        poseStack.pushPose();

        // モデルのオフセット調整 (必要に応じて微調整)
        // Mimicモデルの高さとプレイヤーモデルの高さの違いを考慮して調整
        // MimicEntityのヒットボックスサイズ (0.7f, 0.7f) とプレイヤー (0.6f, 1.8f) を考慮
        // Mimicのベースをプレイヤーの足元に合わせる調整
        // 例: player.getBbHeight() は約1.8F, dummyMimicEntity.getBbHeight() は約0.7F
        // その差を埋めるためにY軸を調整する必要があるかもしれません。
        // poseStack.translate(0.0, - (player.getBbHeight() - identity.getBoundingBoxDimensions(player.getPose()).y) / 2.0, 0.0);
        // あるいは、Mimicモデルの原点とプレイヤーの原点の関係で調整
        // Identity Modでは通常、プレイヤーの足元がエンティティの足元になるようにモデルを調整します。
        // Geckolibモデルによっては原点が異なるため、実際にゲーム内で確認し調整が必要です。
        poseStack.translate(0.0, 0.0, 0.0); // 暫定

        renderer.render(dummyMimicEntity, player.getYRot(), partialTicks, poseStack, buffer, packedLight);

        poseStack.popPose();
    }

    @Override
    public void renderHandItem(MimicIdentity identity, Player player, PoseStack poseStack, MultiBufferSource buffer, int packedLight, float partialTicks, net.minecraft.world.InteractionHand hand, net.minecraft.world.item.ItemStack itemStack) {
        // Mimicのモデルに合わせてアイテムのレンダリング位置を調整
        // これはかなり複雑になるため、最初は省略しても良いでしょう。
        // Identity Modでは、各Mobのボーン構造に合わせてアイテムをアタッチするロジックがあります。
        // 例として、Mimicの「口」や「手」のボーンにアイテムをマッピングするなどの処理。
        // 現状、Mimicはアイテムを持つ概念が薄いため、この実装は低優先度でも良いかもしれません。
    }

    // ClientForgeEventsから呼ばれるダミーエンティティのリセットは不要になる
    // PlayerIdentityRendererが生存期間を管理するため
}