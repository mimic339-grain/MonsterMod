package com.mimic.monstermod.network.packets;

import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー → 全クライアント: Hunter状態 & スキルCD同期パケット。
 *
 * EFM参考:
 *   - network/server/SPChangeSkill.java
 *   - network/server/SPCooldown.java (EFMのCDパターン)
 *
 * 配置: com/mimic/monstermod/network/packets/S2CHunterSyncPacket.java
 */
public class S2CHunterSyncPacket {

    private final int    entityId;
    private final boolean isHunter;
    private final String  hunterType;
    private final String[] skillSlots;
    private final int[]    cooldowns;

    public S2CHunterSyncPacket(int entityId, boolean isHunter, String hunterType,
                               String[] skillSlots, int[] cooldowns) {
        this.entityId   = entityId;
        this.isHunter   = isHunter;
        this.hunterType = hunterType;
        this.skillSlots = skillSlots.clone();
        this.cooldowns  = cooldowns.clone();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeBoolean(isHunter);
        buf.writeUtf(hunterType);
        buf.writeInt(skillSlots.length);
        for (String s : skillSlots) buf.writeUtf(s != null ? s : "empty");
        buf.writeInt(cooldowns.length);
        for (int cd : cooldowns) buf.writeInt(cd);
    }

    public static S2CHunterSyncPacket decode(FriendlyByteBuf buf) {
        int    entityId   = buf.readInt();
        boolean isHunter  = buf.readBoolean();
        String  type      = buf.readUtf();
        int     slotCount = buf.readInt();
        String[] slots    = new String[slotCount];
        for (int i = 0; i < slotCount; i++) {
            String v = buf.readUtf();
            slots[i] = "empty".equals(v) ? null : v;
        }
        int cdCount = buf.readInt();
        int[] cds   = new int[cdCount];
        for (int i = 0; i < cdCount; i++) cds[i] = buf.readInt();
        return new S2CHunterSyncPacket(entityId, isHunter, type, slots, cds);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(entityId);
            if (!(entity instanceof Player player)) return;

            player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
                // NBT経由で同期（deserializeNBT再利用）
                net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                tag.putBoolean("isHunter", isHunter);
                tag.putString("hunterType", hunterType);
                net.minecraft.nbt.ListTag slotTag = new net.minecraft.nbt.ListTag();
                for (String s : skillSlots)
                    slotTag.add(net.minecraft.nbt.StringTag.valueOf(s != null ? s : "empty"));
                tag.put("skillSlots", slotTag);
                tag.putIntArray("cooldowns", cooldowns);
                cap.deserializeNBT(tag);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}