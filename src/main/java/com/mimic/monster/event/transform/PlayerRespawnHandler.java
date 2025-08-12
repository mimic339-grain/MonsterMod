package com.mimic.monster.event.transform;

import com.mimic.monster.capability.CapabilityRegistry;
import com.mimic.monster.network.ModNetwork;
import com.mimic.monster.network.client.S2CUpdateTransformPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = "monstermod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerRespawnHandler {

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(cap -> {
            if (cap.isTransformed() && cap.getTransformedType() != null)  {
                ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(cap.getTransformedType());
                if (typeId != null) {
                    // リスポーン直後のプレイヤーに変身状態を同期
                    ModNetwork.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new S2CUpdateTransformPacket(player.getId(), typeId)
                    );
                }
            }
        });
    }
}
