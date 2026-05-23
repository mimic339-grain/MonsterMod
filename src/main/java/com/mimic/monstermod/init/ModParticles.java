package com.mimic.monstermod.init;

import com.mimic.monstermod.particle.Tornado.TornadoParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, "monstermod");

    public static final RegistryObject<ParticleType<TornadoParticleOptions>> TORNADO =
            PARTICLE_TYPES.register("tornado", () -> new ParticleType<TornadoParticleOptions>(false, TornadoParticleOptions.DESERIALIZER) {
                @Override
                public com.mojang.serialization.Codec<TornadoParticleOptions> codec() {
                    return TornadoParticleOptions.CODEC;
                }
            });
}