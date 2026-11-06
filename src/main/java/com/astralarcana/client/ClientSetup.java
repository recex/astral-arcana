package com.astralarcana.client;

import com.astralarcana.AstralArcana;
import com.astralarcana.Reg;
import com.astralarcana.client.particle.GlowParticle;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = AstralArcana.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(Reg.METEOR.get(), MeteorRenderer::new);
        e.registerEntityRenderer(Reg.BLACK_HOLE.get(), BlackHoleRenderer::new);
        e.registerEntityRenderer(Reg.SERPENT.get(), CosmicSerpentRenderer::new);
    }

    @SubscribeEvent
    public static void particles(RegisterParticleProvidersEvent e) {
        e.registerSpriteSet(Reg.GLOW.get(), GlowParticle.Provider::new);
    }

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent e) throws IOException {
        e.registerShader(
                new ShaderInstance(e.getResourceProvider(), AstralArcana.id("distort"), DefaultVertexFormat.POSITION_TEX),
                shader -> ClientFx.distortShader = shader);
    }
}
