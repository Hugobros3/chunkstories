//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.gui.elements.Button;

public class MessageBox extends Layer
{
	Button okButton = new Button(this, 0, 0, 300, "#{menu.ok}");
	String message;
	
	public MessageBox(GameWindow scene, Layer parent, String message)
	{
		super(scene, parent);
		//Thread.dumpStack();
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
		parentLayer.render(renderingContext);
		
		float dekal = renderingContext.getFontRenderer().defaultFont().getWidth(message);
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().defaultFont(), renderingContext.getWindow().getWidth()/2-dekal*1.5f, renderingContext.getWindow().getHeight() / 2 + 64, message, 3f, 3f, new Vector4f(1,0.2f,0.2f,1));
		
		okButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight() / 2 - 32);
		okButton.render(renderingContext);
	}
}
