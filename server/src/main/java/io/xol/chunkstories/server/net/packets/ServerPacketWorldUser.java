package io.xol.chunkstories.server.net.packets;

import java.io.DataInputStream;
import java.io.IOException;

import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.packets.PacketWorldUser;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.server.player.ServerPlayer;

public class ServerPacketWorldUser extends PacketWorldUser {

	public ServerPacketWorldUser(World world) {
		super(world);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException {
		super.process(sender, in, processor);
		
		if(type == Type.REGISTER_SUMMARY || type == Type.UNREGISTER_SUMMARY) {
			
			int sizeInRegions = processor.getWorld().getSizeInChunks() / 8;
			int filteredX = x % sizeInRegions;
			if(filteredX < 0)
				filteredX += sizeInRegions;
			
			int filteredZ = z % sizeInRegions;
			if(filteredZ < 0)
				filteredZ += sizeInRegions;
			
			if(x != filteredX || z != filteredZ) {
				//System.out.println("warning: someone forgot to sanitize their region coordinates!");
				x = filteredX;
				z = filteredZ;
				
				//System.out.println("og: "+x+": "+z);
				//System.out.println(filteredX+":"+filteredZ);
			}
		}
		
		if(sender instanceof ServerPlayer) {
			ServerPlayer player = (ServerPlayer)sender;
			player.loadingAgent.handleClientRequest(this);
		}
	}
}
