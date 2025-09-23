package net.mimic.monstermod.capability;

import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.identity.PlayerIdentityRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;

import javax.annotation.Nullable;
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
    private String lastSyncedAnimation = "";

    @Override public boolean hasSavedOriginalStats() { return originalStatsSaved; }
    @Override public void setOriginalHealth(double hp) { this.originalHealth = hp; originalStatsSaved = true; }
    @Override public void setOriginalMaxHealth(double maxHp) { this.originalMaxHealth = maxHp; }
    @Override public void setOriginalAttackDamage(double dmg) { this.originalAttack = dmg; }
    @Override public void setOriginalArmor(double armor) { this.originalArmor = armor; }
    @Override public void setOriginalMoveSpeed(double speed) { this.originalSpeed = speed; }
    @Override public double getOriginalHealth() { return originalHealth; }
    @Override public double getOriginalMaxHealth() { return originalMaxHealth; }
    @Override public double getOriginalAttackDamage() { return originalAttack; }
    @Override public double getOriginalArmor() { return originalArmor; }
    @Override public double getOriginalMoveSpeed() { return originalSpeed; }
    @Override public void clearOriginalStats() { originalStatsSaved = false; }
    @Override public boolean isNoKnockback() { return noKnockback; }
    @Override public void setNoKnockback(boolean value) { noKnockback = value; }

    @Override @Nullable public Entity getTransformedEntity() { return transformedEntity; }
    @Override public void setTransformedEntity(@Nullable Entity entity) { this.transformedEntity = entity; }
    @Override public boolean isTransformed() { return isTransformed; }
    @Override public void setTransformed(boolean transformed) { this.isTransformed = transformed; }
    @Override public ResourceLocation getTransformedMobId() { return transformedMobId; }
    @Override public void setTransformedMobId(ResourceLocation mobId) { this.transformedMobId = mobId; }

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

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                ResourceLocation mobId = transformation.getTransformedMobId();
                if (mobId == null) return;

                MonsterState state = transformation.getMonsterState(mobId);
                if (state == null) return;

                MimicEntity.MimicAnimationState animState = state.getAnimationEnum();
                if (animState == MimicEntity.MimicAnimationState.IDLE ||
                        animState == MimicEntity.MimicAnimationState.OPEN_IDLE ||
                        animState == MimicEntity.MimicAnimationState.OPENJUMP ||
                        animState == MimicEntity.MimicAnimationState.CLOSEJUMP) {
                    transformation.syncToClient(player);
                }
            });
        }
    }

    @Override
    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        ResourceLocation mobId = transformedMobId;
        if (mobId == null) return;

        MonsterState state = getMonsterState(mobId);
        if (state == null) return;

        String currentAnim = state.animationState;
        int currentTick = state.animationTick;

        // 初回変身時も送信するため lastSyncedAnimation が null または空なら無視
        if (lastSyncedAnimation != null && lastSyncedAnimation.equals(currentAnim)) return;

        ModMessages.sendToPlayer(new S2CTransformSyncPacket(
                true,
                mobId,
                currentAnim,
                currentTick
        ), serverPlayer);

        lastSyncedAnimation = currentAnim;
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
            stateTag.putInt("animationTick", entry.getValue().animationTick); // ← 追加
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
                state.animationTick = stateTag.getInt("animationTick"); // ← 追加
                monsterStates.put(mobId, state);
            }
        }
    }

    @Override @Nullable
    public IPlayerIdentity getTransformedIdentity() {
        return (isTransformed && transformedMobId != null) ? PlayerIdentityRegistry.getIdentity(transformedMobId) : null;
    }

    public static class MonsterState {
        public String animationState = "IDLE";
        public int animationTick = 0;

        // フラグ管理用
        private final Map<String, Boolean> flags = new HashMap<>();

        public MimicEntity.MimicAnimationState getAnimationEnum() {
            if (animationState == null) return MimicEntity.MimicAnimationState.IDLE;
            try { return MimicEntity.MimicAnimationState.valueOf(animationState); }
            catch (IllegalArgumentException e) { return MimicEntity.MimicAnimationState.IDLE; }
        }

        // flag 用メソッド
        public boolean getFlag(String key) {
            return flags.getOrDefault(key, false);
        }

        public void setFlag(String key, boolean value) {
            flags.put(key, value);
        }
    }

}
