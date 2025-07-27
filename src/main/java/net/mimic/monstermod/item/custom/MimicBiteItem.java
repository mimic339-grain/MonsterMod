package net.mimic.monstermod.item.custom;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.impl.MimicIdentity;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.MimicBiteC2SPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MimicBiteItem extends Item {
    public MimicBiteItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide()) {
            return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
        }

        pPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (transformation.isTransformed()) {
                // ★変更: Capabilityから直接Mimicの状態を取得
                if (transformation.getTransformedMobId() != null && transformation.getTransformedMobId().equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"))) {
                    MimicEntity.MimicAnimationState currentState = transformation.getMimicState();
                    if (currentState == MimicEntity.MimicAnimationState.OPEN || currentState == MimicEntity.MimicAnimationState.OPENING) {
                        ModMessages.sendToServer(new MimicBiteC2SPacket());
                        // クライアント側でメッセージを表示（デバッグ用）
                        pPlayer.sendSystemMessage(Component.literal("Mimic が噛みつこうとしています！ (クライアント)"));
                    } else {
                        pPlayer.sendSystemMessage(Component.literal("Mimicは口を開いていません。(クライアント)"));
                    }
                }
            } else {
                pPlayer.sendSystemMessage(Component.literal("変身していません。(クライアント)"));
            }
        });

        return InteractionResultHolder.sidedSuccess(pPlayer.getItemInHand(pUsedHand), pLevel.isClientSide());
    }
}