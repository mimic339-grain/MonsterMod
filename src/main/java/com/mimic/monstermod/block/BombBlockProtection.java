package com.mimic.monstermod.block;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.init.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 設置ボムを掘って無かったことにできないようにする。
 *
 * 【硬さ -1 だけでは足りない理由】
 * クリエイティブモードは硬さを無視してブロックを壊せるため、
 * 設定だけでは運営や本人が事故で消してしまえる。
 * 破壊そのものを止めないと「壊せない」とは言えないので、イベントで弾いている。
 *
 * 止めたいときは解除キットを使う。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public class BombBlockProtection {

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!event.getState().is(ModBlocks.PLACED_BOMB.get())) return;

        event.setCanceled(true);
        event.getPlayer().displayClientMessage(Component.literal("これは壊せない。解除キットを使え")
                .withStyle(ChatFormatting.RED), true);
    }
}
