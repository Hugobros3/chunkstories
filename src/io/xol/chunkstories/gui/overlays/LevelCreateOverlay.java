package io.xol.chunkstories.gui.overlays;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.WorldInfo.WorldSize;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.InputText;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class LevelCreateOverlay extends Overlay
{
	//GuiElementsHandler guiHandler = new GuiElementsHandler();
	Button cancelOption = new Button(0, 0, 150, 32, ("Cancel"), BitmapFont.SMALLFONTS, 1);
	Button createOption = new Button(0, 0, 150, 32, ("Create"), BitmapFont.SMALLFONTS, 1);
	
	InputText levelName = new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS);
	
	public LevelCreateOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		
		guiHandler.add(cancelOption);
		guiHandler.add(createOption);
		
		guiHandler.add(levelName);
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int positionStartX, int positionStartY, int width, int height)
	{
		if(parent != null)
			this.parent.drawToScreen(renderingContext, positionStartX, positionStartY, width, height);
		
		int x = 48;
		// int y = 48;
		CorneredBoxDrawer.drawCorneredBoxTiled(width/2, height/2, width - 48 * 2, height - 48 * 2, 8, "gui/scalableButton", 32, 2);

		cancelOption.setPosition(x + 128, 96);
		cancelOption.draw();

		createOption.setPosition(width - 192, 96);
		createOption.draw();

		levelName.setPosition(x + 48, GameWindowOpenGL.windowHeight - 256);
		levelName.drawWithBackGround();
		
		if (cancelOption.clicked())
		{
			this.mainScene.changeOverlay(this.parent);
		}
		if (createOption.clicked())
		{
			String generator = "flat";
			WorldInfo info = new WorldInfo(levelName.text, ""+System.currentTimeMillis(), "", WorldSize.MEDIUM, generator);
			
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
