package net.narutomod.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class NarutoRenderTypes extends RenderType {
    private static final int EFFECT_BUFFER_SIZE = 256;

    private static final TransparencyStateShard SRC_ALPHA_ADDITIVE_TRANSPARENCY = new TransparencyStateShard(
        "naruto_src_alpha_additive",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        },
        () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    );

    private static final TransparencyStateShard ONE_ONE_ADDITIVE_TRANSPARENCY = new TransparencyStateShard(
        "naruto_one_one_additive",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        },
        () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    );

    private static final Function<ResourceLocation, RenderType> ENERGY_ADDITIVE = Util.memoize(texture -> create(
        "naruto_energy_additive",
        DefaultVertexFormat.NEW_ENTITY,
        VertexFormat.Mode.QUADS,
        EFFECT_BUFFER_SIZE,
        false,
        true,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
            .setTransparencyState(SRC_ALPHA_ADDITIVE_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_WRITE)
            .setOverlayState(OVERLAY)
            .createCompositeState(false)
    ));

    private static final Function<ResourceLocation, RenderType> ENERGY_FULL_ADDITIVE = Util.memoize(texture -> create(
        "naruto_energy_full_additive",
        DefaultVertexFormat.NEW_ENTITY,
        VertexFormat.Mode.QUADS,
        EFFECT_BUFFER_SIZE,
        false,
        true,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
            .setTransparencyState(ONE_ONE_ADDITIVE_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_WRITE)
            .setOverlayState(OVERLAY)
            .createCompositeState(false)
    ));

    private static final Function<ResourceLocation, RenderType> TRANSLUCENT_EMISSIVE_NO_CULL = Util.memoize(texture -> create(
        "naruto_translucent_emissive_no_cull",
        DefaultVertexFormat.NEW_ENTITY,
        VertexFormat.Mode.QUADS,
        EFFECT_BUFFER_SIZE,
        false,
        true,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_WRITE)
            .setOverlayState(OVERLAY)
            .createCompositeState(false)
    ));

    private record ScrollKey(ResourceLocation texture, float uPerTick, float vPerTick, boolean additive) {
    }

    private record EntityScrollKey(ResourceLocation texture, float uPerTick, float vPerTick) {
    }

    private static final Function<ScrollKey, RenderType> SCROLLING = Util.memoize(key -> create(
        key.additive() ? "naruto_scrolling_additive" : "naruto_scrolling_translucent",
        DefaultVertexFormat.NEW_ENTITY,
        VertexFormat.Mode.QUADS,
        EFFECT_BUFFER_SIZE,
        false,
        true,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(key.texture(), false, false))
            .setTransparencyState(key.additive() ? SRC_ALPHA_ADDITIVE_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_WRITE)
            .setOverlayState(OVERLAY)
            .setTexturingState(scrollingTexturing(key.uPerTick(), key.vPerTick()))
            .createCompositeState(false)
    ));

    private static final Function<EntityScrollKey, RenderType> SCROLLING_ENTITY_TRANSLUCENT = Util.memoize(key -> create(
        "naruto_scrolling_entity_translucent",
        DefaultVertexFormat.NEW_ENTITY,
        VertexFormat.Mode.QUADS,
        EFFECT_BUFFER_SIZE,
        false,
        true,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(key.texture(), false, false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_WRITE)
            .setOverlayState(OVERLAY)
            .setTexturingState(scrollingTexturing(key.uPerTick(), key.vPerTick()))
            .createCompositeState(false)
    ));

    private record ColorModeKey(VertexFormat.Mode mode) {
    }

    private static final Function<ColorModeKey, RenderType> COLOR_ADDITIVE = Util.memoize(key -> create(
        "naruto_color_additive_" + key.mode().name().toLowerCase(),
        DefaultVertexFormat.POSITION_COLOR,
        key.mode(),
        EFFECT_BUFFER_SIZE,
        false,
        true,
        CompositeState.builder()
            .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
            .setTransparencyState(LIGHTNING_TRANSPARENCY)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(false)
    ));

    private NarutoRenderTypes(
        String name,
        VertexFormat format,
        VertexFormat.Mode mode,
        int bufferSize,
        boolean affectsCrumbling,
        boolean sortOnUpload,
        Runnable setupState,
        Runnable clearState
    ) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }

    public static RenderType energyAdditive(ResourceLocation texture) {
        return ENERGY_ADDITIVE.apply(texture);
    }

    public static RenderType energyFullAdditive(ResourceLocation texture) {
        return ENERGY_FULL_ADDITIVE.apply(texture);
    }

    public static RenderType translucentEmissiveNoCull(ResourceLocation texture) {
        return TRANSLUCENT_EMISSIVE_NO_CULL.apply(texture);
    }

    public static RenderType scrollingTranslucent(ResourceLocation texture, float uPerTick, float vPerTick) {
        return SCROLLING.apply(new ScrollKey(texture, uPerTick, vPerTick, false));
    }

    public static RenderType scrollingAdditive(ResourceLocation texture, float uPerTick, float vPerTick) {
        return SCROLLING.apply(new ScrollKey(texture, uPerTick, vPerTick, true));
    }

    public static RenderType scrollingEntityTranslucent(ResourceLocation texture, float uPerTick, float vPerTick) {
        return SCROLLING_ENTITY_TRANSLUCENT.apply(new EntityScrollKey(texture, uPerTick, vPerTick));
    }

    public static RenderType colorAdditive(VertexFormat.Mode mode) {
        return COLOR_ADDITIVE.apply(new ColorModeKey(mode));
    }

    public static RenderType colorAdditiveQuads() {
        return colorAdditive(VertexFormat.Mode.QUADS);
    }

    public static RenderType colorAdditiveTriangles() {
        return colorAdditive(VertexFormat.Mode.TRIANGLES);
    }

    public static RenderType colorAdditiveTriangleStrip() {
        return colorAdditive(VertexFormat.Mode.TRIANGLE_STRIP);
    }

    public static RenderType colorAdditiveTriangleFan() {
        return colorAdditive(VertexFormat.Mode.TRIANGLE_FAN);
    }

    private static RenderStateShard.TexturingStateShard scrollingTexturing(float uPerTick, float vPerTick) {
        return new RenderStateShard.TexturingStateShard(
            "naruto_scroll",
            () -> {
                float ticks = (Util.getMillis() % 3_600_000L) / 50.0F;
                RenderSystem.setTextureMatrix(new Matrix4f().translation(
                    wrapUnit(ticks * uPerTick),
                    wrapUnit(ticks * vPerTick),
                    0.0F
                ));
            },
            RenderSystem::resetTextureMatrix
        );
    }

    private static float wrapUnit(float value) {
        float wrapped = value % 1.0F;
        return wrapped < 0.0F ? wrapped + 1.0F : wrapped;
    }
}
