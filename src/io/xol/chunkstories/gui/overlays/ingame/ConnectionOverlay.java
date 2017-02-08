package io.xol.chunkstories.gui.overlays.ingame;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientSideConnectionSequence;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.gui.elements.Button;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

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
		String color = "606060";
		//color += HexTools.intToHex((int) (Math.random() * 255));
		//color += HexTools.intToHex((int) (Math.random() * 255));
		//color += HexTools.intToHex((int) (Math.random() * 255));
		
		String connection = "Connecting, please wait";
		FontRenderer2.drawTextUsingSpecificFont(renderingContext.getWindow().getWidth() / 2 - FontRenderer2.getTextLengthUsingFont(96, connection, BitmapFont.SMALLFONTS) / 2, 
				renderingContext.getWindow().getHeight() / 2 + 48 * 3, 0, 96, connection, BitmapFont.SMALLFONTS);
		
		String currentConnectionStep = connectionSequence.getStatus().getStepText();
		FontRenderer2.drawTextUsingSpecificFont(renderingContext.getWindow().getWidth() / 2 - FontRenderer2.getTextLengthUsingFont(48, currentConnectionStep, BitmapFont.SMALLFONTS) / 2, renderingContext.getWindow().getHeight() / 2 + 36 * 3, 0, 48,
				"#"+color+currentConnectionStep, BitmapFont.SMALLFONTS);

		exitButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 - 24);
		
		exitButton.draw();
		
		if(exitButton.clicked())
			Client.getInstance().exitToMainMenu();
		
		//Once the connection sequence is done, we hide this overlay
		if(connectionSequence.isDone())
			this.mainScene.changeOverlay(parent);
		
		String fail = connectionSequence.hasFailed();
		if(fail != null)
			Client.getInstance().exitToMainMenu(fail);
	}
}
