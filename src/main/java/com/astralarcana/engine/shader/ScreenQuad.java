package com.astralarcana.engine.shader;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

/** Quad de tela cheia (coordenadas ja em NDC). Requer shader POSITION_TEX ligado. */
public final class ScreenQuad {
    private ScreenQuad() {}

    public static void draw() {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(-1.0, -1.0, 0.0).uv(0f, 0f).endVertex();
        bb.vertex(1.0, -1.0, 0.0).uv(1f, 0f).endVertex();
        bb.vertex(1.0, 1.0, 0.0).uv(1f, 1f).endVertex();
        bb.vertex(-1.0, 1.0, 0.0).uv(0f, 1f).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }
}
