package net.narutomod.particle;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

public final class NarutoParticleType extends ParticleType<NarutoParticleOptions> {
    private static final ParticleOptions.Deserializer<NarutoParticleOptions> DESERIALIZER = new ParticleOptions.Deserializer<>() {
        @Override
        public NarutoParticleOptions fromCommand(ParticleType<NarutoParticleOptions> type, StringReader reader) throws CommandSyntaxException {
            List<Integer> args = new ArrayList<>();
            NarutoParticleType narutoType = (NarutoParticleType)type;
            while (reader.canRead() && args.size() < narutoType.kind.argumentCount()) {
                reader.expect(' ');
                args.add(reader.readInt());
            }
            return new NarutoParticleOptions(narutoType, args);
        }

        @Override
        public NarutoParticleOptions fromNetwork(ParticleType<NarutoParticleOptions> type, FriendlyByteBuf buffer) {
            NarutoParticleType narutoType = (NarutoParticleType)type;
            return new NarutoParticleOptions(narutoType, buffer.readVarIntArray(narutoType.kind.argumentCount()));
        }
    };

    private final NarutoParticleKind kind;
    private final Codec<NarutoParticleOptions> codec;

    public NarutoParticleType(NarutoParticleKind kind) {
        super(true, DESERIALIZER);
        this.kind = kind;
        this.codec = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.listOf()
                        .optionalFieldOf("args", List.of())
                        .forGetter(NarutoParticleOptions::argsList)
        ).apply(instance, args -> new NarutoParticleOptions(this, args)));
    }

    public NarutoParticleKind kind() {
        return this.kind;
    }

    public NarutoParticleOptions options(int... args) {
        return new NarutoParticleOptions(this, args);
    }

    @Override
    public Codec<NarutoParticleOptions> codec() {
        return this.codec;
    }
}
