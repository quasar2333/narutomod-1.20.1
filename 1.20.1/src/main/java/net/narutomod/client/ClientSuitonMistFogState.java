package net.narutomod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.narutomod.network.SuitonMistFogMessage;

public final class ClientSuitonMistFogState {
    private static final float MIN_ACTIVE_DENSITY = 0.001F;

    private static float density;
    private static long expiresAtTick;
    private static long lastUpdateTick = Long.MIN_VALUE;

    private ClientSuitonMistFogState() {
    }

    public static void apply(SuitonMistFogMessage message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || message.durationTicks() <= 0 || message.density() <= MIN_ACTIVE_DENSITY) {
            clear();
            return;
        }

        long now = level.getGameTime();
        float clampedDensity = Mth.clamp(message.density(), MIN_ACTIVE_DENSITY, 1.0F);
        if (now == lastUpdateTick && now < expiresAtTick) {
            density = Math.max(density, clampedDensity);
        } else {
            density = clampedDensity;
        }
        lastUpdateTick = now;
        expiresAtTick = now + message.durationTicks();
    }

    public static float density() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.getGameTime() >= expiresAtTick) {
            clear();
            return 0.0F;
        }
        return density;
    }

    private static void clear() {
        density = 0.0F;
        expiresAtTick = 0L;
        lastUpdateTick = Long.MIN_VALUE;
    }
}
