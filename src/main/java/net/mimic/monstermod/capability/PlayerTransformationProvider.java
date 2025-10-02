package net.mimic.monstermod.capability;

import net.mimic.monstermod.entity.custom.MimicEntity;
import net.mimic.monstermod.networking.ModMessages;
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerTransformationProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final Capability<IPlayerTransformation> PLAYER_TRANSFORMATION =
            CapabilityManager.get(new CapabilityToken<IPlayerTransformation>() {});

    private IPlayerTransformation transformation = null;
    private final LazyOptional<IPlayerTransformation> optional = LazyOptional.of(this::createPlayerTransformation);

    private IPlayerTransformation createPlayerTransformation() {
        if (transformation == null) {
            transformation = new PlayerTransformation();
        }
        return transformation;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == PLAYER_TRANSFORMATION) return optional.cast();
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return createPlayerTransformation().serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        createPlayerTransformation().deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }

    @Nullable
    public MimicEntity.MimicAnimationState getAnimationState(ResourceLocation transformedMobId) {
        if (transformation == null) return null;
        PlayerTransformation.MonsterState state = transformation.getMonsterState(transformedMobId);
        return state != null ? state.getAnimationEnum() : null;
    }

    public void syncToClient(ServerPlayer player) {
        if (transformation != null) {
            transformation.syncToClient(player);
        }
    }

    public static void sendToAllPlayers(S2CTransformSyncPacket packet) {
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            ModMessages.sendToPlayer(packet, player);
        }
    }
}
