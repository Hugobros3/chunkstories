package io.xol.chunkstories.gui.overlays.ingame;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldMaster;
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
	Button resumeButton = new Button(this, 0, 0, 320, "#{menu.resume}");
	Button optionsButton = new Button(this, 0, 0, 320, "#{menu.options}");
	Button modsButton = new Button(this, -100, 0, 320, "#{menu.mods}");
	Button exitButton = new Button(this, 0, 0, 320, "#{menu.backto}");
	
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
	public void render(RenderingInterface renderer)
	{
		parentLayer.render(renderer);
		
		FontRenderer2.drawTextUsingSpecificFont(renderer.getWindow().getWidth() / 2 - FontRenderer2.getTextLengthUsingFont(48, "#{ingame.pause}", BitmapFont.SMALLFONTS) / 2, renderer.getWindow().getHeight() / 2 + 48 * 3, 0, 48, "In-game menu", BitmapFont.SMALLFONTS);

		resumeButton.setPosition(renderer.getWindow().getWidth()/2, renderer.getWindow().getHeight()/2 + 48 * 2);
		optionsButton.setPosition(renderer.getWindow().getWidth()/2, renderer.getWindow().getHeight()/2 + 48 * 1);
		//modsButton.setPosition(renderingContext.getWindow().getWidth()/2, renderingContext.getWindow().getHeight()/2 + 48 * 0);
		exitButton.setPosition(renderer.getWindow().getWidth()/2, renderer.getWindow().getHeight()/2 - 48);
		
		resumeButton.render(renderer);
		optionsButton.render(renderer);
		//modsButton.draw();
		exitButton.render(renderer);
	}
	
	@Override
	public boolean handleInput(Input input)
	{
		if(input.equals("exit")) {
			gameWindow.setLayer(parentLayer);
			return true;
		}
		
		super.handleInput(input);
		
		return true;
	}
}
