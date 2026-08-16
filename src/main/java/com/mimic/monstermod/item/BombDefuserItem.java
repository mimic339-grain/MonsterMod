package com.mimic.monstermod.item;

import com.mimic.monstermod.bomb.BombAttachment;
import com.mimic.monstermod.bomb.BombInstance;
import com.mimic.monstermod.bomb.BombKind;
import com.mimic.monstermod.bomb.BombStore;
import com.mimic.monstermod.init.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * ボムの解除キット。
 *
 *  自分に右クリック(空中)   : 自分に付いたボムを全部外す
 *  他人に右クリック         : その人に付いたボムを全部外す
 *  ブロックに右クリック     : そのブロックに仕掛けられたボムを外す
 *  手に持ったアイテムに対しては、そのアイテムを持って空中でShift+右クリック
 *
 * 【全部まとめて外す理由】
 * 受け渡しボムと別のボムを同時に背負っている状態(2重掛け)でも、
 * 一度の解除で両方消えるべき、という要望に合わせている。
 *
 * 外したぶんだけ「解除したボムの残骸」が手に入る。
 * ただし偽物(ダミー)は残骸が出ないうえ、解除した瞬間に小さく爆発する。
 */
public class BombDefuserItem extends Item {

    /** 解除にかかる硬直。連打で全員分を一瞬で外せないようにする */
    private static final int COOLDOWN_TICKS = 30;

    public BombDefuserItem() {
        super(new Item.Properties().stacksTo(1));
    }

    /** 空中で右クリック: 自分に付いたボムを外す */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        // Shift+右クリックは、手に持っているアイテムに仕掛けられたボムを外す
        if (player.isShiftKeyDown()) {
            ItemStack off = player.getOffhandItem();
            if (BombAttachment.has(off)) {
                defuseStack(player, off);
                return InteractionResultHolder.success(stack);
            }
        }

        defuseEntity(player, player);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.success(stack);
    }

    /** ブロックに右クリック: そこに仕掛けられたボムを外す */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        // 設置ボム(ブロックそのものがボム)は、ブロックごと取り除いて残骸に変える。
        // 壊せない設定にしてあるので、解除キットが唯一の撤去手段になる
        if (level.getBlockState(ctx.getClickedPos())
                .is(com.mimic.monstermod.init.ModBlocks.PLACED_BOMB.get())) {
            level.removeBlock(ctx.getClickedPos(), false);
            finishDefuse(player, List.of(
                    new BombInstance(BombKind.PLACED, null, 1, 0.0F, false)));
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            return InteractionResult.SUCCESS;
        }

        BombStore store = BombStore.get(serverLevel);
        BombInstance bomb = store.remove(ctx.getClickedPos());
        if (bomb == null) {
            player.displayClientMessage(Component.literal("ここには何も仕掛けられていない")
                    .withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
        }

        finishDefuse(player, List.of(bomb));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }

    /** 他人に右クリック: その人に付いたボムを外す */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                  LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;

        defuseEntity(player, target);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS;
    }

    private void defuseEntity(Player user, LivingEntity target) {
        List<BombInstance> bombs = BombAttachment.get(target);
        if (bombs.isEmpty()) {
            user.displayClientMessage(Component.literal("ボムは付いていない")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        BombAttachment.clear(target);
        finishDefuse(user, bombs);
    }

    private void defuseStack(Player user, ItemStack target) {
        List<BombInstance> bombs = BombAttachment.get(target);
        if (bombs.isEmpty()) return;
        BombAttachment.clear(target);
        finishDefuse(user, bombs);
    }

    /**
     * 解除の後始末。
     * 偽物は残骸が出ないうえ、その場で小さく爆発する(解除キットを無駄遣いさせる狙い)。
     */
    private void finishDefuse(Player user, List<BombInstance> bombs) {
        int remnants = 0;
        boolean dummyTriggered = false;

        for (BombInstance bomb : bombs) {
            if (bomb.getKind() == BombKind.DUMMY) {
                dummyTriggered = true;
            } else if (bomb.getKind().dropsRemnant()) {
                remnants++;
            }
        }

        if (remnants > 0) {
            ItemStack drop = new ItemStack(ModItems.BOMB_REMNANT.get(), remnants);
            if (!user.getInventory().add(drop)) user.drop(drop, false);
        }

        user.level().playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.PLAYERS, 1.0F, 1.4F);

        if (dummyTriggered) {
            // 偽物: 死なないが体力の半分ほどを持っていく
            user.hurt(user.damageSources().explosion(null, null),
                    Math.max(1.0F, user.getMaxHealth() * 0.5F));
            user.level().playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.5F);
            user.displayClientMessage(Component.literal("偽物だった！")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        user.displayClientMessage(Component.literal("ボムを " + bombs.size() + " 個解除した")
                .withStyle(ChatFormatting.GREEN), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("右クリック: 自分のボムを外す").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("相手に右クリック: その人のボムを外す").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("ブロックに右クリック: 仕掛けを外す").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("2重に掛かっていても一度で全部外れる").withStyle(ChatFormatting.DARK_GRAY));
    }
}
