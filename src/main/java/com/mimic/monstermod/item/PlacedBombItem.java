package com.mimic.monstermod.item;

import com.mimic.monstermod.block.PlacedBombBlockEntity;
import com.mimic.monstermod.init.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 設置する大型ボム(ボマーのスキル5で手に入る)。
 *
 * ブロックに右クリックで置くだけ。置いた時点ではまだ時間が決まっておらず、
 * 置いたボムを右クリックして初めて起爆時間を選ぶ({@link com.mimic.monstermod.client.BombTimerScreen})。
 *
 * 【アイテム側で時間を決めない理由】
 * 置く前に決める形だと、置き直したいときに拾い直す必要があり、
 * そもそも置いたボムは壊せないので詰んでしまう。
 * 「置く → 決める」の順にすると、置いてから落ち着いて選べる。
 */
public class PlacedBombItem extends Item {

    public PlacedBombItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (!level.getBlockState(pos).canBeReplaced()) {
            player.displayClientMessage(Component.literal("ここには置けない")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.FAIL;
        }

        level.setBlockAndUpdate(pos, ModBlocks.PLACED_BOMB.get().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof PlacedBombBlockEntity be) {
            be.setOwner(player.getUUID());
        }

        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        player.displayClientMessage(Component.literal("設置した。右クリックで起爆時間を決める")
                .withStyle(ChatFormatting.YELLOW), true);

        if (!player.isCreative()) ctx.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ブロックに右クリック: 設置").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("設置後に右クリック: 起爆時間を決める").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("火打ち石で右クリック: 即爆").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("置いたら壊せない(解除キットのみ)").withStyle(ChatFormatting.RED));
    }
}
