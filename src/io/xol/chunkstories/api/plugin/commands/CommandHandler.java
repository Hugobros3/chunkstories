package io.xol.chunkstories.api.plugin.commands;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface CommandHandler
{
	public boolean handleCommand(CommandEmitter emitter, Command command, String[] arguments);
}
