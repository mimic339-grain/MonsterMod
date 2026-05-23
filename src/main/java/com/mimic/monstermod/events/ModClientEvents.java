package com.mimic.monstermod.events; // 適切なパッケージ名にしてください

import com.mimic.monstermod.MonsterMod;
import com.mimic.monstermod.init.ModParticles;
import com.mimic.monstermod.particle.Tornado.TornadoParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        // ここで「レジストリにあるTORNADO」と「TornadoParticleのProvider」を紐付けます
        // これを書くことで TornadoParticle がシステムに組み込まれ、警告も消えます
        event.registerSpriteSet(ModParticles.TORNADO.get(), TornadoParticle.Provider::new);
    }
}