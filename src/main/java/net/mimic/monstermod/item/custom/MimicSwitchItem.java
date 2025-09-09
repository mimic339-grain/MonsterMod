package net.mimic.monstermod.item.custom;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.item.BaseMonsterItem;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.MimicSwitchC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class MimicSwitchItem extends BaseMonsterItem {

    public MimicSwitchItem(Properties properties) {
        // クールダウン時間を指定（例: 5000ms = 5秒）
        super(properties, 5000);
    }

    @Override
    protected boolean isTargetMonster(ResourceLocation mobId) {
        return mobId.equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"));
    }

    @Override
    protected void activateSkill(Player player) {
        if (player.level().isClientSide()) {
            // クライアント側でパケットをサーバーに送信
            ModMessages.sendToServer(new MimicSwitchC2SPacket());

            // クライアント表示用メッセージ
            sendClientMessage(player, "Mimicの状態を切り替えます！");
        }
    }
}
