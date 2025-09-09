package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.MonsterMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.capability.PlayerTransformation;

import java.util.function.Supplier;

public class PlayerTransformC2SPacket {
    private final boolean transform;
    private final ResourceLocation identityId;
    //送信
    public PlayerTransformC2SPacket(boolean transform, ResourceLocation identityId) {
        this.transform = transform;
        this.identityId = identityId;
    }
    //受信
    public PlayerTransformC2SPacket(FriendlyByteBuf buf) {
        this.transform = buf.readBoolean();
        this.identityId = buf.readNullable(FriendlyByteBuf::readResourceLocation);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.transform);
        buf.writeNullable(this.identityId, FriendlyByteBuf::writeResourceLocation);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        //プレイヤー取得
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            //情報取得
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                transformation.setTransformed(this.transform);

                if (this.transform) {
                    if (this.identityId != null) {
                        transformation.setTransformedMobId(this.identityId);

                        PlayerTransformation.MonsterState state = new PlayerTransformation.MonsterState();
                        state.animationState = "IDLE";
                        transformation.setMonsterState(this.identityId, state);

                        MonsterMod.getLogger().debug("{} が {} に変身しました。", player.getName().getString(), this.identityId.getPath());
                    } else {
                        transformation.setTransformed(false);
                        transformation.setTransformedMobId(null);
                        MonsterMod.getLogger().warn("無効なIdentity IDで変身要求を受信しました: {}", this.identityId);
                    }
                } else {
                    //変身解除
                    transformation.setTransformedMobId(null);
                    if (this.identityId != null) {
                        PlayerTransformation.MonsterState state = new PlayerTransformation.MonsterState();
                        state.animationState = "IDLE";
                        transformation.setMonsterState(this.identityId, state);
                    }
                    MonsterMod.getLogger().debug("{} が変身を解除しました。", player.getName().getString());
                }

                // クライアントに同期
                transformation.syncToClient(player);
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
