package io.xol.chunkstories.server.net.packets;

import java.io.DataInputStream;
import java.io.IOException;

import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.net.packets.PacketWorldUser;
import io.xol.chunkstories.server.player.ServerPlayer;

public class ServerPacketWorldUser extends PacketWorldUser {

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException {
		super.process(sender, in, processor);
		
		if(sender instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer)sender;
			player.loadingAgent.handleClientRequest(this);
		}
	}
}
