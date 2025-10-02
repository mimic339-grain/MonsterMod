package net.mimic.monstermod.capability;


import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.impl.MimicIdentity;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PlayerTransformation implements IPlayerTransformation {

    private boolean originalStatsSaved = false;
    private double originalHealth, originalMaxHealth, originalAttack, originalArmor, originalSpeed;
    private boolean noKnockback = false;
    private boolean isTransformed = false;
    private ResourceLocation transformedMobId = null;

    private final Map<ResourceLocation, MonsterState> monsterStates = new HashMap<>();

    @Override
    public boolean hasSavedOriginalStats() { return originalStatsSaved; }
    @Override public void setOriginalHealth(double hp) { originalHealth = hp; originalStatsSaved = true; }
    @Override public void setOriginalMaxHealth(double maxHp) { originalMaxHealth = maxHp; }
    @Override public void setOriginalAttackDamage(double dmg) { originalAttack = dmg; }
    @Override public void setOriginalArmor(double armor) { originalArmor = armor; }
    @Override public void setOriginalMoveSpeed(double speed) { originalSpeed = speed; }
    @Override public double getOriginalHealth() { return originalHealth; }
    @Override public double getOriginalMaxHealth() { return originalMaxHealth; }
    @Override public double getOriginalAttackDamage() { return originalAttack; }
    @Override public double getOriginalArmor() { return originalArmor; }
    @Override public double getOriginalMoveSpeed() { return originalSpeed; }
    @Override public void clearOriginalStats() { originalStatsSaved = false; }
    @Override public boolean isNoKnockback() { return noKnockback; }
    @Override public void setNoKnockback(boolean value) { noKnockback = value; }
    @Nullable
    @Override public Entity getTransformedEntity() { return null; }
    @Override public void setTransformedEntity(@Nullable Entity entity) {}
    @Override public boolean isTransformed() { return isTransformed; }
    @Override public void setTransformed(boolean transformed) { isTransformed = transformed; }
    @Override public ResourceLocation getTransformedMobId() { return transformedMobId; }
    @Override public void setTransformedMobId(ResourceLocation mobId) { transformedMobId = mobId; }

    @Override
    public MonsterState getMonsterState(ResourceLocation mobId) {
        return mobId != null ? monsterStates.getOrDefault(mobId, new MonsterState()) : new MonsterState();
    }

    @Override
    public void setMonsterState(ResourceLocation mobId, MonsterState state) {
        if (mobId != null) monsterStates.put(mobId, state);
    }

    @Override
    public MimicEntity.MimicAnimationState getAnimationState(ResourceLocation mobId) {
        return getMonsterState(mobId).getAnimationEnum();
    }

    @Override
    public int getAnimationTick(ResourceLocation mobId) {
        return getMonsterState(mobId).animationTick;
    }

    @Override
    public MimicIdentity getTransformedIdentity() {
        if (transformedMobId == null) return null;
        return MimicIdentity.INSTANCE;
    }

    @Override
    public void setAnimationTick(ResourceLocation mobId, int tick) {
        MonsterState state = getMonsterState(mobId);
        // ★ 修正：tick は必ず上書きではなく差分チェック
        if (Math.abs(state.animationTick - tick) > 2) {
            state.animationTick = tick;
        }
        setMonsterState(mobId, state);
        MonsterMod.getLogger().trace("[setAnimationTick] mobId={} tick={}", mobId, tick);
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
            stateTag.putInt("animationTick", entry.getValue().animationTick);

            CompoundTag flagsTag = new CompoundTag();
            for (Map.Entry<String, Boolean> flag : entry.getValue().customFlags.entrySet()) {
                flagsTag.putBoolean(flag.getKey(), flag.getValue());
            }
            stateTag.put("customFlags", flagsTag);

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
                state.animationTick = stateTag.getInt("animationTick");

                if (stateTag.contains("customFlags")) {
                    CompoundTag flagsTag = stateTag.getCompound("customFlags");
                    for (String flagKey : flagsTag.getAllKeys()) {
                        state.setFlag(flagKey, flagsTag.getBoolean(flagKey));
                    }
                }
                monsterStates.put(mobId, state);
            }
        }
        MonsterMod.getLogger().debug("[deserializeNBT] isTransformed={} transformedMobId={} monsterStates={}", isTransformed, transformedMobId, monsterStates.keySet());
    }

    private MimicEntity.MimicAnimationState lastSentAnimationState = null;
    private boolean lastSentTransform = false;

    public boolean shouldSync(ResourceLocation identityId, boolean transform, MimicEntity.MimicAnimationState currentState, int currentTick) {
        if (transform != lastSentTransform) return true;
        if (currentState != lastSentAnimationState) return true;
        if (Math.abs(currentTick - getAnimationTick(identityId)) > 2) return true;
        return false;
    }

    public void markSynced(ResourceLocation identityId, boolean transform, MimicEntity.MimicAnimationState currentState) {
        lastSentTransform = transform;
        lastSentAnimationState = currentState;
    }

    @Override
    public void syncToClient(Player player) {
        syncToClient(player, "generic");
    }

    // 呼び出し元が理由を指定できるオーバーロード
    public void syncToClient(Player player, String reason) {
        if (!(player instanceof ServerPlayer)) return;

        ResourceLocation mobId = getTransformedMobId();
        if (mobId == null) return;

        MonsterState state = getMonsterState(mobId);

        S2CTransformSyncPacket packet = new S2CTransformSyncPacket(
                player.getUUID(),
                mobId,
                state.animationState != null ? state.animationState : MimicEntity.MimicAnimationState.IDLE.name(),
                state.animationTick,
                state.customFlags
        );

        MonsterMod.getLogger().debug(
                "[syncToClient] reason={} mobId={} state={} tick={} flags={}",
                reason, mobId, state.animationState, state.animationTick, state.customFlags
        );

        ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), packet);
    }

    @Override
    public void setAnimationStateAndSync(ResourceLocation mobId, MimicEntity.MimicAnimationState newState, ServerPlayer player) {
        if (mobId == null || newState == null || player == null) return;

        MonsterState monsterState = getMonsterState(mobId);
        MimicEntity.MimicAnimationState oldState = monsterState.getAnimationEnum();

        monsterState.animationState = newState.name();
        setMonsterState(mobId, monsterState);

        if (oldState != newState) {
            MonsterMod.getLogger().debug(
                    "[setAnimationStateAndSync] mobId={} CHANGED {} -> {} player={}",
                    mobId, oldState, newState, player.getName().getString()
            );
        } else {
            MonsterMod.getLogger().debug(
                    "[setAnimationStateAndSync] mobId={} SAME_STATE={} (sync anyway) player={}",
                    mobId, newState, player.getName().getString()
            );
        }

        // 同期時に「理由」を渡す
        syncToClient(player, "setAnimationStateAndSync");
    }

    public static class MonsterState {
        public String animationState = "IDLE";
        public int animationTick = 0;
        public final Map<String, Boolean> customFlags = new HashMap<>();

        public void setFlag(String key, boolean value) { customFlags.put(key, value); }
        public boolean getFlag(String key) { return customFlags.getOrDefault(key, false); }
        public MimicEntity.MimicAnimationState getAnimationEnum() {
            if (animationState == null) return MimicEntity.MimicAnimationState.IDLE;
            try { return MimicEntity.MimicAnimationState.valueOf(animationState); }
            catch (IllegalArgumentException e) { return MimicEntity.MimicAnimationState.IDLE; }
        }
    }
}
