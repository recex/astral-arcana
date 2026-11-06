package com.astralarcana.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;

/** Particula que orbita um centro (disco de acrecao, circulo magico, espirais). */
public class OrbitParticle extends GlowParticle {
    private final double cx, cy, cz, angSpeed, inward, tilt;
    private double angle, radius;

    public OrbitParticle(ClientLevel level, double cx, double cy, double cz, double angle, double radius,
                         double angSpeed, double inward, double tilt, float[] col, float size, int life) {
        super(level, cx, cy, cz, 0, 0, 0, col[0], col[1], col[2], size, life, 0f, 1f);
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        this.angle = angle;
        this.radius = radius;
        this.angSpeed = angSpeed;
        this.inward = inward;
        this.tilt = tilt;
        place();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
    }

    private void place() {
        double x0 = Math.cos(angle) * radius;
        double z0 = Math.sin(angle) * radius;
        setPos(cx + x0, cy - Math.sin(tilt) * z0, cz + Math.cos(tilt) * z0);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            remove();
            return;
        }
        angle += angSpeed;
        radius -= inward;
        if (radius < 0.4) {
            remove();
            return;
        }
        place();
        fade();
    }
}
