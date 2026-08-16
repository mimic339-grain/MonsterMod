package com.mimic.monstermod.item;

import com.mimic.monstermod.entity.obj.VortexEntity;
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
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 竜巻(渦)の見た目を確認するための道具。
 *
 *  ブロックに右クリック       : そこに竜巻を置く(30秒)
 *  右クリック(空中)           : 大きさを切り替える(小→中→大)
 *  Shift + 右クリック(空中)   : 色を切り替える
 *  Shift + ブロックに右クリック : 近くのエフェクトをまとめて消す
 *
 * 実体は {@link VortexEntity}(見た目専用でダメージは無い)。
 */
public class VortexWandItem extends Item {

    private static final String TAG_COLOR = "vortex_color";
    private static final String TAG_SIZE = "vortex_size";

    // 加算合成なので明るい色ほど白飛びしやすい。全体的に彩度を落とした色にしてある
    private static final int[] COLORS = {
            0x9CC4DE, // 青灰(参考画像に近い)
            0x8C96A0, // 灰(砂ぼこり寄り。一番地味でリアル)
            0xC0A87C, // 土色
            0x9C86C0, // 紫
            0x86C0A0  // 緑
    };

    /**
     * 高さ / 下の半径 / 上の半径。
     * 下は細い柱、上は皿のように大きく開く形にしたいので、上下の半径差を大きく取る。
     * (差が小さいとコップのような形にしか見えない)
     */
    private static final float[][] SIZES = {
            { 16.0F, 1.5F,  10.0F },  // 小
            { 28.0F, 3.0F,  22.0F },  // 中
            { 45.0F, 5.0F,  38.0F }   // 大
    };

    private static final String[] SIZE_NAMES = { "小", "中", "大" };

    public VortexWandItem() {
        super(new Item.Properties().stacksTo(1));
    }

    private static int colorIndex(ItemStack stack) {
        return stack.getOrCreateTag().getInt(TAG_COLOR) % COLORS.length;
    }

    private static int sizeIndex(ItemStack stack) {
        return stack.getOrCreateTag().getInt(TAG_SIZE) % SIZES.length;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        if (player.isShiftKeyDown()) {
            int next = (colorIndex(stack) + 1) % COLORS.length;
            stack.getOrCreateTag().putInt(TAG_COLOR, next);
            player.displayClientMessage(Component.literal("色を変えました")
                    .withStyle(ChatFormatting.YELLOW), true);
        } else {
            int next = (sizeIndex(stack) + 1) % SIZES.length;
            stack.getOrCreateTag().putInt(TAG_SIZE, next);
            player.displayClientMessage(Component.literal("大きさ: " + SIZE_NAMES[next])
                    .withStyle(ChatFormatting.YELLOW), true);
        }
        return InteractionResultHolder.success(stack);
    }

    /** ブロックを右クリック: その上に竜巻を置く。動かないので回り込んで眺められる */
    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;

        ItemStack stack = ctx.getItemInHand();

        if (player.isShiftKeyDown()) {
            int removed = BeamWandItem.clearNearbyEffects(level, player);
            player.displayClientMessage(Component.literal("近くのエフェクトを " + removed + " 個消しました")
                    .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.SUCCESS;
        }

        float[] size = SIZES[sizeIndex(stack)];
        Vec3 pos = Vec3.atBottomCenterOf(ctx.getClickedPos().above());

        VortexEntity vortex = new VortexEntity(ModEntitieType.VORTEX.get(), level);
        vortex.setPos(pos);
        vortex.configure(size[0], size[1], size[2], 600);
        vortex.setColor(COLORS[colorIndex(stack)]);
        level.addFreshEntity(vortex);

        level.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.PLAYERS, 0.6F, 1.4F);

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("大きさ: " + SIZE_NAMES[sizeIndex(stack)]).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("ブロックに右クリック: 竜巻を置く").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("右クリック: 大きさを変える").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift+右クリック: 色を変える").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal("Shift+ブロックに右クリック: 近くのエフェクトを消す").withStyle(ChatFormatting.DARK_GRAY));
    }
}
