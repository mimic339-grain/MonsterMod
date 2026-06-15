package com.mimic.monstermod.network.client;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.identity.HunterIdentity;
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
            if (ht == null || !ht.isActive()) {
                LOGGER.info("DEBUG: Hunter is inactive or null. Active={}", ht != null ? ht.isActive() : "null");
                return;
            }

            // 鞘の切り替え
            if (sheathToggle) {
                ht.setSheathed(player, !ht.isSheathed());
                LOGGER.info("DEBUG: Sheath state toggled to {}", ht.isSheathed());
            }

            // ★修正ポイント：Identity が存在する場合、実行前に必ず強制同期を行う
            if (ht.getIdentity() instanceof HunterIdentity hi) {
                // HunterTransformation 内の refreshIdentitySkills メソッドを直接呼び出すか、
                // 同等の処理をここで実行します
                ht.refreshIdentitySkills();

                if (skillIndex >= 0) {
                    LOGGER.info("DEBUG: Calling serverExecuteSkill for index {}", skillIndex);
                    hi.serverExecuteSkill(player, skillIndex);
                } else {
                    LOGGER.info("DEBUG: Invalid index ({})", skillIndex);
                }
            } else {
                LOGGER.info("DEBUG: Identity is null or not a HunterIdentity");
            }
        });
        ctx.setPacketHandled(true);
    }
}