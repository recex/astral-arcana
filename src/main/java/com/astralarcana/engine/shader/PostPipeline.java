package com.astralarcana.engine.shader;

import com.astralarcana.engine.core.Quality;
import com.astralarcana.engine.core.Settings;
import com.astralarcana.engine.perf.PerformanceEngine;
import com.astralarcana.engine.world.WorldVisualEngine;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * Pipeline de pos-processamento (executa em AFTER_LEVEL):
 *  1) passe de cena  -> 'cinematic' (ou o fallback 'distort'): distorcao, motion blur, luzes dinamicas,
 *     ondas 3D, neblina, orbes volumetricos (raymarch) e color grading, lendo cor + profundidade;
 *  2) bloom          -> extracao, blur gaussiano separavel em resolucao reduzida, soma aditiva.
 * So roda quando ha efeitos ativos; cada etapa e opcional via configuracao/qualidade.
 */
public final class PostPipeline {
    private PostPipeline() {}

    private static final PostParams PARAMS = new PostParams();
    private static final Matrix4f PV = new Matrix4f();
    private static final Matrix4f INV = new Matrix4f();
    private static final Matrix4f PREV_PV = new Matrix4f();
    private static Vec3 prevCam = Vec3.ZERO;
    private static boolean hasPrev;
    private static long frames;

    public static PostParams params() {
        PARAMS.reset();
        return PARAMS;
    }

    public static long frames() {
        return frames;
    }

    public static void run(RenderLevelStageEvent e, PostParams p) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        Vec3 cam = e.getCamera().getPosition();
        PV.set(e.getProjectionMatrix()).mul(e.getPoseStack().last().pose());

        boolean world = Settings.volumetrics && WorldVisualEngine.needsScenePass();
        float mb = Settings.motionBlur ? p.motionBlur : 0f;
        boolean teleported = !hasPrev || cam.distanceToSqr(prevCam) > 256.0;
        if (teleported) mb = 0f;
        boolean scene = Settings.postFx && (p.hasDistortion() || world || mb > 0.01f);

        try {
            if (scene) {
                ShaderProgram cin = ShaderEngine.get("cinematic");
                ShaderProgram sp = cin != null ? cin : ShaderEngine.get("distort");
                if (sp != null) scenePass(sp, cin != null, main, p, cam, e.getPartialTick(), mb, mc);
            }
            if (Settings.bloom && Settings.postFx && p.bloom > 0.02f) bloomPass(main, p);
        } finally {
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            main.bindWrite(true);
        }
        PREV_PV.set(PV);
        prevCam = cam;
        hasPrev = true;
        frames++;
    }

    private static void scenePass(ShaderProgram sp, boolean full, RenderTarget main, PostParams p, Vec3 cam,
                                  float pt, float mb, Minecraft mc) {
        RenderTarget scene = FramebufferPool.acquire("scene", main.width, main.height, full);
        GlStateManager._glBindFramebuffer(36008, main.frameBufferId);
        GlStateManager._glBindFramebuffer(36009, scene.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, scene.width, scene.height, 16384, 9728);
        if (full) scene.copyDepthFrom(main);
        main.bindWrite(false);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        sp.bind();
        RenderSystem.setShaderTexture(0, scene.getColorTextureId());
        if (full) RenderSystem.setShaderTexture(1, scene.getDepthTextureId());

        sp.set2("ScreenSize", main.width, main.height);
        sp.set4("Lens", p.lensX, p.lensY, p.lensS, p.lensR);
        sp.set1("Horizon", p.horizon);
        sp.set4("Wave", p.waveX, p.waveY, p.waveR, p.waveW);
        sp.set1("WaveStr", p.waveStr);
        sp.set3("Blur", p.blurX, p.blurY, p.blur);
        sp.set1("Chroma", p.chroma);
        sp.set4("Swirl", p.swX, p.swY, p.swS, p.swR);
        sp.set4("Grade", p.gDesat, p.gInv, p.gVig, p.glitch);
        sp.set4("Tint", p.tR, p.tG, p.tB, p.tA);
        sp.set1("Clock", (mc.level == null ? 0f : mc.level.getGameTime() + pt) / 20f);

        if (full) {
            INV.set(PV).invert();
            sp.setMat("InvPV", INV);
            sp.setMat("PrevPV", PREV_PV);
            Vec3 d = cam.subtract(prevCam);
            sp.set3("CamDelta", (float) d.x, (float) d.y, (float) d.z);
            Quality q = PerformanceEngine.quality();
            sp.set4("Info", (float) cam.y, mb, q.raySteps, 0f);
            WorldVisualEngine.fill(sp, cam, pt, q);
        }
        ScreenQuad.draw();
        if (full) RenderSystem.setShaderTexture(1, 0);
    }

    private static void bloomPass(RenderTarget main, PostParams p) {
        ShaderProgram ex = ShaderEngine.get("bloom_extract");
        ShaderProgram bl = ShaderEngine.get("blur");
        ShaderProgram co = ShaderEngine.get("bloom_composite");
        if (ex == null || bl == null || co == null) return;
        Quality q = PerformanceEngine.quality();
        int bw = Math.max(1, main.width / q.bloomDiv);
        int bh = Math.max(1, main.height / q.bloomDiv);
        RenderTarget a = FramebufferPool.acquire("bloomA", bw, bh, false);
        RenderTarget b = FramebufferPool.acquire("bloomB", bw, bh, false);

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        a.clear(Minecraft.ON_OSX);
        a.bindWrite(true);
        ex.bind();
        RenderSystem.setShaderTexture(0, main.getColorTextureId());
        ex.set4("Params", 0.8f, 0.35f, 1f, 0f);
        ScreenQuad.draw();

        for (int i = 0; i < q.blurPasses; i++) {
            b.bindWrite(true);
            bl.bind();
            RenderSystem.setShaderTexture(0, a.getColorTextureId());
            bl.set2("Dir", 1f / bw, 0f);
            ScreenQuad.draw();
            a.bindWrite(true);
            bl.bind();
            RenderSystem.setShaderTexture(0, b.getColorTextureId());
            bl.set2("Dir", 0f, 1f / bh);
            ScreenQuad.draw();
        }

        main.bindWrite(true);
        co.bind();
        RenderSystem.setShaderTexture(0, a.getColorTextureId());
        co.set4("Params", 0f, 0f, Settings.bloomIntensity * Math.min(p.bloom, 1.5f), 0f);
        ScreenQuad.draw();
    }
}
