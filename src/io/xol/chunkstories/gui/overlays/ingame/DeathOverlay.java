package io.xol.chunkstories.gui.overlays.ingame;

import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.chunkstories.world.WorldClientRemote;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.math.HexTools;
import io.xol.engine.math.lalgb.vector.sp.Vector4fm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class DeathOverlay extends Overlay
{
	//GuiElementsHandler guiHandler = new GuiElementsHandler();
	
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
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(0, 0, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4fm(0.0, 0.0, 0.0, 0.5));
		
		String color = "";
		color += HexTools.intToHex((int) (Math.random() * 255));
		color += HexTools.intToHex((int) (Math.random() * 255));
		color += HexTools.intToHex((int) (Math.random() * 255));
		
		FontRenderer2.drawTextUsingSpecificFont(renderingContext.getWindow().getWidth() / 2 - FontRenderer2.getTextLengthUsingFont(96, "YOU DIEDED", BitmapFont.SMALLFONTS) / 2, renderingContext.getWindow().getHeight() / 2 + 48 * 3, 0, 96, "#FF0000YOU DIEDED", BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(renderingContext.getWindow().getWidth() / 2 - FontRenderer2.getTextLengthUsingFont(48, "git gud scrub", BitmapFont.SMALLFONTS) / 2, renderingContext.getWindow().getHeight() / 2 + 36 * 3, 0, 48, "#"+color+"git gud scrub", BitmapFont.SMALLFONTS);

		respawnButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 + 48);
		exitButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 - 24);
		
		respawnButton.draw();
		exitButton.draw();
		
		//When the new entity arrives
		if(Client.getInstance().getPlayer().getControlledEntity() != null)
		{
			mainScene.changeOverlay(parent);
		}

		if(respawnButton.clicked())
		{
			//TODO this has no functionality whatsoever for local worlds yet
			if(Client.getInstance().getWorld() instanceof WorldClientRemote)
				((WorldClientRemote) Client.getInstance().getWorld()).getConnection().sendTextMessage("world/respawn");
			else
				((WorldClientLocal) Client.getInstance().getWorld()).spawnPlayer(Client.getInstance().getPlayer());
			
			mainScene.changeOverlay(parent);
		}
		
		if(Mouse.isGrabbed())
			Mouse.setGrabbed(false);
		
		if(exitButton.clicked())
		{
			Client.getInstance().exitToMainMenu();
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
