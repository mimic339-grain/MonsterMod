package com.mimic.monstermod.network.packets;

import com.mimic.monstermod.skill.SkillLeadRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * サーバー → 全クライアント: スキルのフェーズ状態変化を通知。
 *
 * EFM参考:
 *   - network/server/SPAnimatorControl.java — アニメーション状態のS2C
 *
 * 受信時の処理:
 *   - クライアント側の SkillLeadRegistry にキャッシュ
 *   - PlayerRendererMixin がここからアニメーション名を取得して再生
 *   - MMOプレビュー系は PREVIEW フェーズ受信時に PreviewRenderer を起動
 *
 * 配置: com/mimic/monstermod/network/packets/S2CSkillStatePacket.java
 */
public class S2CSkillStatePacket {

    private final int       entityId;
    private final String    skillId;
    private final SkillState state;

    public S2CSkillStatePacket(int entityId, String skillId, SkillState state) {
        this.entityId = entityId;
        this.skillId  = skillId;
        this.state    = state;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeUtf(skillId);
        buf.writeUtf(state.name());
    }

    public static S2CSkillStatePacket decode(FriendlyByteBuf buf) {
        return new S2CSkillStatePacket(
                buf.readInt(),
                buf.readUtf(),
                SkillState.valueOf(buf.readUtf())
        );
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity entity = mc.level.getEntity(entityId);
            if (!(entity instanceof Player player)) return;

            // クライアント側スキル状態を更新
            SkillLeadRegistry.setClientSkillState(player.getUUID(), skillId, state);

            // PREVIEW フェーズ開始 → MMOプレビューレンダラーに通知
            if (state == SkillState.PREVIEW) {
                // PreviewRenderer.startPreview(player, skillId);  // Math/overlay系と連携
            }
            // CONTACT フェーズ → プレビュー終了
            if (state == SkillState.CONTACT || state == SkillState.END) {
                // PreviewRenderer.endPreview(player.getUUID());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}