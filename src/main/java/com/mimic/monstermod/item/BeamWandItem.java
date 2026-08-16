package com.mimic.monstermod.item;

import com.mimic.monstermod.entity.obj.BeamEntity;
import com.mimic.monstermod.init.ModEntitieType;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * ビームの見た目と当たり判定を確認するための道具。
 *
 *  右クリック(空中)       : 自分からビームを撃つ(視点に追従する。2秒)
 *  右クリック(ブロック)   : そこに固定ビームを置く(30秒)
 *                           動かないので、自分で歩き回って好きな角度から見られる
 *  Shift + 右クリック(空中)     : 色を切り替える
 *  Shift + 右クリック(ブロック) : 近くの固定ビームをまとめて消す
 *
 * 実体は {@link BeamEntity}。スキルから撃たせたい場合も
 * BeamEntity を生成して fireFrom / placeStatic を呼ぶだけでよい。
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

    static int currentColor(ItemStack stack) {
        return COLORS[getColorIndex(stack)];
    }

    /** 色替えは持ち替えずに行えるようShift+右クリック(空中)に割り当てている */
    static void cycleColor(Player player, ItemStack stack) {
        int next = (getColorIndex(stack) + 1) % COLORS.length;
        stack.getOrCreateTag().putInt(TAG_COLOR, next);
        player.displayClientMessage(Component.literal("色を変えました")
                .withStyle(ChatFormatting.YELLOW), true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) cycleColor(player, stack);
            return InteractionResultHolder.success(stack);
        }

        if (!level.isClientSide) {
            BeamEntity beam = new BeamEntity(ModEntitieType.BEAM.get(), level);
            // 長さ48 / 太さ0.35 / 1回4ダメージ / 4tickごと / 40tick(2秒)持続
            beam.fireFrom(player, 48.0F, 0.35F, 4.0F, 4, 40);
            beam.setColor(currentColor(stack));
            level.addFreshEntity(beam);
            playShotSound(level, player);
        }

        player.getCooldowns().addCooldown(this, 45);
        return InteractionResultHolder.success(stack);
    }

    /**
     * ブロックを右クリック: その場に固定ビームを置く。
     * 追従ビームだと自分の視点からしか見られないため、
     * 見た目を確かめるにはこちらで置いてから回り込むのが早い。
     */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack = ctx.getItemInHand();

        if (player.isShiftKeyDown()) {
            int removed = clearNearbyEffects(level, player);
            player.displayClientMessage(Component.literal("近くのエフェクトを " + removed + " 個消しました")
                    .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.SUCCESS;
        }

        // 置いた面の上、プレイヤーの目線の高さあたりから、見ている方向へ伸ばす
        Vec3 pos = Vec3.atCenterOf(ctx.getClickedPos().above()).add(0.0, 0.5, 0.0);

        BeamEntity beam = new BeamEntity(ModEntitieType.BEAM.get(), level);
        // ダメージ0にしてあるので、置いたまま近づいて眺められる
        beam.placeStatic(pos, player.getYRot(), player.getXRot(),
                48.0F, 0.35F, 0.0F, 4, 600);
        beam.setColor(currentColor(stack));
        level.addFreshEntity(beam);
        playShotSound(level, player);

        return InteractionResult.SUCCESS;
    }

    private static void playShotSound(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.6F);
    }

    /** 置いたエフェクト(ビーム・竜巻)を周囲24ブロックから消す */
    static int clearNearbyEffects(Level level, Player player) {
        AABB area = player.getBoundingBox().inflate(24.0D);
        List<net.minecraft.world.entity.Entity> found = level.getEntities(player, area,
                e -> e instanceof BeamEntity || e instanceof com.mimic.monstermod.entity.obj.VortexEntity);
        for (var e : found) e.discard();
        return found.size();
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("右クリック: ビームを撃つ").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("ブロックに右クリック: 固定ビームを置く").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift+右クリック: 色を変える").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift+ブロックに右クリック: 近くのエフェクトを消す").withStyle(ChatFormatting.DARK_GRAY));
    }
}
