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

        // サーバ側のEntity IDを取得
        int entityId = transformation.getTransformedEntityId();
        if (entityId == -1) return;

        // IDでEntity取得
        Entity entity = player.level().getEntity(entityId);
        if (!(entity instanceof MimicEntity mimic)) return;

        // 前フレーム回転を保持
        mimic.yBodyRotO = mimic.yBodyRot;
        mimic.yHeadRotO = mimic.yHeadRot;

        // 回転補間
        float lerp = 0.5f;
        mimic.yBodyRot += (mimic.getBodyRot() - mimic.yBodyRot) * lerp;
        mimic.yHeadRot += (mimic.getHeadRot() - mimic.yHeadRot) * lerp;

        // アニメーション更新
        mimic.updateAnimationStateClient();
    }
}
