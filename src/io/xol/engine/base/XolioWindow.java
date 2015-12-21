package io.xol.engine.base;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import static org.lwjgl.opengl.GL11.*;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.gui.GameplayScene;
import io.xol.engine.scene.Scene;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.PixelFormat;

public class XolioWindow
{
	Scene currentScene = null;

	public static int frameW = 1024;
	public static int frameH = 640;
	public static boolean resized = false;
	public static boolean forceResize = false;

	public static int targetFPS = 60;

	public static String engineVersion = "2.2a";

	static boolean closeRequest = false;

	static String[] modes;

	public XolioWindow(String name, int fw, int fh)
	{
		try
		{
			if (fw != -1)
				frameW = fw;
			if (fh != -1)
				frameH = fh;
			System.out.println("Initializing XolioEngine 3D v" + engineVersion + " [Game:" + name + ",Width:" + frameW + ",Height:" + frameH + "]");

			computeDisplayModes();

			Display.setDisplayMode(new DisplayMode(frameW, frameH));
			Display.setTitle(name);
			Display.setResizable(true);
			//ContextAttribs contextAtrributes = new ContextAttribs(3, 2).withForwardCompatible(true);
			Display.create(new PixelFormat());//, contextAtrributes);

			glInfo();

			switchResolution();
		}
		catch (Exception e)
		{
			System.out.println("A fatal error occured ! If you see the dev, show him this message !");
			e.printStackTrace();
		}
	}

	private void glInfo()
	{
		// Will print some debug information on the openGL context
		String glVersion = glGetString(GL_VERSION);
		System.out.println("Render device : " + glGetString(GL_RENDERER) + " made by " + glGetString(GL_VENDOR) + " driver version " + glVersion);
		// Check OpenGL 3.x capacity
		glVersion = glVersion.split(" ")[0];
		float glVersionf = Float.parseFloat(glVersion.split("\\.")[0] + "." + glVersion.split("\\.")[1]);
		System.out.println("OpenGL VERSION STRING = " + glGetString(GL_VERSION) + " parsed: " + glVersionf);
		System.out.println("OpenGL Extensions avaible : " + glGetString(GL_EXTENSIONS));
		if (glVersionf < 3.1f)
		{
			FastConfig.openGL3Capable = false;
			if (GLContext.getCapabilities().GL_EXT_framebuffer_object && GLContext.getCapabilities().GL_ARB_texture_rg && GLContext.getCapabilities().GL_ARB_vertex_type_2_10_10_10_rev)
			{
				FastConfig.fbExtCapable = true;
				System.out.println("Pre-OpenGL 3.0 Hardware with needed extensions support detected.");
			}
			else
			{
				// bien le moyen-âge ?
				System.out.println("Pre-OpenGL 3.0 Hardware without needed extensions support detected.");
				System.out.println("Game isn't made to run in those conditions, please update your drivers or upgrade your graphics card.");
				Runtime.getRuntime().exit(0);
			}
		}
		else
			System.out.println("OpenGL 3.0 Hardware detected.");

	}

	public void run()
	{
		try
		{
			Keyboard.enableRepeatEvents(true);

			Client.onStart();
			while (!Display.isCloseRequested() && !closeRequest)
			{
				if(this.currentScene == null || !(currentScene instanceof GameplayScene))
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				if (resized)
					resized = false;
				if (Display.wasResized() || forceResize)
				{
					if (forceResize)
						forceResize = false;
					XolioWindow.frameW = Display.getWidth();
					XolioWindow.frameH = Display.getHeight();

					glViewport(0, 0, Display.getWidth(), Display.getHeight());

					if (currentScene != null)
					{
						// System.out.println("orr");
						currentScene.onResize();
						currentScene.resized = true;
					}
					resized = true;
				}

				// Update audio streams
				Client.getSoundManager().update();

				if (currentScene != null)
				{
					// update inputs first
					InputAbstractor.update(this);
					// then do the game logic
					currentScene.update();
				}

				if (targetFPS != -1)
				{
					long time = System.currentTimeMillis();
					int milisToWait = 1000 / targetFPS;
					long sleep = milisToWait - timeTookLastTime;
					if (sleep > milisToWait)
						sleep = milisToWait;
					if (sleep > 0)
						Thread.sleep(sleep);
					glFinish();
					long timeTook = System.currentTimeMillis() - time;
					// System.out.println("Finishing frame took "+timeTook+"ms,
					// last time it was "+timeTookLastTime+"ms slept "+sleep+"ms
					// target fps:"+targetFPS);
					timeTookLastTime = timeTook;
				}
				// System.out.println("target fps:"+targetFPS);
				// if (targetFPS > 0)
				// sync(targetFPS);

				Display.update();
			}
			System.out.println("Copyright 2015 XolioWare Interactive");
			Client.onClose();
			Display.destroy();
		}
		catch (Exception e)
		{
			System.out.println("A fatal error occured ! If you see the dev, show him this message !");
			e.printStackTrace();
		}
	}

