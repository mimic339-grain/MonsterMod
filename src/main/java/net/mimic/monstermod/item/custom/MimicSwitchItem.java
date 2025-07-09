package net.mimic.monstermod.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.MimicSwitchC2SPacket;
import net.minecraft.resources.ResourceLocation;

public class MimicSwitchItem extends Item {
    public MimicSwitchItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide()) {
            if (pPlayer instanceof ServerPlayer serverPlayer) {
                serverPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                    if (transformation.isTransformed() && transformation.getTransformedMobId() != null &&
                            transformation.getTransformedMobId().equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"))) {

                        ModMessages.sendToServer(new MimicSwitchC2SPacket());
                    } else {
                        serverPlayer.sendSystemMessage(Component.literal("You must be a Mimic to use this!"));
                    }
                });
            }
        }
        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }
}