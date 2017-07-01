package io.xol.chunkstories.gui.overlays.config;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.gui.elements.Button;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class LogPolicyAsk extends Layer
{
	//GuiElementsHandler guiHandler = new GuiElementsHandler();
	Button acceptButton = new Button(this, 0, 0, 300, 32, ("#{logpolicy.accept}"), BitmapFont.SMALLFONTS, 1);
	Button refuseButton = new Button(this, 0, 0, 300, 32, ("#{logpolicy.deny}"), BitmapFont.SMALLFONTS, 1);
	
	String message = Client.getInstance().getContent().localization().getLocalizedString("logpolicy.asktext");
	
	public LogPolicyAsk(GameWindow gameWindow, Layer parent)
	{
		super(gameWindow, parent);
		
		this.acceptButton.setAction(new Runnable() {

			@Override
			public void run() {
				Client.getInstance().configDeprecated().setString("log-policy", "send");
				Client.getInstance().configDeprecated().save();
				gameWindow.setLayer(parentLayer);
			}
			
		});
		
		this.refuseButton.setAction(new Runnable() {

			@Override
			public void run() {
				Client.getInstance().configDeprecated().setString("log-policy", "dont");
				Client.getInstance().configDeprecated().save();
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
		
		//ObjectRenderer.renderColoredRect(renderingContext.getWindow().getWidth() / 2, renderingContext.getWindow().getHeight() / 2, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight(), 0, "000000", 0.5f);
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(0, 0, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4fm(0.0, 0.0, 0.0, 0.5));
		
		FontRenderer2.drawTextUsingSpecificFont(30, renderingContext.getWindow().getHeight()-64, 0, 64, Client.getInstance().getContent().localization().getLocalizedString("logpolicy.title"), BitmapFont.SMALLFONTS);
		
		int linesTaken = renderingContext.getFontRenderer().defaultFont().getLinesHeight(message, (width-128) / 2 );
		float scaling = 2;
		if(linesTaken*32 > height)
			scaling  = 1f;
		
		renderingContext.getFontRenderer().drawString(renderingContext.getFontRenderer().defaultFont(), 30, renderingContext.getWindow().getHeight()-128, message, scaling, width-128);
		
		//FontRenderer2.drawTextUsingSpecificFont(30, 100, 0, 32, message, BitmapFont.SMALLFONTS);
		//FontRenderer2.setLengthCutoff(false, width - 128);
		
		acceptButton.setPosition(renderingContext.getWindow().getWidth()/2 - 256, renderingContext.getWindow().getHeight() / 4 - 32);
		acceptButton.render(renderingContext);
		
		refuseButton.setPosition(renderingContext.getWindow().getWidth()/2 + 256, renderingContext.getWindow().getHeight() / 4 - 32);
		refuseButton.render(renderingContext);
	}
}
