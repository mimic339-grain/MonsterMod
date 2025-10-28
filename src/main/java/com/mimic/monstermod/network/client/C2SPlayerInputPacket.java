package com.mimic.monstermod.network.client;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SPlayerInputPacket {
    private final float forward;
    private final float strafe;
    private final boolean jump;
    private final boolean sprint;
    private final boolean useSkill;
    private final boolean menuOpen;
    private final int skillIndex;

    // 移動用
    public C2SPlayerInputPacket(float forward, float strafe, boolean jump, boolean sprint) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sprint = sprint;
        this.useSkill = false;
        this.menuOpen = false;
        this.skillIndex = -1;
    }

    // スキル・メニュー用
    public C2SPlayerInputPacket(boolean useSkill, boolean menuOpen, int skillIndex) {
        this.forward = 0;
        this.strafe = 0;
        this.jump = false;
        this.sprint = false;
        this.useSkill = useSkill;
        this.menuOpen = menuOpen;
        this.skillIndex = skillIndex;
    }

    // ===== Encode / Decode =====
    public static void encode(C2SPlayerInputPacket pkt, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeFloat(pkt.forward);
        buf.writeFloat(pkt.strafe);
        buf.writeBoolean(pkt.jump);
        buf.writeBoolean(pkt.sprint);
        buf.writeBoolean(pkt.useSkill);
        buf.writeBoolean(pkt.menuOpen);
        buf.writeInt(pkt.skillIndex);
    }

    public static C2SPlayerInputPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        float f = buf.readFloat();
        float s = buf.readFloat();
        boolean j = buf.readBoolean();
        boolean sp = buf.readBoolean();
        boolean use = buf.readBoolean();
        boolean menu = buf.readBoolean();
        int idx = buf.readInt();
        if (use || menu) return new C2SPlayerInputPacket(use, menu, idx);
        return new C2SPlayerInputPacket(f, s, j, sp);
    }

    // ===== Handle =====
    public static void handle(C2SPlayerInputPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                    .ifPresent(trans -> {
                        BaseMonsterEntity entity = trans.getEntity();
                        BaseMonsterIdentity identity = trans.getIdentity();
                        if (entity == null || identity == null) return;

                        // 移動入力
                        if (pkt.forward != 0 || pkt.strafe != 0) entity.moveRelative(pkt.forward, pkt.strafe);
                        if (pkt.jump) entity.jumpFromGround();
                        entity.setSprinting(pkt.sprint);
                        entity.setPlayerActiveMove(pkt.forward != 0 || pkt.strafe != 0 || pkt.jump);

                        // スキル入力
                        if (pkt.useSkill && pkt.skillIndex >= 0) {
                            // サーバー側クールタイムチェック
                            int[] cooldowns = identity.getAbilityCooldowns();
                            if (cooldowns[pkt.skillIndex] <= 0) {
                                // performAbilityを呼ぶ
                                entity.performAbility(pkt.skillIndex);

                                // モンスターごとの可変クールタイム設定
                                int cd = entity.getMonsterData() != null ?
                                        entity.getMonsterData().getSkillCooldown(pkt.skillIndex) : 20;
                                cooldowns[pkt.skillIndex] = cd;
                            }
                        }

                        // メニュー入力
                        if (pkt.menuOpen) {
                            identity.handleClientInput(player, false, true, -1);
                        }
                    });
        });
        ctx.setPacketHandled(true);
    }

    // ===== Getter =====
    public float getForward() { return forward; }
    public float getStrafe() { return strafe; }
    public boolean isJump() { return jump; }
    public boolean isSprint() { return sprint; }
    public boolean isUseSkill() { return useSkill; }
    public boolean isMenuOpen() { return menuOpen; }
    public int getSkillIndex() { return skillIndex; }
}
