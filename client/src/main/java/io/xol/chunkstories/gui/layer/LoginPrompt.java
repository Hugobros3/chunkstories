//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer;

import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.client.ClientImplementation;
import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.FocusableGuiElement;
import io.xol.chunkstories.api.gui.GuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.BaseButton;
import io.xol.chunkstories.api.gui.elements.InputText;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.bugsreporter.JavaCrashesUploader;
import io.xol.chunkstories.client.ClientLimitations;
import io.xol.chunkstories.gui.layer.config.LanguageSelectionScreen;
import io.xol.chunkstories.net.http.RequestResultAction;
import io.xol.chunkstories.net.http.SimplePostRequest;
import io.xol.chunkstories.renderer.opengl.util.ObjectRenderer;

public class LoginPrompt extends Layer {
	InputText usernameForm = new InputText(this, 0, 0, 250);
	InputText passwordForm = new InputText(this, 0, 0, 250);

	BaseButton loginButton = new BaseButton(this, 0, 0, 64, "#{login.login}");

	boolean logging_in = false;
	boolean autologin = false;

	long startCounter = 0l;

	String message = "";

	private boolean can_next = false;
	private boolean failed_login;

	public LoginPrompt(Gui gui, Layer parent) {
		super(gui, parent);

		elements.add(usernameForm);
		passwordForm.setPassword(true);
		elements.add(passwordForm);
		elements.add(loginButton);

		// Autologin fills in the forms automagically
		// TODO Secure storage of password
		if (gui.getClient().getConfiguration().getStringOption("client.login.auto").equals("ok")) {
			usernameForm.setText(gui.getClient().getConfiguration().getStringOption("client.login.username"));
			passwordForm.setText(gui.getClient().getConfiguration().getStringOption("client.login.password"));
			autologin = true;
		}

		loginButton.setAction(() -> connect());

		this.setFocusedElement(usernameForm);
		startCounter = System.currentTimeMillis();
	}

	@Override
	public void render(RenderingInterface renderer) {
		parentLayer.render(renderer);
		float scale = this.getGuiScale();

		if (gui.getClient().getConfiguration().getStringOption("client.game.language").equals("undefined")) {
			gameWindow.setLayer(new LanguageSelectionScreen(gameWindow, this, false));
			// this.mainScene.changeOverlay(new LanguageSelectionScreen(mainScene, this,
			// false));
		}

		if (can_next)
			gameWindow.setLayer(new MainMenu(gameWindow, parentLayer));

		ObjectRenderer.renderTexturedRect(renderer.getWindow().getWidth() / 2,
				renderer.getWindow().getHeight() / 2 + 90 * scale, 256 * scale, 256 * scale, "./textures/logo.png");

		usernameForm.setPosition(renderer.getWindow().getWidth() / 2 - 125 * scale,
				renderer.getWindow().getHeight() / 2 + 16 * scale);
		usernameForm.render(renderer);
		passwordForm.setPosition(usernameForm.getPositionX(),
				usernameForm.getPositionY() - usernameForm.getHeight() - (20 + 4) * scale);
		passwordForm.render(renderer);

		loginButton.setPosition(usernameForm.getPositionX(), passwordForm.getPositionY() - 30 * scale);

		renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
				usernameForm.getPositionX(), usernameForm.getPositionY() + usernameForm.getHeight() + 4 * scale,
				gui.getClient().getContent().localization().localize("#{login.username}"), scale, scale,
				new Vector4f(1.0f));
		renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
				passwordForm.getPositionX(), passwordForm.getPositionY() + usernameForm.getHeight() + 4 * scale,
				gui.getClient().getContent().localization().localize("#{login.password}"), scale, scale,
				new Vector4f(1.0f));

		if (logging_in) {
			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
					renderer.getWindow().getWidth() / 2 - 230, renderer.getWindow().getHeight() / 2 - 90,
					gui.getClient().getContent().localization().localize("#{login.loggingIn}"), scale, scale,
					new Vector4f(1.0f));
		} else {
			float decal_lb = loginButton.getWidth();
			loginButton.render(renderer);

			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
					loginButton.getPositionX() + 4 * scale + decal_lb, loginButton.getPositionY() + 2 * scale,
					gui.getClient().getContent().localization().localize("#{login.register}"), scale, scale,
					new Vector4f(1.0f));

			if (failed_login)
				renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
						renderer.getWindow().getWidth() / 2 - 250, renderer.getWindow().getHeight() / 2 - 160, message,
						scale, scale, new Vector4f(1.0f, 0.0f, 0.0f, 1.0f));
		}

		if (autologin) {
			int seconds = 10;
			String autologin2 = gui.getClient().getContent().localization().localize("#{login.auto1} "
					+ (seconds - (System.currentTimeMillis() - startCounter) / 1000) + " #{login.auto2}");

			float autologinLength = renderer.getFontRenderer().defaultFont().getWidth(autologin2) * 2.0f;
			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
					renderer.getWindow().getWidth() / 2 - autologinLength / 2,
					renderer.getWindow().getHeight() / 2 - 170, autologin2, 2, 2, new Vector4f(0.0f, 1.0f, 0.0f, 1.0f));
			if ((System.currentTimeMillis() - startCounter) / 1000 > seconds) {
				connect();
				autologin = false;
			}
		}

		renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), 12, 12,
				"2015-2018 XolioWare Interactive", scale, scale, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
	}

	private void connect() {
		if (usernameForm.getText().equals("OFFLINE")) {
			ClientImplementation.setOffline(true);
			ClientImplementation.setUsername("OfflineUser" + (int) (Math.random() * 1000));
			gameWindow.setLayer(new MainMenu(gameWindow, parentLayer));
		} else {
			logging_in = true;

			RequestResultAction postAction = (result) -> {
				gameWindow.getClient().logger().debug("Received login answer");

				logging_in = false;
				if (result == null) {
					failed_login = true;
					message = "Can't connect to server.";
					return;
				}
				if (result.startsWith("ok")) {
					String session = result.split(":")[1];
					ClientImplementation.setUsername(usernameForm.getText());
					ClientImplementation.setSession_key(session);
					gui.getClient().getConfiguration().getOption("client.login.auto").trySetting("ok");
					gui.getClient().getConfiguration().getOption("client.login.username")
							.trySetting(usernameForm.getText());
					gui.getClient().getConfiguration().getOption("client.login.password")
							.trySetting(passwordForm.getText());

					if (ClientImplementation.getUsername().equals("Gobrosse") || ClientImplementation.getUsername().equals("kektest")) {
						ClientLimitations.isDebugAllowed = true;
					}

					// If the user didn't opt-out, look for crash files and upload those
					if (gui.getClient().getConfiguration().getStringOption("client.game.log-policy")
							.equals("send")) {
						JavaCrashesUploader t = new JavaCrashesUploader(gui.getClient());
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
