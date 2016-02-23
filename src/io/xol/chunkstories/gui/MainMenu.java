package io.xol.chunkstories.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.gui.menus.LoginOverlay;
import io.xol.chunkstories.gui.menus.MainMenuOverlay;
import io.xol.chunkstories.gui.menus.MessageBoxOverlay;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.FBO;
import io.xol.engine.base.ObjectRenderer;
import io.xol.engine.base.XolioWindow;
import io.xol.engine.gui.GuiDrawer;
import io.xol.engine.shaders.ShaderProgram;
import io.xol.engine.shaders.ShadersLibrary;
import io.xol.engine.textures.GBufferTexture;
import io.xol.engine.textures.Texture;
import io.xol.engine.textures.TexturesHandler;

public class MainMenu extends OverlayableScene
{
	// GUI Objects
	Overlay currentOverlay = new MainMenuOverlay(this, null);

	// Stuff for rendering the background
	ShaderProgram blurH;
	ShaderProgram blurV;
	ShaderProgram menuSkyBox;
	ShaderProgram blit;

	String skyBox;
	Camera cam = new Camera();

	private GBufferTexture unblurred = new GBufferTexture(Texture.TextureType.RGBA_8BPP, XolioWindow.frameW, XolioWindow.frameH);
	private GBufferTexture blurredH = new GBufferTexture(Texture.TextureType.RGBA_8BPP, XolioWindow.frameW, XolioWindow.frameH);
	private GBufferTexture blurredV = new GBufferTexture(Texture.TextureType.RGBA_8BPP, XolioWindow.frameW, XolioWindow.frameH);

	private FBO unblurredFBO = new FBO(null, unblurred);
	private FBO blurredHFBO = new FBO(null, blurredH);
	private FBO blurredVFBO = new FBO(null, blurredV);

	// private String splashText = getRandomSplashScreen();

	public MainMenu(XolioWindow XolioWindow, boolean askForLogin)
	{
		super(XolioWindow);
		menuSkyBox = ShadersLibrary.getShaderProgram("mainMenuSkyBox");
		blurH = ShadersLibrary.getShaderProgram("blurH");
		blurV = ShadersLibrary.getShaderProgram("blurV");
		blit = ShadersLibrary.getShaderProgram("blit");
		selectRandomSkybox();
		if(askForLogin)
			currentOverlay = new LoginOverlay(this, null);
	}

	public MainMenu(XolioWindow eng, String string)
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
			skyBox = "./res/textures/skybox";
		} else
		{
			// Choose a random one.
			Random rnd = new Random();
			skyBox = "./skyboxscreens/" + possibleSkyboxes[rnd.nextInt(possibleSkyboxes.length)];
		}
		cam.view_rotx = 25;
		cam.view_roty = -45;
	}

	String getRandomSplashScreen()
	{
		List<String> splashes = new ArrayList<String>();
		try
		{
			InputStream ips = new FileInputStream(new File("res/splash.txt"));
			InputStreamReader ipsr = new InputStreamReader(ips, "UTF-8");
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

	public void onResize()
	{
		unblurredFBO.resizeFBO(XolioWindow.frameW, XolioWindow.frameH);
		blurredHFBO.resizeFBO(XolioWindow.frameW, XolioWindow.frameH);
		blurredVFBO.resizeFBO(XolioWindow.frameW, XolioWindow.frameH);
	}

	public void destroy()
	{
		unblurredFBO.destroy(true);
		blurredHFBO.destroy(true);
		blurredVFBO.destroy(true);
	}

	public void update()
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
		unblurredFBO.bind();
		cam.justSetup();
		menuSkyBox.use(true);
		cam.setupShader(menuSkyBox);
		menuSkyBox.setUniformSamplerCubemap(0, "skyBox", TexturesHandler.getCubemapID(skyBox));
		cam.view_rotx = 35 + (float) (Math.sin(cam.view_roty / 15)) * 5f;
		cam.view_roty = (System.currentTimeMillis()%1000000)/200.0f;
		ObjectRenderer.drawFSQuad(menuSkyBox.getVertexAttributeLocation("vertexIn"));
		
		// Blurring to H
		blurredHFBO.bind();
		blurH.use(true);
		blurH.setUniformFloat2("screenSize", XolioWindow.frameW, XolioWindow.frameH);
		blurH.setUniformSampler(0, "inputTexture", unblurred.getID());
		ObjectRenderer.drawFSQuad(blurH.getVertexAttributeLocation("vertexIn"));

		for (int i = 0; i < 1; i++)
		{
			blurredVFBO.bind();
			blurV.use(true);
			blurV.setUniformFloat("lookupScale", 1);
			blurV.setUniformFloat2("screenSize", XolioWindow.frameW / 2, XolioWindow.frameH / 2);
			blurV.setUniformSampler(0, "inputTexture", blurredH.getID());
			ObjectRenderer.drawFSQuad(blurV.getVertexAttributeLocation("vertexIn"));

			blurredHFBO.bind();
			blurH.use(true);
			blurH.setUniformFloat2("screenSize", XolioWindow.frameW / 2, XolioWindow.frameH / 2);
			blurH.setUniformSampler(0, "inputTexture", blurredV.getID());
			ObjectRenderer.drawFSQuad(blurH.getVertexAttributeLocation("vertexIn"));
		}

		blurredVFBO.bind();
		blurV.use(true);
		blurV.setUniformFloat2("screenSize", XolioWindow.frameW, XolioWindow.frameH);
		blurV.setUniformSampler(0, "inputTexture", blurredH.getID());
		ObjectRenderer.drawFSQuad(blurV.getVertexAttributeLocation("vertexIn"));
		blurV.use(false);

		FBO.unbind();
		blit.use(true);
		blit.setUniformFloat2("screenSize", XolioWindow.frameW, XolioWindow.frameH);
		blit.setUniformSampler(0, "inputTexture", blurredV.getID());
		ObjectRenderer.drawFSQuad(blit.getVertexAttributeLocation("vertexIn"));
		blit.use(false);

		//XolioWindow.setup2d();
		// ObjectRenderer.renderTexturedRectAlpha(256,XolioWindow.frameH/2,
		// 512,XolioWindow.frameH,"gui/menufade", 1f);
		// Place buttons

		currentOverlay.drawToScreen(0, 0, XolioWindow.frameW, XolioWindow.frameH);

		// float time2 = ((System.currentTimeMillis()%5000)/100f);
		// CorneredBoxDrawer.drawCorneredBoxTiled(550, 250,
		// 232-(int)(Math.sin(time2*0.5+4)*33),
		// 188+(int)(Math.sin(time2*0.5)*30), 8, "gui/debug", 32, 2);

		// System.out.println("now");
		// GuiDrawer.debugDraw();
		// GuiDrawer.drawBox(-1, -1, 1, 1, 0, 1, 1, 0,
		// TexturesHandler.idTexture("res/textures/logo.png"), false, new
		// Vector4f(1f, 1f, 1f, 1f));
		GuiDrawer.drawBuffer();
		// System.out.println("notnow");
	}

	public boolean onClick(int posx, int posy, int button)
	{
		if (currentOverlay != null)
			return currentOverlay.onClick(posx, posy, button);
		return true;
	}

	public boolean onKeyPress(int k)
	{
		if (currentOverlay != null && currentOverlay.handleKeypress(k))
		{
			return true;
		}
		return false;
	}
	
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
