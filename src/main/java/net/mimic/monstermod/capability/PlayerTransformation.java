package net.mimic.monstermod.capability;

import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.identity.PlayerIdentityRegistry;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 複数Monster対応・アニメーション統合版PlayerTransformation
 */
public class PlayerTransformation implements IPlayerTransformation {

    private boolean originalStatsSaved = false;
    private double originalHealth, originalMaxHealth, originalAttack, originalArmor, originalSpeed;
    private boolean noKnockback = false;
    private Entity transformedEntity;
    private boolean isTransformed = false;
    private ResourceLocation transformedMobId = null;

    // Monsterごとの状態をMapで管理
    private final Map<ResourceLocation, MonsterState> monsterStates = new HashMap<>();

    // --- 元ステータス管理 ---
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

    /** 指定Monsterの状態を取得 */
    public MonsterState getMonsterState(ResourceLocation mobId) {
        return mobId != null ? monsterStates.getOrDefault(mobId, new MonsterState()) : new MonsterState();
    }

    /** 指定Monsterの状態を更新 */
    public void setMonsterState(ResourceLocation mobId, MonsterState state) {
        if (mobId != null) monsterStates.put(mobId, state);
    }

    /** ★ ResourceLocation 版で統一 */
    @Override
    public MimicEntity.MimicAnimationState getAnimationState(ResourceLocation mobId) {
        return getMonsterState(mobId).getAnimationEnum();
    }

    private MimicEntity.MimicAnimationState lastSyncedState = MimicEntity.MimicAnimationState.IDLE;

    @Override
    public void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ResourceLocation mobId = transformedMobId;
            MonsterState state = getMonsterState(mobId);

            // 変化があったときだけ同期
            if (state.getAnimationEnum() != lastSyncedState) {
                ModMessages.sendToPlayer(new S2CTransformSyncPacket(
                        isTransformed,
                        mobId,
                        state.animationState
                ), serverPlayer);
                lastSyncedState = state.getAnimationEnum();
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

    @Override @Nullable
    public IPlayerIdentity getTransformedIdentity() {
        return (isTransformed && transformedMobId != null) ? PlayerIdentityRegistry.getIdentity(transformedMobId) : null;
    }

    /** Monster共通の状態を保持するクラス */
    public static class MonsterState {
        public String animationState = "IDLE";
        private final Map<String, Boolean> customFlags = new HashMap<>();
        public void setFlag(String key, boolean value) { customFlags.put(key, value); }
        public boolean getFlag(String key) { return customFlags.getOrDefault(key, false); }
        /** animationState を MimicAnimationState に変換して返す */
        public MimicEntity.MimicAnimationState getAnimationEnum() {
            if (animationState == null) return MimicEntity.MimicAnimationState.IDLE;
            try { return MimicEntity.MimicAnimationState.valueOf(animationState); }
            catch (IllegalArgumentException e) { return MimicEntity.MimicAnimationState.IDLE; }
        }
    }

        private MimicEntity.MimicAnimationState baseState = MimicEntity.MimicAnimationState.IDLE;

        @Override
        public MimicEntity.MimicAnimationState getBaseState() {
            return baseState;
        }

        @Override
        public void setBaseState(MimicEntity.MimicAnimationState state) {
            this.baseState = state;
        }

}
