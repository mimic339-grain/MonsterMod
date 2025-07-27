package net.mimic.monstermod.client.renderer.identity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.mimic.monstermod.identity.IPlayerIdentity;

/**
 * プレイヤーが変身したMobのレンダリングロジックを定義するインターフェース。
 * 各IPlayerIdentityに対応するレンダラーを提供します。
 */
public interface IPlayerIdentityRenderer<T extends IPlayerIdentity> {
    /**
     * 変身後のMobのモデルを描画します。
     * @param identity 描画するIdentityインスタンス（ここではシングルトン）
     * @param entity レンダリング対象のLivingEntity（通常はプレイヤー）
     * @param entityYaw エンティティのY軸の回転
     * @param partialTicks 部分ティック
     * @param poseStack ポーズスタック
     * @param buffer バッファソース
     * @param packedLight パックされた光のデータ
     */
    void render(T identity, LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight);
}