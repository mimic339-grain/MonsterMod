package com.mimic.monstermod.block;

import com.mimic.monstermod.bomb.BombInstance;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 設置された大型ボムのブロック。
 *
 * 【壊せない理由】
 * 掘って無かったことにできると仕掛けとして成立しないため、硬さを -1(破壊不可)にしている。
 * 爆発耐性も高くしてあるので、他の爆発に巻き込まれても消えない。
 * 止めたい場合は解除キットを使う。
 *
 * 右クリックで残り時間が分かり、火打ち石を持って右クリックすると3秒後に起爆する
 * (自爆覚悟の早撃ち)。
 *
 * タイマーそのものは {@link BombStore} が座標で持っており、
 * {@link com.mimic.monstermod.bomb.BombTicker} が進めている。
 * ブロック側はあくまで見た目と操作の受け口。
 */
public class PlacedBombBlock extends Block {

    /** 火打ち石で起爆したときの猶予 */
    private static final int MANUAL_FUSE_TICKS = 3 * BombTiming.TICKS_PER_SECOND;

    public PlacedBombBlock() {
        super(Properties.copy(net.minecraft.world.level.block.Blocks.TNT)
                .strength(-1.0F, 3600000.0F)  // 破壊不可・爆発でも消えない
                .noLootTable());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        BombStore store = BombStore.get(serverLevel);
        BombInstance bomb = store.at(pos);
        if (bomb == null) {
            // 何らかの理由でタイマーだけ失われた場合は、置物として残しておく意味がないので消す
            level.removeBlock(pos, false);
            return InteractionResult.SUCCESS;
        }

        // 火打ち石を持っていれば強制起爆。周りに味方がいると自分も巻き込まれる
        if (player.getItemInHand(hand).is(Items.FLINT_AND_STEEL)) {
            bomb.detonateIn(MANUAL_FUSE_TICKS);
            store.setDirty();

            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.literal("3秒後に起爆する")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.SUCCESS;
        }

        player.displayClientMessage(Component.literal(
                        "残り " + BombTiming.format(bomb.getFuseTicks())
                                + " / 半径 " + String.format("%.1f", bomb.getRadius()))
                .withStyle(ChatFormatting.YELLOW), true);
        return InteractionResult.SUCCESS;
    }

    /** 何かの拍子にブロックだけ消えた場合、タイマーも道連れにして残骸を作らない */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            BombStore.get(serverLevel).remove(pos);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
