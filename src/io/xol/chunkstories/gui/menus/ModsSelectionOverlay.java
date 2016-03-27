package io.xol.chunkstories.gui.menus;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.font.BitmapFont;
import io.xol.engine.gui.ClickableButton;
import io.xol.engine.gui.FocusableObjectsHandler;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ModsSelectionOverlay extends Overlay
{
	FocusableObjectsHandler guiHandler = new FocusableObjectsHandler();
	ClickableButton backOption = new ClickableButton(0, 0, 300, 32, ("Back"), BitmapFont.SMALLFONTS, 1);
	
	public ModsSelectionOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		guiHandler.add(backOption);
	}

	@Override
	public void drawToScreen(int positionStartX, int positionStartY, int width, int height)
	{
		backOption.setPos(positionStartX + 192, 96);
		backOption.draw();

		if (backOption.clicked())
		{
			this.mainScene.changeOverlay(this.parent);
		}
	}

}
