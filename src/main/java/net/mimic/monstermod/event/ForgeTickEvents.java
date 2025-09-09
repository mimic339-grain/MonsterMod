package net.mimic.monstermod.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.identity.IPlayerIdentity; // IPlayerIdentityをインポート


@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeTickEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && event.player instanceof Player) {
            //変身中かどうかをチェック
            event.player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                //変身中ならIdentity情報を取得
                if (transformation.isTransformed()) {
                    IPlayerIdentity currentIdentity = transformation.getTransformedIdentity();

                    //変身中のIdentity固有の能力を適用
                    if (!event.player.level().isClientSide) {
                        currentIdentity.applySpecificAbilities(event.player);
                    }

                } else {
                    // 変身していない場合に飛行能力が残ってしまうバグを防ぐために、強制的に飛行権限を外す
                    if (!event.player.level().isClientSide && event.player instanceof ServerPlayer serverPlayer) {
                        if (serverPlayer.getAbilities().mayfly && !serverPlayer.isCreative()) {
                            serverPlayer.getAbilities().mayfly = false;
                            serverPlayer.onUpdateAbilities();
                        }
                    }
                }
            });
        }
    }
}