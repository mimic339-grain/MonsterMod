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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
    /** ブロックへ仕掛けるのに必要なスニーク時間 */
    private static final int PLANT_HOLD_TICKS = 10 * BombTiming.TICKS_PER_SECOND;
    /** 仕掛けられる距離 */
    private static final double PLANT_REACH = 5.0;

    /** 仕掛けに成功したときのクールダウン(押した時点では課金しない) */
    private static final int CD_TOUCH = 120;
    private static final int CD_BLOCK = 160;
    private static final int CD_RELAY = 200;

    // ---------------- 仕掛ける ----------------

    /**
     * 殴ったとき。
     *
     * ボマーが武装していれば相手にボムを付ける。
     * それとは別に、受け渡しボムを背負っている人は誰であれ、殴った相手へ押し付けられる
     * (ボマーでなくても移せないと「敵同士で押し付け合う」が成立しない)。
     */
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        BomberIdentity bomber = BomberIdentity.of(player);

        if (bomber != null && bomber.consumeArmed(BomberIdentity.SLOT_TOUCH)) {
            // 殴打ボムは必ずタイマーが動く。残り時間は相手にも自分にも見せない
            BombAttachment.add(target, new BombInstance(
                    BombKind.TOUCH, player.getUUID(),
                    BombTiming.rollTimedFuse(level), BomberSkills.TOUCH_RADIUS, true));

            notifyPlanted(player, "仕掛けた");
            playPlantSound(level, target);
            bomber.setCooldown(BomberIdentity.SLOT_TOUCH, CD_TOUCH);
            BomberIdentity.sync(player);
            return;
        }

        boolean armedRelay = bomber != null && bomber.consumeArmed(BomberIdentity.SLOT_RELAY);

        // 受け渡しは持っている人なら誰でも移せる。これがないと押し付け合いが起きない
        if (armedRelay || BombAttachment.hasKind(player, BombKind.RELAY)) {
            handoverRelay(player, target, level, armedRelay);
            if (bomber != null) {
                if (armedRelay) bomber.setCooldown(BomberIdentity.SLOT_RELAY, CD_RELAY);
                BomberIdentity.sync(player);
            }
        }
    }

    /**
     * 受け渡しボムを相手へ移す。
     * 自分が持っていればそれを渡し、持っていなければ(ボマーの武装時のみ)新しく作って渡す。
     * タイマーは引き継がれるので、押し付け合っても猶予は増えない。
     */
    private static void handoverRelay(Player player, LivingEntity target, ServerLevel level,
                                      boolean canCreate) {
        BombInstance moved = BombAttachment.takeOne(player, BombKind.RELAY);
        if (moved == null) {
            if (!canCreate) return;
            moved = new BombInstance(BombKind.RELAY, player.getUUID(),
                    BombTiming.rollTimedFuse(level), BomberSkills.TOUCH_RADIUS, true);
        }
        BombAttachment.add(target, moved);
        playPlantSound(level, target);

        notifyPlanted(player, "押し付けた");
        if (target instanceof Player victim) {
            victim.displayClientMessage(Component.literal("何かを押し付けられた…")
                    .withStyle(ChatFormatting.RED), true);
        }
    }

    /**
     * ブロックへの設置。見ているブロックへスニークし続けると仕掛かる。
     *
     * 【スニーク長押しにしている理由】
     * 殴る操作は「壊す」と紛らわしく、右クリックは箱を開く・ドアを使うといった操作と衝突する。
     * その場にとどまる必要がある形なら、仕掛けている最中を他人に見られる隙も生まれて、
     * ボマー側にも相応のリスクが乗る。
     * 途中でやめたり、そもそもブロックが射程に無い場合はクールダウンを取らない。
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (!(player.level() instanceof ServerLevel level)) return;

        tickBlockPlanting(player, level);

        if (player.tickCount % STEP_CHECK_INTERVAL == 0) {
            checkSteppedOnTrap(player, level);
        }
    }

    private static void tickBlockPlanting(Player player, ServerLevel level) {
        BomberIdentity bomber = BomberIdentity.of(player);
        if (bomber == null || !bomber.isArmed(BomberIdentity.SLOT_BLOCK)) return;

        BlockPos target = lookedAtBlock(player);
        if (!player.isShiftKeyDown() || target == null) {
            // 中断。押し損にならないよう、進み具合だけ戻して武装は残す
            if (bomber.getPlantProgress() != 0) {
                bomber.setPlantProgress(0);
                BomberIdentity.sync(player);
            }
            return;
        }

        int progress = bomber.getPlantProgress() + 1;
        if (progress < PLANT_HOLD_TICKS) {
            bomber.setPlantProgress(progress);
            // ゲージは1秒ごとに更新すれば十分。毎tick送ると無駄が多い
            if (progress % 10 == 0) {
                BomberIdentity.sync(player);
                showGauge(player, progress);
            }
            return;
        }

        // 完成
        bomber.setPlantProgress(0);
        bomber.consumeArmed(BomberIdentity.SLOT_BLOCK);
        bomber.setCooldown(BomberIdentity.SLOT_BLOCK, CD_BLOCK);
        BomberIdentity.sync(player);

        BombStore store = BombStore.get(level);
        if (store.has(target)) {
            notifyPlanted(player, "そこには既に仕掛けてある");
            return;
        }
        // armed=false のまま置く。踏まれて初めてタイマーが動き出す
        store.put(target, new BombInstance(BombKind.BLOCK, player.getUUID(),
                BombTiming.rollTimedFuse(level), BomberSkills.TRAP_RADIUS, false));

        notifyPlanted(player, "ブロックに仕掛けた");
        playPlantSound(level, player);
    }

    /** 見ているブロック。射程外や空を向いていれば null */
    private static BlockPos lookedAtBlock(Player player) {
        HitResult hit = player.pick(PLANT_REACH, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return ((BlockHitResult) hit).getBlockPos();
    }

    /** 仕掛けている最中の進み具合。文字のゲージで見せる */
    private static void showGauge(Player player, int progress) {
        int filled = progress * 10 / PLANT_HOLD_TICKS;
        StringBuilder bar = new StringBuilder("設置中 [");
        for (int i = 0; i < 10; i++) bar.append(i < filled ? '|' : '.');
        bar.append("] ").append((PLANT_HOLD_TICKS - progress) / BombTiming.TICKS_PER_SECOND + 1).append("秒");

        player.displayClientMessage(Component.literal(bar.toString())
                .withStyle(ChatFormatting.AQUA), true);
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
     *
     * 踏んだ人にボムが移り、ブロック側の仕掛けは無くなる(1回きり)。
     * そのぶん、踏まれた場所は10秒間だけ全員に赤く見えるようにして
     * 「ここに仕掛けてあった」と分かる証拠を残す。
     */
    private static void checkSteppedOnTrap(Player player, ServerLevel level) {
        BombStore store = BombStore.get(level);
        if (store.all().isEmpty()) return;

        // 立っているブロックと、その1つ下(感圧板・ハーフブロックの両対応)
        BlockPos on = player.blockPosition();
        BlockPos found = store.has(on) ? on : (store.has(on.below()) ? on.below() : null);
        if (found == null) return;

        BombInstance bomb = store.remove(found);
        if (bomb == null) return;

        bomb.detonateIn(BombTiming.rollFuse(level));
        BombAttachment.add(player, bomb);
        store.reveal(found);

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
                SoundEvents.NOTE_BLOCK_HAT.get(), SoundSource.PLAYERS, 0.4F, 0.6F);
    }

    /** 起動した合図。周りにも聞こえて構わない */
    private static void playTriggerSound(ServerLevel level, Entity at) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.get(), SoundSource.PLAYERS, 1.0F, 0.6F);
    }
}
