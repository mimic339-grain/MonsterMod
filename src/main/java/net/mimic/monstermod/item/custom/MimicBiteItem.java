package net.mimic.monstermod.item.custom;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.item.BaseMonsterItem;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.MimicBiteC2SPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

public class MimicBiteItem extends BaseMonsterItem {

    public MimicBiteItem(Properties properties) {
        super(properties, 5000);
    }

    @Override
    protected boolean isTargetMonster(ResourceLocation mobId) {
        return mobId.equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"));
    }

    @Override
    protected void activateSkill(Player player) {
        // プレイヤーが変身しているMimicEntityを取得
        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (transformation.getTransformedEntity() instanceof MimicEntity mimic) {
                // CLOSED 状態でない場合はスキルを発動しない
                if (mimic.getAnimationState() != MimicEntity.MimicAnimationState.CLOSED) {
                    if (player.level().isClientSide()) {
                        sendClientMessage(player, "Mimicは閉じていないので噛めません！");
                    }
                    return;
                }
            }
        });
        if (player.level().isClientSide()) {
            // クライアント側メッセージ
            sendClientMessage(player, "Mimic が噛みつこうとしています！（クライアント表示のみ）");
        } else {
            // サーバーに BITE パケット送信
            ModMessages.sendToServer(new MimicBiteC2SPacket());

            // ダメージ計算と範囲攻撃
            int baseDamage = 4;
            int attackBonus = (int) player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
            int totalDamage = baseDamage + attackBonus;

            // 前方2×横3 の範囲攻撃（足元のY座標で固定）
            skillUtility.castRect(player, 1, 0, 2, totalDamage);
        }
    }
}
