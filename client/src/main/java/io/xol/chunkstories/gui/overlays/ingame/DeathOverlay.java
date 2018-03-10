//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.overlays.ingame;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.math.HexTools;
import org.joml.Vector4f;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.chunkstories.world.WorldClientRemote;
import io.xol.engine.gui.elements.Button;

public class DeathOverlay extends Layer
{;
	
	Button respawnButton = new Button(this, 0, 0, 320, "#{ingame.respawn}");
	Button exitButton = new Button(this, 0, 0, 320, "#{ingame.exit}");

	public DeathOverlay(GameWindow scene, Layer parent)
	{
		super(scene, parent);
		
		this.respawnButton.setAction(new Runnable() {

			@Override
			public void run() {
				//TODO this has no functionality whatsoever for local worlds yet
				if(Client.getInstance().getWorld() instanceof WorldClientRemote)
					((WorldClientRemote) Client.getInstance().getWorld()).getConnection().sendTextMessage("world/respawn");
				else
					((WorldClientLocal) Client.getInstance().getWorld()).spawnPlayer(Client.getInstance().getPlayer());

				gameWindow.setLayer(parentLayer);
			}
			
		});
		
		this.exitButton.setAction(new Runnable() {

			@Override
			public void run() {
				Client.getInstance().exitToMainMenu();
			}
			
		});
		
		elements.add(respawnButton);
		elements.add(exitButton);
	}

	@Override
	public void render(RenderingInterface renderer)
	{
		parentLayer.render(renderer);
		
		renderer.getGuiRenderer().drawBoxWindowsSpace(0, 0, renderer.getWindow().getWidth(), renderer.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));
		
		String color = "#";
		color += HexTools.intToHex((int) (Math.random() * 255));
		color += HexTools.intToHex((int) (Math.random() * 255));
		color += HexTools.intToHex((int) (Math.random() * 255));

		Font font = renderer.getFontRenderer().getFont("LiberationSans-Regular", 11);
		/*
		renderer.getFontRenderer().drawStringWithShadow(font, , , , 3, 3, new Vector4f(1));
		*/

		renderer.getFontRenderer().drawStringWithShadow(font, renderer.getWindow().getWidth() / 2 - font.getWidth("YOU DIEDED") * 3f, renderer.getWindow().getHeight() / 2 + 48 * 3, "#FF0000YOU DIEDED" , 6, 6, new Vector4f(1));
		renderer.getFontRenderer().drawStringWithShadow(font, renderer.getWindow().getWidth() / 2 - font.getWidth("git gud scrub") * 1.5f, renderer.getWindow().getHeight() / 2 + 36 * 3 , color + "git gud scrub", 3, 3, new Vector4f(1));
		
		respawnButton.setPosition(renderer.getWindow().getWidth()/2, renderer.getWindow().getHeight()/2 + 48);
		exitButton.setPosition(renderer.getWindow().getWidth()/2, renderer.getWindow().getHeight()/2 - 24);
		
		respawnButton.render(renderer);
		exitButton.render(renderer);
		
		//When the new entity arrives
		if(Client.getInstance().getPlayer().getControlledEntity() != null)
			gameWindow.setLayer(parentLayer);
		
		//Make sure to ungrab the mouse
		Mouse mouse = gameWindow.getInputsManager().getMouse();
		if(mouse.isGrabbed())
			mouse.setGrabbed(false);
	}
}
