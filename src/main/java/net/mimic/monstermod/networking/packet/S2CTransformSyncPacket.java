package net.mimic.monstermod.networking.packet;

import net.mimic.monstermod.capability.PlayerTransformation;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバ -> クライアント: 変身状態同期パケット
 *
 * 互換性を保つために
 * - S2CTransformSyncPacket(boolean)
 * - S2CTransformSyncPacket(boolean, ResourceLocation)
 * - S2CTransformSyncPacket(boolean, ResourceLocation, int) ← 新規
 */
public class S2CTransformSyncPacket {

    private final boolean isTransformed;
    private final ResourceLocation transformedMobId; // nullable
    private final String animationState; // nullable
    private final Integer transformedEntityId; // nullable

    // ---------- コンストラクタ群 ----------
    public S2CTransformSyncPacket(boolean isTransformed) {
        this(isTransformed, null, null);
    }

    public S2CTransformSyncPacket(boolean isTransformed, ResourceLocation transformedMobId) {
        this(isTransformed, transformedMobId, null);
    }

    // 元の animationState 用
    public S2CTransformSyncPacket(boolean isTransformed, ResourceLocation transformedMobId, String animationState) {
        this.isTransformed = isTransformed;
        this.transformedMobId = transformedMobId;
        this.animationState = animationState;
        this.transformedEntityId = null;
    }

    // 新規：Entity ID 用
    public S2CTransformSyncPacket(boolean isTransformed, ResourceLocation transformedMobId, int transformedEntityId) {
        this.isTransformed = isTransformed;
        this.transformedMobId = transformedMobId;
        this.transformedEntityId = transformedEntityId;
        this.animationState = null;
    }

    // デコーダ（ネットワークから来たとき）
    public S2CTransformSyncPacket(FriendlyByteBuf buf) {
        this.isTransformed = buf.readBoolean();
        this.transformedMobId = buf.readNullable(FriendlyByteBuf::readResourceLocation);

        boolean hasAnim = buf.readBoolean();
        this.animationState = hasAnim ? buf.readUtf(32767) : null;

        boolean hasId = buf.readBoolean();
        this.transformedEntityId = hasId ? buf.readInt() : null;
    }

    // エンコード（送信時）
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.isTransformed);
        buf.writeNullable(this.transformedMobId, FriendlyByteBuf::writeResourceLocation);

        buf.writeBoolean(this.animationState != null);
        if (this.animationState != null) buf.writeUtf(this.animationState);

        buf.writeBoolean(this.transformedEntityId != null);
        if (this.transformedEntityId != null) buf.writeInt(this.transformedEntityId);
    }

    // ハンドラ（クライアント側で実行）
    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(cap -> {
                // 変身フラグ / ID を更新
                cap.setTransformed(this.isTransformed);
                cap.setTransformedMobId(this.transformedMobId);

                // animationState が来ていれば MonsterState を更新
                if (this.transformedMobId != null && this.animationState != null) {
                    PlayerTransformation.MonsterState state = new PlayerTransformation.MonsterState();
                    state.animationState = this.animationState;
                    cap.setMonsterState(this.transformedMobId, state);
                }

                // transformedEntityId が来ていれば保持
                if (this.transformedEntityId != null) {
                    cap.setTransformedEntityId(this.transformedEntityId);
                }

                // 変身解除ならクライアント偽装エンティティを破棄
                if (!this.isTransformed) {
                    cap.setClientTransformedEntity(null);
                }

                // プレイヤーの可視切り替え
                player.setInvisible(this.isTransformed);
            });
        });
        ctx.setPacketHandled(true);
    }
}
