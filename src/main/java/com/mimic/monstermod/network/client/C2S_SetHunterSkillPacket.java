package com.mimic.monstermod.network.client;

import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.hunter.HunterSkill;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2S_SetHunterSkillPacket {
    private final HunterSkill.HunterSkillSlot slot;
    private final SkillId skillId;

    public C2S_SetHunterSkillPacket(HunterSkill.HunterSkillSlot slot, SkillId skillId) {
        this.slot = slot;
        this.skillId = skillId;
    }

    // デコード（読み込み）：SkillId.read を使用
    public C2S_SetHunterSkillPacket(FriendlyByteBuf buf) {
        this.slot = buf.readEnum(HunterSkill.HunterSkillSlot.class);
        this.skillId = SkillId.read(buf);
    }

    // エンコード（書き込み）：skillId.write を使用
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(this.slot);
        this.skillId.write(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(CapabilityRegistry.HUNTER_TRANSFORMATION).ifPresent(cap -> {
                // Capabilityにスキルを保存
                cap.setSkill(this.slot, this.skillId, player);
            });
        });
        context.setPacketHandled(true);
    }
}