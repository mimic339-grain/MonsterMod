package com.mimic.monstermod.network.server;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SPlayerSkillInputPacket {

    private final int skillIndex;

    public C2SPlayerSkillInputPacket(int skillIndex) { this.skillIndex = skillIndex; }
    public C2SPlayerSkillInputPacket(net.minecraft.network.FriendlyByteBuf buf) { this.skillIndex = buf.readInt(); }
    public void toBytes(net.minecraft.network.FriendlyByteBuf buf) { buf.writeInt(skillIndex); }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                    .ifPresent(trans -> {
                        BaseMonsterIdentity identity = trans.getIdentity();
                        if (identity == null) return;

                        boolean isSkill = skillIndex >= 0;
                        boolean isMenu = skillIndex == -1;

                        // サーバー側で発動可能かチェック（例：クールタイム）
                        if (isSkill) {
                            // TODO: Identity 内でクールタイムチェック
                        }

                        // Identity 側で入力処理
                        identity.handleClientInput(player, isSkill, isMenu, skillIndex);

                        // 発動結果やクールタイム更新を S2C パケットで送る場合はここで
                        // ModMessages.INSTANCE.send(...);
                    });
        });
        context.setPacketHandled(true);
    }

    public int getSkillIndex() { return skillIndex; }
}
