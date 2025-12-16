package com.mimic.monstermod.mixin.player;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.mimic.monstermod.capability.HunterTransformation.isHunter;
/**
 * todo修正したい　これのままだとなぜかmimicがノックバックする　ただクリティカルは消えてる
 * attack(Entity) の開始時点でフックして、Hunter状態なら
 * Vanilla のクールタイム・攻撃補正・スイープを無効化
 */
@Mixin(Player.class)
public abstract class PlayerAttackMixin {

    /**
     * attack(Entity) の開始時点でフックして、Hunter状態なら
     * Vanilla のクールタイム・攻撃補正・スイープを無効化
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void hunterAttackOverride(net.minecraft.world.entity.Entity target, CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // Hunter でないなら何もしない
        if (!isHunter(player)) return;

        // 攻撃対象が空ならスキップ
        if (target == null || !target.isAttackable()) {
            ci.cancel();
            return;
        }

        // ===== ダメージ固定処理（自作で調整可能） =====
        float baseDamage = (float) player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

        // 武器に応じた追加補正や自作スキルによる補正をここで入れる
        ItemStack weapon = player.getMainHandItem();
        float damage = baseDamage;
        if (weapon != null && !weapon.isEmpty()) {
            // ここで WeaponItem やカスタムスキルによる攻撃力調整可能
            damage *= 1.0F; // 例：倍率1.0でそのまま
        }

        // ノックバック、炎などは必要に応じて自作
        float knockback = 0.5F;
        if (target instanceof LivingEntity living) {
            living.hurt(player.damageSources().playerAttack(player), damage);
            // ノックバック
            Vec3 motion = new Vec3(
                    -Math.sin(Math.toRadians(player.getYRot())) * knockback,
                    0.1,
                    Math.cos(Math.toRadians(player.getYRot())) * knockback
            );
            living.push(motion.x, motion.y, motion.z);
        } else {
            target.hurt(player.damageSources().playerAttack(player), damage);
        }

        // Client/Server 双方の攻撃結果同期は通常通り Vanilla に任せるか
        // 独自にパケット同期する場合はここで呼ぶ

        // Vanilla の attack() をキャンセル
        ci.cancel();
    }
}
