package com.mimic.monstermod.capability;

import com.mimic.monstermod.network.ModMessages;
import com.mimic.monstermod.network.server.S2CTransformSyncPacket;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerTransformationProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static final Capability<PlayerTransformation> PLAYER_TRANSFORMATION =
            CapabilityManager.get(new CapabilityToken<PlayerTransformation>() {});

    private PlayerTransformation transformation = null;
    private final LazyOptional<PlayerTransformation> optional = LazyOptional.of(this::createPlayerTransformation);

    private PlayerTransformation createPlayerTransformation() {
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

    // ====== 同期処理 ======
    public void syncToClient(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // NBT丸ごと同期版に合わせる
        CompoundTag nbt = createPlayerTransformation().serializeNBT();
        S2CTransformSyncPacket packet = new S2CTransformSyncPacket(player.getUUID(), nbt);

        ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), packet);
    }

    // 全プレイヤーに同期パケットを送る
    public static void sendToAllPlayers(S2CTransformSyncPacket packet) {
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            ModMessages.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    // Getter
    public PlayerTransformation get() {
        return createPlayerTransformation();
    }
}
