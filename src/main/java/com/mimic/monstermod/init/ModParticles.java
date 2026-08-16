package com.mimic.monstermod.init;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, "monstermod");

    // 【停止中】パーティクル版の竜巻。VortexEntity に置き換えたため登録を外している。
    // 復活させる場合は TornadoParticle / ModClientEvents のコメントアウトも戻すこと。
    /*
    public static final RegistryObject<ParticleType<TornadoParticleOptions>> TORNADO =
            PARTICLE_TYPES.register("tornado", () -> new ParticleType<TornadoParticleOptions>(false, TornadoParticleOptions.DESERIALIZER) {
                @Override
                public com.mojang.serialization.Codec<TornadoParticleOptions> codec() {
                    return TornadoParticleOptions.CODEC;
                }
            });
    */
}