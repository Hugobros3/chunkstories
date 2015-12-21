package io.xol.chunkstories.gui.menus;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.xol.chunkstories.GameDirectory;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.GameplayScene;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.world.WorldInfo;
import io.xol.chunkstories.world.WorldLocalClient;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;
import io.xol.engine.gui.ClickableButton;
import io.xol.engine.gui.FocusableObjectsHandler;
import io.xol.engine.gui.LocalWorldButton;

public class LevelSelectOverlay extends MenuOverlay
{

	FocusableObjectsHandler guiHandler = new FocusableObjectsHandler();
	ClickableButton backOption = new ClickableButton(0, 0, 300, 32, ("Back"), BitmapFont.SMALLFONTS, 1);
	List<WorldInfo> localWorlds = new ArrayList<WorldInfo>();
	List<LocalWorldButton> worldsButtons = new ArrayList<LocalWorldButton>();

	public LevelSelectOverlay(OverlayableScene scene, MenuOverlay parent)
	{
		super(scene, parent);
		// Gui buttons
		guiHandler.add(backOption);
		File worldsFolder = new File(GameDirectory.getGameFolderPath() + "/worlds");
		if(!worldsFolder.exists())
			worldsFolder.mkdir();
		for (File f : worldsFolder.listFiles())
		{
			File infoTxt = new File(f.getAbsolutePath() + "/info.txt");
			if (infoTxt.exists())
			{
				localWorlds.add(new WorldInfo(infoTxt, f.getName()));
			}
		}
		for (WorldInfo wi : localWorlds)
		{
			LocalWorldButton worldButton = new LocalWorldButton(0, 0, wi);
			// System.out.println(worldButton.toString());
			worldButton.height = 64 + 8;
			guiHandler.add(worldButton);
			worldsButtons.add(worldButton);
		}
	}

	int scroll = 0;

	public void drawToScreen(int x, int y, int w, int h)
	{
		if (scroll < 0)
			scroll = 0;

		int posY = XolioWindow.frameH - 128;
		FontRenderer2.drawTextUsingSpecificFont(64, posY + 64, 0, 48, "Select a level ...", BitmapFont.SMALLFONTS);
		for (LocalWorldButton worldButton : worldsButtons)
		{
			if (worldButton.clicked())
			{
				// System.out.println("big deal");
				Client.world = new WorldLocalClient(worldButton.info);
				this.mainScene.eng.changeScene(new GameplayScene(mainScene.eng, false));
			}
			int maxWidth = (int) (XolioWindow.frameW - 64 * 2);
			worldButton.width = maxWidth;
			worldButton.setPos(64 + worldButton.width / 2, posY);
			worldButton.draw();
			posY -= 96;
		}

		backOption.setPos(x + 192, 96);
		backOption.draw();

		if (backOption.clicked())
		{
			this.mainScene.changeOverlay(this.parent);
		}
	}

	public boolean handleKeypress(int k)
	{
		return false;
	}

	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}
}
