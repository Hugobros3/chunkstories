package io.xol.chunkstories.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.xol.chunkstories.api.gui.Layer;
import org.joml.Vector4f;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.target.RenderTargetAttachementsConfiguration;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.gui.overlays.MainMenuOverlay;
import io.xol.chunkstories.renderer.Camera;
import io.xol.engine.graphics.fbo.FrameBufferObjectGL;
import io.xol.engine.graphics.textures.Texture2DRenderTargetGL;
import io.xol.engine.graphics.textures.Texture2DGL;
import io.xol.engine.graphics.textures.TexturesHandler;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class MainMenu extends Layer
{
	// Stuff for rendering the background

	String skyBox;
	Camera cam = new Camera();

	private Texture2DRenderTargetGL unblurred = new Texture2DRenderTargetGL(TextureFormat.RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());
	private Texture2DRenderTargetGL blurredH = new Texture2DRenderTargetGL(TextureFormat.RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());
	private Texture2DRenderTargetGL blurredV = new Texture2DRenderTargetGL(TextureFormat.RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());

	private RenderTargetAttachementsConfiguration unblurredFBO = new FrameBufferObjectGL(null, unblurred);
	private RenderTargetAttachementsConfiguration blurredHFBO = new FrameBufferObjectGL(null, blurredH);
	private RenderTargetAttachementsConfiguration blurredVFBO = new FrameBufferObjectGL(null, blurredV);

	// private String splashText = getRandomSplashScreen();

	public MainMenu(GameWindow gameWindow)
	{
		super(gameWindow, null);
		selectRandomSkybox();
		
		/*if(askForLogin)
			gameWindow.setLayer(new LoginOverlay(gameWindow, this));
			//currentOverlay = new LoginOverlay(this, null);
		else
			gameWindow.setLayer(new MainMenuOverlay(gameWindow, this));
			//currentOverlay = new MainMenuOverlay(this, null);*/
	}

	/*public MainMenu(GameWindow eng, String string)
	{
		this(eng, false);
		gameWindow.setLayer(new MessageBoxOverlay(gameWindow, this, string));
		//this.changeOverlay(new MessageBoxOverlay(this, currentOverlay, string));
	}*/

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
			InputStreamReader ipsr = new InputStreamReader(Client.getInstance().getContent().getAsset("./splash.txt").read(), "UTF-8");
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
		return "en vrai j'ai jamais trop joué à pokémon";
	}

	@Override
	public void onResize(int newWidth, int newHeight)
	{
		unblurredFBO.resizeFBO(gameWindow.getWidth(), gameWindow.getHeight());
		blurredHFBO.resizeFBO(gameWindow.getWidth(), gameWindow.getHeight());
		blurredVFBO.resizeFBO(gameWindow.getWidth(), gameWindow.getHeight());
	}

	@Override
	public void destroy()
	{
		unblurredFBO.destroy(true);
		blurredHFBO.destroy(true);
		blurredVFBO.destroy(true);
	}

	@Override
	public void render(RenderingInterface renderingContext)
	{
		if(gameWindow.getLayer() == this)
			gameWindow.setLayer( new MainMenuOverlay(gameWindow, this));
		//System.out.println(gameWindow.getLayer());
		
		try // Ugly fps caps yay
		{
			Thread.sleep(33L);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		// Render this shit boy
		
		renderingContext.getRenderTargetManager().setConfiguration(unblurredFBO);
		//unblurredFBO.bind();
		cam.setupUsingScreenSize(gameWindow.getWidth(), gameWindow.getHeight());
		ShaderInterface menuSkyBox = renderingContext.useShader("mainMenuSkyBox");
		//menuSkyBox.use(true);
		cam.setupShader(menuSkyBox);
		
		renderingContext.bindCubemap("skybox", TexturesHandler.getCubemap(skyBox));
		//menuSkyBox.setUniformSamplerCubemap(0, "skyBox", TexturesHandler.getCubemapID(skyBox));
		cam.rotationX = 35 + (float) (Math.sin(cam.rotationY / 15)) * 5f;
		cam.rotationY = (System.currentTimeMillis()%1000000)/200.0f;
		renderingContext.drawFSQuad();
		
		// Blurring to H
		renderingContext.getRenderTargetManager().setConfiguration(blurredHFBO);
		//blurredHFBO.bind();
		ShaderInterface blurH = renderingContext.useShader("blurH");
		//blurH.use(true);
		blurH.setUniform2f("screenSize", gameWindow.getWidth(), gameWindow.getHeight());
		
		renderingContext.bindTexture2D("inputTexture", unblurred);
		//blurH.setUniformSampler(0, "inputTexture", unblurred.getId());
		renderingContext.drawFSQuad();

		for (int i = 0; i < 1; i++)
		{
			renderingContext.getRenderTargetManager().setConfiguration(blurredVFBO);
			//blurredVFBO.bind();
			ShaderInterface blurV = renderingContext.useShader("blurV");
			//blurV.use(true);
			blurV.setUniform1f("lookupScale", 1);
			blurV.setUniform2f("screenSize", gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
			
			//blurV.setUniformSampler(0, "inputTexture", blurredH.getId());
			renderingContext.bindTexture2D("inputTexture", blurredH);
			renderingContext.drawFSQuad();

			renderingContext.getRenderTargetManager().setConfiguration(blurredHFBO);
			//blurredHFBO.bind();
			blurH = renderingContext.useShader("blurH");
			//blurH.use(true);
			blurH.setUniform2f("screenSize", gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
			//blurH.setUniformSampler(0, "inputTexture", blurredV.getId());
			renderingContext.bindTexture2D("inputTexture", blurredV);
			renderingContext.drawFSQuad();
		}

		renderingContext.getRenderTargetManager().setConfiguration(blurredVFBO);
		//blurredVFBO.bind();
		ShaderInterface blurV = renderingContext.useShader("blurV");
		//blurV.use(true);
		blurV.setUniform2f("screenSize", gameWindow.getWidth(), gameWindow.getHeight());
		//blurV.setUniformSampler(0, "inputTexture", blurredH.getId());
		renderingContext.bindTexture2D("inputTexture", blurredH);
		renderingContext.drawFSQuad();
		//blurV.use(false);

		renderingContext.getRenderTargetManager().setConfiguration(null);
		//FrameBufferObject.unbind();
		ShaderInterface blit = renderingContext.useShader("background");
		//blit.use(true);
		blit.setUniform2f("screenSize", gameWindow.getWidth(), gameWindow.getHeight());
		//blit.setUniformSampler(0, "diffuseTexture", blurredV.getId());
		//renderingContext.bindTexture2D("inputTexture", blurredV);
		Texture2DGL backgroundTexture = TexturesHandler.getTexture("./textures/gui/darker.png");
		backgroundTexture.setLinearFiltering(false);
		renderingContext.bindTexture2D("diffuseTexture", backgroundTexture);
		renderingContext.bindTexture2D("backgroundTexture", blurredV);
		renderingContext.drawFSQuad();
		//FrameBufferObject.unbind();

		Texture2DGL logoTexture = TexturesHandler.getTexture("./textures/gui/icon.png");
		//System.out.println(logoTexture.getId());
		float alphaIcon = (float) (0.25 + Math.sin((System.currentTimeMillis() % (1000 * 60 * 60) / 3000f)) * 0.25f);
		renderingContext.setBlendMode(BlendMode.MIX);
		float diagonal = (float) Math.sqrt(gameWindow.getWidth() * gameWindow.getWidth() + gameWindow.getHeight() * gameWindow.getHeight());
		float iconSize = (float) (diagonal / 3 + 50 * Math.sin((System.currentTimeMillis() % (1000 * 60 * 60) / 30000f)));
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(gameWindow.getWidth() / 2 - iconSize / 2, gameWindow.getHeight() / 2 - iconSize / 2, gameWindow.getWidth() / 2 + iconSize / 2, gameWindow.getHeight() / 2 + iconSize / 2, 0, 1, 1, 0, logoTexture, true, true, new Vector4f(1.0f, 1.0f, 1.0f, alphaIcon));
		//renderingContext.getGuiRenderer().drawBuffer();
		
		//TODO swap out
		//currentOverlay.drawToScreen(renderingContext, 0, 0, gameWindow.getWidth(), gameWindow.getHeight());
	}
}
