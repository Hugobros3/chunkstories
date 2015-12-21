package io.xol.chunkstories.gui;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ServerConnection;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;
import io.xol.engine.gui.ClickableButton;
import io.xol.engine.gui.FocusableObjectsHandler;
import io.xol.engine.scene.Scene;

public class ConnectScene extends Scene
{
	FocusableObjectsHandler guiHandler = new FocusableObjectsHandler();

	String message = "";
	private boolean loginOk = false;

	ClickableButton cancelButton = new ClickableButton(0, 0, 128, 32, ("Cancel"), BitmapFont.SMALLFONTS, 1);
	String serverName = "";

	public ConnectScene(XolioWindow XolioWindow, String ip, int port)
	{
		super(XolioWindow);
		Client.connection = new ServerConnection(ip, port);
		serverName = ip;
		if (port != 30410)
			serverName += ":" + port;
		
		
		guiHandler.add(cancelButton);
	}
	
	boolean authK = false;
	
	public void update()
	{
		// ObjectRenderer.renderTexturedRect(XolioWindow.frameW / 2,
		// XolioWindow.frameH / 2 + 180, 512, 512, "logo");

		cancelButton.setPos(XolioWindow.frameW / 2, XolioWindow.frameH / 2 - 80);
		cancelButton.draw();
		
		if(!authK && Client.connection.authentificated)
		{
			authK = true;
			message = "Asking server for world info...";
			Client.connection.sendTextMessage("world/info");
		}
		
		//WorldInfo info = Client.connection.getWorldInfo();
		if(Client.world != null)
		{
			/*Client.world = new WorldClient(info);
			message = Client.world.name;
			System.out.println(info.name);*/
			loginOk = true;
		}

		if (loginOk)
		{
			this.eng.changeScene(new GameplayScene(eng, true));
		}
		float c = 1.0f;
		drawCenteredText("Connecting to " + serverName, XolioWindow.frameH / 2, 64, c, c, c, 1f);
		c = 0.5f;
		drawCenteredText(message, XolioWindow.frameH / 2 - 32, 32, c, c, c, 1f);
		FontRenderer2.drawTextUsingSpecificFontRVBA(12, 12, 0, 32, "Copyright 2015 XolioWare Interactive", BitmapFont.SMALLFONTS, 1f, 0.3f, 0.3f, 0.3f);
		super.update();
		
		if (cancelButton.clicked())
			cancel();
	}

	void drawCenteredText(String t, float height, int basesize, float r, float v, float b, float a)
	{
		FontRenderer2.drawTextUsingSpecificFontRVBA(XolioWindow.frameW / 2 - FontRenderer2.getTextLengthUsingFont(basesize, t, BitmapFont.SMALLFONTS) / 2, height, 0, basesize, t, BitmapFont.SMALLFONTS, a, r, v, b);
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
			
		this.eng.changeScene(new MainMenu(eng));
	}

	public boolean onKeyPress(int k)
	{
		if (k == 1)
			cancel();
		else
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
