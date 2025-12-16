package com.mimic.monstermod.network.client;

import com.mimic.monstermod.animation.Animate;
import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.util.HunterUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー戦闘入力パケット
 * - Skill1 / Skill2 / Skill3
 * - Dodge
 * - Sheath / Draw
 */
public class C2SHunterInputPacket {

    private final int skillIndex; // 0 = none, 1~3 = skill
    private final boolean dodge;
    private final boolean sheath;

    // ================================
    // コンストラクタ
    // ================================
    public C2SHunterInputPacket(int skillIndex, boolean dodge, boolean sheath) {
        this.skillIndex = skillIndex;
        this.dodge = dodge;
        this.sheath = sheath;
    }

    // ================================
    // Decode
    // ================================
    public C2SHunterInputPacket(FriendlyByteBuf buf) {
        this.skillIndex = buf.readInt();
        this.dodge = buf.readBoolean();
        this.sheath = buf.readBoolean();
    }

    // ================================
    // Encode
    // ================================
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(skillIndex);
        buf.writeBoolean(dodge);
        buf.writeBoolean(sheath);
    }

    // ================================
    // Handle (Server)
    // ================================
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            HunterTransformation ht = HunterUtil.getHunter(player);
            if (ht == null || !ht.isActive()) return;

            // ============================
            // Skill
            // ============================
            if (skillIndex > 0) {
                String anim = switch (skillIndex) {
                    case 1 -> ht.getSkill1AnimationName();
                    case 2 -> ht.getSkill2AnimationName();
                    case 3 -> ht.getSkill3AnimationName();
                    default -> null;
                };
                if (anim != null) Animate.play(player, anim);
            }

            // ============================
            // Dodge
            // ============================
            if (dodge) {
                String anim = ht.getDodgeAnimationName();
                if (anim != null) Animate.play(player, anim);
            }

            // ============================
            // Sheath / Draw
            // ============================
            if (sheath) {
                if (ht.isSheathed()) ht.unsheatheWeapon(player);
                else ht.sheatheWeapon(player);

                String anim = ht.isSheathed()
                        ? ht.getSheathAnimationName()
                        : ht.getDrawAnimationName();

                if (anim != null) Animate.play(player, anim);
            }
        });
        ctx.setPacketHandled(true);
    }
}
