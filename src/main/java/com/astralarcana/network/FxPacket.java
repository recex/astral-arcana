package com.astralarcana.network;

import com.astralarcana.client.ClientFx;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Pacote servidor -> cliente que dispara os efeitos cinematograficos (particulas, tremor, flash...). */
public class FxPacket {
    public enum Type { METEOR_IMPACT, SUPERNOVA, BEAM, BLACKHOLE_BIRTH, BLACKHOLE_COLLAPSE }

    public final int type;
    public final double x, y, z, x2, y2, z2;
    public final float power;

    public FxPacket(int type, double x, double y, double z, double x2, double y2, double z2, float power) {
        this.type = type;
        this.x = x; this.y = y; this.z = z;
        this.x2 = x2; this.y2 = y2; this.z2 = z2;
        this.power = power;
    }

    public static void encode(FxPacket p, FriendlyByteBuf buf) {
        buf.writeByte(p.type);
        buf.writeDouble(p.x); buf.writeDouble(p.y); buf.writeDouble(p.z);
        buf.writeDouble(p.x2); buf.writeDouble(p.y2); buf.writeDouble(p.z2);
        buf.writeFloat(p.power);
    }

    public static FxPacket decode(FriendlyByteBuf buf) {
        return new FxPacket(buf.readByte(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientFx.handle(this)));
        ctx.get().setPacketHandled(true);
    }
}
