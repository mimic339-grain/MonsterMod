package com.mimic.monstermod.network;

import com.mimic.monstermod.entity.base.CustomEntityBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * サーバー→クライアントへのOBB同期パケット。
 *
 * Phase 7: 毎tick送るのは帯域を使いすぎるため、
 * 変化があった場合のみ差分送信（SYNC_INTERVAL=3tick）。
 *
 * 送信するデータ:
 *   - エンティティID
 *   - 変化したボーンのOBBデータ（center, halfExtents, orientation, partGroup）
 *
 * 配置: com/mimic/monstermod/network/OBBSyncPacket.java
 */
public class OBBSyncPacket {

    private final int entityId;
    private final Map<String, OBBEntry> obbEntries;

    public OBBSyncPacket(int entityId, Map<String, CustomEntityBase.OBBData> obbMap) {
        this.entityId = entityId;
        this.obbEntries = new HashMap<>();
        for (Map.Entry<String, CustomEntityBase.OBBData> entry : obbMap.entrySet()) {
            CustomEntityBase.OBBData data = entry.getValue();
            obbEntries.put(entry.getKey(), new OBBEntry(
                    data.center, data.halfExtents, data.orientation, data.partGroup));
        }
    }

    // ── デシリアライズ（クライアント側受信） ──────────────────────
    public OBBSyncPacket(FriendlyByteBuf buf) {
        this.entityId = buf.readInt();
        int count = buf.readInt();
        this.obbEntries = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            String boneName = buf.readUtf();
            Vector3f center = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
            Vector3f half   = new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat());
            Quaternionf rot = new Quaternionf(
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat());
            String part = buf.readUtf();
            obbEntries.put(boneName, new OBBEntry(center, half, rot, part));
        }
    }

    // ── シリアライズ（サーバー側送信） ───────────────────────────
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeInt(obbEntries.size());
        for (Map.Entry<String, OBBEntry> entry : obbEntries.entrySet()) {
            buf.writeUtf(entry.getKey());
            OBBEntry e = entry.getValue();
            buf.writeFloat(e.center.x); buf.writeFloat(e.center.y); buf.writeFloat(e.center.z);
            buf.writeFloat(e.half.x);   buf.writeFloat(e.half.y);   buf.writeFloat(e.half.z);
            buf.writeFloat(e.rot.x);    buf.writeFloat(e.rot.y);    buf.writeFloat(e.rot.z);
            buf.writeFloat(e.rot.w);
            buf.writeUtf(e.partGroup);
        }
    }

    // ── クライアント側処理 ────────────────────────────────────────
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;
            Entity entity = Minecraft.getInstance().level.getEntity(entityId);
            if (!(entity instanceof CustomEntityBase customEntity)) return;

            for (Map.Entry<String, OBBEntry> entry : obbEntries.entrySet()) {
                OBBEntry e = entry.getValue();
                customEntity.getOBBMap().put(entry.getKey(),
                        new CustomEntityBase.OBBData(e.center, e.half, e.rot, e.partGroup));
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // ── データ構造 ────────────────────────────────────────────────
    private record OBBEntry(Vector3f center, Vector3f half, Quaternionf rot, String partGroup) {}
}