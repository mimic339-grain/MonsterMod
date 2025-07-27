package net.mimic.monstermod.item.custom;

import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.MimicSwitchC2SPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MimicSwitchItem extends Item {
    public MimicSwitchItem(Properties pProperties) {
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
                    ModMessages.sendToServer(new MimicSwitchC2SPacket());
                    pPlayer.sendSystemMessage(Component.literal("Mimicの状態を切り替えます！ (クライアント)"));
                }
            } else {
                pPlayer.sendSystemMessage(Component.literal("変身していません。(クライアント)"));
            }
        });

        return InteractionResultHolder.sidedSuccess(pPlayer.getItemInHand(pUsedHand), pLevel.isClientSide());
    }
}