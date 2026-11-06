package com.astralarcana.client.particle;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** Particula brilhante com blend aditivo (brilha mais quanto mais se acumula). */
public class GlowParticle extends TextureSheetParticle {
    public static SpriteSet SPRITES;

    public static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override
        public void begin(BufferBuilder builder, TextureManager tm) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }

        @Override
        public void end(Tesselator tess) {
            tess.end();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
        }

        @Override
        public String toString() {
            return "ASTRAL_ADDITIVE";
        }
    };

    protected final float baseSize;

    public GlowParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz,
                        float r, float g, float b, float size, int life, float gravity, float drag) {
        super(level, x, y, z);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
        this.baseSize = size;
        this.quadSize = size;
        this.lifetime = Math.max(2, life);
        this.gravity = gravity;
        this.friction = drag;
        this.hasPhysics = false;
        if (SPRITES != null) this.pickSprite(SPRITES);
    }

    protected void fade() {
        float t = (float) this.age / (float) this.lifetime;
        this.alpha = Math.min(1f, this.age * 0.5f + 0.25f) * (1f - t * t);
        this.quadSize = this.baseSize * (1f - 0.5f * t);
    }

    @Override
    public void tick() {
        super.tick();
        fade();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ADDITIVE;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        public Provider(SpriteSet sprites) {
            SPRITES = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new GlowParticle(level, x, y, z, vx, vy, vz, 1f, 1f, 1f, 0.3f, 20, 0f, 0.95f);
        }
    }
}
