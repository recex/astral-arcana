package com.astralarcana.engine.shader;

/** Parametros de um frame de pos-processamento (preenchidos pelo ClientFx). */
public final class PostParams {
    public float lensX = 0.5f, lensY = 0.5f, lensS, lensR = 0.2f, horizon;
    public float waveX = 0.5f, waveY = 0.5f, waveR, waveW = 0.06f, waveStr;
    public float blurX = 0.5f, blurY = 0.5f, blur;
    public float chroma;
    public float swX = 0.5f, swY = 0.5f, swS, swR = 0.3f;
    public float gDesat, gInv, gVig, glitch;
    public float tR = 1f, tG = 1f, tB = 1f, tA;
    public float motionBlur;
    public float bloom;

    public void reset() {
        lensX = 0.5f; lensY = 0.5f; lensS = 0f; lensR = 0.2f; horizon = 0f;
        waveX = 0.5f; waveY = 0.5f; waveR = 0f; waveW = 0.06f; waveStr = 0f;
        blurX = 0.5f; blurY = 0.5f; blur = 0f;
        chroma = 0f;
        swX = 0.5f; swY = 0.5f; swS = 0f; swR = 0.3f;
        gDesat = 0f; gInv = 0f; gVig = 0f; glitch = 0f;
        tR = 1f; tG = 1f; tB = 1f; tA = 0f;
        motionBlur = 0f;
        bloom = 0f;
    }

    public boolean hasDistortion() {
        return lensS > 0f || waveStr > 0f || blur > 0.004f || chroma > 0.0005f || swS > 0f
                || gDesat > 0f || gInv > 0f || gVig > 0f || glitch > 0.01f || tA > 0.01f;
    }
}
