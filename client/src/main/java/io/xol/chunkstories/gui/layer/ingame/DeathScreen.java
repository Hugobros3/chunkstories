//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.ingame;

import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.client.ClientImplementation;
import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.BaseButton;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.math.HexTools;
import io.xol.chunkstories.api.net.packets.PacketText;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.api.world.WorldClientNetworkedRemote;
import io.xol.chunkstories.api.world.WorldMaster;

/** The screen shown when you die */
public class DeathScreen extends Layer {
	private BaseButton respawnButton = new BaseButton(this, 0, 0, 160, "#{ingame.respawn}");
	private BaseButton exitButton = new BaseButton(this, 0, 0, 160, "#{ingame.exit}");

	public DeathScreen(Gui gui, Layer parent) {
		super(gui, parent);

		this.respawnButton.setAction(() -> {
			if (gameWindow.getClient().getWorld() instanceof WorldMaster)
				((WorldMaster) gameWindow.getClient().getWorld()).spawnPlayer(ClientImplementation.getInstance().getPlayer());
			else
				((WorldClientNetworkedRemote) gameWindow.getClient().getWorld()).getRemoteServer()
						.pushPacket(new PacketText("world/respawn"));

			gameWindow.setLayer(parentLayer);
		});

		this.exitButton.setAction(() -> gameWindow.getClient().exitToMainMenu());

		elements.add(respawnButton);
		elements.add(exitButton);
	}

	@Override
	public void render(GuiDrawer drawer) {
		parentLayer.render(drawer);

		drawer.getGuiRenderer().drawBoxWindowsSpace(0, 0, drawer.getWindow().getWidth(),
				drawer.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));

		String color = "#";
		color += HexTools.intToHex((int) (Math.random() * 255));
		color += HexTools.intToHex((int) (Math.random() * 255));
		color += HexTools.intToHex((int) (Math.random() * 255));

		Font font = drawer.getFontRenderer().getFont("LiberationSans-Regular", 11);

		drawer.getFontRenderer().drawStringWithShadow(font,
				drawer.getWindow().getWidth() / 2 - font.getWidth("YOU DIEDED") * 3f,
				drawer.getWindow().getHeight() / 2 + 48 * 3, "#FF0000YOU DIEDED", 6, 6, new Vector4f(1));
		drawer.getFontRenderer().drawStringWithShadow(font,
				drawer.getWindow().getWidth() / 2 - font.getWidth("git --gud scrub") * 1.5f,
				drawer.getWindow().getHeight() / 2 + 36 * 3, color + "git --gud scrub", 3, 3, new Vector4f(1));

		respawnButton.setPosition(drawer.getWindow().getWidth() / 2 - respawnButton.getWidth() / 2,
				drawer.getWindow().getHeight() / 2 + 48);
		exitButton.setPosition(drawer.getWindow().getWidth() / 2 - exitButton.getWidth() / 2,
				drawer.getWindow().getHeight() / 2 - 24);

		respawnButton.render(drawer);
		exitButton.render(drawer);

		// When the new entity arrives
		if (ClientImplementation.getInstance().getPlayer().getControlledEntity() != null)
			gameWindow.setLayer(parentLayer);

		// Make sure to ungrab the mouse
		Mouse mouse = gameWindow.getInputsManager().getMouse();
		if (mouse.isGrabbed())
			mouse.setGrabbed(false);
	}
}
