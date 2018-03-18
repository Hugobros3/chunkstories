//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.overlays;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.FocusableGuiElement;
import io.xol.chunkstories.api.gui.GuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.bugsreporter.JavaCrashesUploader;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.gui.elements.Button;
import io.xol.chunkstories.gui.elements.InputText;
import io.xol.chunkstories.gui.overlays.config.LanguageSelectionScreen;
import io.xol.chunkstories.net.http.HttpRequestThread;
import io.xol.chunkstories.net.http.HttpRequester;
import io.xol.chunkstories.renderer.opengl.util.ObjectRenderer;

public class LoginOverlay extends Layer implements HttpRequester
{
	InputText usernameForm = new InputText(this, 0, 0, 500);
	InputText passwordForm = new InputText(this, 0, 0, 500);
	
	Button loginButton = new Button(this, 0, 0, 128, "#{login.login}");
	
	public LoginOverlay(GameWindow scene, Layer parent)
	{
		super(scene, parent);
		
		elements.add(usernameForm);
		elements.add(passwordForm);
		elements.add(loginButton);
		
		//Autologin fills in the forms automagically
		//TODO Secure storage of password
		if (Client.getInstance().getConfiguration().getStringOption("client.login.auto").equals("ok"))
		{
			usernameForm.setText(Client.getInstance().getConfiguration().getStringOption("client.login.username"));
			passwordForm.setText(Client.getInstance().getConfiguration().getStringOption("client.login.password"));
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
	public void render(RenderingInterface renderer)
	{
		parentLayer.render(renderer);
		
		if(Client.getInstance().getConfiguration().getStringOption("client.game.language").equals("undefined"))
		{
			gameWindow.setLayer(new LanguageSelectionScreen(gameWindow, this, false));
			//this.mainScene.changeOverlay(new LanguageSelectionScreen(mainScene, this, false));
		}
		
		if (can_next)
			gameWindow.setLayer(new MainMenuOverlay(gameWindow, parentLayer));
		
		ObjectRenderer.renderTexturedRect(renderer.getWindow().getWidth() / 2, renderer.getWindow().getHeight() / 2 + 180, 512, 512, "./textures/logo.png");

		loginButton.setPosition(usernameForm.getPositionX() + loginButton.getWidth() / 2f - 8, renderer.getWindow().getHeight() / 2 - 80);

		usernameForm.setPosition(renderer.getWindow().getWidth() / 2 - 250f, renderer.getWindow().getHeight() / 2 + 40);
		usernameForm.drawWithBackGround(renderer);
		passwordForm.setPosition(renderer.getWindow().getWidth() / 2 - 250f, renderer.getWindow().getHeight() / 2 - 40);
		passwordForm.drawWithBackGroundPassworded(renderer);

		renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), renderer.getWindow().getWidth() / 2 - 250, renderer.getWindow().getHeight() / 2 + 74, Client.getInstance().getContent().localization().localize("#{login.username}"), 2, 2, new Vector4f(1.0f));
		renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), renderer.getWindow().getWidth() / 2 - 250, renderer.getWindow().getHeight() / 2 - 6, Client.getInstance().getContent().localization().localize("#{login.password}"), 2, 2, new Vector4f(1.0f));
		
		if (logging_in)
		{
			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), renderer.getWindow().getWidth() / 2 - 230, renderer.getWindow().getHeight() / 2 - 90, Client.getInstance().getContent().localization().localize("#{login.loggingIn}"), 2, 2, new Vector4f(1.0f));
		}
		else
		{
			float decal_lb = loginButton.getWidth();
			loginButton.render(renderer);

			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), usernameForm.getPositionX() + 16 + decal_lb, renderer.getWindow().getHeight() / 2 - 95, Client.getInstance().getContent().localization().localize("#{login.register}"), 2, 2, new Vector4f(1.0f));
		
			if (failed_login)
				renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), renderer.getWindow().getWidth() / 2 - 250, renderer.getWindow().getHeight() / 2 - 160, message, 2, 2, new Vector4f(1.0f, 0.0f, 0.0f, 1.0f));
		}

		if (autologin)
		{
			int seconds = 10;
			String autologin2 = Client.getInstance().getContent().localization().localize("#{login.auto1} "+(seconds-(System.currentTimeMillis()-startCounter)/1000)+" #{login.auto2}");
			
			float autologinLength = renderer.getFontRenderer().defaultFont().getWidth(autologin2) * 2.0f;
			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), renderer.getWindow().getWidth() / 2 - autologinLength / 2, renderer.getWindow().getHeight() / 2 - 170, autologin2, 2, 2, new Vector4f(0.0f, 1.0f, 0.0f, 1.0f));
			if ((System.currentTimeMillis()-startCounter)/1000 > seconds)
			{
				connect();
				autologin = false;
			}
		}
		
		renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), 12, 12 , "2015-2018 XolioWare Interactive", 2, 2, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
	}
	
	void connect()
	{
		if (usernameForm.text.equals("OFFLINE"))
		{
			Client.offline = true;
			Client.username = "OfflineUser" + (int) (Math.random() * 1000);
			gameWindow.setLayer(new MainMenuOverlay(gameWindow, parentLayer));
			//this.mainScene.changeOverlay(new MainMenuOverlay(mainScene, null));//eng.changeScene(new MainMenu(eng));
		}
		else
		{
			logging_in = true;
			new HttpRequestThread(this, "login", "http://chunkstories.xyz/api/login.php", "user=" + usernameForm.text + "&pass=" + passwordForm.text);
		}
	}
	
	@Override
	public boolean handleInput(Input input) {
		if(input.equals("exit"))
			autologin = false;
		else if(input.equals("enter"))
			connect();
		else if(input.equals("tab")) {
			int shift = gameWindow.getInputsManager().getInputByName("shift").isPressed() ? -1 : 1;
			int i = this.elements.indexOf(this.focusedElement);
			
			GuiElement elem = null;
			
			while(elem == null || !(elem instanceof FocusableGuiElement)) {
				i += shift;
				if(i < 0)
					i = this.elements.size();
				if(i >= this.elements.size())
					i = 0;
				
				elem = this.elements.get(i);
			}
			
			if(elem != null)
				this.focusedElement = (FocusableGuiElement) elem;
		}
		
		return super.handleInput(input);
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
				Client.getInstance().getConfiguration().getOption("client.login.auto").trySetting("ok");
				Client.getInstance().getConfiguration().getOption("client.login.username").trySetting(usernameForm.text);
				Client.getInstance().getConfiguration().getOption("client.login.password").trySetting(passwordForm.text);
				
				if(Client.username.equals("Gobrosse") || Client.username.equals("kektest"))
				{
					RenderingConfig.isDebugAllowed = true;
				}
				
				//If the user didn't opt-out, look for crash files and upload those
				if(Client.getInstance().getConfiguration().getStringOption("client.game.log-policy").equals("send"))
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
