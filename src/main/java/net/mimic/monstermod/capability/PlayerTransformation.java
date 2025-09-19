package net.mimic.monstermod.capability;

import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.identity.PlayerIdentityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PlayerTransformation implements IPlayerTransformation {

    private boolean originalStatsSaved = false;
    private double originalHealth, originalMaxHealth, originalAttack, originalArmor, originalSpeed;
    private boolean noKnockback = false;
    private Entity transformedEntity;
    private boolean isTransformed = false;
    private ResourceLocation transformedMobId = null;

    private final Map<ResourceLocation, MonsterState> monsterStates = new HashMap<>();
    private MimicEntity.MimicAnimationState baseState = MimicEntity.MimicAnimationState.IDLE;
    private MimicEntity.MimicAnimationState lastSyncedState = MimicEntity.MimicAnimationState.IDLE;

    // ----------------------------
    // 元ステータス管理
    // ----------------------------
    public boolean hasSavedOriginalStats() { return originalStatsSaved; }
    public void setOriginalHealth(double hp) { this.originalHealth = hp; originalStatsSaved = true; }
    public void setOriginalMaxHealth(double maxHp) { this.originalMaxHealth = maxHp; }
    public void setOriginalAttackDamage(double dmg) { this.originalAttack = dmg; }
    public void setOriginalArmor(double armor) { this.originalArmor = armor; }
    public void setOriginalMoveSpeed(double speed) { this.originalSpeed = speed; }
    public double getOriginalHealth() { return originalHealth; }
    public double getOriginalMaxHealth() { return originalMaxHealth; }
    public double getOriginalAttackDamage() { return originalAttack; }
    public double getOriginalArmor() { return originalArmor; }
    public double getOriginalMoveSpeed() { return originalSpeed; }
    public void clearOriginalStats() { originalStatsSaved = false; }
    public boolean isNoKnockback() { return noKnockback; }
    public void setNoKnockback(boolean value) { noKnockback = value; }

    // ----------------------------
    // Transformed Entity 管理
    // ----------------------------
    @Override @Nullable
    public Entity getTransformedEntity() { return transformedEntity; }
    @Override
    public void setTransformedEntity(@Nullable Entity entity) { this.transformedEntity = entity; }
    @Override
    public boolean isTransformed() { return isTransformed; }
    @Override
    public void setTransformed(boolean transformed) { this.isTransformed = transformed; }
    @Override
    public ResourceLocation getTransformedMobId() { return transformedMobId; }
    @Override
    public void setTransformedMobId(ResourceLocation mobId) { this.transformedMobId = mobId; }

    public MonsterState getMonsterState(ResourceLocation mobId) {
        return mobId != null ? monsterStates.getOrDefault(mobId, new MonsterState()) : new MonsterState();
    }

    public void setMonsterState(ResourceLocation mobId, MonsterState state) {
        if (mobId != null) monsterStates.put(mobId, state);
    }

    @Override
    public MimicEntity.MimicAnimationState getAnimationState(ResourceLocation mobId) {
        return getMonsterState(mobId).getAnimationEnum();
    }

    @Override
    public void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResourceLocation mobId = transformedMobId;
            MonsterState state = getMonsterState(mobId);
            if (state.getAnimationEnum() != lastSyncedState) {
                // 通信パケット送信（省略）
                lastSyncedState = state.getAnimationEnum();
                System.out.println("[DEBUG] syncToClient: player=" + player.getName().getString() + ", animation=" + lastSyncedState);
            }
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) nbt.putString("transformedMobId", transformedMobId.toString());
        CompoundTag statesTag = new CompoundTag();
        for (Map.Entry<ResourceLocation, MonsterState> entry : monsterStates.entrySet()) {
            CompoundTag stateTag = new CompoundTag();
            stateTag.putString("animationState", entry.getValue().animationState);
            statesTag.put(entry.getKey().toString(), stateTag);
        }
        nbt.put("monsterStates", statesTag);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        isTransformed = nbt.getBoolean("isTransformed");
        transformedMobId = nbt.contains("transformedMobId") ? new ResourceLocation(nbt.getString("transformedMobId")) : null;
        monsterStates.clear();
        if (nbt.contains("monsterStates")) {
            CompoundTag statesTag = nbt.getCompound("monsterStates");
            for (String key : statesTag.getAllKeys()) {
                ResourceLocation mobId = new ResourceLocation(key);
                CompoundTag stateTag = statesTag.getCompound(key);
                MonsterState state = new MonsterState();
                state.animationState = stateTag.getString("animationState");
                monsterStates.put(mobId, state);
            }
        }
    }

    // ----------------------------
    // Tick & BaseState 管理
    // ----------------------------
    public void tickTransformation(Player player) {
        if (!isTransformed()) {
            System.out.println("[DEBUG] Player " + player.getName().getString() + " is NOT transformed.");
            return;
        }

        if (player.isDeadOrDying()) {
            System.out.println("[DEBUG] Player " + player.getName().getString() + " died. Resetting transformation.");
            setTransformed(false);
            setTransformedEntity(null);
            setBaseState(MimicEntity.MimicAnimationState.IDLE);
        } else {
            System.out.println("[DEBUG] Player " + player.getName().getString() + " is transformed. BaseState=" + baseState);
        }
    }

    @Override
    public MimicEntity.MimicAnimationState getBaseState() {
        return baseState;
    }

    @Override
    public void setBaseState(MimicEntity.MimicAnimationState state) {
        System.out.println("[DEBUG] setBaseState called. New state=" + state);
        this.baseState = state;
    }

    public boolean canUpdateBaseState(ClientMimicEntity entity) {
        MimicEntity.MimicAnimationState current = entity.getLastRequestedAnimation();
        return current == null || entity.isLoopAnimation(current);
    }

    public void updateBaseStateSafely(ClientMimicEntity entity, MimicEntity.MimicAnimationState newBaseState) {
        if (entity == null) {
            System.out.println("[DEBUG] updateBaseStateSafely called with null entity.");
            return;
        }
        MimicEntity.MimicAnimationState currentAnimation = entity.getLastRequestedAnimation();
        System.out.println("[DEBUG] updateBaseStateSafely: currentAnimation=" + currentAnimation + ", newBaseState=" + newBaseState);
        if (currentAnimation == null || entity.isLoopAnimation(currentAnimation)) {
            System.out.println("[DEBUG] BaseState updated safely to " + newBaseState);
            this.baseState = newBaseState;
        } else {
            System.out.println("[DEBUG] Non-loop animation playing, baseState not updated.");
        }
    }

    @Override @Nullable
    public IPlayerIdentity getTransformedIdentity() {
        return (isTransformed && transformedMobId != null) ? PlayerIdentityRegistry.getIdentity(transformedMobId) : null;
    }

    // ----------------------------
    // MonsterState 内部クラス
    // ----------------------------
    public static class MonsterState {
        public String animationState = "IDLE";
        private final Map<String, Boolean> customFlags = new HashMap<>();
        public void setFlag(String key, boolean value) { customFlags.put(key, value); }
        public boolean getFlag(String key) { return customFlags.getOrDefault(key, false); }
        public MimicEntity.MimicAnimationState getAnimationEnum() {
            if (animationState == null) return MimicEntity.MimicAnimationState.IDLE;
            try { return MimicEntity.MimicAnimationState.valueOf(animationState); }
            catch (IllegalArgumentException e) { return MimicEntity.MimicAnimationState.IDLE; }
        }
    }
}
