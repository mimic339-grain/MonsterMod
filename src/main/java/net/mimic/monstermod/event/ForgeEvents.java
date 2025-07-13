package net.mimic.monstermod.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.mimic.monstermod.MonsterMod;
import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.minecraft.network.chat.Component;
import net.mimic.monstermod.entity.custom.MimicEntity; // MimicEntityをインポート

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {

    @SubscribeEvent
    public static void onPlayerAttackEntity(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
                boolean isMimicForm = transformation.isTransformed()
                        && transformation.getTransformedMobId() != null
                        && transformation.getTransformedMobId().equals(new ResourceLocation(MonsterMod.MOD_ID, "mimic"));

                if (isMimicForm) {
                    MimicEntity.MimicAnimationState currentState = transformation.getMimicState();

                    // MimicがOPEN状態またはOPENING状態の場合のみ攻撃を許可
                    if (currentState == MimicEntity.MimicAnimationState.OPEN || currentState == MimicEntity.MimicAnimationState.OPENING) {
                        // 攻撃を許可するので何もしない
                    } else {
                        // MimicがOPEN状態でない場合、攻撃をキャンセル
                        event.setCanceled(true);
                        player.sendSystemMessage(Component.literal("Mimic must be open to bite!"));
                    }
                }
            });
        }
    }
}