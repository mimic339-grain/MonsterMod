package com.mimic.monstermod.item;

import com.mimic.monstermod.bomb.BombInstance;
import com.mimic.monstermod.bomb.BombKind;
import com.mimic.monstermod.bomb.BombStore;
import com.mimic.monstermod.bomb.BombTiming;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * 設置する大型ボム(ボマーのスキル5で手に入る)。
 *
 *  右クリック(空中)     : 時間を切り替える(30秒 → 1分 → 2分)
 *  ブロックに右クリック : その上に設置する。置いたら壊せない
 *
 * 【時間が長いほど大きく爆発する】
 * 早く起爆したいなら小さく、大きく吹き飛ばしたいなら長く待つ、という選択になる。
 * 半径は {@link BombTiming#radiusForFuse} が決めている。
 *
 * 設置したものは火打ち石で3秒後に起爆できる(自爆覚悟の早撃ち)。
 * その処理は {@link com.mimic.monstermod.block.PlacedBombBlock} 側にある。
 */
public class PlacedBombItem extends Item {

    private static final String TAG_SECONDS = "fuse_seconds";

    /** 選べる設置時間(秒)。長いほど爆発半径が大きい */
    public static final int[] FUSE_SECONDS = { 30, 60, 120 };

    public PlacedBombItem() {
        super(new Item.Properties().stacksTo(1));
    }

    private static int index(ItemStack stack) {
        return stack.getOrCreateTag().getInt(TAG_SECONDS) % FUSE_SECONDS.length;
    }

    public static int selectedSeconds(ItemStack stack) {
        return FUSE_SECONDS[index(stack)];
    }

    /** 空中で右クリック: 設置時間を切り替える */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        int next = (index(stack) + 1) % FUSE_SECONDS.length;
        stack.getOrCreateTag().putInt(TAG_SECONDS, next);

        int sec = FUSE_SECONDS[next];
        float radius = BombTiming.radiusForFuse(sec * BombTiming.TICKS_PER_SECOND);
        player.displayClientMessage(Component.literal(
                        sec + "秒 / 半径 " + String.format("%.1f", radius))
                .withStyle(ChatFormatting.AQUA), true);

        return InteractionResultHolder.success(stack);
    }

    /** ブロックに右クリック: その上に設置する */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) {
            player.displayClientMessage(Component.literal("ここには置けない")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }

        ItemStack stack = ctx.getItemInHand();
        int seconds = selectedSeconds(stack);
        int fuse = seconds * BombTiming.TICKS_PER_SECOND;

        level.setBlockAndUpdate(pos,
                com.mimic.monstermod.init.ModBlocks.PLACED_BOMB.get().defaultBlockState());

        // タイマーは設置した瞬間から動き出す(armed=true)
        BombStore.get(serverLevel).put(pos, new BombInstance(
                BombKind.PLACED, player.getUUID(), fuse,
                BombTiming.radiusForFuse(fuse), true));

        level.playSound(null, pos, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 0.8F);
        player.displayClientMessage(Component.literal(seconds + "秒後に爆発する")
                .withStyle(ChatFormatting.RED), true);

        if (!player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int sec = selectedSeconds(stack);
        float radius = BombTiming.radiusForFuse(sec * BombTiming.TICKS_PER_SECOND);
        tooltip.add(Component.literal("設定: " + sec + "秒 / 半径 " + String.format("%.1f", radius))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("右クリック: 時間を切り替える").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("ブロックに右クリック: 設置(壊せなくなる)").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("設置後は火打ち石で3秒起爆").withStyle(ChatFormatting.DARK_GRAY));
        // 置ける場所の目安として、置き換え可能なブロックの例を出しておく
        if (flag.isAdvanced()) {
            tooltip.add(Component.literal("(" + Blocks.AIR.getName().getString() + " など空きマスに設置)")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
