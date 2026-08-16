package com.mimic.monstermod.item;

import com.mimic.monstermod.entity.obj.MegiddoEntity;
import com.mimic.monstermod.init.ModEntitieType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * メギドの見た目を確認するための道具。
 *
 *  ブロックに右クリック : そこにメギドを出す
 *  右クリック(空中)     : 大きさとためる時間を切り替える
 *
 * その場から動かないので、出したあと歩き回って好きな角度から眺められる。
 */
public class MegiddoWandItem extends Item {

    private static final String TAG_SIZE = "megiddo_size";

    /**
     * 球の半径 / ためる時間(秒) / 爆発の巻き込み範囲 / 威力。
     * 飛び散る光はこの範囲の2.9倍あたりまで広がるので、
     * 中でも半径90m近くまで光が届く。
     */
    private static final float[][] SIZES = {
            { 2.0F, 6.0F,  18.0F, 20.0F },  // 小
            { 3.5F, 10.0F, 32.0F, 40.0F },  // 中
            { 6.0F, 16.0F, 55.0F, 70.0F }   // 大
    };

    private static final String[] SIZE_NAMES = { "小", "中", "大" };

    public MegiddoWandItem() {
        super(new Item.Properties().stacksTo(1));
    }

    private static int index(ItemStack stack) {
        return stack.getOrCreateTag().getInt(TAG_SIZE) % SIZES.length;
    }

    /** 空中で右クリック: 大きさを切り替える */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        int next = (index(stack) + 1) % SIZES.length;
        stack.getOrCreateTag().putInt(TAG_SIZE, next);
        player.displayClientMessage(Component.literal("大きさ: " + SIZE_NAMES[next])
                .withStyle(ChatFormatting.YELLOW), true);

        return InteractionResultHolder.success(stack);
    }

    /** ブロックに右クリック: その上にメギドを出す */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        float[] cfg = SIZES[index(ctx.getItemInHand())];
        BlockPos pos = ctx.getClickedPos().above();

        MegiddoEntity megiddo = new MegiddoEntity(ModEntitieType.MEGIDDO.get(), level);
        megiddo.setPos(Vec3.atBottomCenterOf(pos));
        megiddo.configure(cfg[0], Math.round(cfg[1] * 20), Math.round(cfg[2]), cfg[3]);
        level.addFreshEntity(megiddo);

        player.displayClientMessage(Component.literal(
                        Math.round(cfg[1]) + "秒後にはじける")
                .withStyle(ChatFormatting.RED), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("大きさ: " + SIZE_NAMES[index(stack)]).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("ブロックに右クリック: メギドを出す").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("右クリック: 大きさを変える").withStyle(ChatFormatting.DARK_GRAY));
    }
}
