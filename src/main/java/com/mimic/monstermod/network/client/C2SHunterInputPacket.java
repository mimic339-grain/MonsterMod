package com.mimic.monstermod.network.client;

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
 * Hunter入力パケット（入力解釈＋スキル発動）
 */
public class C2SHunterInputPacket {

    private static final Logger LOGGER =
            LoggerFactory.getLogger("C2SHunterInputPacket");

    private final int skillIndex;
    private final boolean dodge;
    private final boolean sheathToggle;

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

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            HunterTransformation ht = HunterUtil.getHunter(player);
            if (ht == null || !ht.isActive()) return;

            // 納刀/抜刀
            if (sheathToggle) {
                ht.setSheathed(player, !ht.isSheathed());
            }

            // スキル実行 (IdentityのhandleAbilityを呼ぶ)
            if (skillIndex >= 0 && skillIndex <= 3) {
                // identity.handleAbility を呼ぶことで、Identity内部のCDチェックと
                // その中からの SkillUtil.tryExecute 呼び出しが連鎖します
                ht.getIdentity().handleAbility(player, skillIndex);
            }
        });

        ctx.setPacketHandled(true);
    }
}