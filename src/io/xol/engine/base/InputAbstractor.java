package io.xol.engine.base;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class InputAbstractor
{

	// This class is a link between the game and the game library.
	// That way, by modifying this class to use another lib than LWJGL, it makes
	// it easy
	// to support another system, like Android.

	public static boolean isKeyDown(int k)
	{
		return Keyboard.isKeyDown(k);
	}

	public static boolean isMouseButtonDown(int b)
	{
		return Mouse.isButtonDown(b);
	}

	public static void update(XolioWindow engine)
	{
		/*
		 * This part is about Keyboard/mouse events handling. It does be
		 * dependent of LWJGL.
		 */
		// Keyboard events handling
		while (Keyboard.next())
		{
			int k = Keyboard.getEventKey();
			if (Keyboard.getEventKeyState())
			{
				engine.handleSpecialKey(k);
				engine.currentScene.onKeyPress(k);
			}
			else
			{
				engine.currentScene.onKeyRelease(k);
			}
		}
		// Mouse events
		while (Mouse.next())
		{
			if (Mouse.getEventButtonState())
			{
				//Client.getSoundManager().playSoundEffect("sfx/shoot.ogg", 0, 0, 0, 1, 1);
				engine.currentScene.onClick(Mouse.getX(), Mouse.getY(), Mouse.getEventButton());
			}
		}
		// Mouse scroll
		int dx = Mouse.getDWheel();
		if (dx != 0)
		{
			engine.currentScene.onScroll(dx);
		}

	}
}
