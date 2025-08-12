package com.mimic.monster.capability;

import com.mimic.monster.network.NetworkHandler;
import com.mimic.monster.network.client.S2CUpdateTransformPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;


public class TransformUtils {

    // デフォルトのステータス値を定数として持つ
    private static final double DEFAULT_MAX_HEALTH = 20.0D;
    private static final double DEFAULT_ATTACK_DAMAGE = 1.0D;
    private static final double DEFAULT_MOVEMENT_SPEED = 0.1D;

    //プレイヤーの変身を解除し、元の状態に戻す処理
    public static void revertTransform(ServerPlayer player) {
        if (player == null) return;

        // Capabilityのデータリセット
        player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(cap -> {
            cap.reset();  // Capabilityの変身状態のリセット
        });

        // プレイヤーの表示を戻す
        player.setInvisible(false);

        // 当たり判定を元に戻すためサイズをリフレッシュ
        player.refreshDimensions();

        // ステータスをデフォルト値に戻す
        if (player.getAttribute(Attributes.MAX_HEALTH) != null) {
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(DEFAULT_MAX_HEALTH);
            player.setHealth((float) Math.min(player.getHealth(), DEFAULT_MAX_HEALTH));
        }
        if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(DEFAULT_ATTACK_DAMAGE);
        }
        if (player.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            player.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(DEFAULT_MOVEMENT_SPEED);
        }

        // クライアントに変身解除を通知（エラーはログ出しで無視）
        try {
            NetworkHandler.sendToClient(new S2CUpdateTransformPacket(player.getId(), null), player);
        } catch (Exception e) {
            System.err.println("UpdateTransformPacketを送るのに失敗しました" + e.getMessage());
        }


        // 周囲クライアントに変身解除を通知
        try {
            NetworkHandler.sendToAllTracking(new S2CUpdateTransformPacket(player.getId(), null), player);
        } catch (Exception e) {
            System.err.println("UpdateTransformPacketを送るのに失敗しました" + e.getMessage());
        }
    }
}
