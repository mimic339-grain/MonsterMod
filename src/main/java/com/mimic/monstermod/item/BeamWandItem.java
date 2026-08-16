package com.mimic.monstermod.item;

import com.mimic.monstermod.entity.obj.BeamEntity;
import com.mimic.monstermod.init.ModEntitieType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * ビームの見た目と当たり判定を確認するための道具。
 *
 *  右クリック         : 黄色いビームを撃つ(視点に追従して伸びる)
 *  Shift + 右クリック : 色を切り替える(黄→青→緑→紫→赤→黄)
 *
 * 実体は {@link BeamEntity}。スキルから撃たせたい場合も同じように
 * BeamEntity を生成して fireFrom を呼ぶだけでよい。
 */
public class BeamWandItem extends Item {

    private static final String TAG_COLOR = "beam_color";

    /** 切り替えで使う色。先頭が参考画像に近い黄橙色 */
    private static final int[] COLORS = {
            0xFFB020, // 黄橙
            0x40A0FF, // 水色
            0x60FF80, // 緑
            0xC060FF, // 紫
            0xFF4040  // 赤
    };

    public BeamWandItem() {
        super(new Item.Properties().stacksTo(1));
    }

    private static int getColorIndex(ItemStack stack) {
        return stack.getOrCreateTag().getInt(TAG_COLOR) % COLORS.length;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 色替えは持ち替えずに行えるようにShift+右クリックへ割り当てる
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                int next = (getColorIndex(stack) + 1) % COLORS.length;
                stack.getOrCreateTag().putInt(TAG_COLOR, next);
                player.displayClientMessage(Component.literal("ビームの色を変えました")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return InteractionResultHolder.success(stack);
        }

        if (!level.isClientSide) {
            BeamEntity beam = new BeamEntity(ModEntitieType.BEAM.get(), level);
            // 長さ48 / 太さ0.35 / 1回4ダメージ / 4tickごと / 40tick(2秒)持続
            beam.fireFrom(player, 48.0F, 0.35F, 4.0F, 4, 40);
            beam.setColor(COLORS[getColorIndex(stack)]);
            level.addFreshEntity(beam);

            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
        }

        player.getCooldowns().addCooldown(this, 45);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("右クリック: ビームを撃つ").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift+右クリック: 色を変える").withStyle(ChatFormatting.DARK_GRAY));
    }
}
