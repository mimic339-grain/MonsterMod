package net.mimic.monstermod.entity.custom;

import net.mimic.monstermod.entity.BaseMonsterEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class MimicEntity extends BaseMonsterEntity<MimicEntity.MimicAnimationState> {

    public enum MimicAnimationState {
        IDLE,
        OPEN_IDLE,
        OPEN,
        CLOSE,
        OPENJUMP,
        CLOSEJUMP,
        BITE
    }

    private static final EntityDataAccessor<Boolean> OPEN =
            SynchedEntityData.defineId(MimicEntity.class, EntityDataSerializers.BOOLEAN);

    private String linkedPlayerUUID = null; // このMimicがリンクしているプレイヤーUUID

    public MimicEntity(EntityType<? extends BaseMonsterEntity<?>> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(OPEN, false);
    }

    public boolean isOpen() {
        return this.entityData.get(OPEN);
    }

    public void setOpen(boolean open) {
        this.entityData.set(OPEN, open);
    }

    public void linkToPlayer(String playerUUID) {
        this.linkedPlayerUUID = playerUUID;
    }

    public boolean isLinkedTo(Player player) {
        return player != null && player.getUUID().toString().equals(this.linkedPlayerUUID);
    }

    @Override
    protected Class<MimicAnimationState> getAnimationStateClass() {
        return MimicAnimationState.class;
    }

    @Override
    protected MimicAnimationState getDefaultAnimationState() {
        return MimicAnimationState.IDLE;
    }

    @Override
    protected String getAnimationName(MimicAnimationState state) {
        return switch (state) {
            case BITE -> "animation.mimic.bite";
            case OPEN -> "animation.mimic.open";
            case CLOSE -> "animation.mimic.close";
            case OPENJUMP -> "animation.mimic.openjump";
            case CLOSEJUMP -> "animation.mimic.closejump";
            case IDLE -> "animation.mimic.idle";
            case OPEN_IDLE -> "animation.mimic.open_idle";
        };
    }

    @Override
    protected boolean shouldLoop(MimicAnimationState state) {
        return switch (state) {
            case OPENJUMP, CLOSEJUMP, OPEN_IDLE, IDLE -> true;
            case OPEN, CLOSE, BITE -> false;
        };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return BaseMonsterEntity.createDefaultAttributes(200.0D, 0.25D, 4.0D, 0.2D, 2.0D, 1.0D);
    }
}
