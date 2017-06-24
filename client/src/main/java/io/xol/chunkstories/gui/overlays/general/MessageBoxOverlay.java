package io.xol.chunkstories.gui.overlays.general;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.gui.elements.Button;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MessageBoxOverlay extends Layer
{
	Button okButton = new Button(this, 0, 0, 300, 32, ("Ok"), BitmapFont.SMALLFONTS, 1);
	String message;
	
	public MessageBoxOverlay(GameWindow scene, Layer parent, String message)
	{
		super(scene, parent);
		// Gui buttons
		this.message = message;
		this.okButton.setAction(new Runnable() {

			@Override
			public void run() {
				gameWindow.setLayer(parentLayer);
			}
			
		});
		
		elements.add(okButton);
	}

	@Override
	public void render(RenderingInterface renderingContext)
	{
		int dekal = renderingContext.getFontRenderer().defaultFont().getWidth(message) / 2;
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().defaultFont(), renderingContext.getWindow().getWidth()/2-dekal*1.5f, renderingContext.getWindow().getHeight() / 2 + 64, message, 3f, 3f, new Vector4fm(1,0.2f,0.2f,1));
		
		okButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight() / 2 - 32);
		okButton.render(renderingContext);
	}
}
