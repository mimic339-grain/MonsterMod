package com.mimic.monstermod.particle.Tornado;

import com.mimic.monstermod.init.ModParticles;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

public class TornadoParticleOptions implements ParticleOptions {
    private final float size;

    // Codec の実装
    public static final Codec<TornadoParticleOptions> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(
                    Codec.FLOAT.fieldOf("size").forGetter((d) -> d.size)
            ).apply(instance, TornadoParticleOptions::new)
    );

    // Deserializer の実装
    public static final ParticleOptions.Deserializer<TornadoParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<TornadoParticleOptions>() {
        @Override
        public TornadoParticleOptions fromCommand(ParticleType<TornadoParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            float size = reader.readFloat();
            return new TornadoParticleOptions(size);
        }

        @Override
        public TornadoParticleOptions fromNetwork(ParticleType<TornadoParticleOptions> type, FriendlyByteBuf buffer) {
            return new TornadoParticleOptions(buffer.readFloat());
        }
    };

    public TornadoParticleOptions(float size) {
        this.size = size;
    }

    public float getSize() { return size; }

    @Override
    public ParticleType<TornadoParticleOptions> getType() {
        return ModParticles.TORNADO.get();
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.size);
    }

    @Override
    public String writeToString() {
        return String.format("tornado %.2f", this.size);
    }
}