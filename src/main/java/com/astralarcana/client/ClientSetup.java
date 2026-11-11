package com.astralarcana.client;

import com.astralarcana.AstralArcana;
import com.astralarcana.Reg;
import com.astralarcana.client.particle.GlowParticle;
import com.astralarcana.engine.shader.FramebufferPool;
import com.astralarcana.engine.shader.ShaderEngine;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AstralArcana.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(Reg.METEOR.get(), MeteorRenderer::new);
        e.registerEntityRenderer(Reg.BLACK_HOLE.get(), BlackHoleRenderer::new);
        e.registerEntityRenderer(Reg.SERPENT.get(), CosmicSerpentRenderer::new);
        e.registerEntityRenderer(Reg.ZONE.get(), ZoneRenderer::new);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(Reg.GLOW.get(), GlowParticle.Provider::new);
    }

    /** Todos os programas passam pelo ShaderEngine: erro de compilacao nao derruba o jogo, apenas desativa o efeito. */
    @SubscribeEvent
    public static void shaders(RegisterShadersEvent e) {
        for (String name : new String[]{"cinematic", "distort", "bloom_extract", "blur", "bloom_composite"}) {
            ShaderEngine.register(e, name, DefaultVertexFormat.POSITION_TEX);
        }
    }

    /** No reload de recursos (F3+T, /astral reload) libera os framebuffers para serem recriados do tamanho certo. */
    @SubscribeEvent
    public static void reloadListeners(RegisterClientReloadListenersEvent e) {
        e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> FramebufferPool.clear());
    }
}
