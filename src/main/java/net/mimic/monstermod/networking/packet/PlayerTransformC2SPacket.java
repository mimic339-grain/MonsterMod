package net.mimic.monstermod.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.item.ModItems;
import net.mimic.monstermod.networking.ModMessages;

import java.util.function.Supplier;

public class PlayerTransformC2SPacket {
    private final boolean transformToMimic;

    public PlayerTransformC2SPacket(boolean transformToMimic) {
        this.transformToMimic = transformToMimic;
    }

    public PlayerTransformC2SPacket(FriendlyByteBuf buf) {
        this.transformToMimic = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(transformToMimic);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                ResourceLocation mimicId = new ResourceLocation(MonsterMod.MOD_ID, "mimic");

                if (transformToMimic) {
                    if (!transformation.isTransformed()) {
                        transformation.setTransformed(true);
                        transformation.setTransformedMobId(mimicId);
                        transformation.setMimicOpen(false);

                        ItemStack switchItem = new ItemStack(ModItems.MIMIC_SWITCH.get());
                        if (!player.getInventory().contains(switchItem)) {
                            player.getInventory().add(switchItem);
                            player.sendSystemMessage(Component.literal("You transformed into a Mimic!"));
                        }

                        ModMessages.sendToPlayer(new S2CTransformSyncPacket(transformation.isTransformed(), transformation.getTransformedMobId(), transformation.isMimicOpen()), player);
                    }
                } else {
                    if (transformation.isTransformed() && transformation.getTransformedMobId().equals(mimicId)) {
                        transformation.setTransformed(false);
                        transformation.setTransformedMobId(null);
                        transformation.setMimicOpen(false);

                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            ItemStack stack = player.getInventory().getItem(i);
                            if (stack.getItem() == ModItems.MIMIC_SWITCH.get()) {
                                stack.shrink(1);
                                if (stack.isEmpty()) {
                                    player.getInventory().setItem(i, ItemStack.EMPTY);
                                }
                                break;
                            }
                        }
                        player.sendSystemMessage(Component.literal("You transformed back to human!"));

                        ModMessages.sendToPlayer(new S2CTransformSyncPacket(transformation.isTransformed(), transformation.getTransformedMobId(), transformation.isMimicOpen()), player);
                    }
                }
            });
        });
        return true;
    }
}