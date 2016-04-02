package io.xol.chunkstories.gui.menus;

import java.util.Random;

import io.xol.engine.math.lalgb.Vector4f;

import io.xol.chunkstories.VersionInfo;
import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.font.BitmapFont;
import io.xol.engine.font.FontRenderer2;
import io.xol.engine.font.TrueTypeFont;
import io.xol.engine.gui.ClickableButton;
import io.xol.engine.gui.FocusableObjectsHandler;

public class MessageBoxOverlay extends Overlay
{

	FocusableObjectsHandler guiHandler = new FocusableObjectsHandler();
	ClickableButton okButton = new ClickableButton(0, 0, 300, 32, ("Ok"), BitmapFont.SMALLFONTS, 1);
	String message;
	
	public MessageBoxOverlay(OverlayableScene scene, Overlay parent, String message)
	{
		super(scene, parent);
		// Gui buttons
		this.message = message;
		guiHandler.add(okButton);
	}

	@Override
	public void drawToScreen(int x, int y, int w, int h)
	{
		ObjectRenderer.renderTexturedRectAlpha(XolioWindow.frameW/2, XolioWindow.frameH / 2 + 256, 1024, 1024, "logo", 1f);
		FontRenderer2.drawTextUsingSpecificFontRVBA(XolioWindow.frameW/2+192, XolioWindow.frameH / 2 + 160, 0, 48, "Indev " + VersionInfo.version, BitmapFont.SMALLFONTS, 1, 0.5f, 1, 1);

		Random rng = new Random();
		rng.setSeed(System.currentTimeMillis() / 100);
		
		int dekal = TrueTypeFont.arial12.getWidth(message);
		
		//FontRenderer2.drawTextUsingSpecificFontRVBA(XolioWindow.frameW/2-dekal, XolioWindow.frameH - 256 , 0, 32, message, BitmapFont.SMALLFONTS, 1, 0.5f, 1, 1);

		TrueTypeFont.arial12.drawStringWithShadow(XolioWindow.frameW/2-dekal*1.5f, XolioWindow.frameH / 2 + 64, message, 3f, 3f, new Vector4f(1,0.2f,0.2f,1));
		
		okButton.setPos(XolioWindow.frameW/2, XolioWindow.frameH / 2 - 32);
		okButton.draw();

		if (okButton.clicked())
		{
			mainScene.changeOverlay(this.parent);
		}
	}

	@Override
	public boolean handleKeypress(int k)
	{
		return false;
	}

	@Override
	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		return true;
	}
}
