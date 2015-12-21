package io.xol.chunkstories.gui;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.chunkstories.gui.menus.MenuOverlay;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.scene.Scene;

public abstract class OverlayableScene extends Scene
{

	public OverlayableScene(XolioWindow XolioWindow)
	{
		super(XolioWindow);
	}

	public MenuOverlay currentOverlay = null;

	public void changeOverlay(MenuOverlay newOverlay)
	{
		this.currentOverlay = newOverlay;
	}
}
