package net.mimic.monstermod.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mimic.monstermod.MonsterMod;
// 修正: ModEntities の正しいパッケージパスに修正
import net.mimic.monstermod.common.entity.ModEntities;
import net.mimic.monstermod.common.entity.MimicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation; // ResourceLocation をインポート

import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.model.GeoModel;

public class PlayerMorphRenderer extends GeoEntityRenderer<MimicEntity> {

    public PlayerMorphRenderer(EntityRendererProvider.Context renderContext) {
        // MimicEntityModel クラスが net.mimic.monstermod.client.model パッケージにあると仮定
        // ここでの MimicEntityModel のパスも再確認してください。
        super(renderContext, new net.mimic.monstermod.client.model.MimicEntityModel());
    }

    public void renderTransformedPlayer(Player player, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // ModEntities.MIMIC.get() が MimicEntity の EntityType を返すことを前提とします。
        MimicEntity dummyMimicEntity = new MimicEntity(ModEntities.MIMIC.get(), player.level());

        dummyMimicEntity.copyPosition(player);
        dummyMimicEntity.setXRot(player.getXRot());
        dummyMimicEntity.setYRot(player.getYRot());
        dummyMimicEntity.yHeadRot = player.yHeadRot;
        dummyMimicEntity.yBodyRot = player.yBodyRot;

        // 変身状態をプレイヤーのNBTからダミーエンティティに同期 (仮の実装)
        if (player.getPersistentData().contains("MonsterModMimicState")) {
            try {
                dummyMimicEntity.setCurrentAnimationState(
                        MimicEntity.MimicAnimationState.valueOf(player.getPersistentData().getString("MonsterModMimicState")));
            } catch (IllegalArgumentException e) {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.sendSystemMessage(Component.literal("DEBUG: 無効なMimicState: " + player.getPersistentData().getString("MonsterModMimicState")));
                }
            }
        }
        if (player.getPersistentData().contains("MonsterModIsBiting")) {
            dummyMimicEntity.setBiting(player.getPersistentData().getBoolean("MonsterModIsBiting"));
        }

        super.render(dummyMimicEntity, player.getYRot(), partialTicks, poseStack, buffer, packedLight);
    }

    // 修正: getTextureLocation メソッドをオーバーライド
    @Override
    public ResourceLocation getTextureLocation(MimicEntity entity) {
        // MimicEntity クラスに TEXTURE_RESOURCE が public static final で定義されている必要があります
        return MimicEntity.TEXTURE_RESOURCE;
    }
}