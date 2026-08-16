package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.level.Level;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract Level level();

    /**
     * Entity#isOnFire() 自体にインジェクション。
     * 「クライアント側の描画処理中」であれば、変身中のプレイヤーに対して false を返す。
     */
    @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
    private void hideFireEffectOnClient(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;

        // 1. クライアント側（描画スレッド）であることを確認
        // 2. プレイヤーであることを確認
        if (self.level() != null && self.level().isClientSide() && self instanceof Player player) {

            // 3. 変身中かどうかをチェック
            player.getCapability(CapabilityRegistry.PLAYER_TRANSFORMATION).ifPresent(trans -> {
                // 変身先の体を持つ場合のみ隠す。
                // ボマーのように見た目がプレイヤーのままの役職は、普通に燃えて見えるべき
                if (trans.isTransformed()
                        && (trans.getIdentity() == null || trans.getIdentity().hasOwnBody())) {
                    // ★ 描画システムに対して「燃えてないよ」と嘘を吐く
                    cir.setReturnValue(false);
                }
            });
        }
        // ここで cancel しなければ、通常通りバニラの isOnFire ロジックが走るため
        // サーバー側（ダメージ計算）には一切影響を与えない。　故に燃えてないっていうことにしてもいい
    }
}