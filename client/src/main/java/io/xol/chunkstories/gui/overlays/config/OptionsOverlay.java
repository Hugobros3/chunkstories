package io.xol.chunkstories.gui.overlays.config;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.xol.chunkstories.api.Content.LocalizationManager;
import io.xol.chunkstories.api.gui.GuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.gui.Ingame;
import io.xol.chunkstories.input.lwjgl3.Lwjgl3KeyBind;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.gui.elements.Button;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class OptionsOverlay extends Layer 
{
	//GuiElementsHandler guiHandler = new GuiElementsHandler();
	Button exitButton = new Button(this, 0, 0, 300, 32, ("#{menu.back}"), BitmapFont.SMALLFONTS, 1);
	
	List<ConfigTab> configTabs = new ArrayList<ConfigTab>();

	abstract class ConfigButton extends Button
	{
		Runnable run = null;
	
		public ConfigButton setApplyAction(Runnable run)
		{
			this.run = run;
			return this;
		}
		
		public void apply()
		{
			save();
			RenderingConfig.define();
			if(run != null)
				run.run();
		}
		
		String parameter;
		String value;

		public ConfigButton(String n)
		{
			super(OptionsOverlay.this, 0, 0, 320, 32, n, BitmapFont.SMALLFONTS, 1);
			this.parameter = n;
			this.value = Client.getInstance().getConfig().getProp(parameter, value);
		}

		public void updateText()
		{
			this.text = locMgr.getLocalizedString(parameter) + " : " + value;
		}

		public abstract void onClick(float posx, float posy, int button);

		public void save()
		{
			Client.getInstance().getConfig().setString(parameter, value);
		}
	}

	class ConfigButtonToggle extends ConfigButton
	{

		public ConfigButtonToggle(String n)
		{
			super(n);
			value = Client.getInstance().getConfig().getBoolean(n, false)+"";
		}

		@Override
		public void onClick(float posx, float posy, int button)
		{
			value = !Boolean.parseBoolean(value) + "";
		}

	}

	class ConfigButtonMultiChoice extends ConfigButton
	{
		String values[];
		int cuVal = 0;

		public ConfigButtonMultiChoice(String n, String values[])
		{
			super(n);
			this.values = values;
			for (int i = 0; i < values.length; i++)
			{
				if (values[i].equals(value))
					cuVal = i;
			}
		}

		@Override
		public void onClick(float posx, float posy, int button)
		{
			if (button == 0)
				cuVal++;
			else
				cuVal--;
			if (cuVal < 0)
				cuVal = values.length - 1;
			if (cuVal >= values.length)
				cuVal = 0;
			value = values[cuVal];
		}

	}

	class ConfigButtonKey extends ConfigButton
	{
		Lwjgl3KeyBind kbi;
		OptionsOverlay options;

		public ConfigButtonKey(Lwjgl3KeyBind kbi, OptionsOverlay options)
		{
			super("bind."+kbi.getName());
			this.kbi = kbi;
			this.options = options;
		}

		@Override
		public void updateText()
		{
			this.text = locMgr.getLocalizedString(parameter) + " : " + glfwGetKeyName(Integer.parseInt(value), 0);//Keyboard.getKeyName(Integer.parseInt(value));
		}

		@Override
		public void onClick(float posx, float posy, int button)
		{
			options.gameWindow.setLayer(new KeyBindSelectionOverlay(options.gameWindow, options, this));
		}

		public void callBack(int key)
		{
			value = key + "";
			apply();
			kbi.reload();
			//options.applyAndSave();
		}

	}

	class ConfigButtonScale extends ConfigButton
	{
		float min,max,steps;
		
		public ConfigButtonScale(String n, float min, float max, float steps)
		{
			super(n);
			this.min = min;
			this.max = max;
			this.steps = steps;
		}

		@Override
		public void updateText()
		{
			this.text = locMgr.getLocalizedString(parameter) + " : " + value;
		}

		@Override
		public void onClick(float posx, float posy, int button)
		{
			float relPos = posx - this.xPosition + 8;
			float scaled = (float) (160 + Math.min(160, Math.max(-160.0, relPos))) / 320.0f;
			scaled *= (max-min);
			scaled += min;
			scaled /= steps;
			scaled = (float) (Math.floor(scaled)) * steps;
					
			value = scaled+"";
			//options.mainScene.changeOverlay(new KeyBindSelectionOverlay(options.mainScene, options, this));
		}
		
		@Override
		public void render(RenderingInterface renderer)
		{
			int textWidth = FontRenderer2.getTextLengthUsingFont(size * 16, text, font);
			if (width < 0)
			{
				width = textWidth;
			}
			int textDekal = -textWidth;
			TexturesHandler.getTexture("./textures/gui/scalableField.png").setLinearFiltering(false);
			CorneredBoxDrawer.drawCorneredBoxTiled(xPosition - 4, yPosition, width + 8, height + 16, 8, "./textures/gui/scalableField.png", 32, 2);
			ObjectRenderer.renderTexturedRect(xPosition - 160 + 320 * (Float.parseFloat(value)-min)/(max-min), yPosition, 64, 64, 0, 0, 32, 32, 32, "./textures/gui/barCursor.png");
			FontRenderer2.drawTextUsingSpecificFont(textDekal + xPosition, yPosition - height / 2, 0, size * 32, text, font);
			//return width * 2 * size - 12;
		}

	}
	
	class ConfigTab
	{
		String name;
		ConfigButton[] configButtons;

		public ConfigTab(String name, ConfigButton[] buttons)
		{
			this.name = name;
			this.configButtons = buttons;
		}
	}

	List<Button> tabsButtons = new ArrayList<Button>();
	int selectedConfigTab = 0;
	private final LocalizationManager locMgr;

	public OptionsOverlay(GameWindow scene, Layer parent)
	{
		super(scene, parent);
		
		locMgr = Client.getInstance().getContent().localization();
		
		exitButton.setAction(new Runnable() {

			@Override
			public void run() {
				//applyAndSave();
				Client.getInstance().getConfig().save();
				gameWindow.setLayer(parentLayer);
			}
			
		});
		elements.add(exitButton);

		configTabs.add(new ConfigTab("#{Rendering}", new ConfigButton[] { 
				new ConfigButtonMultiChoice("viewDistance",new String[] { "64", "96", "128", "144", "160", "192", "224", "256", "512", "768" }),
				new ConfigButtonToggle("doRealtimeReflections").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						ShadersLibrary.getShaderProgram("shadows_apply").reload(RenderingConfig.getShaderConfig());
					}
				}),
				new ConfigButtonToggle("doDynamicCubemaps").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						ShadersLibrary.getShaderProgram("shadows_apply").reload(RenderingConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("terrain").reload(RenderingConfig.getShaderConfig());
					}
				}),
				new ConfigButtonToggle("doShadows").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						ShadersLibrary.getShaderProgram("shadows_apply").reload(RenderingConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("terrain").reload(RenderingConfig.getShaderConfig());
					}
				}),
				new ConfigButtonMultiChoice("shadowMapResolutions", new String[] { "512", "1024", "2048", "4096" }).setApplyAction(new Runnable(){
						@Override
						public void run()
						{
							if (parentLayer.getRootLayer() instanceof Ingame && shouldReload){
								Client.getInstance().getWorld().getWorldRenderer().resizeShadowMaps();
							//gps.worldRenderer.resizeShadowMaps();
						}
					}
				}),
				new ConfigButtonToggle("dynamicGrass").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						ShadersLibrary.getShaderProgram("blocks_opaque").reload(RenderingConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("postprocess").reload(RenderingConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("shadows").reload(RenderingConfig.getShaderConfig());
					}
				}),
				new ConfigButtonToggle("hqTerrain").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						ShadersLibrary.getShaderProgram("terrain").reload(RenderingConfig.getShaderConfig());
					}
				}),
				/*new ConfigButtonToggle("rainyMode").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						if(Client.world != null)
							Client.world.setWeather(Client.getInstance().getConfig().getBooleanProp("rainyMode", false));
					}
				}),*/
				/*new ConfigButtonToggle("perPixelFresnel").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						ShadersLibrary.getShaderProgram("blocks_opaque").reload(RenderingConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("blocks_liquid_pass1").reload(RenderingConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("blocks_liquid_pass2").reload(RenderingConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("entities").reload(RenderingConfig.getShaderConfig());
					}
				}),
				new ConfigButtonToggle("doClouds").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						ShadersLibrary.reloadAllShaders();
					}
				}),*/
				/*new ConfigButtonMultiChoice("ssaoQuality", new String[] { "0", "1", "2"}).setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						ShadersLibrary.getShaderProgram("blocks_opaque").reload(FastConfig.getShaderConfig());
						ShadersLibrary.getShaderProgram("shadows_apply").reload(FastConfig.getShaderConfig());
					}
				}),*/
				new ConfigButtonToggle("doBloom").setApplyAction(new Runnable(){
						@Override
						public void run()
						{
							if (parentLayer instanceof Ingame && shouldReload){
								Client.getInstance().getWorld().getWorldRenderer().setupRenderSize();
							ShadersLibrary.getShaderProgram("postprocess").reload(RenderingConfig.getShaderConfig());
						}
					}
				}),
				new ConfigButtonMultiChoice("framerate",new String[] { "30", "60", "120", "-1" }).setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						Client.getInstance().getGameWindow().setTargetFPS(Client.getInstance().getConfig().getInteger("framerate", -1));
					}
				}),
				}));

		//for (String loc : Client.getInstance().getContent().localization().listTranslations())
		Collection<String> translationsCollection = Client.getInstance().getContent().localization().listTranslations();
		String[] translations = new String[translationsCollection.size()];
		int z = 0;
		for(String loc : translationsCollection)
		{
			translations[z] = loc;
			z++;
		}
		
		configTabs.add(new ConfigTab("#{Video}", new ConfigButton[] {
				new ConfigButtonScale("fov", 25f, 85f, 1f),
				new ConfigButtonToggle("fullScreen"),
				new ConfigButtonMultiChoice("fullScreenResolution", Client.getInstance().getGameWindow().getDisplayModes()),
				new ConfigButtonMultiChoice("language", translations).setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						String code = Client.getInstance().getConfig().getProp("language", "en");
						Client.getInstance().configDeprecated().setString("language", code);
						Client.getInstance().getContent().localization().loadTranslation(code);
					}
				}),
				}));

		
		List<ConfigButton> controlsButtons = new ArrayList<ConfigButton>();
		controlsButtons.add(new ConfigButtonScale("mouseSensitivity", 0.5f, 2f, 0.05f));
		
		Iterator<Input> inputsIterator = Client.getInstance().getInputsManager().getAllInputs();
		while(inputsIterator.hasNext())
		{
			Input keyBind = inputsIterator.next();
			if(keyBind instanceof Lwjgl3KeyBind)
			{
				Lwjgl3KeyBind kbi = (Lwjgl3KeyBind)keyBind;
				if(kbi.isEditable())
					controlsButtons.add(new ConfigButtonKey(kbi, this));
			}
		}
		
		ConfigButton[] controlsButtonsArray = new ConfigButton[controlsButtons.size()];
		int i = 0;
		for(ConfigButton cb : controlsButtons)
		{
			controlsButtonsArray[i] = cb;
			i++;
		}
		
		configTabs.add(new ConfigTab("#{Controls}", controlsButtonsArray));
		
		//TODO sound config ?
		configTabs.add(new ConfigTab("#{Sound}", new ConfigButton[] {}));
		
		if(RenderingConfig.isDebugAllowed)
		{
			configTabs.add(new ConfigTab("#{Debug}", new ConfigButton[] { 
					new ConfigButtonToggle("debugGBuffers").setApplyAction(new Runnable(){
						@Override
						public void run()
						{
							ShadersLibrary.getShaderProgram("postprocess").reload(RenderingConfig.getShaderConfig());
						}
					}),
					new ConfigButtonToggle("physicsVisualization"),
					new ConfigButtonToggle("showDebugInfo"),
					new ConfigButtonToggle("frametimeGraph"),
					new ConfigButtonMultiChoice("log-policy",new String[] { "send", "dont" }),
					}));
		}
		else
		{
			configTabs.add(new ConfigTab("#{Debug}", new ConfigButton[] { 
					
					//No cheat-allowing debug functions
					new ConfigButtonToggle("showDebugInfo"),
					new ConfigButtonToggle("frametimeGraph"),
					new ConfigButtonMultiChoice("log-policy",new String[] { "send", "dont" }),
					}));
		}

		int configTabIndex = 0;
		for (ConfigTab tab : configTabs)
		{
			for (GuiElement f : tab.configButtons)
				elements.add(f);
			
			String txt = tab.name;
			int txtlen = FontRenderer2.getTextLengthUsingFont(32, txt, BitmapFont.SMALLFONTS);
			Button tabButton = new Button(this, 0, 0, txtlen + 32, 32, txt, BitmapFont.SMALLFONTS, 1);
			
			final int configTabIndex2 = configTabIndex;
			tabButton.setAction(new Runnable() {

				@Override
				public void run() {
					selectedConfigTab = configTabIndex2;
				}
				
			});
			
			configTabIndex++;
			
			tabsButtons.add(tabButton);
			
			elements.add(tabButton);
		}
	}

	@Override
	public void render(RenderingInterface renderer)
	{
		parentLayer.render(renderer);
		
		int optionsPanelSize = 320 * 2 + 32 + 64;
		
		renderer.getGuiRenderer().drawBoxWindowsSpace(0, 0, renderer.getWindow().getWidth(), renderer.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4fm(0.0, 0.0, 0.0, 0.5));
		renderer.getGuiRenderer().drawBoxWindowsSpace(renderer.getWindow().getWidth() / 2.0f - optionsPanelSize / 2, 0, renderer.getWindow().getWidth()  / 2 + optionsPanelSize / 2, renderer.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4fm(0.0, 0.0, 0.0, 0.5));
		
		//ObjectRenderer.renderColoredRect(renderingContext.getWindow().getWidth() / 2, renderingContext.getWindow().getHeight() / 2, renderingContext.getWindow().getWidth(), renderingContext.getWindow().getHeight(), 0, "000000", 0.5f);
		//ObjectRenderer.renderColoredRect(renderingContext.getWindow().getWidth() / 2, renderingContext.getWindow().getHeight() / 2, optionsPanelSize, renderingContext.getWindow().getHeight(), 0, "000000", 0.25f);

		int dekal = 0;
		int i = 0;
		for (Button b : tabsButtons)
		{
			dekal += b.getWidth() + 32 + 16;
			b.setPosition(renderer.getWindow().getWidth() / 2 - optionsPanelSize / 2 + dekal, renderer.getWindow().getHeight() - 128);
			b.render(renderer);
			dekal += b.getWidth();
			
			//if (b.clicked())
			//	selectedConfigTab = i;
			
			i++;
		}

		ConfigTab currentConfigTab = configTabs.get(selectedConfigTab);
		int a = 0, b = 0;
		int startPosX = renderer.getWindow().getWidth() / 2 - optionsPanelSize / 2 + 160 + 32;
		int startPosY = renderer.getWindow().getHeight() - 128 - 64;
		
		Mouse mouse = gameWindow.getInputsManager().getMouse();
		
		for (ConfigButton c : currentConfigTab.configButtons)
		{
			c.setPosition(startPosX + b * (320 + 32), startPosY - (float) Math.floor(a / 2) * 64);
			c.updateText();
			c.render(renderer);
			if(c instanceof ConfigButtonScale && c.isMouseOver() && mouse.getMainButton().isPressed())
			{
				ConfigButtonScale cs = (ConfigButtonScale)c;
				cs.onClick(mouse.getCursorX(), mouse.getCursorY(), 0);
				cs.apply();
				//applyAndSave();
			}
			
			a++;
			b = a % 2;
		}

		FontRenderer2.drawTextUsingSpecificFont(renderer.getWindow().getWidth() / 2 - optionsPanelSize / 2 + 32, renderer.getWindow().getHeight() - 48 * 2, 0, 48, "Options menu", BitmapFont.SMALLFONTS);

		exitButton.setPosition(renderer.getWindow().getWidth() / 2, 48);
		exitButton.render(renderer);

		if(currentConfigTab.name.contains("Rendering") || currentConfigTab.name.equals("") || currentConfigTab.name.contains("Debug"))
			shouldReload = true;
	}

	/*@Override
	public boolean handleKeypress(int k)
	{
		//TODO handleKeypress to take KeyBind
		
		//if (Client.getInstance().getInputsManager().getInputByName("exit").isPressed())
		if (Client.getInstance().getInputsManager().getInputByName("exit").isPressed())
		{
			Client.getInstance().getConfig().save();
			mainScene.changeOverlay(parent);
			return true;
		}
		guiHandler.handleInput(k);
		return true;
	}

	@Override
	public boolean onClick(int posx, int posy, int button)
	{
		if (button == 0)
			guiHandler.handleClick(posx, posy);
		if (guiHandler.getFocusedObject() != null && guiHandler.getFocusedObject() instanceof ConfigButton)
		{
			((ConfigButton) guiHandler.getFocusedObject()).onClick(posx, posy, button);
		}
		for (ConfigButton b : configTabs.get(selectedConfigTab).configButtons)
		{
			if (b.isMouseOver())
			{
				b.onClick(posx, posy, button);
				b.apply();
				//applyAndSave();
			}
		}
		return true;
	}*/
	
	@Override
	public boolean handleInput(Input input) {
		if(input.equals("exit"))
		{
			Client.getInstance().getConfig().save();
			gameWindow.setLayer(parentLayer);
			return true;
		} else if(input instanceof MouseButton) {
			MouseButton mb = (MouseButton)input;
			for (ConfigButton b : configTabs.get(selectedConfigTab).configButtons)
			{
				if (b.isMouseOver())
				{
					b.onClick(mb.getMouse().getCursorX(), mb.getMouse().getCursorY(), mb.getName().equals("mouse.left") ? 0 : 1);
					b.apply();
					//applyAndSave();
				}
			}
		}
		
		super.handleInput(input);
		
		return true;
	}

	boolean shouldReload = false;
}
