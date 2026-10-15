package org.kvxd.sophisticatedintegrations.gametest;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
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
        super(player.server, connected(), player, cookie);
    }

    private static Connection connected() {
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        return connection;
    }

    @Override
    public void send(Packet<?> packet) {
        packets.add(packet);
    }

    @Override
    public void send(Packet<?> packet, PacketSendListener listener) {
        packets.add(packet);
    }

    public List<Packet<?>> packets() { return packets; }
}
