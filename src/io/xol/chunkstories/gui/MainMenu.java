package io.xol.chunkstories.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.content.DefaultModsManager;
import io.xol.chunkstories.gui.overlays.LoginOverlay;
import io.xol.chunkstories.gui.overlays.MainMenuOverlay;
import io.xol.chunkstories.gui.overlays.general.MessageBoxOverlay;
import io.xol.chunkstories.renderer.Camera;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fbo.FrameBufferObject;
import io.xol.engine.graphics.textures.Texture2DRenderTarget;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TextureFormat;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.vector.sp.Vector4fm;
import io.xol.engine.base.GameWindowOpenGL;

public class MainMenu extends OverlayableScene
{
	// Stuff for rendering the background

	String skyBox;
	Camera cam = new Camera();

	private Texture2DRenderTarget unblurred = new Texture2DRenderTarget(TextureFormat.RGBA_8BPP, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
	private Texture2DRenderTarget blurredH = new Texture2DRenderTarget(TextureFormat.RGBA_8BPP, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
	private Texture2DRenderTarget blurredV = new Texture2DRenderTarget(TextureFormat.RGBA_8BPP, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);

	private FrameBufferObject unblurredFBO = new FrameBufferObject(null, unblurred);
	private FrameBufferObject blurredHFBO = new FrameBufferObject(null, blurredH);
	private FrameBufferObject blurredVFBO = new FrameBufferObject(null, blurredV);

	// private String splashText = getRandomSplashScreen();

	public MainMenu(GameWindowOpenGL XolioWindow, boolean askForLogin)
	{
		super(XolioWindow);
		selectRandomSkybox();
		
		if(askForLogin)
			currentOverlay = new LoginOverlay(this, null);
		else
			currentOverlay = new MainMenuOverlay(this, null);
	}

	public MainMenu(GameWindowOpenGL eng, String string)
	{
		this(eng, false);
		this.changeOverlay(new MessageBoxOverlay(this, currentOverlay, string));
	}

	void selectRandomSkybox()
	{
		String[] possibleSkyboxes = (new File("./skyboxscreens/")).list();
		if (possibleSkyboxes == null || possibleSkyboxes.length == 0)
		{
			// No skyboxes screen avaible, default to basic skybox
			skyBox = "./textures/skybox";
		} else
		{
			// Choose a random one.
			Random rnd = new Random();
			skyBox = "./skyboxscreens/" + possibleSkyboxes[rnd.nextInt(possibleSkyboxes.length)];
		}
		
		//TODO uncuck this
		skyBox = "./textures/skybox";
		cam.rotationX = 25;
		cam.rotationY = -45;
	}

	String getRandomSplashScreen()
	{
		List<String> splashes = new ArrayList<String>();
		try
		{
			InputStreamReader ipsr = new InputStreamReader(DefaultModsManager.getAsset("./splash.txt").read(), "UTF-8");
			BufferedReader br = new BufferedReader(ipsr);
			String ligne;
			while ((ligne = br.readLine()) != null)
			{
				splashes.add(ligne);
			}
			br.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		if (splashes.size() > 0)
		{
			Random rnd = new Random();
			return splashes.get(rnd.nextInt(splashes.size()));
		}
		return "en vrai j'ai pas joué à pokémon";
	}

	@Override
	public void onResize()
	{
		unblurredFBO.resizeFBO(GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		blurredHFBO.resizeFBO(GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		blurredVFBO.resizeFBO(GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
	}

	@Override
	public void destroy()
	{
		unblurredFBO.destroy(true);
		blurredHFBO.destroy(true);
		blurredVFBO.destroy(true);
	}

	@Override
	public void update(RenderingContext renderingContext)
	{
		try // Ugly fps caps yay
		{
			Thread.sleep(33L);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		// Render this shit boy
		
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(unblurredFBO);
		//unblurredFBO.bind();
		cam.justSetup(GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		ShaderInterface menuSkyBox = renderingContext.useShader("mainMenuSkyBox");
		//menuSkyBox.use(true);
		cam.setupShader(menuSkyBox);
		
		renderingContext.bindCubemap("skybox", TexturesHandler.getCubemap(skyBox));
		//menuSkyBox.setUniformSamplerCubemap(0, "skyBox", TexturesHandler.getCubemapID(skyBox));
		cam.rotationX = 35 + (float) (Math.sin(cam.rotationY / 15)) * 5f;
		cam.rotationY = (System.currentTimeMillis()%1000000)/200.0f;
		renderingContext.drawFSQuad();
		
		// Blurring to H
		renderingContext.getRenderTargetManager().setCurrentRenderTarget(blurredHFBO);
		//blurredHFBO.bind();
		ShaderInterface blurH = renderingContext.useShader("blurH");
		//blurH.use(true);
		blurH.setUniform2f("screenSize", GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		
		renderingContext.bindTexture2D("inputTexture", unblurred);
		//blurH.setUniformSampler(0, "inputTexture", unblurred.getId());
		renderingContext.drawFSQuad();

		for (int i = 0; i < 1; i++)
		{
			renderingContext.getRenderTargetManager().setCurrentRenderTarget(blurredVFBO);
			//blurredVFBO.bind();
			ShaderInterface blurV = renderingContext.useShader("blurV");
			//blurV.use(true);
			blurV.setUniform1f("lookupScale", 1);
			blurV.setUniform2f("screenSize", GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2);
			
			//blurV.setUniformSampler(0, "inputTexture", blurredH.getId());
			renderingContext.bindTexture2D("inputTexture", blurredH);
			renderingContext.drawFSQuad();

			renderingContext.getRenderTargetManager().setCurrentRenderTarget(blurredHFBO);
			//blurredHFBO.bind();
			blurH = renderingContext.useShader("blurH");
			//blurH.use(true);
			blurH.setUniform2f("screenSize", GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2);
			//blurH.setUniformSampler(0, "inputTexture", blurredV.getId());
			renderingContext.bindTexture2D("inputTexture", blurredV);
			renderingContext.drawFSQuad();
		}

		renderingContext.getRenderTargetManager().setCurrentRenderTarget(blurredVFBO);
		//blurredVFBO.bind();
		ShaderInterface blurV = renderingContext.useShader("blurV");
		//blurV.use(true);
		blurV.setUniform2f("screenSize", GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		//blurV.setUniformSampler(0, "inputTexture", blurredH.getId());
		renderingContext.bindTexture2D("inputTexture", blurredH);
		renderingContext.drawFSQuad();
		//blurV.use(false);

		renderingContext.getRenderTargetManager().setCurrentRenderTarget(null);
		//FrameBufferObject.unbind();
		ShaderInterface blit = renderingContext.useShader("background");
		//blit.use(true);
		blit.setUniform2f("screenSize", GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		//blit.setUniformSampler(0, "diffuseTexture", blurredV.getId());
		//renderingContext.bindTexture2D("inputTexture", blurredV);
		Texture2D backgroundTexture = TexturesHandler.getTexture("./textures/gui/darker.png");
		backgroundTexture.setLinearFiltering(false);
		renderingContext.bindTexture2D("diffuseTexture", backgroundTexture);
		renderingContext.bindTexture2D("backgroundTexture", blurredV);
		renderingContext.drawFSQuad();
		//FrameBufferObject.unbind();

		Texture2D logoTexture = TexturesHandler.getTexture("./textures/gui/icon.png");
		//System.out.println(logoTexture.getId());
		float alphaIcon = (float) (0.25 + Math.sin((System.currentTimeMillis() % (1000 * 60 * 60) / 3000f)) * 0.25f);
		renderingContext.setBlendMode(BlendMode.MIX);
		float diagonal = (float) Math.sqrt(GameWindowOpenGL.windowWidth * GameWindowOpenGL.windowWidth + GameWindowOpenGL.windowHeight * GameWindowOpenGL.windowHeight);
		float iconSize = (float) (diagonal / 3 + 50 * Math.sin((System.currentTimeMillis() % (1000 * 60 * 60) / 30000f)));
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(GameWindowOpenGL.windowWidth / 2 - iconSize / 2, GameWindowOpenGL.windowHeight / 2 - iconSize / 2, GameWindowOpenGL.windowWidth / 2 + iconSize / 2, GameWindowOpenGL.windowHeight / 2 + iconSize / 2, 0, 1, 1, 0, logoTexture, true, true, new Vector4fm(1.0, 1.0, 1.0, alphaIcon));
		//renderingContext.getGuiRenderer().drawBuffer();
		
		currentOverlay.drawToScreen(renderingContext, 0, 0, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
	}

	@Override
	public boolean onMouseButtonDown(int posx, int posy, int button)
	{
		if (currentOverlay != null)
			return currentOverlay.onClick(posx, posy, button);
		return true;
	}

	@Override
	public boolean onKeyDown(int k)
	{
		if (currentOverlay != null && currentOverlay.handleKeypress(k))
		{
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onScroll(int dx)
	{
		if (currentOverlay != null && currentOverlay.onScroll(dx))
			return true;
		return false;
	}

	@Override
	public void changeOverlay(Overlay overlay)
	{
		this.currentOverlay = overlay;
	}
}
