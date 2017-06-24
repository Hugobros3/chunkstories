package io.xol.chunkstories.gui.overlays.ingame;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.overlays.config.ModsSelectionOverlay;
import io.xol.chunkstories.gui.overlays.config.OptionsOverlay;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.gui.elements.Button;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PauseOverlay extends Layer
{
	Button resumeButton = new Button(this, 0, 0, 320, 32, "#{menu.resume}", BitmapFont.SMALLFONTS, 1);
	Button optionsButton = new Button(this, 0, 0, 320, 32, "#{menu.options}", BitmapFont.SMALLFONTS, 1);
	Button modsButton = new Button(this, -100, 0, 320, 32, "#{menu.mods}", BitmapFont.SMALLFONTS, 1);
	Button exitButton = new Button(this, 0, 0, 320, 32, "#{menu.backto}", BitmapFont.SMALLFONTS, 1);
	
	public PauseOverlay(GameWindow scene, Layer parent)
	{
		super(scene, parent);
		
		this.resumeButton.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(parentLayer);
			}
		});
		
		this.optionsButton.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(new OptionsOverlay(gameWindow, PauseOverlay.this));
			}
		});
		
		this.modsButton.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(new ModsSelectionOverlay(gameWindow, PauseOverlay.this));
			}
		});
		
		this.exitButton.setAction(new Runnable() {
			@Override
			public void run() {
				Client.getInstance().exitToMainMenu();
			}
		});
		
		elements.add(resumeButton);
		elements.add(optionsButton);
		elements.add(modsButton);
		elements.add(exitButton);
	}

	@Override
	public void render(RenderingInterface renderingContext)
	{
		parentLayer.render(renderingContext);
		
		FontRenderer2.drawTextUsingSpecificFont(renderingContext.getWindow().getWidth() / 2 - FontRenderer2.getTextLengthUsingFont(48, "#{ingame.pause}", BitmapFont.SMALLFONTS) / 2, renderingContext.getWindow().getHeight() / 2 + 48 * 3, 0, 48, "In-game menu", BitmapFont.SMALLFONTS);

		resumeButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 + 48 * 2);
		optionsButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 + 48 * 1);
		//modsButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 + 48 * 0);
		exitButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 - 48);
		
		resumeButton.render(renderingContext);
		optionsButton.render(renderingContext);
		//modsButton.draw();
		exitButton.render(renderingContext);
	}
	
	@Override
	public boolean handleInput(Input input)
	{
		if(input.equals("exit")) {
			gameWindow.setLayer(parentLayer);
			return true;
		}
		
		return super.handleInput(input);
	}
}
