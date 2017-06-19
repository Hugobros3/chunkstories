//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

package io.xol.chunkstories.api.server;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.player.Player;
import io.xol.chunkstories.api.world.WorldMaster;

public interface ServerPacketsProcessor {
	public ServerInterface getContext();
	
	public WorldMaster getWorld();
	
	/** Players each have a subclass of this interface */
	public interface ServerPlayerPacketsProcessor extends PacketsProcessor, ServerPacketsProcessor {
		public Player getPlayer();
	}
}