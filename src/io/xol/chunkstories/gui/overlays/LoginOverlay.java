package io.xol.chunkstories.gui.overlays;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.bugsreporter.JavaCrashesUploader;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.InputText;
import io.xol.engine.net.HttpRequestThread;
import io.xol.engine.net.HttpRequester;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class LoginOverlay extends Overlay implements HttpRequester
{
	public LoginOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		
		guiHandler.add(new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS));
		guiHandler.getInputText(0).setFocus(true);
		// pass
		guiHandler.add(new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS));
		// ok
		guiHandler.add(new Button(0, 0, 128, 32, ("Login"), BitmapFont.SMALLFONTS, 1));
		// check for auto-login dataaa
		if (Client.getConfig().getProp("autologin", "ko").equals("ok"))
		{
			guiHandler.getInputText(0).setText(Client.getConfig().getProp("user", ""));
			guiHandler.getInputText(1).setText(Client.getConfig().getProp("pass", ""));
			autologin = true;
		}
		startCounter = System.currentTimeMillis();
	}

	//GuiElementsHandler guiHandler = new GuiElementsHandler();

	boolean logging_in = false;
	boolean autologin = false;
	
	long startCounter = 0l;

	String message = "";

	private boolean can_next = false;
	private boolean failed_login;
	
	@Override
	public void drawToScreen(RenderingContext renderingContext, int x, int y, int w, int h)
	{
		if (can_next)
			mainScene.changeOverlay(new MainMenuOverlay(mainScene, null));
		ObjectRenderer.renderTexturedRect(GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2 + 180, 512, 512, "./textures/logo.png");

		guiHandler.getButton(2).setPosition(GameWindowOpenGL.windowWidth / 2 - 245 + 58, GameWindowOpenGL.windowHeight / 2 - 80);

		guiHandler.getInputText(0).setPosition(GameWindowOpenGL.windowWidth / 2 - 250, GameWindowOpenGL.windowHeight / 2 + 40);
		guiHandler.getInputText(0).drawWithBackGround();
		guiHandler.getInputText(1).setPosition(GameWindowOpenGL.windowWidth / 2 - 250, GameWindowOpenGL.windowHeight / 2 - 40);
		guiHandler.getInputText(1).drawWithBackGroundPassworded();

		FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth / 2 - 250, GameWindowOpenGL.windowHeight / 2 + 80, 0, 32, "Username", BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth / 2 - 250, GameWindowOpenGL.windowHeight / 2 + 0, 0, 32, "Password", BitmapFont.SMALLFONTS);

		if (logging_in)
		{
			FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth / 2 - 230, GameWindowOpenGL.windowHeight / 2 - 90, 0, 32, "Logging in...", BitmapFont.SMALLFONTS);
		}
		else
		{
			int decal_lb = guiHandler.getButton(2).draw();

			FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth / 2 - 245 - 58 + decal_lb, GameWindowOpenGL.windowHeight / 2 - 95, 0, 32, "Register at http://chunkstories.xyz", BitmapFont.SMALLFONTS);
			// FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW / 2 -
			// 250, XolioWindow.frameH / 2 - 150 + 18, 0, 32,
			// "You currently need hugobros3 to provide you an account.",
			// BitmapFont.SMALLFONTS);
			if (failed_login)
				FontRenderer2.drawTextUsingSpecificFontRVBA(GameWindowOpenGL.windowWidth / 2 - 250, GameWindowOpenGL.windowHeight / 2 - 160, 0, 32, message, BitmapFont.SMALLFONTS, 1, 1, 0, 0);
		}

		if (autologin)
		{
			int seconds = 10;
			String autologin2 = "Autologin in "+(seconds-(System.currentTimeMillis()-startCounter)/1000)+" seconds...";
			FontRenderer2.drawTextUsingSpecificFontRVBA(GameWindowOpenGL.windowWidth / 2 - FontRenderer2.getTextLengthUsingFont(32, autologin2, BitmapFont.SMALLFONTS) / 2, GameWindowOpenGL.windowHeight / 2 - 170, 0, 32, autologin2, BitmapFont.SMALLFONTS, 1, 0, 1, 0);
			if ((System.currentTimeMillis()-startCounter)/1000 > seconds)
			{
				connect();
				autologin = false;
			}
		}
		if(guiHandler.getButton(2).clicked())
			connect();
		FontRenderer2.drawTextUsingSpecificFont(12, 12, 0, 32, "Copyright 2016 XolioWare Interactive", BitmapFont.SMALLFONTS);
	}
	
	@Override
	public boolean handleKeypress(int k)
	{
		if (k == 15)
			guiHandler.next();
		else if (k == 28)
			connect();
		else if (k == 1)
			autologin = false;
		else
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
	
	void connect()
	{
		if (guiHandler.getInputText(0).text.equals("OFFLINE"))
		{
			Client.offline = true;
			Client.username = "OfflineUser" + (int) (Math.random() * 1000);
			this.mainScene.changeOverlay(new MainMenuOverlay(mainScene, null));//eng.changeScene(new MainMenu(eng));
		}
		else
		{
			logging_in = true;
			new HttpRequestThread(this, "login", "http://chunkstories.xyz/api/login.php", "user=" + guiHandler.getInputText(0).text + "&pass=" + guiHandler.getInputText(1).text);
		}
	}
	
	@Override
	public void handleHttpRequest(String info, String result)
	{
		if (info.equals("login"))
		{
			logging_in = false;
			if (result == null)
			{
				failed_login = true;
				message = "Can't connect to server.";
				return;
			}
			if (result.startsWith("ok"))
			{
				String session = result.split(":")[1];
				Client.username = guiHandler.getInputText(0).text;
				Client.session_key = session;
				Client.getConfig().setString("autologin", "ok");
				Client.getConfig().setString("user", guiHandler.getInputText(0).text);
				Client.getConfig().setString("pass", guiHandler.getInputText(1).text);
				
				if(Client.username.equals("Gobrosse") || Client.username.equals("kektest"))
				{
					RenderingConfig.isDebugAllowed = true;
				}
				
				//If the user didn't opt-out, look for crash files and upload those
				if(Client.clientConfig.getProp("log-policy", "undefined").equals("send"))
				{
					JavaCrashesUploader t = new JavaCrashesUploader();
					t.start();
				}
				
				can_next = true;
			}
			else if (result.startsWith("ko"))
			{
				failed_login = true;
				String reason = result.split(":")[1];
				if (reason.equals("notpremium"))
					message = ("User is not premium");
				else if (reason.equals("invalidcredentials"))
					message = ("Invalid credentials");
			}
			else
			{
				message = ("Unknown error");
			}
		}
	}
}
