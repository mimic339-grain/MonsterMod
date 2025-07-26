package net.mimic.monstermod.mixin; // パッケージ名を確認

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags; // 必要であればインポート
import net.minecraft.util.Mth; // Mth (MathHelper) のインポート

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // Mixinの対象であるLivingEntityインスタンス自身を取得するヘルパーメソッド
    // @SuppressWarnings("unchecked") が必要な場合があります
    private LivingEntity self() {
        return (LivingEntity)(Object)this;
    }

    // `player.playStepSound` の修正
    // このInjectポイントは仮です。`monstermod.mixins.json` の設定に合わせてください。
    // 例: LivingEntityが足を動かす、または移動するようなメソッドにフック
    @Inject(method = "travel", at = @At("TAIL")) // `travel` メソッドの最後にインジェクトする例
    private void mimichandleTravel(CallbackInfoReturnable<Float> cir) {
        LivingEntity player = self();

        // プレイヤーの足元の座標計算
        // Y座標は double から int への変換が必要
        int blockY = Mth.floor(player.getY() - 0.2); // 小数点以下を切り捨てて int に変換
        BlockPos playerFootPos = new BlockPos(player.getBlockX(), blockY, player.getBlockZ());

        // `player.level` -> `player.level()` (getterメソッドを使用)
        // `player.onGround` -> `player.isOnGround()` (getterメソッドを使用)
        // playStepSoundの第2引数は`BlockState`です。`player.isOnGround()` (boolean) ではありません。
        // おそらくプレイヤーの足元にあるブロックのBlockStateを取得したいのだと思います。
        player.playStepSound(playerFootPos, player.level().getBlockState(playerFootPos));
    }

    // `player.actualHurt` の修正
    // これは`LivingEntity`のダメージ処理に関わるメソッドへのインジェクトであると仮定します。
    // 元のMixinの目的と`monstermod.mixins.json`の設定に合わせて調整してください。
    // `actualHurt` はMinecraftのプライベートな内部メソッドです。ダメージを与えるには`hurt`を使用します。
    // ここでは`LivingEntity`の`hurt`メソッドをオーバーライドするようなミックスインは行わず、
    // `LivingEntity`がダメージを受けた際の別の処理を挿入する例として記述します。
    // 例えば、`LivingEntity#hurt`の呼び出し時に独自のロジックを追加する場合:
    /*
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void mimicApplyDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = self();
        // ここで、entityが変身中かどうかを判断するロジックを実装します。
        // もし変身中であれば、ダメージを調整したり、別の処理を行ったりします。
        // 例: if (entity instanceof Player && ((Player)entity).getPersistentData().getBoolean("IsMimicTransformed")) {
        //     // ダメージをキャンセルする、あるいは調整して再適用するなど
        //     // cir.setReturnValue(false); // ダメージをキャンセルする場合
        // }
    }
    */

    // `cir.setReturnValue(morphEntity.isUndead());` の修正
    // `morphEntity` の型と目的が不明ですが、一般的なアンデッド判定を例として示します。
    // Mixinの対象メソッドとフックポイントに合わせて調整してください。
    /*
    // 例: LivingEntity#isUndead のようなメソッド（もしあれば）へのインジェクト、または特定の判定時
    @Inject(method = "isUndead", at = @At("HEAD"), cancellable = true)
    private void mimicIsUndead(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = self();
        // ここで、entityが変身中かどうかを判断し、変身先のモブがアンデッドであるかをチェックします。
        // 仮に、プレイヤーがミミックに変身している場合、アンデッドではないとしたい場合:
        // if (entity instanceof Player && ((Player)entity).getPersistentData().getBoolean("IsMimicTransformed")) {
        //     cir.setReturnValue(false); // ミミックはアンデッドではない
        //     return;
        // }
        // もし変身先のエンティティのタイプをチェックしたい場合:
        // if (someTransformedEntity != null && someTransformedEntity.getType().is(EntityTypeTags.UNDEAD)) {
        //     cir.setReturnValue(true);
        //     return;
        // }
    }
    */
}