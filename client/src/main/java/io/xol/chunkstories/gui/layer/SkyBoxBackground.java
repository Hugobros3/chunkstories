//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;

public class SkyBoxBackground extends Layer {
	// Stuff for rendering the background
	String skyBox;

	public SkyBoxBackground(Gui gui) {
		super(gui, null);

		selectRandomSkybox();
	}

	private void selectRandomSkybox() {
		String[] possibleSkyboxes = (new File("./skyboxscreens/")).list();
		if (possibleSkyboxes == null || possibleSkyboxes.length == 0) {
			// No skyboxes screen avaible, default to basic skybox
			skyBox = "./textures/skybox";
		} else {
			// Choose a random one.
			Random rnd = new Random();
			skyBox = "./skyboxscreens/" + possibleSkyboxes[rnd.nextInt(possibleSkyboxes.length)];
		}

		// TODO uncuck this
		skyBox = "./textures/skybox";
	}

	public String getRandomSplashScreen() {
		List<String> splashes = new ArrayList<String>();
		try {
			InputStreamReader ipsr = new InputStreamReader(
					gui.getClient().getContent().getAsset("./splash.txt").read(), "UTF-8");
			BufferedReader br = new BufferedReader(ipsr);
			String ligne;
			while ((ligne = br.readLine()) != null) {
				splashes.add(ligne);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (splashes.size() > 0) {
			Random rnd = new Random();
			return splashes.get(rnd.nextInt(splashes.size()));
		}
		return "";
	}

	@Override
	public void render(GuiDrawer drawer) {
		if (gui.getTopLayer() == this)
			gui.setTopLayer(new MainMenu(gui, this));

		float alphaIcon = (float) (0.25 + Math.sin((System.currentTimeMillis() % (1000 * 60 * 60) / 3000f)) * 0.25f);
		int iconSize = 256;

		drawer.drawBox(gui.getViewportWidth() / 2 - iconSize / 2,
				gui.getViewportHeight() / 2 - iconSize / 2, iconSize,
				iconSize, 0, 1, 1, 0, "./textures/gui/icon.png",
				new Vector4f(1.0f, 1.0f, 1.0f, alphaIcon));

	}
}
