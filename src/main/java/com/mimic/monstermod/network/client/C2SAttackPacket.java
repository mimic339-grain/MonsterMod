package com.mimic.monstermod.network.client;

import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.item.weapon.WeaponItem;
import com.mimic.monstermod.util.HunterUtil;
import com.mimic.monstermod.variable.CapabilityRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SAttackPacket {

    public C2SAttackPacket() {}
    public C2SAttackPacket(FriendlyByteBuf buf) {}
    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {

            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            // --- 変身状態 ---
            HunterTransformation ht = HunterUtil.getHunter(player);
            if (ht == null || !ht.isActive()) return;

            // --- 武器チェック ---
            var stack = player.getMainHandItem();
            if (!(stack.getItem() instanceof WeaponItem weapon)) return;

            // --- CombatState ---
            player.getCapability(CapabilityRegistry.HUNTER_COMBAT_STATE).ifPresent(cs -> {

                // 抜刀してないと攻撃不可
                if (!cs.isDrawn()) return;

                // 硬直中は不可
                if (cs.isStiff()) return;

                // ★ CombatUtil へ攻撃処理を委譲（あなたの最新版ロジック）
                CombatUtil.performMeleeAttack(player, stack, ht, cs);

                // ★ 攻撃アニメーション（後で CombatState に統合するならここで呼ぶ）
                // Animate.play(player, "sword_slash1"); など
                // 今はアニメーションシステムが未接続なのでコメントアウト。

                // ★ クライアント同期
                cs.syncToClient(player);
            });

        });

        ctx.setPacketHandled(true);
    }
}
