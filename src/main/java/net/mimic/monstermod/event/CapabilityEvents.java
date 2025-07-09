package net.mimic.monstermod.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer; // ServerPlayerをインポート
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent; // PlayerLoggedInEventをインポート
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent; // PlayerRespawnEventをインポート
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.networking.ModMessages; // ModMessagesをインポート
import net.mimic.monstermod.networking.packet.S2CTransformSyncPacket; // S2CTransformSyncPacketをインポート

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CapabilityEvents {

    @SubscribeEvent
    public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            // 既にCapabilityがアタッチされていないか確認
            if (!event.getObject().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).isPresent()) {
                event.addCapability(new ResourceLocation(MonsterMod.MOD_ID, "transformation"), new PlayerTransformationProvider());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // 死亡またはディメンション移動でクローンされた場合
        if (event.isWasDeath() || event.getEntity().level().isClientSide() && !event.getOriginal().level().isClientSide()) {
            event.getOriginal().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(oldCap -> {
                event.getEntity().getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(newCap -> {
                    newCap.setTransformed(oldCap.isTransformed());
                    newCap.setTransformedMobId(oldCap.getTransformedMobId());
                });
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        // サーバーサイドでのみ実行
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                // クライアントに変身状態を同期
                ModMessages.sendToPlayer(new S2CTransformSyncPacket(transformation.isTransformed(), transformation.getTransformedMobId()), player);
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        // サーバーサイドでのみ実行
        if (event.getEntity() instanceof ServerPlayer player) {
            // クローンイベントでデータは引き継がれているはずだが、念のため再同期
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                ModMessages.sendToPlayer(new S2CTransformSyncPacket(transformation.isTransformed(), transformation.getTransformedMobId()), player);
            });
        }
    }
}