	long timeTookLastTime = 0;

	/**
	 * An accurate sync method
	 * 
	 * Since Thread.sleep() isn't 100% accurate, we assume that it has roughly a
	 * margin of error of 1ms. This method will sleep for the sync time but burn
	 * a few CPU cycles "Thread.yield()" for the last 1 millisecond plus any
	 * remainder micro + nano's to ensure accurate sync time.
	 * 
	 * @param fps
	 *            The desired frame rate, in frames per second
	 */
	static long lastTime = 0;

	public static void sync(int fps)
	{
		if (fps <= 0)
			return;

		long errorMargin = 1000 * 1000; // 1 millisecond error margin for
										// Thread.sleep()
		long sleepTime = 1000000000 / fps; // nanoseconds to sleep this frame

		// if smaller than sleepTime burn for errorMargin + remainder micro &
		// nano seconds
		long burnTime = Math.min(sleepTime, errorMargin + sleepTime % (1000 * 1000));

		long overSleep = 0; // time the sleep or burn goes over by

		try
		{
			while (true)
			{
				long t = getTime() - lastTime;

				if (t < sleepTime - burnTime)
				{
					Thread.sleep(1);
				}
				else if (t < sleepTime)
				{
					// burn the last few CPU cycles to ensure accuracy
					Thread.yield();
				}
				else
				{
					overSleep = Math.min(t - sleepTime, errorMargin);
					break; // exit while loop
				}
			}
		}
		catch (InterruptedException e)
		{
		}

		lastTime = getTime() - overSleep;
	}

	/**
	 * Get System Nano Time
	 * 
	 * @return will return the current time in nano's
	 */
	private static long getTime()
	{
		return (Sys.getTime() * 1000000000) / Sys.getTimerResolution();
	}

	public static void setup3d()
	{
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);

		// glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}

	public static void computeDisplayModes()
	{
		try
		{
			DisplayMode[] dms = Display.getAvailableDisplayModes();
			modes = new String[dms.length];
			for (int i = 0; i < dms.length; i++)
			{
				modes[i] = dms[i].getWidth() + "x" + dms[i].getHeight();
			}
			System.out.println(modes.length + " display modes avaible.");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static String[] getDisplayModes()
	{
		return modes;
	}

	static String currentDM = "";

	public static void switchResolution()
	{
		try
		{
			if (Client.getConfig().getBooleanProp("fullScreen", false))
			{
				String str[] = Client.getConfig().getProp("fullScreenResolution", "800x600").split("x");
				String newDM = Client.getConfig().getProp("fullScreenResolution", "800x600");
				if (newDM.equals(currentDM))
					return;
				int w = Integer.parseInt(str[0]);
				int h = Integer.parseInt(str[1]);

				DisplayMode displayMode = null;
				DisplayMode[] modes = Display.getAvailableDisplayModes();

				for (int i = 0; i < modes.length; i++)
				{
					if (modes[i].getWidth() == w && modes[i].getHeight() == h && modes[i].isFullscreenCapable())
					{
						displayMode = modes[i];
					}
				}

				Display.setDisplayMode(displayMode);
				Display.setFullscreen(true);
				XolioWindow.forceResize = true;

				currentDM = newDM;
			}
			else
			{
				if (Display.isFullscreen())
				{
					Display.setFullscreen(false);
					XolioWindow.forceResize = true;
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static long lastTimeMS = 0;
	private static int framesSinceLS = 0;
	private static int lastFPS = 0;

	public static void tick()
	{
		framesSinceLS++;
		if (lastTimeMS + 1000 < System.currentTimeMillis())
		{
			lastFPS = framesSinceLS;
			lastTimeMS = System.currentTimeMillis();
			framesSinceLS = 0;
		}
	}

	public static void setTargetFPS(int target)
	{
		targetFPS = target;
	}

	public static int getFPS()
	{
		return lastFPS;
	}

	public void changeScene(Scene s)
	{
		Mouse.setGrabbed(false);
		if (currentScene != null)
			currentScene.destroy();
		currentScene = s;
	}

	public void close()
	{
		closeRequest = true;
	}

	public Scene getCurrentScene()
	{
		return currentScene;
	}

	public void handleSpecialKey(int k)
	{

	}
}
