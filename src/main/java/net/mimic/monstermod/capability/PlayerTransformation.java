package net.mimic.monstermod.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer; // ServerPlayerをインポート
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.networking.ModMessages; // ModMessagesをインポート
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket; // S2CTransformSyncPacketをインポート

public class PlayerTransformation implements IPlayerTransformation {
    private boolean isTransformed = false;
    private ResourceLocation transformedMobId = null;
    private LivingEntity originalMob = null;

    private MimicEntity.MimicAnimationState mimicState = MimicEntity.MimicAnimationState.IDLE;
    private boolean isBiting = false;

    @Override
    public boolean isTransformed() {
        return isTransformed;
    }

    @Override
    public void setTransformed(boolean transformed) {
        this.isTransformed = transformed;
        if (!transformed) {
            this.mimicState = MimicEntity.MimicAnimationState.IDLE;
            this.isBiting = false;
        }
    }

    @Override
    public ResourceLocation getTransformedMobId() {
        return transformedMobId;
    }

    @Override
    public void setTransformedMobId(ResourceLocation mobId) {
        this.transformedMobId = mobId;
    }

    @Override
    public LivingEntity getOriginalMob() {
        return originalMob;
    }

    @Override
    public void setOriginalMob(LivingEntity mob) {
        this.originalMob = mob;
    }

    @Override
    public MimicEntity.MimicAnimationState getMimicState() {
        return mimicState;
    }

    @Override
    public void setMimicState(MimicEntity.MimicAnimationState state) {
        this.mimicState = state;
    }

    @Override
    public boolean isBiting() {
        return isBiting;
    }

    @Override
    public void setBiting(boolean biting) {
        this.isBiting = biting;
    }

    @Override
    public void saveNBTData(CompoundTag nbt) {
        nbt.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) {
            nbt.putString("transformedMobId", transformedMobId.toString());
        }
        nbt.putString("mimicState", mimicState.name());
        nbt.putBoolean("isBiting", isBiting);
    }

    @Override
    public void loadNBTData(CompoundTag nbt) {
        isTransformed = nbt.getBoolean("isTransformed");
        if (nbt.contains("transformedMobId")) {
            transformedMobId = new ResourceLocation(nbt.getString("transformedMobId"));
        } else {
            transformedMobId = null;
        }
        if (nbt.contains("mimicState")) {
            try {
                mimicState = MimicEntity.MimicAnimationState.valueOf(nbt.getString("mimicState"));
            } catch (IllegalArgumentException e) {
                mimicState = MimicEntity.MimicAnimationState.IDLE;
            }
        } else {
            mimicState = MimicEntity.MimicAnimationState.IDLE;
        }
        isBiting = nbt.getBoolean("isBiting");
    }

    // サーバーからクライアントへ状態を同期するためのヘルパーメソッドの実装
    @Override
    public void syncToClient(ServerPlayer player) {
        ModMessages.sendToPlayer(new S2CTransformSyncPacket(isTransformed, transformedMobId, mimicState.name(), isBiting), player);
    }
}