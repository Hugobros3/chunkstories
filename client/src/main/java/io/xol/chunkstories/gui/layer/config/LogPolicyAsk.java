//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.config;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.elements.Button;

public class LogPolicyAsk extends Layer
{
	Button acceptButton = new Button(this, 0, 0, 300, "#{logpolicy.accept}");
	Button refuseButton = new Button(this, 0, 0, 300, "#{logpolicy.deny}");
	
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
	public void render(RenderingInterface renderingContext)
	{
		parentLayer.render(renderingContext);
		
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(0, 0, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));
		
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("LiberationSans-Regular", 11), 30, renderingContext.getWindow().getHeight()-64, Client.getInstance().getContent().localization().getLocalizedString("logpolicy.title"), 3, 3, new Vector4f(1));
		
		int linesTaken = renderingContext.getFontRenderer().defaultFont().getLinesHeight(message, (width-128) / 2 );
		float scaling = 2;
		if(linesTaken*32 > height)
			scaling  = 1f;
		
		renderingContext.getFontRenderer().drawString(renderingContext.getFontRenderer().defaultFont(), 30, renderingContext.getWindow().getHeight()-128, message, scaling, width-128);
		
		acceptButton.setPosition(renderingContext.getWindow().getWidth()/2 - 256, renderingContext.getWindow().getHeight() / 4 - 32);
		acceptButton.render(renderingContext);
		
		refuseButton.setPosition(renderingContext.getWindow().getWidth()/2 + 256, renderingContext.getWindow().getHeight() / 4 - 32);
		refuseButton.render(renderingContext);
	}
}
