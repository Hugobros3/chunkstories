package io.xol.chunkstories.gui.menus;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.gui.MainMenu;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;
import io.xol.engine.gui.ClickableButton;
import io.xol.engine.gui.FocusableObjectsHandler;

public class PauseOverlay extends MenuOverlay
{
	FocusableObjectsHandler guiHandler = new FocusableObjectsHandler();
	
	ClickableButton resumeButton = new ClickableButton(0, 0, 320, 32, "Resume", BitmapFont.SMALLFONTS, 1);
	ClickableButton optionsButton = new ClickableButton(0, 0, 320, 32, "Options", BitmapFont.SMALLFONTS, 1);
	ClickableButton exitButton = new ClickableButton(0, 0, 320, 32, "Quit to menu", BitmapFont.SMALLFONTS, 1);
	
	public PauseOverlay(OverlayableScene scene, MenuOverlay parent)
	{
		super(scene, parent);
		guiHandler.add(resumeButton);
		guiHandler.add(optionsButton);
		guiHandler.add(exitButton);
	}

	public void drawToScreen(int x, int y, int w, int h)
	{
		ObjectRenderer.renderColoredRect(XolioWindow.frameW / 2, XolioWindow.frameH / 2, XolioWindow.frameW, XolioWindow.frameH, 0, "000000", 0.5f);
		FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW / 2 - FontRenderer2.getTextLengthUsingFont(48, "In-game menu", BitmapFont.SMALLFONTS) / 2, XolioWindow.frameH / 2 + 48 * 3, 0, 48, "In-game menu", BitmapFont.SMALLFONTS);

		resumeButton.setPos(XolioWindow.frameW/2, XolioWindow.frameH/2 + 48 * 2);
		optionsButton.setPos(XolioWindow.frameW/2, XolioWindow.frameH/2 + 48);
		exitButton.setPos(XolioWindow.frameW/2, XolioWindow.frameH/2 - 48);
		
		resumeButton.draw();
		optionsButton.draw();
		exitButton.draw();
		

		if(resumeButton.clicked())
			mainScene.changeOverlay(parent);
		
		if(optionsButton.clicked())
		{
			mainScene.changeOverlay(new OptionsOverlay(mainScene, this));
		}
		
		if(exitButton.clicked())
		{
			if(Client.world != null)
				Client.world.clear();
			mainScene.eng.changeScene(new MainMenu(mainScene.eng));
		}
	}
	
	public boolean handleKeypress(int k)
	{
		if (k == FastConfig.EXIT_KEY)
		{
			mainScene.changeOverlay(parent);
			return true;
		}
		guiHandler.handleInput(k);
		return true;
	}

	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}
}
