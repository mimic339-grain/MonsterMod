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
            event.player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                if (transformation.isTransformed()) {
                    IPlayerIdentity currentIdentity = transformation.getTransformedIdentity();

                    // 変身中のIdentity固有の能力を適用
                    // これはサーバーサイドでのみ実行される
                    if (!event.player.level().isClientSide) {
                        currentIdentity.applySpecificAbilities(event.player);
                    }

                } else {
                    // 変身していない場合
                    if (!event.player.level().isClientSide && event.player instanceof ServerPlayer serverPlayer) {
                        // プレイヤーがクリエイティブモードでなく、現在飛行能力がある場合、それを解除する
                        // IdentityRegistry.EMPTYのapplySpecificAbilitiesでは何も起こらないため、
                        // 明示的に飛行能力を解除する必要があるのはここ
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