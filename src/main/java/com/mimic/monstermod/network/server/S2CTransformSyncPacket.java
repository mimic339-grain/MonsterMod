package com.mimic.monstermod.network.server;

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.util.MonsterTransformUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class S2CTransformSyncPacket {
    private final UUID playerId;
    private final CompoundTag nbt;

    public S2CTransformSyncPacket(UUID playerId, CompoundTag nbt) {
        this.playerId = playerId;
        this.nbt = nbt;
    }

    public static void encode(S2CTransformSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeNbt(msg.nbt);
    }

    public static S2CTransformSyncPacket decode(FriendlyByteBuf buf) {
        return new S2CTransformSyncPacket(buf.readUUID(), buf.readNbt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Player player = mc.level.getPlayerByUUID(playerId);
            if (player == null) return;

            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                transformation.deserializeNBT(nbt);

                if (!transformation.isTransformed()) {
                    // 変身解除時の Player 属性と HP を復元
                    MonsterTransformUtil.resetPlayerAttributes(player, true);
                    float prevHP = nbt.contains("playerHealth") ? nbt.getFloat("playerHealth") : 20f;
                    player.setHealth(prevHP);
                } else {
                    // 変身中は IdentityHP を Player HP に反映
                    float identityHP = nbt.contains("identityHP")
                            ? nbt.getFloat("identityHP")
                            : (float) player.getAttributeValue(Attributes.MAX_HEALTH);
                    float maxHP = transformation.getEntity() != null
                            ? (float) transformation.getEntity().getAttributeValue(Attributes.MAX_HEALTH)
                            : identityHP;

                    player.setHealth(Math.min(identityHP, maxHP));

                    if (player.getAttribute(Attributes.MAX_HEALTH) != null) {
                        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHP);
                    }
                }

                // Dimension refresh
                if (transformation.consumeDimensionRefresh()) {
                    player.refreshDimensions();
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }

    public static CompoundTag createNBT(Player player) {
        CompoundTag tag = new CompoundTag();

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            tag.merge(transformation.serializeNBT());

            // IdentityHP は変身中なら必ず保存
            if (transformation.getIdentity() != null && transformation.getIdentity().hasCurrentHP()) {
                tag.putFloat("identityHP", transformation.getIdentity().getCurrentHP());
            }

            // Player HP は常に保存（変身解除時に復元用）
            float playerHP = player.getHealth();
            tag.putFloat("playerHealth", playerHP);
        });

        return tag;
    }
}
