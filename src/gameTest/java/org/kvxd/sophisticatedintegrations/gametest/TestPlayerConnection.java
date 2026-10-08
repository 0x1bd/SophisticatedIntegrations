package org.kvxd.sophisticatedintegrations.gametest;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.ArrayList;
import java.util.List;

public final class TestPlayerConnection extends ServerGamePacketListenerImpl {
    private final List<Packet<?>> packets = new ArrayList<>();

    public TestPlayerConnection(ServerPlayer player, CommonListenerCookie cookie) {
        super(player.server, new Connection(PacketFlow.SERVERBOUND), player, cookie);
    }

    @Override
    public void send(Packet<?> packet) {
        packets.add(packet);
    }

    public List<Packet<?>> packets() { return packets; }
}
