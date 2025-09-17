package net.mimic.monstermod.networking;

import net.mimic.monstermod.capability.PlayerTransformationProvider;
import net.mimic.monstermod.entity.custom.MimicEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        player.getCapability(PlayerTransformationProvider.PLAYER_TRANSFORMATION).ifPresent(transformation -> {
            if (!transformation.isTransformed()) return;

            Entity entity = transformation.getClientTransformedEntity();
            if (!(entity instanceof MimicEntity mimic)) return;

            // サーバから受け取った同期用の最新値を補間して反映
            float lerp = 0.3f; // 補間係数（値を小さくするとより滑らか）
            mimic.yBodyRot += (mimic.getBodyRot() - mimic.yBodyRot) * lerp;
            mimic.yHeadRot += (mimic.getHeadRot() - mimic.yHeadRot) * lerp;
        });
    }
}
