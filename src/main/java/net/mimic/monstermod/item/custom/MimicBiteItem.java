package net.mimic.monstermod.item.custom;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation; // Add this import statement
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.MimicBiteC2SPacket;

public class MimicBiteItem extends Item {
    public MimicBiteItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide()) { // Server-side processing only
            pPlayer.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                // Check if the player is transformed into a Mimic
                boolean isMimicForm = transformation.isTransformed()
                        && transformation.getTransformedMobId() != null
                        && transformation.getTransformedMobId().equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"));

                if (isMimicForm) {
                    // If transformed into Mimic, send the bite packet to the server
                    ModMessages.sendToServer(new MimicBiteC2SPacket());
                    pPlayer.sendSystemMessage(Component.literal("Mimic bites!")); // Display message to player
                } else {
                    pPlayer.sendSystemMessage(Component.literal("You must be a Mimic to use this!"));
                }
            });
        }
        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }
}