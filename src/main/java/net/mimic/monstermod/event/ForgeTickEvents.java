package net.mimic.monstermod.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeTickEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof Player) {
            event.player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()
                        && transformation.getTransformedMobId() != null
                        && transformation.getTransformedMobId().equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"))) {

                    if (!event.player.level().isClientSide) {
                        ServerPlayer serverPlayer = (ServerPlayer) event.player;
                        if (!serverPlayer.getAbilities().mayfly) {
                            serverPlayer.getAbilities().mayfly = true;
                            serverPlayer.onUpdateAbilities();
                        }
                    }

                } else {
                    if (!event.player.level().isClientSide && event.player.getAbilities().mayfly && !event.player.isCreative()) {
                        ServerPlayer serverPlayer = (ServerPlayer) event.player;
                        serverPlayer.getAbilities().mayfly = false;
                        serverPlayer.onUpdateAbilities();
                    }
                }
            });
        }
    }
}