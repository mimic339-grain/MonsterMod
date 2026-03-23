package com.mimic.monstermod.network.server;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.events.PreviewEvents;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * S2C_SpawnSkillLeadPacket
 *
 * 【役割】
 * ・Server が決定した SkillLead + MathMain を Client に同期
 * ・Client 側で Preview（可視化）のみを開始する
 *
 * 【重要】
 * ・Client は Math / 判定 / 攻撃を決定しない
 * ・AoeMarker / Manager 等は一切存在しない
 * ・Preview の実体は MathMain
 * ・自分のプレイヤーのローカル描画は spawnLocal() で行っているため、
 *   S2C受信時は自分に対しては何もしない
 */
public final class S2C_SpawnSkillLeadPacket {

    public final int casterEntityId;
    public final SkillId skillId;
    public final MathMain math;

    /* =============================================================
     * Constructor
     * ============================================================= */

    public S2C_SpawnSkillLeadPacket(int casterEntityId, SkillLead lead, MathMain math) {
        this.casterEntityId = casterEntityId;
        this.skillId = lead.skillId();
        this.math = math;
    }

    private S2C_SpawnSkillLeadPacket(int casterEntityId, SkillId skillId, MathMain math) {
        this.casterEntityId = casterEntityId;
        this.skillId = skillId;
        this.math = math;
    }

    /* =============================================================
     * Encode / Decode
     * ============================================================= */

    public static void encode(S2C_SpawnSkillLeadPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.casterEntityId);
        msg.skillId.write(buf);
        msg.math.write(buf);
    }

    public static S2C_SpawnSkillLeadPacket decode(FriendlyByteBuf buf) {
        int casterEntityId = buf.readInt();
        SkillId skillId = SkillId.read(buf);
        MathMain math = MathMain.read(buf);
        return new S2C_SpawnSkillLeadPacket(casterEntityId, skillId, math);
    }

    /* =============================================================
     * Handle (Client)
     * ============================================================= */

    public static void handle(S2C_SpawnSkillLeadPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();

            if (mc.level == null) {
                System.out.println("[SkillPreview] level == null");
                return;
            }

            Entity caster = mc.level.getEntity(msg.casterEntityId);
            if (caster == null) {
                System.out.println("[SkillPreview] caster not found");
                return;
            }

            // ★ 自分のプレイヤーはローカル描画しているため無視
            if (caster == mc.player) return;

            SkillLead lead = SkillLeadRegistry.getNullable(msg.skillId);
            if (lead == null) {
                System.out.println("[SkillPreview] SkillLead missing on client: " + msg.skillId);
                return;
            }

            System.out.println("[SkillPreview] Spawning preview for " + caster.getName().getString());

            PreviewEvents.spawnFromServer(caster, lead, msg.math);
        });

        context.setPacketHandled(true);
    }
}