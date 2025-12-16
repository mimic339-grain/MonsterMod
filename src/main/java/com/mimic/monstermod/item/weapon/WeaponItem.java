package com.mimic.monstermod.item.weapon;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.client.C2S_SetWeaponSlotPacket;
import com.mimic.monstermod.weapon.WeaponCategory;
import com.mimic.monstermod.weapon.WeaponCategoryUtil;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.renderer.GeoItemRenderer;

import java.util.function.Consumer;

/**
 * WeaponItem（完全版・最終設計）
 *
 * - UI / Menu / Slot に一切依存しない
 * - Capability を唯一の真実とする
 * - S2C パケットでクライアント描画を同期
 */
public abstract class WeaponItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache =
            new SingletonAnimatableInstanceCache(this);

    private final WeaponCategory category;

    protected WeaponItem(Properties props, WeaponCategory category) {
        super(props);
        this.category = category;
        WeaponCategoryUtil.registerCategoryItem(category, this);
    }

    /* ===============================
     *  GeoItem
     * =============================== */

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    public abstract GeoItemRenderer<? extends WeaponItem> getRenderer();

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return getRenderer();
            }
        });
    }

    /* ===============================
     *  Category
     * =============================== */

    public WeaponCategory getCategory() {
        return category;
    }

    /* ===============================
     *  右クリック装備
     * =============================== */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (held.isEmpty()) return InteractionResultHolder.pass(held);

        if (!level.isClientSide) {
            ModMessages.sendToServer(new C2S_SetWeaponSlotPacket(held));
        }

        return InteractionResultHolder.sidedSuccess(held, level.isClientSide());
    }

}
