package com.mimic.monstermod.network.client;

import com.mimic.monstermod.animation.Animate;
import com.mimic.monstermod.capability.HunterTransformation;
import com.mimic.monstermod.util.HunterUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * クライアント → サーバー入力パケット
 * キー入力に応じてスキル・DODGE・抜刀/納刀・メニューを反映
 * サーバー側でアニメーションも再生
 */
public class C2SHunterInputPacket {

    private final boolean pressed;
    private final int skillIndex; // 1,2,3 または 0:スキル無し
    private final boolean menu;
    private final boolean dodge;
    private final boolean sheath;

    // ================================
    // コンストラクタ
    // ================================
    public C2SHunterInputPacket(int skillIndex, boolean dodge, boolean sheath, boolean menu) {
        this.pressed = skillIndex > 0;
        this.skillIndex = skillIndex;
        this.menu = menu;
        this.dodge = dodge;
        this.sheath = sheath;
    }

    // デシリアライズ
    public C2SHunterInputPacket(FriendlyByteBuf buf) {
        this.pressed = buf.readBoolean();
        this.skillIndex = buf.readInt();
        this.menu = buf.readBoolean();
        this.dodge = buf.readBoolean();
        this.sheath = buf.readBoolean();
    }

    // シリアライズ
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(pressed);
        buf.writeInt(skillIndex);
        buf.writeBoolean(menu);
        buf.writeBoolean(dodge);
        buf.writeBoolean(sheath);
    }

    // サーバー側処理
    public void handle(Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            HunterTransformation ht = HunterUtil.getHunter(player);
            if (ht == null || !ht.isActive()) return;

            // ============================
            // スキル使用
            // ============================
            if (pressed && skillIndex > 0) {
                String skillAnim = switch (skillIndex) {
                    case 1 -> ht.getSkill1AnimationName();
                    case 2 -> ht.getSkill2AnimationName();
                    case 3 -> ht.getSkill3AnimationName();
                    default -> null;
                };
                if (skillAnim != null) {
                    // テスト用：攻撃判定は未実装
                    Animate.play(player, skillAnim);
                }
            }

            // ============================
            // DODGE
            // ============================
            if (dodge) {
                String dodgeAnim = ht.getDodgeAnimationName();
                if (dodgeAnim != null) Animate.play(player, dodgeAnim);
            }

            // ============================
            // Sheath / Draw
            // ============================
            if (sheath) {
                if (ht.isSheathed()) ht.unsheatheWeapon(player);
                else ht.sheatheWeapon(player);

                String sheathAnim = ht.isSheathed() ? ht.getSheathAnimationName() : ht.getDrawAnimationName();
                if (sheathAnim != null) Animate.play(player, sheathAnim);
            }

            // ============================
            // Menu
            // ============================
            if (menu) {
                // GUI開く処理（後で実装）
            }
        });
        ctx.setPacketHandled(true);
    }
}
