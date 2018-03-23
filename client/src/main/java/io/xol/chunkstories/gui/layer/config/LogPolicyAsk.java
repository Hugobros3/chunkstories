//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.config;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.BaseButton;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.client.Client;

public class LogPolicyAsk extends Layer
{
	BaseButton acceptButton = new BaseButton(this, 0, 0, 150, "#{logpolicy.accept}");
	BaseButton refuseButton = new BaseButton(this, 0, 0, 150, "#{logpolicy.deny}");
	
	String message = Client.getInstance().getContent().localization().getLocalizedString("logpolicy.asktext");
	
	public LogPolicyAsk(GameWindow gameWindow, Layer parent)
	{
		super(gameWindow, parent);
		
		this.acceptButton.setAction(new Runnable() {

			@Override
			public void run() {
				Client.getInstance().getConfiguration().getOption("client.game.log-policy").trySetting("send");
				Client.getInstance().getConfiguration().save();
				gameWindow.setLayer(parentLayer);
			}
			
		});
		
		this.refuseButton.setAction(new Runnable() {

			@Override
			public void run() {
				Client.getInstance().getConfiguration().getOption("client.game.log-policy").trySetting("dont");
				Client.getInstance().getConfiguration().save();
				gameWindow.setLayer(parentLayer);
			}
			
		});
		
		elements.add(acceptButton);
		elements.add(refuseButton);
	}
	
	@Override
	public void render(RenderingInterface renderer)
	{
		parentLayer.render(renderer);
		float scale = gameWindow.getGuiScale();
		
		renderer.getGuiRenderer().drawBoxWindowsSpace(0, 0, renderer.getWindow().getWidth(), renderer.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));
		
		renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().getFont("LiberationSans-Regular__aa", 16 * scale),
				30, renderer.getWindow().getHeight()-64, Client.getInstance().getContent().localization().getLocalizedString("logpolicy.title"), 1, 1, new Vector4f(1));
		
		Font logPolicyTextFont = renderer.getFontRenderer().getFont("LiberationSans-Regular__aa", 12 * scale);
		
		
		
		renderer.getFontRenderer().drawString(logPolicyTextFont, 30, renderer.getWindow().getHeight()-128, message, 1, width-60);
		
		float seperation = 4 * scale;
		float groupSize = acceptButton.getWidth() + refuseButton.getWidth() + seperation;
		
		acceptButton.setPosition(renderer.getWindow().getWidth()/2 - groupSize / 2, renderer.getWindow().getHeight() / 4 - 32);
		acceptButton.render(renderer);
		
		refuseButton.setPosition(renderer.getWindow().getWidth()/2 - groupSize / 2 + seperation + acceptButton.getWidth(), renderer.getWindow().getHeight() / 4 - 32);
		refuseButton.render(renderer);
	}
}
