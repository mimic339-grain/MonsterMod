package com.mimic.monstermod.network.client;

import com.mimic.monstermod.Math.MathMain;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2C_SpawnSkillLeadPacket;
import com.mimic.monstermod.skill.ServerSkillExecutor;
import com.mimic.monstermod.skill.SkillId;
import com.mimic.monstermod.skill.SkillLead;
import com.mimic.monstermod.skill.SkillLeadRegistry;
import com.mimic.monstermod.skill.SkillLeadUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * C2S_SkillCastRequestPacket
 *
 * Client → Server
 *
 * Clientは「SkillIdを使いたい」という意思のみ送る
 * Serverがすべてを決定する
 */
public class C2S_SkillCastRequestPacket {

    private final SkillId skillId;

    /* ============================================================= */

    public C2S_SkillCastRequestPacket(SkillId skillId) {
        this.skillId = skillId;
    }

    /* ============================================================= */
    /* Encode / Decode */
    /* ============================================================= */

    public static void encode(
            C2S_SkillCastRequestPacket msg,
            FriendlyByteBuf buf
    ) {
        msg.skillId.write(buf);
    }

    public static C2S_SkillCastRequestPacket decode(
            FriendlyByteBuf buf
    ) {
        return new C2S_SkillCastRequestPacket(
                SkillId.read(buf)
        );
    }

    /* ============================================================= */
    /* Handle (SERVER) */
    /* ============================================================= */

    public static void handle(
            C2S_SkillCastRequestPacket msg,
            Supplier<NetworkEvent.Context> ctx
    ) {

        System.out.println("### C2S_SkillCastRequestPacket RECEIVED ###");

        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {

            ServerPlayer player = context.getSender();

            if (player == null) {
                System.out.println("SkillCast ERROR: player null");
                return;
            }

            System.out.println("SkillCast Player: " + player.getName().getString());
            System.out.println("SkillCast SkillId: " + msg.skillId);

            SkillLead lead;

            try {

                lead = SkillLeadRegistry.getStrict(msg.skillId);

                System.out.println("SkillLead FOUND: " + msg.skillId);

            } catch (Exception e) {

                System.out.println("SkillLead NOT FOUND: " + msg.skillId);
                e.printStackTrace();

                return;
            }

            Vec3 origin = player.position();

            System.out.println("Origin: " + origin);

            MathMain math = SkillLeadUtil.buildMath(lead, origin);

            System.out.println("Math built");

            if (lead.render2D
                    || lead.render2DOverlay
                    || lead.renderBlock2D
                    || lead.render3DPreview) {

                System.out.println("Sending Preview Packet");

                ModMessages.INSTANCE.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new S2C_SpawnSkillLeadPacket(
                                player.getId(),
                                lead,
                                math
                        )
                );
            }

            System.out.println("Executing ServerSkillExecutor");

            ServerSkillExecutor.execute(
                    player.serverLevel(),
                    player,
                    lead,
                    math
            );

        });

        context.setPacketHandled(true);
    }
}