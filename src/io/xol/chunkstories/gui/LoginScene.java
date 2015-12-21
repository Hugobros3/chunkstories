package io.xol.chunkstories.gui;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.SkyDome;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;
import io.xol.engine.gui.ClickableButton;
import io.xol.engine.gui.FocusableObjectsHandler;
import io.xol.engine.gui.InputText;
import io.xol.engine.misc.HttpRequestThread;
import io.xol.engine.scene.Scene;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class LoginScene extends Scene implements io.xol.engine.misc.HttpRequester
{
	FocusableObjectsHandler guiHandler = new FocusableObjectsHandler();

	boolean logging_in = false;
	boolean autologin = false;
	
	long startCounter = 0l;

	String message = "";

	private boolean can_next = false;
	// 3d shit thing
	SkyDome dome = new SkyDome();
	Camera cam = new Camera();

	private boolean failed_login;

	public LoginScene(XolioWindow XolioWindow)
	{
		super(XolioWindow);
		// login
		guiHandler.add(new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS));
		guiHandler.getInputText(0).focus = true;
		// pass
		guiHandler.add(new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS));
		// ok
		guiHandler.add(new ClickableButton(0, 0, 128, 32, ("Login"), BitmapFont.SMALLFONTS, 1));
		// check for auto-login dataaa
		if (Client.getConfig().getProp("autologin", "ko").equals("ok"))
		{
			guiHandler.getInputText(0).setText(Client.getConfig().getProp("user", ""));
			guiHandler.getInputText(1).setText(Client.getConfig().getProp("pass", ""));
			autologin = true;
		}
		startCounter = System.currentTimeMillis();
	}

	float p = 1f;
	float a = 0f;

	public void update()
	{
		// System.out.println(oggStream.isPlaying()+":"+oggStream.getPosition());
		/*
		 * if(!oggStream.isPlaying() || SoundManager.shouldReOpenStreams()) {
		 * try { oggStream = AudioLoader.getStreamingAudio("OGG", new
		 * URL("http://127.0.0.1:8080/radio.ogg")); } catch (IOException e) { //
		 * TODO Auto-generated catch block e.printStackTrace(); }
		 * //System.out.println("not playing :(");
		 * oggStream.playAsSoundEffect(1, 1, true); }
		 */
		// car_engine.setPosition3dSpace((float) (Math.sin(a/58)*5), 0, (float)
		// (Math.cos(a/58)*5));
		a++;
		// car_engine.setVolume((float) (0.5f+Math.max(0,Math.sin(a/180)))*2);
		if (can_next)
		{
			this.eng.changeScene(new MainMenu(eng));
		}
		// 3d display
		XolioWindow.setup3d();
		cam.justSetup();
		// cam.update();
		cam.view_rotx = 27;
		cam.view_roty = -30;
		dome.time = 0.15f;
		dome.render(cam);
		// 2d part
		//XolioWindow.setup2d();
		if (guiHandler.getButton(2).clicked)
		{
			guiHandler.getButton(2).clicked = false;
			connect();
		}

		// ObjectRenderer.renderTexturedRect(XolioWindow.frameW /
		// 2,XolioWindow.frameH / 2, XolioWindow.frameW, XolioWindow.frameH,0f,
		// 0f, XolioWindow.frameW / 2, XolioWindow.frameH / 2,
		// 128f,"gui/loginbg");

		ObjectRenderer.renderTexturedRect(XolioWindow.frameW / 2, XolioWindow.frameH / 2 + 180, 512, 512, "logo");

		guiHandler.getButton(2).setPos(XolioWindow.frameW / 2 - 245 + 58, XolioWindow.frameH / 2 - 80);

		guiHandler.getInputText(0).setPos(XolioWindow.frameW / 2 - 250, XolioWindow.frameH / 2 + 40);
		guiHandler.getInputText(0).drawWithBackGround();
		guiHandler.getInputText(1).setPos(XolioWindow.frameW / 2 - 250, XolioWindow.frameH / 2 - 40);
		guiHandler.getInputText(1).drawWithBackGroundPassworded();

		FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW / 2 - 250, XolioWindow.frameH / 2 + 80, 0, 32, "Username", BitmapFont.SMALLFONTS);
		FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW / 2 - 250, XolioWindow.frameH / 2 + 0, 0, 32, "Password", BitmapFont.SMALLFONTS);

		if (logging_in)
		{
			FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW / 2 - 230, XolioWindow.frameH / 2 - 90, 0, 32, "Logging in...", BitmapFont.SMALLFONTS);
		}
		else
		{
			int decal_lb = guiHandler.getButton(2).draw();

			FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW / 2 - 245 - 58 + decal_lb, XolioWindow.frameH / 2 - 95, 0, 32, "Register at http://chunkstories.xyz", BitmapFont.SMALLFONTS);
			// FontRenderer2.drawTextUsingSpecificFont(XolioWindow.frameW / 2 -
			// 250, XolioWindow.frameH / 2 - 150 + 18, 0, 32,
			// "You currently need hugobros3 to provide you an account.",
			// BitmapFont.SMALLFONTS);
			if (failed_login)
				FontRenderer2.drawTextUsingSpecificFontRVBA(XolioWindow.frameW / 2 - 250, XolioWindow.frameH / 2 - 160, 0, 32, message, BitmapFont.SMALLFONTS, 1, 1, 0, 0);
		}

		if (autologin)
		{
			int seconds = 10;
			String autologin2 = "Autologin in "+(seconds-(System.currentTimeMillis()-startCounter)/1000)+" seconds...";
			FontRenderer2.drawTextUsingSpecificFontRVBA(XolioWindow.frameW / 2 - FontRenderer2.getTextLengthUsingFont(32, autologin2, BitmapFont.SMALLFONTS) / 2, XolioWindow.frameH / 2 - 170, 0, 32, autologin2, BitmapFont.SMALLFONTS, 1, 0, 1, 0);
			if ((System.currentTimeMillis()-startCounter)/1000 > seconds)
			{
				connect();
				autologin = false;
			}
		}

		FontRenderer2.drawTextUsingSpecificFont(12, 12, 0, 32, "Copyright 2015 XolioWare Interactive", BitmapFont.SMALLFONTS);

		super.update();
	}

	public boolean onKeyPress(int k)
	{
		Client.getSoundManager().playSoundEffect("shoot.ogg");
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

	void connect()
	{
		if (guiHandler.getInputText(0).text.equals("OFFLINE"))
		{
			Client.offline = true;
			Client.username = "OfflineUser" + (int) (Math.random() * 1000);
			this.eng.changeScene(new MainMenu(eng));
		}
		else
		{
			logging_in = true;
			new HttpRequestThread(this, "login", "http://chunkstories.xyz/api/login.php", "user=" + guiHandler.getInputText(0).text + "&pass=" + guiHandler.getInputText(1).text).start();

		}
	}

	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}

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
				Client.getConfig().setProp("autologin", "ok");
				Client.getConfig().setProp("user", guiHandler.getInputText(0).text);
				Client.getConfig().setProp("pass", guiHandler.getInputText(1).text);
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
