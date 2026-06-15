package net.narutomod.client.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Supplier;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.narutomod.NarutomodMod;

final class NarutoParticleRenderTypes {
    private static final ResourceLocation WHITE_TEXTURE = NarutomodMod.location("textures/white_square.png");
    private static final ResourceLocation PARTICLES_TEXTURE = NarutomodMod.location("textures/particles.png");
    private static final ResourceLocation SEAL_FORMULA_TEXTURE = NarutomodMod.location("textures/seal_black_512.png");
    private static final ResourceLocation WHIRLPOOL_TEXTURE = NarutomodMod.location("textures/swirl_white_2.png");

    static final ParticleRenderType EXPANDING_SPHERE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager textureManager) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentEmissiveShader);
            RenderSystem.setShaderTexture(0, WHITE_TEXTURE);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY);
        }

        @Override
        public void end(Tesselator tesselator) {
            tesselator.end();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }

        @Override
        public String toString() {
            return "narutomod:expanding_sphere";
        }
    };

    static final ParticleRenderType SEAL_FORMULA = textured(
            "seal_formula",
            SEAL_FORMULA_TEXTURE,
            DefaultVertexFormat.POSITION_TEX_COLOR,
            GameRenderer::getPositionTexColorShader
    );

    static final ParticleRenderType PARTICLES_ATLAS = textured(
            "particles_atlas",
            PARTICLES_TEXTURE,
            DefaultVertexFormat.PARTICLE,
            GameRenderer::getParticleShader
    );

    static final ParticleRenderType WHIRLPOOL = textured(
            "whirlpool",
            WHIRLPOOL_TEXTURE,
            DefaultVertexFormat.PARTICLE,
            GameRenderer::getParticleShader
    );

    private NarutoParticleRenderTypes() {
    }

    private static ParticleRenderType textured(
            String name,
            ResourceLocation texture,
            VertexFormat format,
            Supplier<ShaderInstance> shader
    ) {
        return new ParticleRenderType() {
            @Override
            public void begin(BufferBuilder builder, TextureManager textureManager) {
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShader(shader);
                RenderSystem.setShaderTexture(0, texture);
                builder.begin(VertexFormat.Mode.QUADS, format);
            }

            @Override
            public void end(Tesselator tesselator) {
                tesselator.end();
                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();
                RenderSystem.defaultBlendFunc();
            }

            @Override
            public String toString() {
                return "narutomod:" + name;
            }
        };
    }
}
