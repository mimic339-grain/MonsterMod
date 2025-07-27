package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.MonsterMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;

import java.util.function.Supplier;

public class S2CTransformSyncPacket {
    private final boolean isTransformed;
    private final ResourceLocation transformedMobId;
    private final String mimicStateName;
    private final boolean isBiting;

    public S2CTransformSyncPacket(boolean isTransformed, ResourceLocation transformedMobId, String mimicStateName, boolean isBiting) {
        this.isTransformed = isTransformed;
        this.transformedMobId = transformedMobId;
        this.mimicStateName = mimicStateName;
        this.isBiting = isBiting;
    }

    public S2CTransformSyncPacket(FriendlyByteBuf buf) {
        this.isTransformed = buf.readBoolean();
        this.transformedMobId = buf.readNullable(FriendlyByteBuf::readResourceLocation);
        this.mimicStateName = buf.readUtf();
        this.isBiting = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isTransformed);
        buf.writeNullable(this.transformedMobId, FriendlyByteBuf::writeResourceLocation);
        buf.writeUtf(this.mimicStateName);
        buf.writeBoolean(this.isBiting);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                    transformation.setTransformed(this.isTransformed);
                    transformation.setTransformedMobId(this.transformedMobId); // これで内部のIPlayerIdentityも更新される

                    try {
                        transformation.setMimicState(MimicEntity.MimicAnimationState.valueOf(this.mimicStateName));
                    } catch (IllegalArgumentException e) {
                        // ★修正: MonsterMod.getLogger() を使用
                        MonsterMod.getLogger().warn("S2Cパケット: 無効なMimicAnimationStateを受信: {}. IDLEに設定。", this.mimicStateName);
                        transformation.setMimicState(MimicEntity.MimicAnimationState.IDLE);
                    }
                    transformation.setBiting(this.isBiting);

                    // Debugメッセージは引き続き残してもOK
                    // Minecraft.getInstance().player.sendSystemMessage(Component.literal("DEBUG: S2Cパケット受信。変身中: " + this.isTransformed + ", ID: " + this.transformedMobId + ", 状態: " + this.mimicStateName + ", バイト: " + this.isBiting));

                    // MimicPlayerRendererがdummyMimicEntityの管理を行うため、
                    // ここで直接dummyMimicEntityの状態を更新する必要はありません。
                    // レンダリングループ内でMimicPlayerRendererがCapabilityから最新の状態を取得します。
                });
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}