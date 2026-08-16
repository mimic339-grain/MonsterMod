package com.mimic.monstermod.identity.bomber;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.bomb.BombAttachment;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ボマーのスキルが実際に効果を出す場所。
 *
 * スキルを押しただけでは何も起きず、ここで「次の行動」を拾って仕掛ける。
 * 押した瞬間に発動しないのは、狙った相手・狙った場所に確実に仕掛けたいから。
 *
 * 仕掛ける処理と、仕掛けたものが起動する処理の両方をここに置いている
 * (どちらもプレイヤーの操作を拾う必要があり、離すと追いづらくなるため)。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class BomberEvents {

    /** ブロックの踏みつけ判定を何tickおきに行うか。毎tickは無駄なので間引く */
    private static final int STEP_CHECK_INTERVAL = 5;

    // ---------------- 仕掛ける ----------------

    /**
     * 殴ったとき。武装していれば相手にボムを付ける。
     * 受け渡しボムを武装している場合は、自分に付いているものを相手へ移す。
     */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        BomberIdentity bomber = BomberIdentity.of(player);
        if (bomber == null) return;

        if (bomber.consumeArmed(BomberIdentity.SLOT_TOUCH)) {
            // 殴打ボムは必ずタイマーが動く。1〜5分のあいだからランダム
            int fuse = BombTiming.rollTimedFuse(level);
            BombAttachment.add(target, new BombInstance(
                    BombKind.TOUCH, player.getUUID(), fuse, BomberSkills.TOUCH_RADIUS, true));

            notifyPlanted(player, "仕掛けた (" + BombTiming.format(fuse) + ")");
            playPlantSound(level, target);
            return;
        }

        if (bomber.consumeArmed(BomberIdentity.SLOT_RELAY)) {
            handoverRelay(player, target, level);
        }
    }

    /**
     * 受け渡しボムを相手へ移す。
     * 自分が持っていればそれを渡し、持っていなければ新しく作って渡す。
     * タイマーは引き継がれるので、押し付け合っても猶予は増えない。
     */
    private static void handoverRelay(Player player, LivingEntity target, ServerLevel level) {
        BombInstance moved = BombAttachment.takeOne(player, BombKind.RELAY);
        if (moved == null) {
            moved = new BombInstance(BombKind.RELAY, player.getUUID(),
                    BombTiming.rollTimedFuse(level), BomberSkills.TOUCH_RADIUS, true);
        }
        BombAttachment.add(target, moved);

        notifyPlanted(player, "受け渡した (残り " + BombTiming.format(moved.getFuseTicks()) + ")");
        playPlantSound(level, target);

        if (target instanceof Player victim) {
            victim.displayClientMessage(Component.literal("何かを押し付けられた…")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    /**
     * ブロックを右クリックしたとき。
     * 武装していればそのブロックに仕掛ける。仕掛けただけでは動かず、踏まれると動き出す。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        BomberIdentity bomber = BomberIdentity.of(player);
        if (bomber == null || !bomber.isArmed(BomberIdentity.SLOT_BLOCK)) return;

        BlockPos pos = event.getPos();
        BombStore store = BombStore.get(level);
        if (store.has(pos)) {
            player.displayClientMessage(Component.literal("そこには既に仕掛けてある")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }

        bomber.consumeArmed(BomberIdentity.SLOT_BLOCK);

        // armed=false のまま置く。踏まれて初めてタイマーが動き出す
        store.put(pos, new BombInstance(BombKind.BLOCK, player.getUUID(),
                BombTiming.rollTimedFuse(level), BomberSkills.TRAP_RADIUS, false));

        notifyPlanted(player, "ブロックに仕掛けた");
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    // ---------------- 仕掛けたものが起動する ----------------

    /**
     * 仕込まれたアイテムを右クリックしたとき。
     * ここでタイマーが動き出すが、3割の確率でその場で爆発する
     * (触った側は毎回賭けになる)。
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        ItemStack stack = event.getItemStack();
        if (!BombAttachment.has(stack)) return;

        List<BombInstance> bombs = BombAttachment.get(stack);
        boolean started = false;

        for (BombInstance bomb : bombs) {
            if (bomb.isArmed()) continue;
            bomb.detonateIn(BombTiming.rollFuse(level));
            started = true;
        }
        if (!started) return;

        // 起動したぶんは持ち主(プレイヤー)側へ移す。
        // アイテムは捨てられてしまうので、付けたままだと逃げられてしまう
        BombAttachment.clear(stack);
        for (BombInstance bomb : bombs) BombAttachment.add(player, bomb);

        player.displayClientMessage(Component.literal("しまった、仕掛けられている！")
                .withStyle(ChatFormatting.RED), true);
        playTriggerSound(level, player);
    }

    /**
     * 仕掛けられたブロックを踏んだとき。
     * 感圧板のように「乗ったら動き出す」を再現するため、足元のブロックを見ている。
     * こちらも3割の確率で即爆発する。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (!(player.level() instanceof ServerLevel level)) return;
        if (player.tickCount % STEP_CHECK_INTERVAL != 0) return;

        BombStore store = BombStore.get(level);
        if (store.all().isEmpty()) return;

        // 立っているブロックと、その1つ下(感圧板・ハーフブロックの両対応)
        BlockPos on = player.blockPosition();
        BombInstance bomb = store.at(on);
        if (bomb == null) bomb = store.at(on.below());
        if (bomb == null || bomb.isArmed()) return;

        bomb.detonateIn(BombTiming.rollFuse(level));
        store.setDirty();

        player.displayClientMessage(Component.literal("足元で何かが動いた")
                .withStyle(ChatFormatting.RED), true);
        playTriggerSound(level, player);
    }

    // ---------------- 補助 ----------------

    private static void notifyPlanted(Player player, String message) {
        player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.GREEN), true);
    }

    /** 仕掛けた本人にだけ分かるよう、小さく短い音にしておく */
    private static void playPlantSound(ServerLevel level, Entity at) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(),
                SoundEvents.NOTE_BLOCK_BIT.get(), SoundSource.PLAYERS, 0.4F, 0.6F);
    }

    /** 起動した合図。周りにも聞こえて構わない */
    private static void playTriggerSound(ServerLevel level, Entity at) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
    }
}
