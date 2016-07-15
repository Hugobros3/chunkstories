package io.xol.chunkstories.gui;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientToServerConnection;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.gui.GuiElementsHandler;
import io.xol.engine.gui.Scene;
import io.xol.engine.gui.elements.Button;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ConnectScene extends Scene
{
	GuiElementsHandler guiHandler = new GuiElementsHandler();

	String message = "";
	private boolean loginOk = false;

	Button cancelButton = new Button(0, 0, 128, 32, ("Cancel"), BitmapFont.SMALLFONTS, 1);
	String serverName = "";

	public ConnectScene(GameWindowOpenGL XolioWindow, String ip, int port)
	{
		super(XolioWindow);
		Client.connection = new ClientToServerConnection(ip, port);
		serverName = ip;
		if (port != 30410)
			serverName += ":" + port;
		
		
		guiHandler.add(cancelButton);
	}
	
	boolean authK = false;
	
	@Override
	public void update()
	{
		cancelButton.setPosition(GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2 - 80);
		cancelButton.draw();
		
		if(!authK && Client.connection.authentificated)
		{
			authK = true;
			message = "Asking server for world info...";
			Client.connection.sendTextMessage("world/info");
		}
		
		//WorldInfo info = Client.connection.getWorldInfo();
		if(Client.world != null)
			loginOk = true;

		if (loginOk)
		{
			this.eng.changeScene(new GameplayScene(eng, true));
		}
		float c = 1.0f;
		drawCenteredText("Connecting to " + serverName, GameWindowOpenGL.windowHeight / 2, 64, c, c, c, 1f);
		c = 0.5f;
		drawCenteredText(message, GameWindowOpenGL.windowHeight / 2 - 32, 32, c, c, c, 1f);
		FontRenderer2.drawTextUsingSpecificFontRVBA(12, 12, 0, 32, "Copyright 2016 XolioWare Interactive", BitmapFont.SMALLFONTS, 1f, 0.3f, 0.3f, 0.3f);
		super.update();
		if (cancelButton.clicked())
			cancel();
		if(Client.connection != null && Client.connection.hasFailed())
			message = "#FF0000"+Client.connection.getLatestErrorMessage();
	}

	void drawCenteredText(String t, float height, int basesize, float r, float v, float b, float a)
	{
		FontRenderer2.drawTextUsingSpecificFontRVBA(GameWindowOpenGL.windowWidth / 2 - FontRenderer2.getTextLengthUsingFont(basesize, t, BitmapFont.SMALLFONTS) / 2, height, 0, basesize, t, BitmapFont.SMALLFONTS, a, r, v, b);
	}

	void cancel()
	{
		Client.connection.close();
		Client.connection = null;
		if(Client.world != null)
		{
			Client.world.destroy();
			Client.world = null;
		}	
		this.eng.changeScene(new MainMenu(eng, false));
	}

	@Override
	public boolean onKeyDown(int k)
	{
		if (k == 1)
			cancel();
		else
			guiHandler.handleInput(k);
		return true;
	}

	@Override
	public boolean onMouseButtonDown(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}

}
