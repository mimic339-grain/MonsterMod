package com.mimic.monstermod.block;

import com.mimic.monstermod.bomb.BombTiming;
import com.mimic.monstermod.init.ModBlockEntities;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_OpenBombTimerPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 設置された大型ボムのブロック。
 *
 * 【操作】
 *  右クリック(素手)     : 時間を決める画面を開く。決めた瞬間からカウントが始まる
 *  右クリック(火打ち石) : 即座に爆発させる(自爆覚悟の早撃ち)
 *  設置済みで時間決定後 : 右クリックすると残り時間が分かる
 *
 * 【壊せない】
 * 掘って無かったことにできると仕掛けとして成立しないので、硬さを -1 にしてある。
 * ただしクリエイティブは硬さを無視して壊せてしまうため、
 * 破壊イベント自体も {@link BombBlockProtection} で止めている。
 * 止めたい場合は解除キットを使う。
 */
public class PlacedBombBlock extends Block implements EntityBlock {

    public PlacedBombBlock() {
        super(Properties.copy(Blocks.TNT)
                .strength(-1.0F, 3600000.0F)  // 破壊不可・爆発でも消えない
                .noLootTable());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof PlacedBombBlockEntity be)) {
            return InteractionResult.SUCCESS;
        }

        // 連鎖ボムは自分では起爆できない。他の爆発を受けたときだけ動く駒として扱う
        if (be.getKind() == com.mimic.monstermod.bomb.BombKind.CHAIN) {
            player.displayClientMessage(Component.literal(
                            "連鎖ボムは時間を決められない（他の爆発を受けると半径"
                                    + (int) com.mimic.monstermod.bomb.BombKind.CHAIN_RADIUS + "で爆発）")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        }

        // 火打ち石は問答無用で即爆。周りに味方がいると自分も巻き込まれる
        if (player.getItemInHand(hand).is(Items.FLINT_AND_STEEL)) {
            be.detonateNow();
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }

        if (be.isArmed()) {
            player.displayClientMessage(Component.literal(
                            "残り " + BombTiming.format(be.getFuseTicks())
                                    + " / 半径 " + String.format("%.1f", be.getRadius()))
                    .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.SUCCESS;
        }

        // まだ時間が決まっていない → 設定画面を開く
        if (player instanceof ServerPlayer sp) {
            ModMessages.sendToPlayer(new S2C_OpenBombTimerPacket(pos), sp);
        }
        return InteractionResult.SUCCESS;
    }

    // ---------------- BlockEntity ----------------

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlacedBombBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        // カウントダウンはサーバー側だけで進める
        if (level.isClientSide) return null;
        return type == ModBlockEntities.PLACED_BOMB.get()
                ? (lvl, pos, st, be) -> PlacedBombBlockEntity.serverTick(lvl, pos, st, (PlacedBombBlockEntity) be)
                : null;
    }

    /** ピストンで押されて消える、といった事故を防ぐ */
    @Override
    public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state) {
        return net.minecraft.world.level.material.PushReaction.BLOCK;
    }

    @Override
    public float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }
}
