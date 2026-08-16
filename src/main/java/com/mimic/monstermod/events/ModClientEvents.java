package com.mimic.monstermod.events;

import com.mimic.monstermod.MonsterMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** パーティクルの見た目(Provider)をレジストリに紐付ける場所 */
@Mod.EventBusSubscriber(modid = MonsterMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerParticleFactories(RegisterParticleProvidersEvent event) {
        // 【停止中】パーティクル版の竜巻。VortexEntity に置き換えたため登録を外している。
        // 復活させる場合は TornadoParticle / ModParticles のコメントアウトも戻すこと。
        // event.registerSpriteSet(ModParticles.TORNADO.get(), TornadoParticle.Provider::new);
    }
}
