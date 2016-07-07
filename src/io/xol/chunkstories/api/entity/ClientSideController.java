package io.xol.chunkstories.api.entity;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ClientSideController extends Controller
{
	/**
	 * @return Is the game GUI in focus or obstructed by other things ?
	 */
	public boolean hasFocus();
}
