package com.mimic.monstermod.client;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.bomb.BombAttachment;
import com.mimic.monstermod.identity.bomber.BomberIdentity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 仕込まれたアイテムに、ボマー本人にだけ赤い印を出す。
 *
 * 【本人にだけ見せる理由】
 * 仕込んだ側は「どれに仕掛けたか」を把握しないと自分で踏む。
 * かといって全員に見えると仕掛けとして成立しないので、
 * 見ているプレイヤーがボマーのときだけ描いている。
 *
 * 【全アイテムに登録している理由】
 * 何に仕込まれるか分からないので、対象を絞れない。
 * 登録自体は表(マップ)に入れるだけなので、数が多くても負担にならない。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class BombItemDecorator implements IItemDecorator {

    private static final BombItemDecorator INSTANCE = new BombItemDecorator();

    @SubscribeEvent
    public static void register(RegisterItemDecorationsEvent event) {
        // 何に仕込まれるか分からないので、登録済みのアイテム全部に付ける
        for (var item : ForgeRegistries.ITEMS) {
            event.register(item, INSTANCE);
        }
    }

    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        if (!BombAttachment.has(stack)) return false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || BomberIdentity.of(mc.player) == null) return false;

        // アイテムより手前に描く。
        // ブロックのアイテムは絵が立体で四隅まで埋まっているため、
        // 奥に描くと模様に紛れて見えなくなる
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 250.0F);

        // スロット全体を赤枠で囲う。中身が何であっても輪郭は必ず見える
        int x2 = x + 16, y2 = y + 16;
        graphics.fill(x, y, x2, y + 1, 0xFFFF2020);         // 上
        graphics.fill(x, y2 - 1, x2, y2, 0xFFFF2020);       // 下
        graphics.fill(x, y, x + 1, y2, 0xFFFF2020);         // 左
        graphics.fill(x2 - 1, y, x2, y2, 0xFFFF2020);       // 右

        // 左上の印。個数表示(右下)や耐久バー(下)と重ならない位置にする
        graphics.fill(x + 1, y + 1, x + 6, y + 6, 0xFFFF2020);
        graphics.fill(x + 2, y + 2, x + 5, y + 5, 0xFFFFE0E0);

        graphics.pose().popPose();
        return false;
    }
}
