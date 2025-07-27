package net.mimic.monstermod.capability;

import net.mimic.monstermod.MonsterMod; // ★追加: Loggerのため
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.identity.IPlayerIdentity;
import net.mimic.monstermod.identity.PlayerIdentityRegistry;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;

/**
 * IPlayerTransformationの実装クラス。
 * プレイヤーの変身状態、変身先のMob ID、Mimic固有の状態などを保持します。
 */
public class PlayerTransformation implements IPlayerTransformation {
    private boolean isTransformed = false;
    private ResourceLocation transformedMobId = null;
    // ★変更: MimicAnimationStateを直接保持
    private MimicEntity.MimicAnimationState mimicState = MimicEntity.MimicAnimationState.IDLE;
    private boolean isBiting = false;

    @Override
    public boolean isTransformed() {
        return isTransformed;
    }

    @Override
    public void setTransformed(boolean transformed) {
        this.isTransformed = transformed;
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
    public void syncToClient(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModMessages.sendToPlayer(new S2CTransformSyncPacket(
                    this.isTransformed,
                    this.transformedMobId,
                    this.mimicState.name(), // Enumの名前をStringで送信
                    this.isBiting
            ), serverPlayer);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("isTransformed", isTransformed);
        if (transformedMobId != null) {
            nbt.putString("transformedMobId", transformedMobId.toString());
        }
        // ★変更: MimicAnimationStateをNBTに保存
        nbt.putString("mimicState", mimicState.name());
        nbt.putBoolean("isBiting", isBiting);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        isTransformed = nbt.getBoolean("isTransformed");
        if (nbt.contains("transformedMobId")) {
            transformedMobId = new ResourceLocation(nbt.getString("transformedMobId"));
        } else {
            transformedMobId = null;
        }
        // ★変更: MimicAnimationStateをNBTからロード
        if (nbt.contains("mimicState")) {
            try {
                mimicState = MimicEntity.MimicAnimationState.valueOf(nbt.getString("mimicState"));
            } catch (IllegalArgumentException e) {
                MonsterMod.getLogger().warn("無効なMimicAnimationStateをロードしました: {}. IDLEにリセット。", nbt.getString("mimicState"));
                mimicState = MimicEntity.MimicAnimationState.IDLE;
            }
        } else {
            mimicState = MimicEntity.MimicAnimationState.IDLE;
        }
        isBiting = nbt.getBoolean("isBiting");
    }

    /**
     * 現在の変身先のIPlayerIdentityインスタンスを取得します。
     * Convenience method.
     */
    public IPlayerIdentity getTransformedIdentity() {
        if (isTransformed && transformedMobId != null) {
            return PlayerIdentityRegistry.getIdentity(transformedMobId);
        }
        return null;
    }
}