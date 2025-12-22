package com.mimic.monstermod.network.client;

import com.mimic.monstermod.animation.Animate;
import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.util.HunterUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Client → Server
 * Hunter入力パケット（入力解釈＋アニメ再生）
 */
public class C2SHunterInputPacket {

    private static final Logger LOGGER =
            LoggerFactory.getLogger("C2SHunterInputPacket");

    private final int skillIndex;
    private final boolean dodge;
    private final boolean sheathToggle;

    // ------------------------------------------------
    // Ctor
    // ------------------------------------------------
    public C2SHunterInputPacket(int skillIndex, boolean dodge, boolean sheathToggle) {
        this.skillIndex = skillIndex;
        this.dodge = dodge;
        this.sheathToggle = sheathToggle;
    }

    public C2SHunterInputPacket(FriendlyByteBuf buf) {
        this.skillIndex = buf.readInt();
        this.dodge = buf.readBoolean();
        this.sheathToggle = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(skillIndex);
        buf.writeBoolean(dodge);
        buf.writeBoolean(sheathToggle);
    }

    // ------------------------------------------------
    // Server Handle
    // ------------------------------------------------
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            HunterTransformation ht = HunterUtil.getHunter(player);
            if (ht == null) return;

            // ----------------------------
            // 納刀 / 抜刀（常に処理）
            // ----------------------------
            if (sheathToggle) {
                boolean before = ht.isSheathed();
                ht.setSheathed(player, !before);

                String anim = ht.isSheathed()
                        ? ht.getSheathAnimationName()
                        : ht.getDrawAnimationName();

                if (anim != null)
                    Animate.play(player, anim);

                LOGGER.info(
                        "[Sheath] {} : {} -> {}",
                        player.getName().getString(),
                        before,
                        ht.isSheathed()
                );
            }

            // ----------------------------
            // Skill / Dodge（Active時のみ）
            // ----------------------------
            if (!ht.isActive()) return;

            if (skillIndex > 0) {
                String anim = switch (skillIndex) {
                    case 1 -> ht.getSkill1AnimationName();
                    case 2 -> ht.getSkill2AnimationName();
                    case 3 -> ht.getSkill3AnimationName();
                    default -> null;
                };

                if (anim != null) {
                    Animate.play(player, anim);
                    LOGGER.info("[Skill] {} -> {}", skillIndex, anim);
                }
            }

            if (dodge) {
                String anim = ht.getDodgeAnimationName();
                if (anim != null) {
                    Animate.play(player, anim);
                    LOGGER.info("[Dodge] {}", anim);
                }
            }
        });

        ctx.setPacketHandled(true);
    }
}
