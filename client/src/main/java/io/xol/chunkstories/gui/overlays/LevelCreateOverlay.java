//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.overlays;

import java.util.Iterator;

import org.joml.Vector4f;

import io.xol.chunkstories.api.content.Content.WorldGenerators.WorldGeneratorDefinition;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.chunkstories.world.WorldInfoImplementation;
import io.xol.chunkstories.world.WorldLoadingException;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.InputText;

public class LevelCreateOverlay extends Layer
{
	Button cancelOption = new Button(this, 0, 0, 150, "Cancel");
	Button createOption = new Button(this, 0, 0, 150, "Create");
	
	InputText levelName = new InputText(this, 0, 0, 500);
	InputText worldGenName = new InputText(this, 0, 0, 500);
	
	public LevelCreateOverlay(GameWindow scene, Layer parent)
	{
		super(scene, parent);
		
		this.cancelOption.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(parentLayer);
			}
		});
		
		this.createOption.setAction(new Runnable() {
			@Override
			public void run() {
				WorldGeneratorDefinition worldGenerator = Client.getInstance().getContent().generators().getWorldGeneratorUnsafe(worldGenName.text);
				if (worldGenerator != null)
				{
					//String generator = "flat";
					String internalName = levelName.text.replaceAll("[^\\w\\s]","_");
					WorldInfoImplementation info = new WorldInfoImplementation(internalName, levelName.text, ""+System.currentTimeMillis(), "", WorldInfo.WorldSize.MEDIUM, worldGenName.text);
					
					try {
						//WorldInfoFile.createNewWorld(new File(GameDirectory.getGameFolderPath() + "/worlds/" + internalName), 
						Client.getInstance().changeWorld(new WorldClientLocal(Client.getInstance(), info));
					} catch (WorldLoadingException e) {
						gameWindow.getClient().exitToMainMenu(e.getMessage());
					}
				}
			}
		});
		
		elements.add(cancelOption);
		elements.add(createOption);
		
		elements.add(levelName);
		elements.add(worldGenName);
		
		worldGenName.text = "flat";
		
		int frame_border_size = 64;

		xPosition = xPosition + frame_border_size;
		yPosition = yPosition + frame_border_size;
		
		width -= frame_border_size * 2;
		height -= frame_border_size * 2;
	}

	@Override
	public void render(RenderingInterface renderingContext)
	{
		if(parentLayer != null)
			this.parentLayer.render(renderingContext);
		
		renderingContext.getGuiRenderer().drawBox(-1.0f, -1.0f, 1.0f, 1.0f, 0, 0, 0, 0, null, true, false, new Vector4f(0.0f, 0.0f, 0.0f, 0.25f));
		
		//int frame_border_size = 64;
		
		float positionStartX = xPosition;// + frame_border_size;
		float positionStartY = yPosition;// + frame_border_size;
		
		//width -= frame_border_size * 2;
		//height -= frame_border_size * 2;
		
		float x = positionStartX + 20;
		// int y = 48;
		CorneredBoxDrawer.drawCorneredBoxTiled(positionStartX + width/2, positionStartY + height/2, width, height, 8, "./textures/gui/scalableButton.png", 32, 2);
		
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("LiberationSans-Regular", 12), x, positionStartY + height - 64, "Create a new World", 3, 3, new Vector4f(1));
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("LiberationSans-Regular", 12), x, positionStartY + height - 64 - 32, "For use in singleplayer", 2, 2, width, new Vector4f(1));
		
		
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("LiberationSans-Regular", 12), x, positionStartY + height - 64 - 96 - 4, "Level name", 2, 2, width, new Vector4f(1));
		int lvlnm_l = renderingContext.getFontRenderer().getFont("LiberationSans-Regular", 12).getWidth("Level name") * 2;
		
		levelName.setPosition(x + lvlnm_l + 20, positionStartY + height - 64 - 96);
		levelName.setWidth(width - (x + lvlnm_l + 20) - 20);
		levelName.drawWithBackGround(renderingContext);
		
		String wg_string = "World generator to use";
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("LiberationSans-Regular", 12), x, positionStartY + height - 64 - 148 - 4, wg_string, 2, 2, width, new Vector4f(1));
		int wg_sl = renderingContext.getFontRenderer().getFont("LiberationSans-Regular", 12).getWidth(wg_string) * 2;
		
		worldGenName.setPosition(x + wg_sl + 20, positionStartY + height - 64 - 148);
		worldGenName.setWidth(width - (x + wg_sl + 20) - 20);
		worldGenName.drawWithBackGround(renderingContext);
		
		WorldGeneratorDefinition wg = Client.getInstance().getContent().generators().getWorldGeneratorUnsafe(worldGenName.text);
		String wg_validity_string;
		if(wg == null) {
			wg_validity_string = "#FF0000'" + worldGenName.text + "' wasnt found in the list of loaded world generators.";
		}
		else {
			wg_validity_string = "#00FF00'" + worldGenName.text + "' is a valid world generator !";
		}
		
		String wg_list = "Available world generators: ";
		Iterator<WorldGeneratorDefinition> iwg = Client.getInstance().getContent().generators().all();
		while(iwg != null && iwg.hasNext()) {
			WorldGeneratorDefinition wgt = iwg.next();
			wg_list += wgt.getName();
			if(iwg.hasNext())
				wg_list+=", ";
		}
		
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("LiberationSans-Regular", 12), x, positionStartY + height - 64 - 196 - 4, wg_validity_string, 2, 2, width, new Vector4f(1));
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("LiberationSans-Regular", 12), x, positionStartY + height - 64 - 196 - 4 - 32, wg_list, 2, 2, width, new Vector4f(1));
		
		cancelOption.setPosition(x + 75, positionStartY + 20 + 16);
		cancelOption.render(renderingContext);

		createOption.setPosition(width - 75 - 20, positionStartY + 20 + 16);
		createOption.render(renderingContext);
	}
}
