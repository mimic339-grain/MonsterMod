package net.mimic.monstermod.client;

import net.mimic.monstermod.entity.client.ClientMimicEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (UUID uuid : ClientMimicEntity.getAllUUIDs()) {
            ClientMimicEntity entity = ClientMimicEntity.getOrCreate(uuid);
            entity.tick(); // GeckoLib の tick もここで進む
            System.out.println("[DEBUG] Ticking ClientMimicEntity for UUID: " + uuid);
        }
    }
}
