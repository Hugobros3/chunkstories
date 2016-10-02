package io.xol.chunkstories.gui;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.gui.Scene;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class OverlayableScene extends Scene
{
	public OverlayableScene(GameWindowOpenGL gameWindow)
	{
		super(gameWindow);
	}

	public Overlay currentOverlay = null;

	public void changeOverlay(Overlay newOverlay)
	{
		this.currentOverlay = newOverlay;
	}
}
