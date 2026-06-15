package com.mimic.monstermod.mixin.player;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {/*
    @Inject(
            method = "getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void monstermod$overrideItemInHand(
            InteractionHand hand,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        LivingEntity entity = (LivingEntity)(Object)this;

        // ---- クライアント限定 ----
        if (!entity.level().isClientSide) return;

        // ---- Player 限定 ----
        if (!(entity instanceof Player player)) return;

        // ---- メインハンドのみ ----
        if (hand != InteractionHand.MAIN_HAND) return;

        HunterTransformation hunter = player
                .getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION)
                .orElse(null);

        if (hunter == null) {
            System.out.println("[MonsterMod] HunterTransformation = null");
            return;
        }

        if (!hunter.isActive()) {
            System.out.println("[MonsterMod] Hunter not active");
            return;
        }

        if (hunter.isSheathed()) {
            System.out.println("[MonsterMod] Hunter weapon is sheathed");
            return;
        }

        ItemStack weapon = hunter.getWeaponSlot();

        // ---- weapon の中身をログ出力 ----
        System.out.println(
                "[MonsterMod] weapon stack = " + weapon +
                        " | item = " + weapon.getItem() +
                        " | count = " + weapon.getCount() +
                        " | empty = " + weapon.isEmpty()
        );

        if (weapon.isEmpty()) return;

        // ★ 描画用に必ず copy()
        ItemStack renderStack = weapon.copy();

        System.out.println(
                "[MonsterMod] return renderStack = " + renderStack +
                        " | item = " + renderStack.getItem() +
                        " | count = " + renderStack.getCount()
        );

        cir.setReturnValue(renderStack);
    }*/
}
