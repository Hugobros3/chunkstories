package io.xol.chunkstories.gui.overlays.config;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.gui.GuiElementsHandler;
import io.xol.engine.gui.elements.Button;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ModsSelectionOverlay extends Overlay
{
	GuiElementsHandler guiHandler = new GuiElementsHandler();
	Button backOption = new Button(0, 0, 300, 32, ("Back"), BitmapFont.SMALLFONTS, 1);
	
	public ModsSelectionOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		guiHandler.add(backOption);
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int positionStartX, int positionStartY, int width, int height)
	{
		backOption.setPosition(positionStartX + 192, 96);
		backOption.draw();

		if (backOption.clicked())
		{
			this.mainScene.changeOverlay(this.parent);
		}
	}

}
