package net.mimic.monstermod.client.renderer.identity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.capability.PlayerTransformation;

/**
 * プレイヤーが変身したMobのレンダリングロジックを定義するインターフェース。
 * 複数Monsterに対応し、アニメーション状態を考慮したレンダラーを提供します。
 */
public interface IPlayerIdentityRenderer<T extends IPlayerIdentity> {

    /**
     * 変身後のMobのモデルを描画します。
     *
     * @param identity 描画するIdentityインスタンス
     * @param entity レンダリング対象のEntity（通常はプレイヤー）
     * @param entityYaw エンティティのY軸の回転
     * @param partialTicks 部分ティック
     * @param poseStack ポーズスタック
     * @param buffer バッファソース
     * @param packedLight パックされた光のデータ
     * @param state 描画対象Monsterの状態（アニメーションや攻撃中フラグ）
     */
    void render(T identity,
                LivingEntity entity,
                float entityYaw,
                float partialTicks,
                PoseStack poseStack,
                MultiBufferSource buffer,
                int packedLight,
                PlayerTransformation.MonsterState state);
}
