//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.config;

import io.xol.chunkstories.api.gui.Font;
import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.api.util.Configuration;
import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.Button;

/** Asks the user if he wishes to have his logs uploaded to the game servers for debugging purposes */
//TODO anonymize those (strip C:\Users\... and such)
public class LogPolicyAsk extends Layer {
	public static final String logPolicyConfigNode = "client.game.logPolicy";

	private Configuration.OptionString option = gui.getClient().getConfiguration().get(logPolicyConfigNode);

	private Button acceptButton = new Button(this, 0, 0, 150, "#{logpolicy.accept}");
	private Button refuseButton = new Button(this, 0, 0, 150, "#{logpolicy.deny}");

	private String logPolicyExplanationText = gui.localization().getLocalizedString("logpolicy.asktext");

	public LogPolicyAsk(Gui gui, Layer parent) {
		super(gui, parent);

		this.acceptButton.setAction(new Runnable() {

			@Override
			public void run() {
				option.trySetting("send");
				gui.getClient().getConfiguration().save();
				LogPolicyAsk.this.gui.setTopLayer(parentLayer);
			}

		});

		this.refuseButton.setAction(new Runnable() {

			@Override
			public void run() {
				option.trySetting("dont");
				gui.getClient().getConfiguration().save();
				LogPolicyAsk.this.gui.setTopLayer(parentLayer);
			}

		});

		elements.add(acceptButton);
		elements.add(refuseButton);
	}

	@Override
	public void render(GuiDrawer drawer) {
		parentLayer.render(drawer);

		drawer.drawBox(0, 0, gui.getViewportWidth(), gui.getViewportHeight(), 0, 0, 0, 0, null, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));

		drawer.drawStringWithShadow(
				drawer.getFonts().getFont("LiberationSans-Regular__aa", 16 * 1), 30,
				gui.getViewportHeight() - 64,
				gui.getClient().getContent().localization().getLocalizedString("logpolicy.title"), -1,
				new Vector4f(1.0F));

		Font logPolicyTextFont = drawer.getFonts().getFont("LiberationSans-Regular__aa", 12);

		drawer.drawString(logPolicyTextFont, 30, gui.getViewportHeight() - 128, logPolicyExplanationText, width - 60, new Vector4f(1.0F));

		int buttonsSpacing = 4;
		int buttonsPlusSpacingLength = acceptButton.getWidth() + refuseButton.getWidth() + buttonsSpacing;

		acceptButton.setPosition(gui.getViewportWidth() / 2 - buttonsPlusSpacingLength / 2,
				gui.getViewportHeight() / 4 - 32);
		acceptButton.render(drawer);

		refuseButton.setPosition(
				gui.getViewportWidth() / 2 - buttonsPlusSpacingLength / 2 + buttonsSpacing + acceptButton.getWidth(),
				gui.getViewportHeight() / 4 - 32);
		refuseButton.render(drawer);
	}
}
