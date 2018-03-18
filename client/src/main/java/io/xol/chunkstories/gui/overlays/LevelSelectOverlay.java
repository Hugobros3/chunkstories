//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.overlays;



import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.gui.elements.Button;
import io.xol.chunkstories.gui.ng.LargeButtonIcon;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.chunkstories.world.WorldInfoMaster;
import io.xol.chunkstories.world.WorldLoadingException;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.graphics.util.ObjectRenderer;

public class LevelSelectOverlay extends Layer
{
	LargeButtonIcon backOption = new LargeButtonIcon(this, "back");
	LargeButtonIcon newWorldOption = new LargeButtonIcon(this, "new");
	List<WorldInfoMaster> localWorlds = new ArrayList<WorldInfoMaster>();
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
			File infoTxt = new File(f.getAbsolutePath() + "/info.world");
			if (infoTxt.exists())
			{
				try {
					localWorlds.add(new WorldInfoMaster(infoTxt));
					
					/*BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(infoTxt), "UTF-8"));
	
					localWorlds.add(new WorldInfoImplementation(reader));
					//localWorlds.add(new WorldInfoImplementation(infoTxt, f.getName()));
					reader.close();*/
				}
				catch(IOException e) {
					Client.getInstance().logger().error("Could not load world declaration file "+ infoTxt);
				}
			}
		}
		for (WorldInfoMaster wi : localWorlds)
		{
			LocalWorldButton worldButton = new LocalWorldButton(0, 0, wi);
			worldButton.setAction(new Runnable() {
				@Override
				public void run() {
					try {
						Client.getInstance().changeWorld(new WorldClientLocal(Client.getInstance(), worldButton.info));
					} catch (WorldLoadingException e) {
						gameWindow.getClient().exitToMainMenu(e.getMessage());
					}
				}
			});
			
			
			// System.out.println(worldButton.toString());
			//worldButton.setHeight(64 + 8);
			elements.add(worldButton);
			worldsButtons.add(worldButton);
		}
	}

	int scroll = 0;

	@Override
	public void render(RenderingInterface renderer)
	{
		parentLayer.getRootLayer().render(renderer);
		
		if (scroll < 0)
			scroll = 0;

		int posY = renderer.getWindow().getHeight() - 128;
		
		Font font = renderer.getFontRenderer().getFont("LiberationSans-Regular", 11);
		
		renderer.getFontRenderer().drawStringWithShadow(font, 64, posY + 64, "Select a level...", 3, 3, new Vector4f(1));
		
		int remainingSpace = (int)Math.floor(renderer.getWindow().getHeight()/96 - 2);
		
		while(scroll + remainingSpace > worldsButtons.size())
			scroll--;
		
		int skip = scroll;
		for (LocalWorldButton worldButton : worldsButtons)
		{
			if(skip-- > 0)
				continue;
			if(remainingSpace-- <= 0)
				break;
			
			int maxWidth = renderer.getWindow().getWidth() - 64 * 2;
			worldButton.setWidth(maxWidth);
			worldButton.setPosition(64 + worldButton.getWidth() / 2, posY);
			worldButton.render(renderer);
			
			posY -= 96;
		}

		backOption.setPosition(8, 8);
		backOption.render(renderer);
		
		newWorldOption.setPosition(renderer.getWindow().getWidth() - newWorldOption.getWidth() - 8, 8);
		newWorldOption.render(renderer);
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
		public WorldInfoMaster info;

		public LocalWorldButton(int x, int y, WorldInfoMaster info)
		{
			super(LevelSelectOverlay.this, x, y, 0, "");
			this.height = 64 + 8;
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
			
			ObjectRenderer.renderTexturedRect(xPosition - width / 2 + 32 + 4, yPosition, 64, 64, GameDirectory.getGameFolderPath()+"/worlds/" + info.getInternalName() + "/info.png");

			Font font = renderer.getFontRenderer().getFont("LiberationSans-Regular", 11);
			
			renderer.getFontRenderer().drawStringWithShadow(font, xPosition - width / 2 + 72, yPosition, info.getName() + "#CCCCCC    Size : " + info.getSize().toString() + " ( " + info.getSize().sizeInChunks / 32 + "x" + info.getSize().sizeInChunks / 32 + " km )", 2, 2, width - 72, new Vector4f(1.0f));
			renderer.getFontRenderer().drawStringWithShadow(font, xPosition - width / 2 + 72, yPosition - 32, info.getDescription(), 2, 2, -1, new Vector4f(1.0f));
			
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
