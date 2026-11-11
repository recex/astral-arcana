package com.astralarcana.engine.vfx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Efeito visual com ciclo de vida, bounds (para frustum culling) e render proprio. */
public abstract class VfxEffect {
    public int age;
    public int life;
    public final Vec3 pos;

    protected VfxEffect(Vec3 pos, int life) {
        this.pos = pos;
        this.life = life;
    }

    public void tick(ClientLevel level) {}

    public abstract void render(Ctx c);

    public abstract AABB bounds();

    public boolean isDone() {
        return age >= life;
    }

    public float progress(float pt) {
        return Mth.clamp((age + pt) / Math.max(1, life), 0f, 1f);
    }

    /** Contexto de render: pose ja transladada para coordenadas relativas ao mundo (camera subtraida). */
    public static final class Ctx {
        public PoseStack ps;
        public VertexConsumer vc;
        public Vec3 cam;
        public float pt;
    }
}
