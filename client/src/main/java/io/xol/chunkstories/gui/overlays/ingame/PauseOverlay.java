//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.overlays.ingame;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.overlays.config.ModsSelectionOverlay;
import io.xol.chunkstories.gui.overlays.config.OptionsOverlay;
import io.xol.engine.gui.elements.Button;

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
		
		Font font = renderer.getFontRenderer().getFont("LiberationSans-Regular", 11);
		String pauseText = renderer.getClient().getContent().localization().getLocalizedString("ingame.pause");
		renderer.getFontRenderer().drawStringWithShadow(font, renderer.getWindow().getWidth() / 2 - font.getWidth(pauseText) *1.5f, renderer.getWindow().getHeight() / 2 + 48 * 3, pauseText, 3, 3, new Vector4f(1));
		
		resumeButton.setPosition(renderer.getWindow().getWidth()/2, renderer.getWindow().getHeight()/2 + 48 * 2);
		optionsButton.setPosition(renderer.getWindow().getWidth()/2, renderer.getWindow().getHeight()/2 + 48 * 1);
		exitButton.setPosition(renderer.getWindow().getWidth()/2, renderer.getWindow().getHeight()/2 - 48);
		
		resumeButton.render(renderer);
		optionsButton.render(renderer);
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
