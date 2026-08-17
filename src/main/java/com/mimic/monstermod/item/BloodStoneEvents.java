package com.mimic.monstermod.item;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_BloodStoneTargetPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

/**
 * 血石を持っている間だけ、追跡先の座標をその人へ送り続ける。
 *
 * 【持っている間だけ送る】
 * 石を持っていない間は何も送らない。クライアント側は
 * 「一定時間パケットが来なければ表示しない」という作りにしてあるので、
 * わざわざ「消してよい」と伝えるパケットを用意しなくてよい。
 *
 * 【毎tickではなく間引く理由】
 * 矢印の向きは5tick(0.25秒)ごとの更新で十分読める。
 * 大人数のサーバーで全員が石を持つ状況を考えると、毎tick送る意味がない。
 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID)
public final class BloodStoneEvents {

    private BloodStoneEvents() {}

    /** 座標を送る間隔(tick) */
    private static final int SEND_INTERVAL = 5;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer holder)) return;
        if (holder.tickCount % SEND_INTERVAL != 0) return;

        ItemStack stone = BloodStoneItem.findHeld(holder);
        if (stone == null) return;

        UUID targetId = BloodStoneItem.getTarget(stone);
        if (targetId == null) return;

        String name = BloodStoneItem.getTargetName(stone);

        ServerPlayer target = holder.server.getPlayerList().getPlayer(targetId);
        if (target == null) {
            // 相手がログアウトしている。座標は無いが「見失っている」ことは伝える
            ModMessages.sendToPlayer(
                    new S2C_BloodStoneTargetPacket(name, false, false, 0, 0, 0), holder);
            return;
        }

        boolean sameDimension = target.level() == holder.level();
        ModMessages.sendToPlayer(new S2C_BloodStoneTargetPacket(
                name, true, sameDimension, target.getX(), target.getY(), target.getZ()), holder);
    }
}
