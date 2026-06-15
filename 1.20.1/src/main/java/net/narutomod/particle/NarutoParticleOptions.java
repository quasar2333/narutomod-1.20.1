package net.narutomod.particle;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.narutomod.NarutomodMod;

public final class NarutoParticleOptions implements ParticleOptions {
    private final NarutoParticleType type;
    private final int[] args;

    public NarutoParticleOptions(NarutoParticleType type, List<Integer> args) {
        this(type, args.stream().mapToInt(Integer::intValue).toArray());
    }

    public NarutoParticleOptions(NarutoParticleType type, int... args) {
        this.type = type;
        this.args = Arrays.copyOf(args, Math.min(args.length, type.kind().argumentCount()));
    }

    @Override
    public ParticleType<?> getType() {
        return this.type;
    }

    public NarutoParticleType narutoType() {
        return this.type;
    }

    public NarutoParticleKind kind() {
        return this.type.kind();
    }

    public int[] args() {
        return Arrays.copyOf(this.args, this.args.length);
    }

    public List<Integer> argsList() {
        return Arrays.stream(this.args).boxed().toList();
    }

    public int arg(int index, int defaultValue) {
        return index >= 0 && index < this.args.length ? this.args[index] : defaultValue;
    }

    @Override
    public void writeToNetwork(FriendlyByteBuf buffer) {
        buffer.writeVarIntArray(this.args);
    }

    @Override
    public String writeToString() {
        StringBuilder builder = new StringBuilder(NarutomodMod.MODID)
                .append(':')
                .append(this.type.kind().registryName());
        for (int arg : this.args) {
            builder.append(' ').append(arg);
        }
        return builder.toString();
    }
}
