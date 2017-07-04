package io.xol.engine.base;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import static org.lwjgl.glfw.GLFW.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryUtil;

import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.input.lwjgl3.Lwjgl3ClientInputsManager;
import io.xol.chunkstories.renderer.debug.FrametimeRenderer;
import io.xol.chunkstories.renderer.debug.MemUsageRenderer;
import io.xol.chunkstories.renderer.debug.WorldLogicTimeRenderer;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;
import io.xol.engine.concurrency.SimpleFence;
import io.xol.engine.graphics.GLCalls;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.VertexBufferGL;
import io.xol.engine.graphics.textures.Texture2DGL;
import io.xol.engine.misc.CPUModelDetection;
import io.xol.engine.misc.IconLoader;
import io.xol.engine.sound.ALSoundManager;

public class GameWindowOpenGL_LWJGL3 implements GameWindow
{
	public static GameWindowOpenGL_LWJGL3 instance;
	private Lwjgl3ClientInputsManager inputsManager;
	
	private final long mainGLThreadId;

	private Client client;
	public RenderingContext renderingContext;
	private ALSoundManager soundManager;

	//private Scene currentScene = null;
	private Layer layer;

	private String windowName;
	
	public final static int defaultWidth = 1024;
	public final static int defaultHeight = 640;
	
	private int windowWidth = defaultWidth;
	private int windowHeight = defaultHeight;
	//public static boolean resized = false;
	//private boolean forceResize = false;

	private int targetFPS = 60;

	private boolean closeRequest = false;

	//Monitors/resolutions probing
	private long monitors[];
	private String[] modes;
	private final List<VideoMode> enumeratedVideoModes = new ArrayList<VideoMode>();

	private long lastTimeMS = 0;
	private int framesSinceLS = 0;
	private int lastFPS = 0;
	private long lastTime = 0;

	public long vramUsageVerticesObjects = 0;

	//private long timeTookLastTime = 0;

	Queue<SynchronousTask> mainThreadQueue = new ConcurrentLinkedQueue<SynchronousTask>();
	
	//GLFW
	public long glfwWindowHandle;
	private GLFWFramebufferSizeCallback framebufferSizeCallback;

	public GameWindowOpenGL_LWJGL3(Client client, String name, int width, int height)
	{
		// Creates Input manager
		this.windowName = name;
		this.client = client;
		
		instance = this;//TODO: no
		
		// Load natives for LWJGL
		// NativesLoader.load();
		
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");
		
		createOpenGLContext();

		this.soundManager = new ALSoundManager();
		this.inputsManager = new Lwjgl3ClientInputsManager(this);

		mainGLThreadId = Thread.currentThread().getId();
	}

