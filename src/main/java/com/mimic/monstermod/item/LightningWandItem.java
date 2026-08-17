package com.mimic.monstermod.item;

import com.mimic.monstermod.entity.obj.LightningBoltEntity;
import com.mimic.monstermod.init.ModEntitieType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 電撃の見た目を確認するための道具。
 *
 *  右クリック(空中)       : 視線の先へ電撃を飛ばす
 *  右クリック(ブロック)   : そこへ落雷を落とす
 *  Shift + 右クリック     : 色を切り替える
 *
 * 実体は {@link LightningBoltEntity}。スキルから出したい場合も
 * 同じように生成して setup を呼ぶだけでよい。
 */
public class LightningWandItem extends Item {

    private static final String TAG_COLOR = "bolt_color";

    private static final int[] COLORS = {
            0x3060FF, // 青(参考画像)
            0x60D0FF, // 水色
            0xFFE060, // 黄
            0xC060FF, // 紫
            0xFF5050  // 赤
    };

    /** 視線の先へ飛ばすときの届く距離 */
    private static final double REACH = 26.0;
    /** 落雷が始まる高さ */
    private static final double STRIKE_HEIGHT = 22.0;

    public LightningWandItem() {
        super(new Item.Properties().stacksTo(1));
    }

    private static int colorOf(ItemStack stack) {
        return COLORS[stack.getOrCreateTag().getInt(TAG_COLOR) % COLORS.length];
    }

    /** 空中で右クリック: 視線の先へ電撃を飛ばす。Shiftなら色替え */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        if (player.isShiftKeyDown()) {
            int next = (stack.getOrCreateTag().getInt(TAG_COLOR) + 1) % COLORS.length;
            stack.getOrCreateTag().putInt(TAG_COLOR, next);
            player.displayClientMessage(Component.literal("電撃の色を変えました")
                    .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResultHolder.success(stack);
        }

        // 手元から、見ている方向へ
        Vec3 start = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.5));
        Vec3 end = start.add(player.getViewVector(1.0F).scale(REACH));
        spawn(level, player, start, end, colorOf(stack), 6.0F, 0.16F);

        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.success(stack);
    }

    /** ブロックに右クリック: そこへ落雷を落とす */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos().above();
        Vec3 hit = Vec3.atBottomCenterOf(pos);
        Vec3 start = hit.add(0.0, STRIKE_HEIGHT, 0.0);

        // 落雷は太く、威力も高い
        spawn(level, player, start, hit, colorOf(ctx.getItemInHand()), 12.0F, 0.28F);

        player.getCooldowns().addCooldown(this, 10);
        return InteractionResult.SUCCESS;
    }

    private static void spawn(Level level, Player owner, Vec3 start, Vec3 end,
                              int color, float damage, float thickness) {
        LightningBoltEntity bolt = new LightningBoltEntity(ModEntitieType.LIGHTNING_BOLT.get(), level);
        bolt.setup(owner, start, end, damage, thickness, 12);
        bolt.setColor(color);
        level.addFreshEntity(bolt);

        level.playSound(null, end.x, end.y, end.z,
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.2F, 1.4F);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("右クリック: 視線の先へ電撃").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("ブロックに右クリック: 落雷").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift+右クリック: 色を変える").withStyle(ChatFormatting.DARK_GRAY));
    }
}
