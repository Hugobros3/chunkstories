package io.xol.chunkstories.gui.overlays;

import io.xol.chunkstories.api.Content.WorldGenerators.WorldGeneratorType;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.world.WorldInfoImplementation;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.InputText;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class LevelCreateOverlay extends Layer
{
	Button cancelOption = new Button(this, 0, 0, 150, "Cancel");
	Button createOption = new Button(this, 0, 0, 150, "Create");
	
	InputText levelName = new InputText(this, 0, 0, 500, 32, BitmapFont.SMALLFONTS);
	InputText worldGenName = new InputText(this, 0, 0, 500, 32, BitmapFont.SMALLFONTS);
	
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
				WorldGeneratorType worldGenerator = Client.getInstance().getContent().generators().getWorldGeneratorUnsafe(worldGenName.text);
				if (worldGenerator != null)
				{
					//String generator = "flat";
					WorldInfoImplementation info = new WorldInfoImplementation(levelName.text, ""+System.currentTimeMillis(), "", WorldInfo.WorldSize.MEDIUM, worldGenName.text);
					
					Client.getInstance().changeWorld(new WorldClientLocal(Client.getInstance(), info));
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
		
		renderingContext.getGuiRenderer().drawBox(0.0f, 0.0f, 1.0f, 1.0f, 0, 0, 0, 0, null, true, false, new Vector4fm(0.0f, 0.0f, 0.0f, 0.75f));
		
		int frame_border_size = 64;
		
		float positionStartX = xPosition;// + frame_border_size;
		float positionStartY = yPosition;// + frame_border_size;
		
		//width -= frame_border_size * 2;
		//height -= frame_border_size * 2;
		
		float x = positionStartX + 20;
		// int y = 48;
		CorneredBoxDrawer.drawCorneredBoxTiled(positionStartX + width/2, positionStartY + height/2, width, height, 8, "./textures/gui/scalableButton.png", 32, 2);
		
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("arial", 12), x, positionStartY + height - 64, "Create a new World", 3, 3, new Vector4fm(1));
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("arial", 12), x, positionStartY + height - 64 - 32, "For use in singleplayer", 2, 2, width, new Vector4fm(1));
		
		
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("arial", 12), x, positionStartY + height - 64 - 96 - 4, "Level name", 2, 2, width, new Vector4fm(1));
		int lvlnm_l = renderingContext.getFontRenderer().getFont("arial", 12).getWidth("Level name") * 2;
		
		levelName.setPosition(x + lvlnm_l + 20, positionStartY + height - 64 - 96);
		levelName.setWidth(width - (x + lvlnm_l + 20) - 20);
		levelName.drawWithBackGround();
		
		String wg_string = "World generator to use";
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("arial", 12), x, positionStartY + height - 64 - 148 - 4, wg_string, 2, 2, width, new Vector4fm(1));
		int wg_sl = renderingContext.getFontRenderer().getFont("arial", 12).getWidth(wg_string) * 2;
		
		worldGenName.setPosition(x + wg_sl + 20, positionStartY + height - 64 - 148);
		worldGenName.setWidth(width - (x + wg_sl + 20) - 20);
		worldGenName.drawWithBackGround();
		
		WorldGeneratorType wg = Client.getInstance().getContent().generators().getWorldGeneratorUnsafe(worldGenName.text);
		String wg_validity_string;
		if(wg == null) {
			wg_validity_string = "#FF0000'" + worldGenName.text + "' wasnt found in the list of loaded world generators.";
		}
		else {
			wg_validity_string = "#00FF00'" + worldGenName.text + "' is a valid world generator !";
		}
		
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("arial", 12), x, positionStartY + height - 64 - 196 - 4, wg_validity_string, 2, 2, width, new Vector4fm(1));
		
		//FontRenderer2.drawTextUsingSpecificFont(20, height - 64, 0, 48, "Create a new World", BitmapFont.SMALLFONTS);

		cancelOption.setPosition(x + 75, positionStartY + 20 + 16);
		cancelOption.render(renderingContext);

		createOption.setPosition(width - 75 - 20, positionStartY + 20 + 16);
		createOption.render(renderingContext);
	}
}
