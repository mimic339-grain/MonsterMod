package com.mimic.monstermod.item.weapon;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_SyncHunterSlotPacket;
import com.mimic.monstermod.weapon.WeaponCategory;
import com.mimic.monstermod.weapon.WeaponCategoryUtil;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.server.level.ServerPlayer;
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
 * WeaponItem 完全版
 * - カテゴリ保持
 * - GeoRenderer 提供
 * - 右クリック・左クリックで WeaponSlot に装備
 * - 既存装備があればインベントリに戻す
 */
public abstract class WeaponItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private final WeaponCategory category;

    public WeaponItem(Properties props, WeaponCategory category) {
        super(props);
        this.category = category;

        // Weapon → Category の登録
        WeaponCategoryUtil.registerCategoryItem(category, this);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}

    public WeaponCategory getCategory() {
        return category;
    }

    public WeaponCategory getCategory(ItemStack stack) {
        return category;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return getRenderer();
            }
        });
    }

    public abstract GeoItemRenderer<? extends WeaponItem> getRenderer();

    public BlockEntityWithoutLevelRenderer getStackRenderer(ItemStack stack) {
        return getRenderer();
    }

    /**
     * 右クリック・左クリックで WeaponSlot に装備
     * 左右どちらでもこのメソッドで処理
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide && player instanceof ServerPlayer serverPlayer) {
            player.getCapability(com.mimic.monstermod.variable.CapabilityRegistry.HUNTER_TRANSFORMATION)
                    .ifPresent(hunter -> {
                        if (!hunter.isActive()) return;

                        ItemStack held = player.getItemInHand(hand);
                        ItemStack currentSlot = hunter.getWeaponSlot();

                        // もしスロットにすでに装備中のアイテムがある場合、インベントリに戻す
                        if (!currentSlot.isEmpty()) {
                            if (!player.addItem(currentSlot.copy())) {
                                player.drop(currentSlot.copy(), false); // インベントリがいっぱいならドロップ
                            }
                        }

                        // 新しいアイテムを WeaponSlot に装備
                        hunter.setWeaponSlot(held.copy(), player);

                        // サーバー→クライアント同期（S2C）
                        ModMessages.sendToPlayer(new S2C_SyncHunterSlotPacket(hunter.getWeaponSlot()), serverPlayer);
                    });
        }

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

}
