package io.xol.chunkstories.api.server;

import io.xol.chunkstories.api.player.Player;

public interface PermissionsManager
{
	public boolean hasPermission(Player player, String permissionNode);
}
