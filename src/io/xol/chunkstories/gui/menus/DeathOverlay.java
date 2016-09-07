package io.xol.chunkstories.gui.menus;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.MainMenu;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.gui.GuiElementsHandler;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.math.HexTools;
import io.xol.engine.math.lalgb.Vector4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DeathOverlay extends Overlay
{
	GuiElementsHandler guiHandler = new GuiElementsHandler();
	
	Button respawnButton = new Button(0, 0, 320, 32, "tryhard", BitmapFont.SMALLFONTS, 1);
	Button exitButton = new Button(0, 0, 320, 32, "ragequit", BitmapFont.SMALLFONTS, 1);

	public DeathOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		guiHandler.add(respawnButton);
		guiHandler.add(exitButton);
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int x, int y, int w, int h)
	{
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(0, 0, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight, 0, 0, 0, 0, 0, false, true, new Vector4f(0.0, 0.0, 0.0, 0.5));
		
		String color = "";
		color += HexTools.intToHex((int) (Math.random() * 255));
		color += HexTools.intToHex((int) (Math.random() * 255));
		color += HexTools.intToHex((int) (Math.random() * 255));
		
		FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth / 2 - FontRenderer2.getTextLengthUsingFont(96, "YOU DIEDED", BitmapFont.SMALLFONTS) / 2, GameWindowOpenGL.windowHeight / 2 + 48 * 3, 0, 96, "#FF0000YOU DIEDED", BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth / 2 - FontRenderer2.getTextLengthUsingFont(48, "git gud scrub", BitmapFont.SMALLFONTS) / 2, GameWindowOpenGL.windowHeight / 2 + 36 * 3, 0, 48, "#"+color+"git gud scrub", BitmapFont.SMALLFONTS);

		respawnButton.setPosition(GameWindowOpenGL.windowWidth/2, GameWindowOpenGL.windowHeight/2 + 48);
		exitButton.setPosition(GameWindowOpenGL.windowWidth/2, GameWindowOpenGL.windowHeight/2 - 24);
		
		respawnButton.draw();
		exitButton.draw();
		
		//When the new entity arrives
		if(Client.getInstance().getClientSideController().getControlledEntity() != null)
		{
			mainScene.changeOverlay(parent);
		}

		if(respawnButton.clicked())
		{
			if(Client.connection != null)
				Client.connection.sendTextMessage("world/respawn");
			mainScene.changeOverlay(parent);
		}
		
		if(exitButton.clicked())
		{
			//if(Client.world != null)
			//	Client.world.unloadEverything();
			mainScene.gameWindows.changeScene(new MainMenu(mainScene.gameWindows, false));
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
