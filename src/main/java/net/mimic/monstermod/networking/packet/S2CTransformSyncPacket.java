package net.mimic.monstermod.networking.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.event.ClientForgeEvents; // このインポート文が重要です
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.function.Supplier;

public class S2CTransformSyncPacket {
    private static final Logger LOGGER = LogUtils.getLogger();

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
        this.transformedMobId = buf.readBoolean() ? buf.readResourceLocation() : null;
        this.mimicStateName = buf.readUtf();
        this.isBiting = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isTransformed);
        buf.writeBoolean(this.transformedMobId != null);
        if (this.transformedMobId != null) {
            buf.writeResourceLocation(this.transformedMobId);
        }
        buf.writeUtf(this.mimicStateName);
        buf.writeBoolean(this.isBiting);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                    transformation.setTransformed(this.isTransformed);
                    transformation.setTransformedMobId(this.transformedMobId);

                    try {
                        transformation.setMimicState(MimicEntity.MimicAnimationState.valueOf(this.mimicStateName));
                    } catch (IllegalArgumentException e) {
                        transformation.setMimicState(MimicEntity.MimicAnimationState.IDLE);
                        LOGGER.error("Received invalid MimicAnimationState: " + this.mimicStateName + " for player " + Minecraft.getInstance().player.getName().getString() + ". Defaulting to IDLE.", e);
                    }
                    transformation.setBiting(this.isBiting);

                    // クライアント側のダミーエンティティに状態を反映
                    // ここで ClientForgeEvents.getDummyMimicEntity() が呼び出されます
                    if (ClientForgeEvents.getDummyMimicEntity() != null) {
                        ClientForgeEvents.getDummyMimicEntity().setCurrentAnimationState(transformation.getMimicState());
                        ClientForgeEvents.getDummyMimicEntity().setBiting(transformation.isBiting());
                    }
                });
            }
        });
        return true;
    }
}