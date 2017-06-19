package io.xol.chunkstories.gui.overlays;

import io.xol.chunkstories.api.Content.WorldGenerators.WorldGeneratorType;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.world.WorldInfo;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.world.WorldInfoImplementation;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.gui.Overlay;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.InputText;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class LevelCreateOverlay extends Overlay
{
	//GuiElementsHandler guiHandler = new GuiElementsHandler();
	Button cancelOption = new Button(0, 0, 150, 32, ("Cancel"), BitmapFont.SMALLFONTS, 1);
	Button createOption = new Button(0, 0, 150, 32, ("Create"), BitmapFont.SMALLFONTS, 1);
	
	InputText levelName = new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS);
	InputText worldGenName = new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS);
	
	public LevelCreateOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		
		guiHandler.add(cancelOption);
		guiHandler.add(createOption);
		
		guiHandler.add(levelName);
		guiHandler.add(worldGenName);
		
		worldGenName.text = "flat";
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int positionStartX, int positionStartY, int width, int height)
	{
		if(parent != null)
			this.parent.drawToScreen(renderingContext, positionStartX, positionStartY, width, height);
		
		renderingContext.getGuiRenderer().renderColoredRect(width / 2, height / 2, width, height, 0, "000000", 0.75f);
		
		int frame_border_size = 64;
		
		positionStartX += frame_border_size;
		positionStartY += frame_border_size;
		
		width -= frame_border_size * 2;
		height -= frame_border_size * 2;
		
		int x = positionStartX + 20;
		// int y = 48;
		CorneredBoxDrawer.drawCorneredBoxTiled(positionStartX + width/2, positionStartY + height/2, width, height, 8, "./textures/gui/scalableButton.png", 32, 2);
		
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("arial", 12), x, positionStartY + height - 64, "Create a new World", 3, 3, new Vector4fm(1));
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("arial", 12), x, positionStartY + height - 64 - 32, "For use in singleplayer", 2, 2, width, new Vector4fm(1));
		
		
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("arial", 12), x, positionStartY + height - 64 - 96 - 4, "Level name", 2, 2, width, new Vector4fm(1));
		int lvlnm_l = renderingContext.getFontRenderer().getFont("arial", 12).getWidth("Level name") * 2;
		
		levelName.setPosition(x + lvlnm_l + 20, positionStartY + height - 64 - 96);
		levelName.setMaxLength(width - (x + lvlnm_l + 20) - 20);
		levelName.drawWithBackGround();
		
		String wg_string = "World generator to use";
		renderingContext.getFontRenderer().drawStringWithShadow(renderingContext.getFontRenderer().getFont("arial", 12), x, positionStartY + height - 64 - 148 - 4, wg_string, 2, 2, width, new Vector4fm(1));
		int wg_sl = renderingContext.getFontRenderer().getFont("arial", 12).getWidth(wg_string) * 2;
		
		worldGenName.setPosition(x + wg_sl + 20, positionStartY + height - 64 - 148);
		worldGenName.setMaxLength(width - (x + wg_sl + 20) - 20);
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
		cancelOption.draw();

		createOption.setPosition(width - 75 - 20, positionStartY + 20 + 16);
		createOption.draw();
		
		if (cancelOption.clicked())
		{
			this.mainScene.changeOverlay(this.parent);
		}
		if (createOption.clicked() && wg != null)
		{
			//String generator = "flat";
			WorldInfoImplementation info = new WorldInfoImplementation(levelName.text, ""+System.currentTimeMillis(), "", WorldInfo.WorldSize.MEDIUM, worldGenName.text);
			
			//Client.world = 
			//Client.world.startLogic();
			//this.mainScene.eng.changeScene(new GameplayScene(mainScene.eng, false));
			
			Client.getInstance().changeWorld(new WorldClientLocal(Client.getInstance(), info));
		}
	}

	@Override
	public boolean handleKeypress(int k)
	{
		guiHandler.handleInput(k);
		return true;
	}
	
	@Override
	public boolean onScroll(int dx)
	{
		return true;
	}

	@Override
	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}
}
