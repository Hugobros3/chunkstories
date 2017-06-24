package io.xol.chunkstories.gui.overlays;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.bugsreporter.JavaCrashesUploader;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.gui.overlays.config.LanguageSelectionScreen;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.InputText;
import io.xol.engine.net.HttpRequestThread;
import io.xol.engine.net.HttpRequester;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class LoginOverlay extends Layer implements HttpRequester
{
	InputText usernameForm = new InputText(this, 0, 0, 500, 32, BitmapFont.SMALLFONTS);
	InputText passwordForm = new InputText(this, 0, 0, 500, 32, BitmapFont.SMALLFONTS);
	
	Button loginButton = new Button(this, 0, 0, 128, 32, ("#{login.login}"), BitmapFont.SMALLFONTS, 1);
	
	public LoginOverlay(GameWindow scene, Layer parent)
	{
		super(scene, parent);
		
		elements.add(usernameForm);
		elements.add(passwordForm);
		elements.add(loginButton);
		
		//Autologin fills in the forms automagically
		//TODO Secure storage of password
		if (Client.getConfig().getProp("autologin", "ko").equals("ok"))
		{
			usernameForm.setText(Client.getConfig().getProp("user", ""));
			passwordForm.setText(Client.getConfig().getProp("pass", ""));
			autologin = true;
		}
		
		loginButton.setAction(new Runnable() {
			@Override
			public void run() {
				connect();
			}
		});
		
		this.setFocusedElement(usernameForm);
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
	public void render(RenderingInterface renderingContext)
	{
		parentLayer.render(renderingContext);
		
		if(Client.clientConfig.getProp("language", "undefined").equals("undefined"))
		{
			gameWindow.setLayer(new LanguageSelectionScreen(gameWindow, this, false));
			//this.mainScene.changeOverlay(new LanguageSelectionScreen(mainScene, this, false));
		}
		
		if (can_next)
			gameWindow.setLayer(new MainMenuOverlay(gameWindow, parentLayer));
		
			//mainScene.changeOverlay(new MainMenuOverlay(mainScene, null));
		ObjectRenderer.renderTexturedRect(renderingContext.getWindow().getWidth() / 2, renderingContext.getWindow().getHeight() / 2 + 180, 512, 512, "./textures/logo.png");

		loginButton.setPosition(renderingContext.getWindow().getWidth() / 2 - 245 + 58, renderingContext.getWindow().getHeight() / 2 - 80);

		usernameForm.setPosition(renderingContext.getWindow().getWidth() / 2 - 250, renderingContext.getWindow().getHeight() / 2 + 40);
		usernameForm.drawWithBackGround();
		passwordForm.setPosition(renderingContext.getWindow().getWidth() / 2 - 250, renderingContext.getWindow().getHeight() / 2 - 40);
		passwordForm.drawWithBackGroundPassworded();

		FontRenderer2.drawTextUsingSpecificFont(renderingContext.getWindow().getWidth() / 2 - 250, renderingContext.getWindow().getHeight() / 2 + 80, 0, 32, Client.getInstance().getContent().localization().localize("#{login.username}"), BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(renderingContext.getWindow().getWidth() / 2 - 250, renderingContext.getWindow().getHeight() / 2 + 0, 0, 32, Client.getInstance().getContent().localization().localize("#{login.password}"), BitmapFont.SMALLFONTS);

		if (logging_in)
		{
			FontRenderer2.drawTextUsingSpecificFont(renderingContext.getWindow().getWidth() / 2 - 230, renderingContext.getWindow().getHeight() / 2 - 90, 0, 32, Client.getInstance().getContent().localization().localize("#{login.loggingIn}"), BitmapFont.SMALLFONTS);
		}
		else
		{
			float decal_lb = loginButton.getWidth();
			loginButton.render(renderingContext);

			FontRenderer2.drawTextUsingSpecificFont(renderingContext.getWindow().getWidth() / 2 - 245 - 58 + decal_lb, renderingContext.getWindow().getHeight() / 2 - 95, 0, 32, Client.getInstance().getContent().localization().localize("#{login.register}"), BitmapFont.SMALLFONTS);
			// FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW / 2 -
			// 250, XolioWindow.frameH / 2 - 150 + 18, 0, 32,
			// "You currently need hugobros3 to provide you an account.",
			// BitmapFont.SMALLFONTS);
			if (failed_login)
				FontRenderer2.drawTextUsingSpecificFontRVBA(renderingContext.getWindow().getWidth() / 2 - 250, renderingContext.getWindow().getHeight() / 2 - 160, 0, 32, message, BitmapFont.SMALLFONTS, 1, 1, 0, 0);
		}

		if (autologin)
		{
			int seconds = 10;
			String autologin2 = Client.getInstance().getContent().localization().localize("#{login.auto1} "+(seconds-(System.currentTimeMillis()-startCounter)/1000)+" #{login.auto2}");
			FontRenderer2.drawTextUsingSpecificFontRVBA(renderingContext.getWindow().getWidth() / 2 - FontRenderer2.getTextLengthUsingFont(32, autologin2, BitmapFont.SMALLFONTS) / 2, renderingContext.getWindow().getHeight() / 2 - 170, 0, 32, autologin2, BitmapFont.SMALLFONTS, 1, 0, 1, 0);
			if ((System.currentTimeMillis()-startCounter)/1000 > seconds)
			{
				connect();
				autologin = false;
			}
		}
		
		FontRenderer2.drawTextUsingSpecificFont(12, 12, 0, 32, "Copyright 2016 XolioWare Interactive", BitmapFont.SMALLFONTS);
	}
	
	/*public boolean handleKeypress(int k)
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
	}*/
	
	void connect()
	{
		if (usernameForm.text.equals("OFFLINE"))
		{
			Client.offline = true;
			Client.username = "OfflineUser" + (int) (Math.random() * 1000);
			gameWindow.setLayer(new MainMenuOverlay(gameWindow, this));
			//this.mainScene.changeOverlay(new MainMenuOverlay(mainScene, null));//eng.changeScene(new MainMenu(eng));
		}
		else
		{
			logging_in = true;
			new HttpRequestThread(this, "login", "http://chunkstories.xyz/api/login.php", "user=" + usernameForm.text + "&pass=" + passwordForm.text);
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
				Client.username = usernameForm.text;
				Client.session_key = session;
				Client.getConfig().setString("autologin", "ok");
				Client.getConfig().setString("user", usernameForm.text);
				Client.getConfig().setString("pass", passwordForm.text);
				
				if(Client.username.equals("Gobrosse") || Client.username.equals("kektest"))
				{
					RenderingConfig.isDebugAllowed = true;
				}
				
				//If the user didn't opt-out, look for crash files and upload those
				if(Client.clientConfig.getProp("log-policy", "undefined").equals("send"))
				{
					JavaCrashesUploader t = new JavaCrashesUploader(Client.getInstance());
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
