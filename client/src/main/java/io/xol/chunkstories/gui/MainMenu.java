//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.BlendMode;
import io.xol.chunkstories.api.rendering.shader.Shader;
import io.xol.chunkstories.api.rendering.target.RenderTargetsConfiguration;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.overlays.MainMenuOverlay;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.opengl.fbo.FrameBufferObjectGL;
import io.xol.chunkstories.renderer.opengl.texture.Texture2DGL;
import io.xol.chunkstories.renderer.opengl.texture.Texture2DRenderTargetGL;
import io.xol.chunkstories.renderer.opengl.texture.TexturesHandler;

public class MainMenu extends Layer
{
	// Stuff for rendering the background

	String skyBox;
	Camera cam = new Camera();

	private Texture2DRenderTargetGL unblurred = new Texture2DRenderTargetGL(TextureFormat.RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());
	private Texture2DRenderTargetGL blurredH = new Texture2DRenderTargetGL(TextureFormat.RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());
	private Texture2DRenderTargetGL blurredV = new Texture2DRenderTargetGL(TextureFormat.RGBA_8BPP, gameWindow.getWidth(), gameWindow.getHeight());

	private RenderTargetsConfiguration unblurredFBO = new FrameBufferObjectGL(null, unblurred);
	private RenderTargetsConfiguration blurredHFBO = new FrameBufferObjectGL(null, blurredH);
	private RenderTargetsConfiguration blurredVFBO = new FrameBufferObjectGL(null, blurredV);

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
		return "en vrai j'ai jamais trop jou� �pok�mon";
	}

	@Override
	public void onResize(int newWidth, int newHeight)
	{
		unblurredFBO.resize(gameWindow.getWidth(), gameWindow.getHeight());
		blurredHFBO.resize(gameWindow.getWidth(), gameWindow.getHeight());
		blurredVFBO.resize(gameWindow.getWidth(), gameWindow.getHeight());
	}

	@Override
	public void destroy()
	{
		unblurredFBO.destroy();
		blurredHFBO.destroy();
		blurredVFBO.destroy();
	}

	@Override
	public void render(RenderingInterface renderingContext)
	{
		if(gameWindow.getLayer() == this)
			gameWindow.setLayer( new MainMenuOverlay(gameWindow, this));
		
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
		cam.setupUsingScreenSize(gameWindow.getWidth(), gameWindow.getHeight());
		Shader menuSkyBox = renderingContext.useShader("mainMenuSkyBox");
		cam.setupShader(menuSkyBox);
		
		renderingContext.bindCubemap("skybox", TexturesHandler.getCubemap(skyBox));
		cam.rotationX = 35 + (float) (Math.sin(cam.rotationY / 15)) * 5f;
		cam.rotationY = (System.currentTimeMillis()%1000000)/200.0f;
		renderingContext.drawFSQuad();
		
		renderingContext.getRenderTargetManager().setConfiguration(blurredHFBO);
		Shader blurH = renderingContext.useShader("blurH");
		blurH.setUniform2f("screenSize", gameWindow.getWidth(), gameWindow.getHeight());
		
		renderingContext.bindTexture2D("inputTexture", unblurred);
		renderingContext.drawFSQuad();

		for (int i = 0; i < 1; i++)
		{
			renderingContext.getRenderTargetManager().setConfiguration(blurredVFBO);
			Shader blurV = renderingContext.useShader("blurV");
			blurV.setUniform1f("lookupScale", 1);
			blurV.setUniform2f("screenSize", gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
			
			renderingContext.bindTexture2D("inputTexture", blurredH);
			renderingContext.drawFSQuad();

			renderingContext.getRenderTargetManager().setConfiguration(blurredHFBO);
			blurH = renderingContext.useShader("blurH");
			blurH.setUniform2f("screenSize", gameWindow.getWidth() / 2, gameWindow.getHeight() / 2);
			renderingContext.bindTexture2D("inputTexture", blurredV);
			renderingContext.drawFSQuad();
		}

		renderingContext.getRenderTargetManager().setConfiguration(blurredVFBO);
		Shader blurV = renderingContext.useShader("blurV");
		blurV.setUniform2f("screenSize", gameWindow.getWidth(), gameWindow.getHeight());
		renderingContext.bindTexture2D("inputTexture", blurredH);
		renderingContext.drawFSQuad();

		renderingContext.getRenderTargetManager().setConfiguration(null);
		Shader blit = renderingContext.useShader("background");
		blit.setUniform2f("screenSize", gameWindow.getWidth(), gameWindow.getHeight());
		Texture2DGL backgroundTexture = TexturesHandler.getTexture("./textures/gui/darker.png");
		backgroundTexture.setLinearFiltering(false);
		renderingContext.bindTexture2D("diffuseTexture", backgroundTexture);
		renderingContext.bindTexture2D("backgroundTexture", blurredV);
		renderingContext.drawFSQuad();

		Texture2DGL logoTexture = TexturesHandler.getTexture("./textures/gui/icon.png");
		float alphaIcon = (float) (0.25 + Math.sin((System.currentTimeMillis() % (1000 * 60 * 60) / 3000f)) * 0.25f);
		renderingContext.setBlendMode(BlendMode.MIX);
		float diagonal = (float) Math.sqrt(gameWindow.getWidth() * gameWindow.getWidth() + gameWindow.getHeight() * gameWindow.getHeight());
		float iconSize = (float) (diagonal / 3 + 50 * Math.sin((System.currentTimeMillis() % (1000 * 60 * 60) / 30000f)));
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(gameWindow.getWidth() / 2 - iconSize / 2, gameWindow.getHeight() / 2 - iconSize / 2, gameWindow.getWidth() / 2 + iconSize / 2, gameWindow.getHeight() / 2 + iconSize / 2, 0, 1, 1, 0, logoTexture, true, true, new Vector4f(1.0f, 1.0f, 1.0f, alphaIcon));
		
	}
}
