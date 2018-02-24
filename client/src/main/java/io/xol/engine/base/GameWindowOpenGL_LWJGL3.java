//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.base;

import static org.lwjgl.opengl.GL11.*;

import static org.lwjgl.opengl.ARBDebugOutput.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL13.*;

import static org.lwjgl.glfw.GLFW.*;

import static org.lwjgl.system.MemoryUtil.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.matthiasmann.twl.utils.PNGDecoder;
import de.matthiasmann.twl.utils.PNGDecoder.Format;
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

	private Layer layer;

	private String windowName;
	
	public final static int defaultWidth = 1024;
	public final static int defaultHeight = 640;
	
	private int windowWidth = defaultWidth;
	private int windowHeight = defaultHeight;
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

	Queue<SynchronousTask> mainThreadQueue = new ConcurrentLinkedQueue<SynchronousTask>();
	
	//GLFW
	public long glfwWindowHandle;
	@SuppressWarnings("unused")
	private GLFWFramebufferSizeCallback framebufferSizeCallback;

	@SuppressWarnings("unused")
	private BusyMainThreadLoop pleaseWait = null;
	protected GLCapabilities capabilities;
	
	public GameWindowOpenGL_LWJGL3(Client client, String name)
	{
		this.windowName = name;
		this.client = client;
		
		instance = this;
		
		// Load natives for LWJGL
		// NativesLoader.load();
		
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");
		
		createOpenGLContext();

		mainGLThreadId = Thread.currentThread().getId();
		//pleaseWait = new BusyMainThreadLoop(this);
	}

	private void createOpenGLContext()
	{
		logger().info("Creating an OpenGL Windows [title:" + windowName + ", width:" + windowWidth + ", height:" + windowHeight + "]");
		try
		{
			computeDisplayModes();
		
			glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
			
			//We want anything above 3.3
			glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
			glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
			glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE);
			glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE); 
			
			if(RenderingConfig.DEBUG_OPENGL)
				glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_TRUE);
			
			glfwWindowHandle = glfwCreateWindow(windowWidth, windowHeight, windowName, 0, 0);
			
			if(glfwWindowHandle == NULL) 
			    throw new RuntimeException("Failed to create window");
			
			glfwMakeContextCurrent(glfwWindowHandle);
			capabilities = GL.createCapabilities();

			systemInfo();
			glInfo();
			
			switchResolution();
			glfwSwapInterval(0);
			glfwShowWindow(glfwWindowHandle);
			
			//Enable error callback
			if(RenderingConfig.DEBUG_OPENGL)
				glDebugMessageCallbackARB(new OpenGLDebugOutputCallback(Thread.currentThread()), 0);
			
			//Oops.. Didn't use any VAOs anywhere so we put this there to be GL 3.2 core compliant
			int vao = glGenVertexArrays();
			glBindVertexArray(vao);
			
			//displaySplashScreen();

			renderingContext = new RenderingContext(this);
		}
		catch (Exception e)
		{
			logger().info("A fatal error occured ! If you see the dev, show him this message !", e);
			//e.printStackTrace();
			//e.printStackTrace(logger().getPrintWriter());
		}
	}

	@SuppressWarnings("unused")
	private void displaySplashScreen() throws IOException {
		int texture = glGenTextures();
		
		InputStream is = getClass().getResourceAsStream("/splash.png");
		PNGDecoder decoder = new PNGDecoder(is);
		int width = decoder.getWidth();
		int height = decoder.getHeight();
		ByteBuffer temp = ByteBuffer.allocateDirect(4 * width * height);
		decoder.decode(temp, width * 4, Format.RGBA);
		is.close();
		
		//ChunkStoriesLogger.getInstance().log("decoded " + width + " by " + height + " pixels (" + name + ")", ChunkStoriesLogger.LogType.RENDERING, ChunkStoriesLogger.LogLevel.DEBUG);
		temp.flip();
		
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, texture);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) temp);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		
		int shaderProgramId = glCreateProgram();
		int vertexShaderId = glCreateShader(GL_VERTEX_SHADER);
		int fragShaderId = glCreateShader(GL_FRAGMENT_SHADER);
		
		String vertexSource = "#version 330\n\n\nin vec3 vertexIn;\nout vec2 texCoord;\nuniform float ratio;\n\nvoid main()\n{\ngl_Position = vec4(vertexIn.x*ratio, vertexIn.y, 0.0, 1.0);\ntexCoord = vertexIn.xy*0.5+0.5;\n}";
		String fragSource = "#version 330\nuniform sampler2D diffuseTexture;\n\nin vec2 texCoord;\nout vec4 fragColor;\n\nvoid main()\n{\nfragColor = texture(diffuseTexture, vec2(texCoord.x, 1.0-texCoord.y));\n}\n";
		
		//System.out.println(vertexSource);
		//System.out.println(fragSource);
		
		glShaderSource(vertexShaderId, vertexSource);
		glCompileShader(vertexShaderId);

		glBindFragDataLocation(shaderProgramId, 0, "fragColor");
		
		glShaderSource(fragShaderId, fragSource);
		glCompileShader(fragShaderId);
		
		glAttachShader(shaderProgramId, vertexShaderId);
		glAttachShader(shaderProgramId, fragShaderId);

		glLinkProgram(shaderProgramId);
		glUseProgram(shaderProgramId);

		int uniformLocation = glGetUniformLocation(shaderProgramId, "diffuseTexture");
		//glUniform2f(uniformLocation, ((Vector2fc)uniformData).x(), ((Vector2fc)uniformData).y());
		glUniform1i(uniformLocation, (Integer)0);
		
		float ratio = (float)windowHeight / windowWidth;
		uniformLocation = glGetUniformLocation(shaderProgramId, "ratio");
		glUniform1f(uniformLocation, ratio);
		
		glValidateProgram(shaderProgramId);
		
		FloatBuffer fsQuadBuffer = BufferUtils.createFloatBuffer(6 * 2);
		fsQuadBuffer.put(new float[] { 1f, 1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f });
		fsQuadBuffer.flip();
		
		int vertexBuffer = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
		glBufferData(GL_ARRAY_BUFFER, (FloatBuffer) fsQuadBuffer, GL_STATIC_DRAW);
		
		int location = glGetAttribLocation(shaderProgramId, "vertexIn");
		glEnableVertexAttribArray(location);
		glVertexAttribPointer(location, 2, GL_FLOAT, false, 0, 0L);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		//while(1 - Math.floor(1) == 0 && !glfwWindowShouldClose(glfwWindowHandle))
		{

			glClearColor(0.25f, 0.25f, 0.25f, 1f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			//Draw happens here
			glDrawArrays(GL_TRIANGLES, 0, 6);
			
			glfwSwapBuffers(glfwWindowHandle);
			
			glfwPollEvents();
		}

		glDisable(GL_BLEND);
		glDisableVertexAttribArray(location);

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glDeleteBuffers(vertexBuffer);
		
		glBindTexture(GL_TEXTURE_2D, 0);
		glDeleteTextures(texture);
		
		glUseProgram(0);
		glDeleteProgram(shaderProgramId);
		glDeleteShader(vertexShaderId);
		glDeleteShader(fragShaderId);
		
		glClearColor(0.0f, 0.0f, 0.0f, 1f);
	}
	
	private void systemInfo()
	{
		// Will print some debug information on the general context
		logger().info("Running on " + System.getProperty("os.name"));
		logger().info(Runtime.getRuntime().availableProcessors() + " avaible CPU cores");
		logger().info("Trying cpu detection :" + CPUModelDetection.detectModel());
		long allocatedRam = Runtime.getRuntime().maxMemory();
		logger().info("Allocated ram : " + allocatedRam);
		if (allocatedRam < 1024*1024*1024L)
		{
			//Warn user if he gave the game too few ram
			logger().info("Less than 1Gib of ram detected");
			JOptionPane.showMessageDialog(null, "Not enought ram, we will offer NO support for crashes and issues when launching the game with less than 1Gb of ram allocated to it."
					+ "\n Use the official launcher to launch the game properly, or add -Xmx1G to the java command.");
		}
	}
	
	private void glInfo()
	{
		// Will print some debug information on the openGL context
		String glVersion = glGetString(GL_VERSION);
		logger().info("Render device :" + glGetString(GL_RENDERER) + " vendor:" + glGetString(GL_VENDOR) + " version:" + glVersion);
		// Check OpenGL 3.3 capacity
		glVersion = glVersion.split(" ")[0];
		float glVersionf = Float.parseFloat(glVersion.split("\\.")[0] + "." + glVersion.split("\\.")[1]);
		logger().info("OpenGL parsed version :" + glGetString(GL_VERSION) + " parsed: " + glVersionf);
		logger().info("OpenGL Extensions avaible :" + glGetString(GL_EXTENSIONS));
		
		//Kill the game early if not fit
		if (glVersionf < 3.3f)
		{
			RenderingConfig.gl_openGL3Capable = false;
			
			// bien le moyen-âge ?
			logger().warn("Pre-OpenGL 3.3 Hardware detected.");
			logger().warn("This game isn't made to run in those conditions, please update your drivers or upgrade your graphics card.");
			JOptionPane.showMessageDialog(null, "Pre-OpenGL 3.0 Hardware without needed extensions support detected.\n" + "This game isn't made to run in those conditions, please update your drivers or upgrade your graphics card.");
			// If you feel brave after all
			if (!RenderingConfig.ignoreObsoleteHardware)
				Runtime.getRuntime().exit(0);
		}
		else
			logger().info("OpenGL 3.3+ Hardware detected OK!");

		//Check for various limitations
		RenderingConfig.gl_MaxTextureUnits = glGetInteger(GL_MAX_TEXTURE_IMAGE_UNITS);
		RenderingConfig.gl_MaxTextureArraySize = glGetInteger(GL_MAX_ARRAY_TEXTURE_LAYERS);
		
		RenderingConfig.gl_IsInstancingSupported = GL.getCapabilities().GL_ARB_draw_instanced;
		RenderingConfig.gl_InstancedArrays = GL.getCapabilities().GL_ARB_instanced_arrays;
	}

	// what is stage 2 ?
	public void stage_2_init() {
		//pleaseWait.takeControl();
		this.soundManager = new ALSoundManager();
		this.inputsManager = new Lwjgl3ClientInputsManager(this);
	}
	
	public void run()
	{
		try
		{
			//Client.onStart();
			new IconLoader(this);

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
			
			while (glfwWindowShouldClose(glfwWindowHandle) == false && !closeRequest)
			{
				//Update pending actions
				vramUsageVerticesObjects = VertexBufferGL.updateVerticesObjects();
				Texture2DGL.updateTextureObjects();

				//Clear windows
				renderingContext.getRenderTargetManager().clearBoundRenderTargetAll();

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
			
			//Sane way of ending the game.
			client.logger().info("Game exitting cleanly.");
			
			soundManager.destroy();
			client.onClose();
			
			glfwDestroyWindow(glfwWindowHandle);
			System.exit(0);
		}
		catch (Throwable e)
		{
			logger().error("A fatal error occured ! If you see the dev, show him this message !", e);
			//e.printStackTrace();
			//e.printStackTrace(logger().getPrintWriter());
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
		logger().info("Retriving monitors and available display modes...");
		
		List<Long> monitorsHandles = new ArrayList<Long>();
		
		long mainMonitor = glfwGetPrimaryMonitor();
		PointerBuffer pb = glfwGetMonitors();
		int monitorCount = 0;
		while(pb.remaining() > 0) {
			monitorCount++;
			
			long monitorHandle = pb.get();
			monitorsHandles.add(monitorHandle);
			String monitorName = "" + monitorCount + ": " + (mainMonitor==monitorHandle ? " (Main)" : "" ) + " " + glfwGetMonitorName(monitorHandle);
			
			logger().info("Found monitor handle: "+monitorHandle + " " + monitorName);
			GLFWVidMode.Buffer videoModes = glfwGetVideoModes(monitorHandle);
			while(videoModes.remaining() > 0) {
				GLFWVidMode videoMode = videoModes.get();
				
				//String videoModeString = videoMode.width() + "x" + videoMode.height() + " @" + videoMode.refreshRate()+"Hz ";
				
				//System.out.println(videoModeString+(videoMode.blueBits()+videoMode.redBits()+videoMode.greenBits()) + "bpp");
				VideoMode vm = new VideoMode(monitorCount, videoMode);
				//System.out.println("vm: "+vm);
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

	private final static Logger logger = LoggerFactory.getLogger("window");
	private Logger logger() {
		return logger;
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
			
			String modeString = client.configDeprecated().getString("fullScreenResolution", null);
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
