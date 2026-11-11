package com.astralarcana.engine.vfx;

import com.astralarcana.engine.particle.ParticleSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Destroços visuais: particulas do bloco real do chao (nao remove nem altera nenhum bloco). */
public final class Debris {
    private Debris() {}

    public static void burst(ClientLevel l, Vec3 c, int count, double speed) {
        BlockState st = l.getBlockState(BlockPos.containing(c.x, c.y - 0.5, c.z));
        if (st.isAir()) st = Blocks.STONE.defaultBlockState();
        RandomSource r = l.random;
        BlockParticleOption opt = new BlockParticleOption(ParticleTypes.BLOCK, st);
        for (int i = 0; i < count; i++) {
            if (!ParticleSystem.allow(c.x, c.y, c.z)) return;
            double a = r.nextDouble() * Math.PI * 2;
            double h = 0.3 + r.nextDouble() * 0.7;
            double sp = speed * (0.4 + r.nextDouble());
            l.addParticle(opt, c.x, c.y + 0.3, c.z, Math.cos(a) * sp * h, sp * (0.6 + r.nextDouble()), Math.sin(a) * sp * h);
        }
    }
}
