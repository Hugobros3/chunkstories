package io.xol.chunkstories.gui.overlays.ingame;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.MainMenu;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.gui.overlays.config.OptionsOverlay;
import io.xol.chunkstories.input.Inputs;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.gui.GuiElementsHandler;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.math.lalgb.Vector4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PauseOverlay extends Overlay
{
	GuiElementsHandler guiHandler = new GuiElementsHandler();
	
	Button resumeButton = new Button(0, 0, 320, 32, "Resume", BitmapFont.SMALLFONTS, 1);
	Button optionsButton = new Button(0, 0, 320, 32, "Options", BitmapFont.SMALLFONTS, 1);
	Button exitButton = new Button(0, 0, 320, 32, "Quit to menu", BitmapFont.SMALLFONTS, 1);
	
	public PauseOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		guiHandler.add(resumeButton);
		guiHandler.add(optionsButton);
		guiHandler.add(exitButton);
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int x, int y, int w, int h)
	{
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(0, 0, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight, 0, 0, 0, 0, null, false, true, new Vector4f(0.0, 0.0, 0.0, 0.5));
		
		//ObjectRenderer.renderColoredRect(GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight, 0, "000000", 0.5f);
		FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth / 2 - FontRenderer2.getTextLengthUsingFont(48, "In-game menu", BitmapFont.SMALLFONTS) / 2, GameWindowOpenGL.windowHeight / 2 + 48 * 3, 0, 48, "In-game menu", BitmapFont.SMALLFONTS);

		resumeButton.setPosition(GameWindowOpenGL.windowWidth/2, GameWindowOpenGL.windowHeight/2 + 48 * 2);
		optionsButton.setPosition(GameWindowOpenGL.windowWidth/2, GameWindowOpenGL.windowHeight/2 + 48);
		exitButton.setPosition(GameWindowOpenGL.windowWidth/2, GameWindowOpenGL.windowHeight/2 - 48);
		
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
			//if(Client.world != null)
			//	Client.world.unloadEverything();
			
			Client.getInstance().exitToMainMenu();
			//mainScene.eng.changeScene(new MainMenu(mainScene.eng, false));
		}
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
