package com.mimic.monstermod.identity.impl;

import com.mimic.monstermod.entity.BaseMonsterEntity;
import com.mimic.monstermod.entity.monster.MimicEntity;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CMimicDodgePacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class MimicIdentity extends BaseMonsterIdentity {

    private static final int SKILL_COUNT = 2; // switch, bite

    public MimicIdentity(@Nullable BaseMonsterEntity entity) {
        super(entity, SKILL_COUNT);
    }

    @Override
    public void handleAbility(Player player, int skillIndex) {
        if (skillIndex < 0 || skillIndex >= abilityCooldowns.length) return;
        if (abilityCooldowns[skillIndex] > 0) return;

        BaseMonsterEntity entity = getEntity();
        if (!(entity instanceof MimicEntity mimic)) return;

        switch (skillIndex) {
            case 0 -> {
                System.out.println("[MimicIdentity] handleAbility skillIndex=0, skill=switch");
                mimic.getMonsterData().setSkill("switch");
            }
            case 1 -> {
                System.out.println("[MimicIdentity] handleAbility skillIndex=1, skill=bite");
                mimic.getMonsterData().setSkill("bite");
            }
        }

        abilityCooldowns[skillIndex] = 0;
    }

    @Override
    public void handleClientInput(Player player, boolean useKey, boolean menuKey, int skillIndex) {
        if (menuKey) handleMenu(player);
        if (useKey && skillIndex >= 0) handleAbility(player, skillIndex);
    }

    /**
     * Mimic 固有の瞬間移動 Dodge
     */
    @Override
    public void handleDodge(Player player) {
        if (player == null) return;

        // 移動量（回避距離）
        float yaw = player.getYRot();
        double radians = Math.toRadians(yaw);
        double distance = 15.0;

        double dx = -Math.sin(radians) * distance;
        double dz = Math.cos(radians) * distance;

        // Player を瞬間移動
        Vec3 targetPos = player.position().add(dx, 0, dz);
        player.setPos(targetPos.x, targetPos.y, targetPos.z);
        player.xOld = targetPos.x;
        player.yOld = targetPos.y;
        player.zOld = targetPos.z;
        player.setDeltaMovement(Vec3.ZERO);

        // サーバ側ならクライアントに同期
        if (!player.level().isClientSide() && player instanceof ServerPlayer sp) {
            ModMessages.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new S2CMimicDodgePacket(player.getId(), targetPos)
            );
        }
    }


    @Override
    public void tickServer(Player player) {
        super.tickServer(player);
        for (int i = 0; i < abilityCooldowns.length; i++) {
            if (abilityCooldowns[i] > 0) abilityCooldowns[i]--;
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        BaseMonsterEntity entity = getEntity();
        if (entity instanceof MimicEntity mimic) {
            tag.putBoolean("isOpen", mimic.isOpen());
        }
        tag.putIntArray("cooldowns", abilityCooldowns);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        super.deserializeNBT(tag);
        BaseMonsterEntity entity = getEntity();
        if (entity instanceof MimicEntity mimic) {
            if (tag.contains("isOpen")) mimic.setOpen(tag.getBoolean("isOpen"));
        }
        if (tag.contains("cooldowns")) {
            int[] cd = tag.getIntArray("cooldowns");
            for (int i = 0; i < abilityCooldowns.length && i < cd.length; i++) {
                abilityCooldowns[i] = cd[i];
            }
        }
    }
}
