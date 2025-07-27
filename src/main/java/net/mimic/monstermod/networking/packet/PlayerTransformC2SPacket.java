package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.MonsterMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.PlayerIdentityRegistry;

import java.util.function.Supplier;

public class PlayerTransformC2SPacket {
    private final boolean transform;
    private final ResourceLocation identityId;

    public PlayerTransformC2SPacket(boolean transform, ResourceLocation identityId) {
        this.transform = transform;
        this.identityId = identityId;
    }

    public PlayerTransformC2SPacket(FriendlyByteBuf buf) {
        this.transform = buf.readBoolean();
        this.identityId = buf.readNullable(FriendlyByteBuf::readResourceLocation);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.transform);
        buf.writeNullable(this.identityId, FriendlyByteBuf::writeResourceLocation);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                transformation.setTransformed(this.transform);

                if (this.transform) {
                    if (this.identityId != null && PlayerIdentityRegistry.hasIdentity(this.identityId)) {
                        transformation.setTransformedMobId(this.identityId);
                        // Mimicに変身する場合、初期状態をIDLEに設定
                        if (this.identityId.equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"))) {
                            transformation.setMimicState(MimicEntity.MimicAnimationState.IDLE);
                            transformation.setBiting(false);
                        }
                        // ★追加: 変身先のMobが持つ属性をプレイヤーに適用するロジックをここで呼び出す
                        // transformation.getTransformedIdentity().applySpecificAbilities(player);
                        MonsterMod.getLogger().debug("{} が {} に変身しました。", player.getName().getString(), this.identityId.getPath());
                    } else {
                        // 無効なIdentity IDが送られてきた場合は変身しない
                        transformation.setTransformed(false);
                        transformation.setTransformedMobId(null);
                        MonsterMod.getLogger().warn("無効なIdentity IDで変身要求を受信しました: {}", this.identityId);
                    }
                } else {
                    transformation.setTransformedMobId(null);
                    // 変身解除時、Mimic関連の状態をリセット
                    transformation.setMimicState(MimicEntity.MimicAnimationState.IDLE);
                    transformation.setBiting(false);
                    // ★追加: 変身解除時にMobの属性をプレイヤーから解除するロジックをここで呼び出す
                    // transformation.getTransformedIdentity().removeSpecificAbilities(player); // 直前のIdentityを解除
                    MonsterMod.getLogger().debug("{} が変身を解除しました。", player.getName().getString());
                }

                transformation.syncToClient(player);
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}