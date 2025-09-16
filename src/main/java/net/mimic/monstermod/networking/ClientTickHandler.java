package net.mimic.monstermod.networking;

import net.mimic.monstermod.capability.PlayerTransformation;
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

        PlayerTransformation transformation = PlayerTransformHandler.getOrCreateTransformation(player);
        if (transformation == null || !transformation.isTransformed()) return;

        Entity entity = transformation.getClientTransformedEntity();
        if (!(entity instanceof MimicEntity mimic)) return;

        // サーバから送られた BODY_ROT / HEAD_ROT を lerp
        mimic.yBodyRotO = mimic.yBodyRot;
        mimic.yHeadRotO = mimic.yHeadRot;

        float lerp = 0.5f;
        mimic.yBodyRot += (mimic.getBodyRot() - mimic.yBodyRot) * lerp;
        mimic.yHeadRot += (mimic.getHeadRot() - mimic.yHeadRot) * lerp;

        mimic.updateAnimationStateClient();
    }
}