	private void createOpenGLContext()
	{
		ChunkStoriesLoggerImplementation.getInstance().log("Creating an OpenGL Windows [title:" + windowName + ", width:" + windowWidth + ", height:" + windowHeight + "]");
		try
		{
			computeDisplayModes();
		
			glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
			glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE); 
			
			glfwWindowHandle = glfwCreateWindow(windowWidth, windowHeight, windowName, 0, 0);
			
			if(glfwWindowHandle == 0) 
			    throw new RuntimeException("Failed to create window");
			
			glfwMakeContextCurrent(glfwWindowHandle);
			GL.createCapabilities();

			systemInfo();
			glInfo();
			
			switchResolution();
			glfwShowWindow(glfwWindowHandle);
			
			//Keyboard.enableRepeatEvents(true);

			renderingContext = new RenderingContext(this);
		}
		catch (Exception e)
		{
			ChunkStoriesLoggerImplementation.getInstance().log("A fatal error occured ! If you see the dev, show him this message !");
			e.printStackTrace();
			e.printStackTrace(ChunkStoriesLoggerImplementation.getInstance().getPrintWriter());
		}
	}

	private void systemInfo()
	{
		// Will print some debug information on the general context
		ChunkStoriesLoggerImplementation.getInstance().log("Running on " + System.getProperty("os.name"));
		ChunkStoriesLoggerImplementation.getInstance().log(Runtime.getRuntime().availableProcessors() + " avaible CPU cores");
		ChunkStoriesLoggerImplementation.getInstance().log("Trying cpu detection : " + CPUModelDetection.detectModel());
		long allocatedRam = Runtime.getRuntime().maxMemory();
		ChunkStoriesLoggerImplementation.getInstance().log("Allocated ram : " + allocatedRam);
		if (allocatedRam < 1024*1024*1024L)
		{
			//Warn user if he gave the game too few ram
			ChunkStoriesLoggerImplementation.getInstance().log("Less than 1Gib of ram detected");
			JOptionPane.showMessageDialog(null, "Not enought ram, we will offer NO support for crashes and issues when launching the game with less than 1Gb of ram allocated to it."
					+ "\n Use the official launcher to launch the game properly, or add -Xmx1G to the java command.");
		}
	}

	private void glInfo()
	{
		// Will print some debug information on the openGL context
		String glVersion = glGetString(GL_VERSION);
		ChunkStoriesLoggerImplementation.getInstance().log("Render device : " + glGetString(GL_RENDERER) + " made by " + glGetString(GL_VENDOR) + " driver version " + glVersion);
		// Check OpenGL 3.x capacity
		glVersion = glVersion.split(" ")[0];
		float glVersionf = Float.parseFloat(glVersion.split("\\.")[0] + "." + glVersion.split("\\.")[1]);
		ChunkStoriesLoggerImplementation.getInstance().log("OpenGL VERSION STRING = " + glGetString(GL_VERSION) + " parsed: " + glVersionf);
		ChunkStoriesLoggerImplementation.getInstance().log("OpenGL Extensions avaible : " + glGetString(GL_EXTENSIONS));
		if (glVersionf < 3.2f)
		{
			RenderingConfig.gl_openGL3Capable = false;
			if (GL.getCapabilities().GL_EXT_framebuffer_object && GL.getCapabilities().GL_ARB_texture_rg)
			{
				RenderingConfig.gl_fbExtCapable = true;
				ChunkStoriesLoggerImplementation.getInstance().log("Pre-OpenGL 3.0 Hardware with needed extensions support detected.");
			}
			else
			{
				// bien le moyen-�ge ?
				ChunkStoriesLoggerImplementation.getInstance().log("Pre-OpenGL 3.0 Hardware without needed extensions support detected.");
				ChunkStoriesLoggerImplementation.getInstance().log("This game isn't made to run in those conditions, please update your drivers or upgrade your graphics card.");
				JOptionPane.showMessageDialog(null, "Pre-OpenGL 3.0 Hardware without needed extensions support detected.\n" + "This game isn't made to run in those conditions, please update your drivers or upgrade your graphics card.");
				// If you feel brave after all
				if (!RenderingConfig.ignoreObsoleteHardware)
					Runtime.getRuntime().exit(0);
			}
		}
		else
			ChunkStoriesLoggerImplementation.getInstance().log("OpenGL 3.2+ Hardware detected.");

		//Check for various limitations
		RenderingConfig.gl_MaxTextureUnits = glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS);
		
		RenderingConfig.gl_IsInstancingSupported = GL.getCapabilities().GL_ARB_draw_instanced;
		RenderingConfig.gl_InstancedArrays = GL.getCapabilities().GL_ARB_instanced_arrays;

	}

	public void run()
	{
		try
		{
			//Client.onStart();
			IconLoader.load(this);

			//Oops.. Didn't use any VAOs anywhere so we put this there to be GL 3.2 core compliant
			int vao = glGenVertexArrays();
			glBindVertexArray(vao);
			
			while (glfwWindowShouldClose(glfwWindowHandle) == false && !closeRequest)
			{
				//Update pending actions
				vramUsageVerticesObjects = VertexBufferGL.updateVerticesObjects();
				Texture2DGL.updateTextureObjects();

				//Clear windows
				renderingContext.getRenderTargetManager().clearBoundRenderTargetAll();

				//Resize window logic
				glfwSetFramebufferSizeCallback(glfwWindowHandle, (framebufferSizeCallback = new GLFWFramebufferSizeCallback() {
				    @Override
				    public void invoke(long window, int width, int height) {
				    	
				    	windowWidth = width;
						windowHeight = height;

						glViewport(0, 0, width, height);

						Layer layer = GameWindowOpenGL_LWJGL3.this.layer;
						while (layer != null) {
							layer.onResize(width, height);
							layer = layer.getParentLayer();
						}
				    }
				}));

				//Do scene changes etc
				Iterator<SynchronousTask> is = mainThreadQueue.iterator();
				while(is.hasNext()) {
					SynchronousTask st = is.next();
					st.run.run();
					st.signal();
					is.remove();
				}
				/*for (Runnable r : mainThreadQueue)
					r.run();
				mainThreadQueue.clear();*/

				// Update audio
				soundManager.update();

				// update inputs first
				client.getInputsManager().pollLWJGLInputs();
				
				// Run scene content
				if (layer != null)
				{
					// then do the game logic
					try
					{
						layer.render(renderingContext);
					}
					//Fucking tired of handling npes everywhere
					catch (NullPointerException npe)
					{
						npe.printStackTrace();
					}
				}

				renderingContext.getGuiRenderer().drawBuffer();
				tick();

				//Clamp fps
				if (targetFPS != -1)
				{
					//long time = System.currentTimeMillis();

					sync(targetFPS);

					//glFinish();
					//long timeTook = System.currentTimeMillis() - time;
					//timeTookLastTime = timeTook;
				}

				//Draw graph
				if (client.getConfig().getBoolean("frametimeGraph", false)) {
					FrametimeRenderer.draw(renderingContext);
					MemUsageRenderer.draw(renderingContext);
					WorldLogicTimeRenderer.draw(renderingContext);
				}

				//Draw last shit
				GameWindowOpenGL_LWJGL3.instance.renderingContext.flush();

				//Update the screen
				//Display.update();
				glfwSwapBuffers(glfwWindowHandle);

				//Reset counters
				GLCalls.nextFrame();
			}
			System.out.println("Copyright 2015-2016 XolioWare Interactive");
			
			soundManager.destroy();
			client.onClose();
			
			glfwDestroyWindow(glfwWindowHandle);
			System.exit(0);
		}
		catch (Throwable e)
		{
			ChunkStoriesLoggerImplementation.getInstance().log("A fatal error occured ! If you see the dev, show him this message !");
			e.printStackTrace();
			e.printStackTrace(ChunkStoriesLoggerImplementation.getInstance().getPrintWriter());
		}
	}

	private void sync(int fps)
	{
		if (fps <= 0) {
			lastTime = (long)((glfwGetTime() * 1000));
			return;
		}

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
				long t = (long) ((glfwGetTime() * 1000000000) - lastTime);

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

		lastTime = (long) (glfwGetTime() * 1000000000) - overSleep;
	}
	
	private void computeDisplayModes()
	{
		enumeratedVideoModes.clear();
		ChunkStoriesLoggerImplementation.getInstance().log("Retriving monitors and available display modes...");
		
		List<Long> monitorsHandles = new ArrayList<Long>();
		
		long mainMonitor = glfwGetPrimaryMonitor();
		PointerBuffer pb = glfwGetMonitors();
		int monitorCount = 0;
		while(pb.remaining() > 0) {
			monitorCount++;
			
			long monitorHandle = pb.get();
			monitorsHandles.add(monitorHandle);
			String monitorName = "" + monitorCount + ": " + (mainMonitor==monitorHandle ? " (Main)" : "" ) + " " + glfwGetMonitorName(monitorHandle);
			
			ChunkStoriesLoggerImplementation.getInstance().log("Found monitor handle: "+monitorHandle + " " + monitorName);
			GLFWVidMode.Buffer videoModes = glfwGetVideoModes(monitorHandle);
			while(videoModes.remaining() > 0) {
				GLFWVidMode videoMode = videoModes.get();
				
				String videoModeString = videoMode.width() + "x" + videoMode.height() + " @" + videoMode.refreshRate()+"Hz ";
				
				System.out.println(videoModeString+(videoMode.blueBits()+videoMode.redBits()+videoMode.greenBits()) + "bpp");
				VideoMode vm = new VideoMode(monitorCount, videoMode);
				System.out.println("vm: "+vm);
				enumeratedVideoModes.add(vm);
			}
		}
		
		modes = new String[enumeratedVideoModes.size()];
		for(int i = 0; i < enumeratedVideoModes.size(); i++) {
			modes[i] = enumeratedVideoModes.get(i).toString();
		}
		
		monitors = new long[monitorsHandles.size()];
		for(int i = 0; i < monitorsHandles.size(); i++) {
			monitors[i] = monitorsHandles.get(i);
		}
	}

	public String[] getDisplayModes()
	{
		return modes;
	}

	private void switchResolution()
	{
		long mainMonitor = glfwGetPrimaryMonitor();
		GLFWVidMode currentVideoMode = glfwGetVideoMode(mainMonitor);
		
		boolean isFullscreenEnabled = client.configDeprecated().getBoolean("fullscreen", false);
		if(isFullscreenEnabled) {
			//Enable fullscreen using desktop resolution, by default on the primary monitor and at it's nominal video mode
			
			String modeString = client.configDeprecated().getProp("fullScreenResolution", null);
			if(modeString == null || modeString.contains("x") || !modeString.contains(":")) {
				modeString = "1:"+currentVideoMode.width()+":"+currentVideoMode.height()+":"+currentVideoMode.refreshRate();
				client.configDeprecated().setString("fullScreenResolution", modeString);
			}
			
			VideoMode videoMode = findMatchForVideoMode(modeString);
			glfwSetWindowMonitor(this.glfwWindowHandle, monitors[videoMode.monitorId - 1], 0, 0, videoMode.videoMode.width(), videoMode.videoMode.height(), videoMode.videoMode.refreshRate());
		
			windowWidth = videoMode.videoMode.width();
			windowHeight = videoMode.videoMode.height();

			glViewport(0, 0, windowWidth, windowHeight);

			if (layer != null)
				layer.onResize(windowWidth, windowHeight);
		}
		else {
			glfwSetWindowMonitor(this.glfwWindowHandle, MemoryUtil.NULL, (currentVideoMode.width() - defaultWidth) / 2, (currentVideoMode.height() - defaultHeight) / 2,
					defaultWidth, defaultHeight, GLFW_DONT_CARE);
			
			windowWidth = defaultWidth;
			windowHeight = defaultHeight;

			glViewport(0, 0, windowWidth, windowHeight);

			if (layer != null)
				layer.onResize(windowWidth, windowHeight);
		}
	}

	private void tick()
	{
		framesSinceLS++;
		if (lastTimeMS + 1000 < System.currentTimeMillis())
		{
			lastFPS = framesSinceLS;
			lastTimeMS = System.currentTimeMillis();
			framesSinceLS = 0;
		}
	}

	public void setTargetFPS(int target)
	{
		targetFPS = target;
	}

	public int getFPS()
	{
		return lastFPS;
	}

	public void close()
	{
		closeRequest = true;
	}
	
	public void toggleFullscreen() {
		boolean isFullscreenEnabled = client.configDeprecated().getBoolean("fullscreen", false);
		isFullscreenEnabled = !isFullscreenEnabled;
		client.configDeprecated().setString("fullscreen", isFullscreenEnabled ? "true" : "false");
		switchResolution();
	}

	private VideoMode findMatchForVideoMode(String modeString) {
		
		String[] s = modeString.split(":");
		int id = Integer.parseInt(s[0]);
		int w = Integer.parseInt(s[1]);
		int h = Integer.parseInt(s[2]);
		int freq = Integer.parseInt(s[3]);
		for(VideoMode v : enumeratedVideoModes) {
			if(v.monitorId == id && v.videoMode.width() == w && v.videoMode.height() == h && v.videoMode.refreshRate() == freq)
				return v;
		}
		
		System.out.println("Couldn't find a resolution/monitor combo matching :"+modeString);
		long mainMonitor = glfwGetPrimaryMonitor();
		GLFWVidMode currentVideoMode = glfwGetVideoMode(mainMonitor);
		return new VideoMode(1, currentVideoMode);
	}

	public static GameWindowOpenGL_LWJGL3 getInstance()
	{
		return instance;
	}

	public ALSoundManager getSoundEngine()
	{
		return soundManager;
	}

	public RenderingContext getRenderingContext()
	{
		return renderingContext;
	}

	public boolean isMainGLWindow()
	{
		return getInstance().isInstanceMainGLWindow();
	}

	public boolean isInstanceMainGLWindow()
	{
		return Thread.currentThread().getId() == mainGLThreadId;
	}

	public Fence queueSynchronousTask(Runnable runnable)
	{
		/*synchronized (mainThreadQueue)
		{
			mainThreadQueue.add(runnable);
		}*/
		SynchronousTask st = new SynchronousTask(runnable);
		mainThreadQueue.add(st);
		return st;
	}
	
	class SynchronousTask extends SimpleFence {
		final Runnable run;
		
		public SynchronousTask(Runnable run) {
			this.run = run;
		}
	}

	@Override
	public int getWidth() {
		return windowWidth;
	}

	@Override
	public int getHeight() {
		return windowHeight;
	}

	public boolean hasFocus() {
		return glfwGetWindowAttrib(glfwWindowHandle, GLFW_FOCUSED) == GLFW_TRUE;
	}

	@Override
	public ClientInterface getClient() {
		return client;
	}

	@Override
	public Layer getLayer() {
		return layer;
	}

	@Override
	public void setLayer(Layer layer) {
		
		System.out.println("Switching to layer "+layer);
		
		/*if(this.layer != null && this.layer != layer && this.layer != layer.getParentLayer()) {
			this.layer.destroy();
		}*/
		
		this.layer = layer;
		this.client.getInputsManager().getMouse().setGrabbed(false);
	}

	public Lwjgl3ClientInputsManager getInputsManager() {
		return this.inputsManager;
	}

	@Override
	public String takeScreenshot() {
		int scrW = getWidth();
		int scrH = getHeight();
		
		ByteBuffer bbuf = MemoryUtil.memAlloc(4 * scrW * scrH);//ByteBuffer.allocateDirect(scrW * scrH * 4).order(ByteOrder.nativeOrder());

		glReadBuffer(GL_FRONT);
		glReadPixels(0, 0, scrW, scrH, GL_RGBA, GL_UNSIGNED_BYTE, bbuf);

		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
		String time = sdf.format(cal.getTime());

		File image = new File(GameDirectory.getGameFolderPath() + "/screenshots/" + time + ".png");

		image.mkdirs();

		BufferedImage pixels = new BufferedImage(scrW, scrH, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < scrW; x++)
			for (int y = 0; y < scrH; y++)
			{
				int i = 4 * (x + scrW * y);
				int r = bbuf.get(i) & 0xFF;
				int g = bbuf.get(i + 1) & 0xFF;
				int b = bbuf.get(i + 2) & 0xFF;
				pixels.setRGB(x, scrH - 1 - y, (0xFF << 24) | (r << 16) | (g << 8) | b);
			}
		try
		{
			ImageIO.write(pixels, "PNG", image);
			return "#FFFF00Saved screenshot as " + time + ".png";
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return "#FF0000Failed to take screenshot ! (" + e.toString() + ")";
		}
	}

	@Override
	public int getGuiScale() {
		//TODO config option to force this up
		
		//Sub-original resolution results in very tiny GUI scaling
		if(getWidth() < 1024)
			return 1;
		
		//Usual resolution scaling up to 1080P
		if(getWidth() <= 1920)
			return 2;
		
		//Special mid-level one for 1440p
		if(getWidth() <= 2560)
			return 3;
		
		//4K
		return 4;
	}
}
