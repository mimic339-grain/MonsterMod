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
public class PlayerJoinHandler {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        //プレイヤーがサーバープレイヤーか確認
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        //変身情報を取得
        player.getCapability(CapabilityRegistry.TRANSFORM).ifPresent(cap -> {
            //変身中かチェック
            if (cap.isTransformed() && cap.getTransformedType() != null) {
                ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(cap.getTransformedType());
                // 参加した本人に変身状態を同期
                if (typeId != null) {
                    ModNetwork.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new S2CUpdateTransformPacket(player.getId(), typeId)
                    );
                }
            }
        });
    }
}
