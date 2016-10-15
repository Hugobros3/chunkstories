package io.xol.chunkstories.gui.overlays.ingame;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientSideConnectionSequence;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.math.HexTools;

public class ConnectionOverlay extends Overlay
{
	ClientSideConnectionSequence connectionSequence;
	
	Button exitButton = new Button(0, 0, 320, 32, "ragequit", BitmapFont.SMALLFONTS, 1);
	
	public ConnectionOverlay(OverlayableScene scene, Overlay parent, String ip, int port)
	{
		super(scene, parent);
		connectionSequence = new ClientSideConnectionSequence( ip,  port);
		
		guiHandler.add(exitButton);
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int positionStartX, int positionStartY, int width, int height)
	{
		String color = "";
		color += HexTools.intToHex((int) (Math.random() * 255));
		color += HexTools.intToHex((int) (Math.random() * 255));
		color += HexTools.intToHex((int) (Math.random() * 255));
		
		String connection = "Connecting, please wait";
		FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth / 2 - FontRenderer2.getTextLengthUsingFont(96, connection, BitmapFont.SMALLFONTS) / 2, 
				GameWindowOpenGL.windowHeight / 2 + 48 * 3, 0, 96, connection, BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth / 2 - FontRenderer2.getTextLengthUsingFont(48, connectionSequence.getStatus(), BitmapFont.SMALLFONTS) / 2, GameWindowOpenGL.windowHeight / 2 + 36 * 3, 0, 48, "#"+color+connectionSequence.getStatus(), BitmapFont.SMALLFONTS);

		exitButton.setPosition(GameWindowOpenGL.windowWidth/2, GameWindowOpenGL.windowHeight/2 - 24);
		
		exitButton.draw();
		
		if(exitButton.clicked())
			Client.getInstance().exitToMainMenu();
		
		//Once the connection sequence is done, we hide this overlay
		if(connectionSequence.isDone())
			this.mainScene.changeOverlay(parent);
	}
}
