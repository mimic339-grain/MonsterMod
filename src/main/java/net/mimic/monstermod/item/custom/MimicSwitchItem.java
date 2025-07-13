package net.mimic.monstermod.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.MimicSwitchC2SPacket; // C2Sパケットを再利用
import net.mimic.monstermod.entity.custom.MimicEntity; // MimicEntityをインポート

public class MimicSwitchItem extends Item {
    public MimicSwitchItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        // ★修正点: クライアント側でのみパケットを送信する
        if (pLevel.isClientSide()) {
            pPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                // クライアント側でも変身状態をチェックし、メッセージを出す（任意）
                boolean isMimicForm = transformation.isTransformed()
                        && transformation.getTransformedMobId() != null
                        && transformation.getTransformedMobId().equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"));

                if (isMimicForm) {
                    ModMessages.sendToServer(new MimicSwitchC2SPacket());
                } else {
                    // クライアント側でメッセージを出すことで、サーバーからの応答を待たずに即座にフィードバック
                    pPlayer.sendSystemMessage(Component.literal("Mimicに変身している必要があります！"));
                }
            });
        }
        // サーバー側では何もせず、パケットが処理されるのを待つ
        // InteractionResultHolder.success を返すことで、アイテム使用アニメーションが再生される
        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }
}