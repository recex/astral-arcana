package com.astralarcana.network;

import com.astralarcana.AstralArcana;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private ModNetwork() {}

    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            AstralArcana.id("main"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        CHANNEL.registerMessage(0, FxPacket.class, FxPacket::encode, FxPacket::decode, FxPacket::handle);
    }

    public static void sendFx(ServerLevel level, FxPacket.Type type, Vec3 a, Vec3 b, float power) {
        FxPacket pkt = new FxPacket(type.ordinal(), a.x, a.y, a.z, b.x, b.y, b.z, power);
        CHANNEL.send(PacketDistributor.NEAR.with(
                () -> new PacketDistributor.TargetPoint(a.x, a.y, a.z, 160.0, level.dimension())), pkt);
    }
}
