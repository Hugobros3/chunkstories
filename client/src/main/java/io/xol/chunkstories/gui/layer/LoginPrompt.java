//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer;

import io.xol.chunkstories.api.gui.*;
import io.xol.chunkstories.client.ClientImplementation;
import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.elements.Button;
import io.xol.chunkstories.api.gui.elements.InputText;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.bugsreporter.JavaCrashesUploader;
import io.xol.chunkstories.gui.layer.config.LanguageSelectionScreen;
import io.xol.chunkstories.net.http.RequestResultAction;
import io.xol.chunkstories.net.http.SimplePostRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginPrompt extends Layer {
	private InputText usernameForm = new InputText(this, 0, 0, 250);
	private InputText passwordForm = new InputText(this, 0, 0, 250);

	private Button loginButton = new Button(this, 0, 0, 64, "#{login.login}");

	private boolean logging_in = false;
	private boolean autologin = false;

	private long startCounter = 0l;

	private String message = "";

	private boolean can_next = false;
	private boolean failed_login;
	private final static Logger logger = LoggerFactory.getLogger("client.login");

	public LoginPrompt(Gui gui, Layer parent) {
		super(gui, parent);

		elements.add(usernameForm);
		passwordForm.setPassword(true);
		elements.add(passwordForm);
		elements.add(loginButton);

		// Autologin fills in the forms automagically
		// TODO Secure storage of password
		if (gui.getClient().getConfiguration().getValue("client.login.auto").equals("ok")) {
			/*usernameForm.setText(gui.getClient().getConfiguration().getValue("client.login.username"));
			passwordForm.setText(gui.getClient().getConfiguration().getValue("client.login.password"));*/
			autologin = true;
		}

		loginButton.setAction(this::connect);

		this.setFocusedElement(usernameForm);
		startCounter = System.currentTimeMillis();
	}

	@Override
	public void render(GuiDrawer drawer) {
		parentLayer.render(drawer);

		if (gui.getClient().getConfiguration().getValue("client.game.language").equals("undefined")) {
			gui.setTopLayer(new LanguageSelectionScreen(gui, this, false));
		}

		if (can_next)
			gui.setTopLayer(new MainMenu(gui, parentLayer));

		//TODO draw logo
		//ObjectRenderer.renderTexturedRect(gui.getViewportWidth() / 2, gui.getViewportHeight() / 2 + 90, 256, 256, "./textures/logo.png");

		usernameForm.setPosition(gui.getViewportWidth() / 2 - 125,
				gui.getViewportHeight() / 2 + 16);
		usernameForm.render(drawer);
		passwordForm.setPosition(usernameForm.getPositionX(),
				usernameForm.getPositionY() - usernameForm.getHeight() - (20 + 4));
		passwordForm.render(drawer);

		loginButton.setPosition(usernameForm.getPositionX(), passwordForm.getPositionY() - 30);

		drawer.drawStringWithShadow(drawer.getFonts().defaultFont(),
				usernameForm.getPositionX(), usernameForm.getPositionY() + usernameForm.getHeight() + 4,
				gui.getClient().getContent().localization().localize("#{login.username}"), -1,
				new Vector4f(1.0f));
		drawer.drawStringWithShadow(drawer.getFonts().defaultFont(),
				passwordForm.getPositionX(), passwordForm.getPositionY() + usernameForm.getHeight() + 4,
				gui.getClient().getContent().localization().localize("#{login.password}"), -1,
				new Vector4f(1.0f));

		if (logging_in) {
			drawer.drawStringWithShadow(drawer.getFonts().defaultFont(),
					gui.getViewportWidth() / 2 - 230, gui.getViewportHeight() / 2 - 90,
					gui.getClient().getContent().localization().localize("#{login.loggingIn}"), -1,
					new Vector4f(1.0f));
		} else {
			int decal_lb = loginButton.getWidth();
			loginButton.render(drawer);

			drawer.drawStringWithShadow(drawer.getFonts().defaultFont(),
					loginButton.getPositionX() + 4 + decal_lb, loginButton.getPositionY() + 2,
					gui.getClient().getContent().localization().localize("#{login.register}"), -1,
					new Vector4f(1.0f));

			if (failed_login)
				drawer.drawStringWithShadow(drawer.getFonts().defaultFont(),
						gui.getViewportWidth() / 2 - 250, gui.getViewportHeight() / 2 - 160, message,
						-1, new Vector4f(1.0f, 0.0f, 0.0f, 1.0f));
		}

		if (autologin) {
			int seconds = 10;
			String autologin2 = gui.getClient().getContent().localization().localize("#{login.auto1} " + (seconds - (System.currentTimeMillis() - startCounter) / 1000) + " #{login.auto2}");

			int autologinLength = drawer.getFonts().defaultFont().getWidth(autologin2) * 2;
			drawer.drawStringWithShadow(drawer.getFonts().defaultFont(2),
					gui.getViewportWidth() / 2 - autologinLength / 2,
					gui.getViewportHeight() / 2 - 170, autologin2, -1, new Vector4f(0.0f, 1.0f, 0.0f, 1.0f));
			if ((System.currentTimeMillis() - startCounter) / 1000 > seconds) {
				connect();
				autologin = false;
			}
		}

		drawer.drawStringWithShadow(drawer.getFonts().defaultFont(), 12, 12,
				"2015-2018 XolioWare Interactive", -1, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
	}

	private void connect() {
		if (usernameForm.getText().equals("OFFLINE")) {
			//TODO setAuthentificationMethod(new NullAuthentification());
			//ClientImplementation.setOffline(true);
			//ClientImplementation.setUsername("OfflineUser" + (int) (Math.random() * 1000));
			gui.setTopLayer(new MainMenu(gui, parentLayer));
		} else {
			logging_in = true;

			RequestResultAction postAction = (result) -> {
				logger.debug("Received login answer");

				logging_in = false;
				if (result == null) {
					failed_login = true;
					message = "Can't connect to server.";
					return;
				}
				if (result.startsWith("ok")) {
					String session = result.split(":")[1];

					//TODO setAuthentificationMethod(new ChunkstoriesXyzAuth());
					//ClientImplementation.setUsername(usernameForm.getText());
					//ClientImplementation.setSession_key(session);

					//TODO secure storage
					/*
					gui.getClient().getConfiguration().getOption("client.login.auto").trySetting("ok");
					gui.getClient().getConfiguration().getOption("client.login.username")
							.trySetting(usernameForm.getText());
					gui.getClient().getConfiguration().getOption("client.login.password")
							.trySetting(passwordForm.getText());*/

					// If the user didn't opt-out, look for crash files and upload those
					if (gui.getClient().getConfiguration().getValue("client.game.logPolicy").equals("send")) {
						JavaCrashesUploader t = new JavaCrashesUploader((ClientImplementation)gui.getClient());
						t.start();
					}

					can_next = true;
				} else if (result.startsWith("ko")) {
					failed_login = true;
					String reason = result.split(":")[1];
					if (reason.equals("notpremium"))
						message = ("User is not premium");
					else if (reason.equals("invalidcredentials"))
						message = ("Invalid credentials");
				} else {
					message = ("Unknown error");
				}
			};

			new SimplePostRequest("https://chunkstories.xyz/api/login.php",
					"user=" + usernameForm.getText() + "&pass=" + passwordForm.getText(), postAction);
		}
	}

	@Override
	public boolean handleInput(Input input) {
		if (input.equals("exit"))
			autologin = false;
		else if (input.equals("enter"))
			connect();
		else if (input.equals("tab")) {
			int shift = gui.getClient().getInputsManager().getInputByName("shift").isPressed() ? -1 : 1;
			int i = this.elements.indexOf(this.focusedElement);

			GuiElement elem = null;

			while (elem == null || !(elem instanceof FocusableGuiElement)) {
				i += shift;
				if (i < 0)
					i = this.elements.size();
				if (i >= this.elements.size())
					i = 0;

				elem = this.elements.get(i);
			}

			if (elem != null)
				this.focusedElement = (FocusableGuiElement) elem;
		}

		return super.handleInput(input);
	}
}
