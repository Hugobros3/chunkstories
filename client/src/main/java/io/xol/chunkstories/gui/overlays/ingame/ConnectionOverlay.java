package io.xol.chunkstories.gui.overlays.ingame;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.net.ClientSideConnectionSequence;
import io.xol.engine.gui.elements.Button;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ConnectionOverlay extends Layer
{
	ClientSideConnectionSequence connectionSequence;
	
	Button exitButton = new Button(this, 0, 0, 320, "#{connection.cancel}");
	
	public ConnectionOverlay(GameWindow scene, Layer parent, String ip, int port)
	{
		super(scene, parent);
		connectionSequence = new ClientSideConnectionSequence( ip,  port);
		
		this.exitButton.setAction(new Runnable() {
			@Override
			public void run() {
				Client.getInstance().exitToMainMenu();
			}
		});
		
		elements.add(exitButton);
	}

	@Override
	public void render(RenderingInterface renderingContext)
	{
		parentLayer.getRootLayer().render(renderingContext);
		
		String color = "#606060";
		//color += HexTools.intToHex((int) (Math.random() * 255));
		//color += HexTools.intToHex((int) (Math.random() * 255));
		//color += HexTools.intToHex((int) (Math.random() * 255));
		
		Font font = renderingContext.getFontRenderer().getFont("arial", 11);
		
		String connection = "Connecting, please wait";
		
		renderingContext.getFontRenderer().drawStringWithShadow(font, renderingContext.getWindow().getWidth() / 2 - font.getWidth(connection) * 1.5f, 
				renderingContext.getWindow().getHeight() / 2 + 48 * 3, connection, 3, 3, new Vector4f(1));
		
		String currentConnectionStep = connectionSequence.getStatus().getStepText();

		renderingContext.getFontRenderer().drawStringWithShadow(font, renderingContext.getWindow().getWidth() / 2 - font.getWidth(currentConnectionStep) * 1.5f, 
				renderingContext.getWindow().getHeight() / 2 + 32 * 3, color + currentConnectionStep, 3, 3, new Vector4f(1));
		
		exitButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 - 24);
		
		exitButton.render(renderingContext);
		
		//Once the connection sequence is done, we hide this overlay
		if(connectionSequence.isDone())
			this.gameWindow.setLayer(parentLayer);
		
		String fail = connectionSequence.hasFailed();
		if(fail != null)
			Client.getInstance().exitToMainMenu(fail);
	}

	public void setParentScene(Layer layer) {
		this.parentLayer = layer;
	}
}
