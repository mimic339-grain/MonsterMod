package com.mimic.monstermod.entity.hitbox;

import com.mimic.monstermod.entity.BaseEntity;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.world.entity.player.Player;

/**
 * モンスターに変身したプレイヤーの部位当たり判定を毎tick更新する。
 *
 * 変身したプレイヤーの見た目は「Capabilityが保持する見た目用プロキシEntity」を
 * 描画したものであり、そのプロキシはワールドに存在しないため tick() が呼ばれない。
 * よってアニメーションの進行(updateActiveAnimation)もここから明示的に駆動する。
 *
 * クライアント・サーバーの両方から呼ぶこと(攻撃対象の選択はクライアントが行うため)。
 */
public final class TransformedPlayerHitboxes {

    private TransformedPlayerHitboxes() {}

    public static void tick(Player player) {
        if (!(player instanceof IHitboxPartOwner owner)) return;
        BoneHitboxPart[] parts = owner.monstermod$getHitboxParts();
        if (parts == null) return;

        var transformation = player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).orElse(null);
        if (transformation == null || !transformation.isTransformed()) {
            deactivateAll(parts);
            return;
        }

        BoneHitboxRegistry.Rig rig = BoneHitboxRegistry.get(transformation.getMobId());
        if (rig == null || !rig.rigData().isLoaded()) {
            deactivateAll(parts);
            return;
        }

        // 見た目用プロキシからアニメーション状態を取る(描画と同じ情報源にすることで
        // 見た目と当たり判定がズレないようにする)
        BaseEntity proxy = transformation.getEntity(player.level());
        if (proxy == null) {
            deactivateAll(parts);
            return;
        }
        proxy.tickCount = player.tickCount;
        proxy.updateActiveAnimation();

        String animation = proxy.getActiveAnimation();
        double elapsed = proxy.getAnimationElapsedSeconds(rig.rigData());

        // 変身先に応じて各パーツへ設定を割り当てる(必要数だけ有効化し、余りは休眠)
        var configs = rig.parts();
        for (int i = 0; i < parts.length; i++) {
            if (i < configs.size()) {
                parts[i].activate(configs.get(i), player);
            } else if (parts[i].isEnabled()) {
                parts[i].deactivate();
            }
        }

        // 【重要】yawは必ず yBodyRot(胴体の向き)を使うこと。
        // BaseIdentity.renderは entity.yBodyRot を GeckoLib へ渡してモデルを回すため、
        // getYRot()(頭/視線の向き)を使うと首を振っただけで判定と見た目がズレる。
        BoneHitboxUpdater.update(rig.rigData(), parts, animation, elapsed,
                player.position(), player.yBodyRot);
    }

    private static void deactivateAll(BoneHitboxPart[] parts) {
        for (BoneHitboxPart part : parts) {
            if (part.isEnabled()) part.deactivate();
        }
    }
}
