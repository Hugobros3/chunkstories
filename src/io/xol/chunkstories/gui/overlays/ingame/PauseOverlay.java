package io.xol.chunkstories.gui.overlays.ingame;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.gui.overlays.config.ModsSelectionOverlay;
import io.xol.chunkstories.gui.overlays.config.OptionsOverlay;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.gui.elements.Button;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PauseOverlay extends Overlay
{
	Button resumeButton = new Button(0, 0, 320, 32, "#{menu.resume}", BitmapFont.SMALLFONTS, 1);
	Button optionsButton = new Button(0, 0, 320, 32, "#{menu.options}", BitmapFont.SMALLFONTS, 1);
	Button modsButton = new Button(-100, 0, 320, 32, "#{menu.mods}", BitmapFont.SMALLFONTS, 1);
	Button exitButton = new Button(0, 0, 320, 32, "#{menu.backto}", BitmapFont.SMALLFONTS, 1);
	
	public PauseOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		guiHandler.add(resumeButton);
		guiHandler.add(optionsButton);
		guiHandler.add(modsButton);
		guiHandler.add(exitButton);
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int x, int y, int w, int h)
	{
		FontRenderer2.drawTextUsingSpecificFont(renderingContext.getWindow().getWidth() / 2 - FontRenderer2.getTextLengthUsingFont(48, "#{ingame.pause}", BitmapFont.SMALLFONTS) / 2, renderingContext.getWindow().getHeight() / 2 + 48 * 3, 0, 48, "In-game menu", BitmapFont.SMALLFONTS);

		resumeButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 + 48 * 2);
		optionsButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 + 48 * 1);
		//modsButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 + 48 * 0);
		exitButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 - 48);
		
		resumeButton.draw();
		optionsButton.draw();
		//modsButton.draw();
		exitButton.draw();

		if(resumeButton.clicked())
			mainScene.changeOverlay(parent);
		
		if(optionsButton.clicked())
			mainScene.changeOverlay(new OptionsOverlay(mainScene, this));
		
		if(modsButton.clicked())
			mainScene.changeOverlay(new ModsSelectionOverlay(mainScene, this));
		
		if(exitButton.clicked())
			Client.getInstance().exitToMainMenu();
	}
	
	@Override
	public boolean handleKeypress(int k)
	{
		if (Client.getInstance().getInputsManager().getInputByName("exit").isPressed())
		{
			mainScene.changeOverlay(parent);
			return true;
		}
		guiHandler.handleInput(k);
		return true;
	}

	@Override
	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}
}
