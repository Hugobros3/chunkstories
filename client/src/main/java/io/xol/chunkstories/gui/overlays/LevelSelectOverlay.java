package io.xol.chunkstories.gui.overlays;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.world.WorldInfoImplementation;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.gui.elements.Button;

public class LevelSelectOverlay extends Layer
{
	//GuiElementsHandler guiHandler = new GuiElementsHandler();
	Button backOption = new Button(this, 0, 0, 300, 32, ("Back"), BitmapFont.SMALLFONTS, 1);
	Button newWorldOption = new Button(this, 0, 0, 300, 32, ("New..."), BitmapFont.SMALLFONTS, 1);
	List<WorldInfoImplementation> localWorlds = new ArrayList<WorldInfoImplementation>();
	List<LocalWorldButton> worldsButtons = new ArrayList<LocalWorldButton>();

	public LevelSelectOverlay(GameWindow scene, Layer parent)
	{
		super(scene, parent);
		// Gui buttons

		this.backOption.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(parentLayer);
			}
			
		});
		
		this.newWorldOption.setAction(new Runnable() {
			@Override
			public void run() {
				gameWindow.setLayer(new LevelCreateOverlay(gameWindow, LevelSelectOverlay.this));
			}
			
		});
		
		elements.add(backOption);
		elements.add(newWorldOption);
		
		File worldsFolder = new File(GameDirectory.getGameFolderPath() + "/worlds");
		if(!worldsFolder.exists())
			worldsFolder.mkdir();
		for (File f : worldsFolder.listFiles())
		{
			File infoTxt = new File(f.getAbsolutePath() + "/info.txt");
			if (infoTxt.exists())
			{
				localWorlds.add(new WorldInfoImplementation(infoTxt, f.getName()));
			}
		}
		for (WorldInfoImplementation wi : localWorlds)
		{
			LocalWorldButton worldButton = new LocalWorldButton(0, 0, wi);
			worldButton.setAction(new Runnable() {
				@Override
				public void run() {
					Client.getInstance().changeWorld(new WorldClientLocal(Client.getInstance(), worldButton.info));
				}
			});
			
			
			// System.out.println(worldButton.toString());
			worldButton.setHeight(64 + 8);
			elements.add(worldButton);
			worldsButtons.add(worldButton);
		}
	}

	int scroll = 0;

	@Override
	public void render(RenderingInterface renderingContext)
	{
		parentLayer.render(renderingContext);
		
		if (scroll < 0)
			scroll = 0;

		int posY = renderingContext.getWindow().getHeight() - 128;
		FontRenderer2.drawTextUsingSpecificFont(64, posY + 64, 0, 48, "Select a level ...", BitmapFont.SMALLFONTS);
		int remainingSpace = (int)Math.floor(renderingContext.getWindow().getHeight()/96 - 2);
		
		while(scroll + remainingSpace > worldsButtons.size())
			scroll--;
		
		int skip = scroll;
		for (LocalWorldButton worldButton : worldsButtons)
		{
			if(skip-- > 0)
				continue;
			if(remainingSpace-- <= 0)
				break;
			
			int maxWidth = renderingContext.getWindow().getWidth() - 64 * 2;
			worldButton.setWidth(maxWidth);
			worldButton.setPosition(64 + worldButton.getWidth() / 2, posY);
			worldButton.render(renderingContext);
			
			posY -= 96;
		}

		backOption.setPosition(xPosition + 192, 48);
		backOption.render(renderingContext);
		
		newWorldOption.setPosition(renderingContext.getWindow().getWidth() - 192, 48);
		newWorldOption.render(renderingContext);
	}
	
	@Override
	public boolean handleInput(Input input) {
		if(input instanceof MouseScroll) {
			MouseScroll ms = (MouseScroll)input;
			if (ms.amount() < 0)
				scroll++;
			else
				scroll--;
			return true;
		}
		
		return super.handleInput(input);
	}
	
	public class LocalWorldButton extends Button
	{
		public WorldInfoImplementation info;

		public LocalWorldButton(int x, int y, WorldInfoImplementation info)
		{
			super(LevelSelectOverlay.this, x, y, 0, 0, "", null, 333);
			this.info = info;
		}

		@Override
		public boolean isMouseOver(Mouse mouse)
		{
			return (mouse.getCursorX() >= xPosition - width / 2 - 4 && mouse.getCursorX() < xPosition + width / 2 + 4 && mouse.getCursorY() >= yPosition - height / 2 - 4 && mouse.getCursorY() <= yPosition + height / 2 + 4);
		}

		@Override
		public void render(RenderingInterface renderer)
		{
			if (isFocused() || isMouseOver())
			{
				CorneredBoxDrawer.drawCorneredBoxTiled(xPosition, yPosition, width, height, 8, "./textures/gui/scalableButtonOver.png", 32, 2);
			}
			else
			{
				CorneredBoxDrawer.drawCorneredBoxTiled(xPosition, yPosition, width, height, 8, "./textures/gui/scalableButton.png", 32, 2);
			}
			
			//System.out.println(GameDirectory.getGameFolderPath()+"/worlds/" + info.getInternalName() + "/info.png");
			ObjectRenderer.renderTexturedRect(xPosition - width / 2 + 32 + 4, yPosition, 64, 64, GameDirectory.getGameFolderPath()+"/worlds/" + info.getInternalName() + "/info.png");

			//System.out.println("a+"+GameDirectory.getGameFolderPath()+"/worlds/" + info.getInternalName() + "/info.png");
			
			FontRenderer2.setLengthCutoff(true, (int) (width - 72));
			FontRenderer2.drawTextUsingSpecificFont(xPosition - width / 2 + 72, yPosition, 0, 1 * 32, info.getName() + "#CCCCCC    Size : " + info.getSize().toString() + " ( " + info.getSize().sizeInChunks / 32 + "x" + info.getSize().sizeInChunks / 32 + " km )", BitmapFont.SMALLFONTS);
			FontRenderer2.drawTextUsingSpecificFontRVBA(xPosition - width / 2 + 72, yPosition - 32, 0, 1 * 32, info.getDescription(), BitmapFont.SMALLFONTS, 1.0f, 0.8f, 0.8f, 0.8f);
			FontRenderer2.setLengthCutoff(false, -1);
			//return width * 2 - 12;
		}

		@Override
		public void setPosition(float f, float g)
		{
			xPosition = (int) f;
			yPosition = (int) g;
		}
		
		public float getWidth()
		{
			return width;
		}
	}
}