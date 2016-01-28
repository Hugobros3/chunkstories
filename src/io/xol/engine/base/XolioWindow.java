package io.xol.engine.base;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import static org.lwjgl.opengl.GL11.*;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.lwjgl.LWJGLException;

import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.gui.GameplayScene;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.gui.menus.MessageBoxOverlay;
import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.misc.CPUModelDetection;
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

	public String name;
	public static int frameW = 1024;
	public static int frameH = 640;
	public static boolean resized = false;
	public static boolean forceResize = false;

	public static int targetFPS = 60;

	public static String engineVersion = "2.2a";

	public static XolioWindow instance;

	static boolean closeRequest = false;

	static String[] modes;

	public XolioWindow(String name, int fw, int fh)
	{
		if (fw != -1)
			frameW = fw;
		if (fh != -1)
			frameH = fh;
		this.name = name;
	}

	private void systemInfo()
	{
		// Will print some debug information on the general context
		ChunkStoriesLogger.getInstance().log("Running on "+System.getProperty("os.name"));
		ChunkStoriesLogger.getInstance().log(Runtime.getRuntime().availableProcessors()+" avaible CPU cores");
		ChunkStoriesLogger.getInstance().log("Trying cpu detection : "+CPUModelDetection.detectModel());
		long allocatedRam = Runtime.getRuntime().maxMemory();
		ChunkStoriesLogger.getInstance().log("Allocated ram : "+allocatedRam);
		if(allocatedRam < 900 * 1000 *1000L)
		{
			ChunkStoriesLogger.getInstance().log("Less than 1Gb of ram detected");
			JOptionPane.showMessageDialog(null, "Not enought ram, we will offer NO support for crashes and issues when launching the game with less than 1Gb of ram allocated to it."
					+ "\n Use the official launcher to launch the game properly, or add -Xmx1G to the java command.");
			//Runtime.getRuntime().exit(0);
		}
	}
	
	private void glInfo()
	{
		// Will print some debug information on the openGL context
		String glVersion = glGetString(GL_VERSION);
		ChunkStoriesLogger.getInstance().log("Render device : " + glGetString(GL_RENDERER) + " made by " + glGetString(GL_VENDOR) + " driver version " + glVersion);
		// Check OpenGL 3.x capacity
		glVersion = glVersion.split(" ")[0];
		float glVersionf = Float.parseFloat(glVersion.split("\\.")[0] + "." + glVersion.split("\\.")[1]);
		ChunkStoriesLogger.getInstance().log("OpenGL VERSION STRING = " + glGetString(GL_VERSION) + " parsed: " + glVersionf);
		ChunkStoriesLogger.getInstance().log("OpenGL Extensions avaible : " + glGetString(GL_EXTENSIONS));
		if (glVersionf < 3.1f)
		{
			FastConfig.openGL3Capable = false;
			if (GLContext.getCapabilities().GL_EXT_framebuffer_object && GLContext.getCapabilities().GL_ARB_texture_rg && GLContext.getCapabilities().GL_ARB_vertex_type_2_10_10_10_rev)
			{
				FastConfig.fbExtCapable = true;
				ChunkStoriesLogger.getInstance().log("Pre-OpenGL 3.0 Hardware with needed extensions support detected.");
			}
			else
			{
				// bien le moyen-âge ?
				ChunkStoriesLogger.getInstance().log("Pre-OpenGL 3.0 Hardware without needed extensions support detected.");
				ChunkStoriesLogger.getInstance().log("This game isn't made to run in those conditions, please update your drivers or upgrade your graphics card.");
				JOptionPane.showMessageDialog(null, "Pre-OpenGL 3.0 Hardware without needed extensions support detected.\n"
						+ "This game isn't made to run in those conditions, please update your drivers or upgrade your graphics card.");
				Runtime.getRuntime().exit(0);
			}
		}
		else
			System.out.println("OpenGL 3.0 Hardware detected.");

	}

	public void createContext()
	{
		System.out.println("Initializing XolioEngine 3D v" + engineVersion + " [Game:" + name + ",Width:" + frameW + ",Height:" + frameH + "]");
		try
		{
			computeDisplayModes();

			Display.setDisplayMode(new DisplayMode(frameW, frameH));
			Display.setTitle(name);
			Display.setResizable(true);
			//ContextAttribs contextAtrributes = new ContextAttribs(3, 2).withForwardCompatible(true);
			Display.create(new PixelFormat());//, contextAtrributes);
			
			systemInfo();
			glInfo();
			switchResolution();

			Keyboard.enableRepeatEvents(true);
		}
		catch (Exception e)
		{
			System.out.println("A fatal error occured ! If you see the dev, show him this message !");
			e.printStackTrace();
		}
	}

	public void run()
	{
		try
		{
			Client.onStart();
			while (!Display.isCloseRequested() && !closeRequest)
			{
				if (this.currentScene == null || !(currentScene instanceof GameplayScene))
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
			System.exit(0);
		}
		catch (Exception e)
		{
			System.out.println("A fatal error occured ! If you see the dev, show him this message !");
			e.printStackTrace();
		}
	}

	long timeTookLastTime = 0;

	/**
	 * An accurate sync method Since Thread.sleep() isn't 100% accurate, we assume that it has roughly a margin of error of 1ms. This method will sleep for the sync time but burn a few CPU cycles "Thread.yield()" for the last 1 millisecond plus any remainder micro + nano's to ensure accurate sync time.
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
			Set<DisplayMode> validModes = new HashSet<DisplayMode>();
			//modes = new String[dms.length];
			for (int i = 0; i < dms.length; i++)
			{
				if(dms[i].isFullscreenCapable() && dms[i].getBitsPerPixel() >= 32 && dms[i].getWidth() >= 640)
				{
					validModes.add(dms[i]);
				}
				else
				{
					//ChunkStoriesLogger.getInstance().info("Rejected displayMode : "+dms[i] + "fs:"+dms[i].isFullscreenCapable());
				}
				//modes[i] = dms[i].getWidth() + "x" + dms[i].getHeight();
			}
			modes = new String[validModes.size()];
			int i = 0;
			for(DisplayMode dm : validModes)
			{
				modes[i] = dm.getWidth() + "x" + dm.getHeight();
				i++;
			}
			ChunkStoriesLogger.getInstance().info(modes.length + " display modes avaible.");
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
				int w = Integer.parseInt(str[0]);
				int h = Integer.parseInt(str[1]);

				//String newDM = Client.getConfig().getProp("fullScreenResolution", "800x600");
				if (Display.isFullscreen() && frameW == w && frameH == h)
					return;
				//if (newDM.equals(currentDM))
				//	return;

				// Look for relevant display mode
				DisplayMode displayMode = null;
				DisplayMode[] modes = Display.getAvailableDisplayModes();
				for (int i = 0; i < modes.length; i++)
				{
					if (modes[i].getWidth() == w && modes[i].getHeight() == h && modes[i].isFullscreenCapable() && modes[i].getBitsPerPixel() >= 32)
					{
						displayMode = modes[i];
					}
				}
				if (displayMode != null)
				{
					DisplayMode current = Display.getDisplayMode();
					try
					{	
						Display.setDisplayMode(displayMode);
						Display.setFullscreen(true);
					}
					catch (LWJGLException e)
					{
						frameW = 800;
						frameH = 600;
						current = new DisplayMode(frameW, frameH);
						ChunkStoriesLogger.getInstance().warning("Couldnt set display to " + displayMode + "reverting to default resolution");
						Client.getConfig().setProp("fullScreenResolution", current.getWidth() + "x" + current.getHeight());
						Client.getConfig().save();
						Display.setFullscreen(false);
						Display.setDisplayMode(current);
						if (Client.windows.currentScene != null && Client.windows.currentScene instanceof OverlayableScene)
						{
							OverlayableScene scene = ((OverlayableScene) Client.windows.currentScene);
							scene.changeOverlay(new MessageBoxOverlay(scene, scene.currentOverlay, "This resolution failed to be set, try another one !"));
						}
					}
				}
				XolioWindow.forceResize = true;
			}
			else
			{
				if (Display.isFullscreen())
				{
					Display.setFullscreen(false);
					Display.setLocation(0, 0);
					Display.setDisplayMode(Display.getDesktopDisplayMode());
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
		//lastTimeMS = 0;
		if (lastTimeMS + 1000 < System.currentTimeMillis())
		{
			lastFPS = framesSinceLS;
			lastTimeMS = System.currentTimeMillis();
			framesSinceLS = 0;
		}
		/*float frameTime = (float)(System.nanoTime() - lastTimeMS);
		System.out.println(frameTime / 1000000f);
		lastFPS = (int) (1000000000f / frameTime);
		lastTimeMS = System.nanoTime();*/
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
		if (k == 87 /* F11 */)
		{
			Client.getConfig().setProp("fullScreen", !Client.getConfig().getBooleanProp("fullScreen", false) + "");
			String fsReso = Display.getDesktopDisplayMode().getWidth() + "x" + Display.getDesktopDisplayMode().getHeight();
			Client.getConfig().setProp("fullScreenResolution", fsReso);
			switchResolution();
		}
	}
}
