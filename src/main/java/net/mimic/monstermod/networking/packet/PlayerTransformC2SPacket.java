package net.mimic.monstermod.networking.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.networking.ModMessages; // ModMessagesをインポート

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

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                    boolean currentTransformedState = transformation.isTransformed();

                    if (transformToMimic) { // 変身したい場合
                        if (!currentTransformedState) { // まだ変身していない場合のみ
                            transformation.setTransformed(true);
                            transformation.setTransformedMobId(new ResourceLocation(MonsterMod.MOD_ID, "mimic"));
                            player.sendSystemMessage(Component.literal("You transformed into a Mimic!"));
                            ModMessages.sendToPlayer(new S2CTransformSyncPacket(true, new ResourceLocation(MonsterMod.MOD_ID, "mimic")), player);
                        }
                    } else { // 変身解除したい場合
                        if (currentTransformedState) { // 変身している場合のみ
                            transformation.setTransformed(false);
                            transformation.setTransformedMobId(null);
                            player.sendSystemMessage(Component.literal("You transformed back to Steve!"));
                            ModMessages.sendToPlayer(new S2CTransformSyncPacket(false, null), player);
                        }
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }
}