package io.xol.chunkstories.gui.overlays.config;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.gui.Ingame;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.input.lwjgl2.KeyBindImplementation;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.GuiElement;
import io.xol.engine.math.lalgb.vector.sp.Vector4fm;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class OptionsOverlay extends Overlay
{
	//GuiElementsHandler guiHandler = new GuiElementsHandler();
	Button exitButton = new Button(0, 0, 300, 32, ("Back"), BitmapFont.SMALLFONTS, 1);

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
			super(0, 0, 320, 32, n, BitmapFont.SMALLFONTS, 1);
			this.parameter = n;
			this.value = Client.getConfig().getProp(parameter, value);
		}

		public void updateText()
		{
			this.text = parameter + " : " + value;
		}

		public abstract void onClick(int posx, int posy, int button);

		public void save()
		{
			Client.getConfig().setString(parameter, value);
		}
	}

	class ConfigButtonToggle extends ConfigButton
	{

		public ConfigButtonToggle(String n)
		{
			super(n);
			value = Client.getConfig().getBoolean(n, false)+"";
		}

		@Override
		public void onClick(int posx, int posy, int button)
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
		public void onClick(int posx, int posy, int button)
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
		KeyBindImplementation kbi;
		OptionsOverlay options;

		public ConfigButtonKey(KeyBindImplementation kbi, OptionsOverlay options)
		{
			super("bind."+kbi.getName());
			this.kbi = kbi;
			this.options = options;
		}

		@Override
		public void updateText()
		{
			this.text = parameter + " : " + Keyboard.getKeyName(Integer.parseInt(value));
		}

		@Override
		public void onClick(int posx, int posy, int button)
		{
			options.mainScene.changeOverlay(new KeyBindSelectionOverlay(options.mainScene, options, this));
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
			this.text = parameter + " : " + value;
		}

		@Override
		public void onClick(int posx, int posy, int button)
		{
			float relPos = posx - this.posx + 8;
			float scaled = (float) (160 + Math.min(160, Math.max(-160.0, relPos))) / 320.0f;
			scaled *= (max-min);
			scaled += min;
			scaled /= steps;
			scaled = (float) (Math.floor(scaled)) * steps;
					
			value = scaled+"";
			//options.mainScene.changeOverlay(new KeyBindSelectionOverlay(options.mainScene, options, this));
		}
		
		@Override
		public int draw()
		{
			int textWidth = FontRenderer2.getTextLengthUsingFont(size * 16, text, font);
			if (width < 0)
			{
				width = textWidth;
			}
			int textDekal = -textWidth;
			TexturesHandler.getTexture("./textures/gui/scalableField.png").setLinearFiltering(false);
			CorneredBoxDrawer.drawCorneredBoxTiled(posx - 4, posy, width + 8, height + 16, 8, "./textures/gui/scalableField.png", 32, 2);
			ObjectRenderer.renderTexturedRect(posx - 160 + 320 * (Float.parseFloat(value)-min)/(max-min), posy, 64, 64, 0, 0, 32, 32, 32, "./textures/gui/barCursor.png");
			FontRenderer2.drawTextUsingSpecificFont(textDekal + posx, posy - height / 2, 0, size * 32, text, font);
			return width * 2 * size - 12;
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

	public OptionsOverlay(OverlayableScene scene, Overlay parent)
	{
		super(scene, parent);
		guiHandler.add(exitButton);

		configTabs.add(new ConfigTab("Rendering", new ConfigButton[] { 
				new ConfigButtonMultiChoice("viewDistance",new String[] { "64", "96", "128", "144", "160", "192", "224", "256" }),
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
					{if (mainScene instanceof Ingame && shouldReload){
						Client.getInstance().getWorld().getWorldRenderer().resizeShadowMaps();
						//gps.worldRenderer.resizeShadowMaps();
					}}
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
							Client.world.setWeather(Client.getConfig().getBooleanProp("rainyMode", false));
					}
				}),*/
				new ConfigButtonToggle("perPixelFresnel").setApplyAction(new Runnable(){
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
				}),
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
					{if (mainScene instanceof Ingame && shouldReload){
						Client.getInstance().getWorld().getWorldRenderer().setupRenderSize(GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
					}
					ShadersLibrary.getShaderProgram("postprocess").reload(RenderingConfig.getShaderConfig()); }
				}),
				new ConfigButtonMultiChoice("framerate",new String[] { "30", "60", "120", "-1" }).setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						GameWindowOpenGL.setTargetFPS(Client.getConfig().getInteger("framerate", -1));
					}
				}),
				}));

		configTabs.add(new ConfigTab("Video", new ConfigButton[] {
				new ConfigButtonScale("fov", 25f, 85f, 1f),
				new ConfigButtonToggle("fullScreen"),
				new ConfigButtonMultiChoice("fullScreenResolution", GameWindowOpenGL.getDisplayModes()),
				}));

		
		List<ConfigButton> controlsButtons = new ArrayList<ConfigButton>();
		controlsButtons.add(new ConfigButtonScale("mouseSensitivity", 0.5f, 2f, 0.05f));
		Iterator<Input> inputsIterator = Client.getInstance().getInputsManager().getAllInputs();
		while(inputsIterator.hasNext())
		{
			Input keyBind = inputsIterator.next();
			if(keyBind instanceof KeyBindImplementation)
			{
				KeyBindImplementation kbi = (KeyBindImplementation)keyBind;
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
		
		configTabs.add(new ConfigTab("Controls", controlsButtonsArray
				
				/*new ConfigButton[] {
				new ConfigButtonScale("mouseSensitivity", 0.5f, 2f, 0.05f),
				
				new ConfigButtonKey("FORWARD_KEY", this),
				new ConfigButtonKey("BACK_KEY", this),
				new ConfigButtonKey("LEFT_KEY", this),
				new ConfigButtonKey("RIGHT_KEY", this),
				new ConfigButtonKey("JUMP_KEY", this),
				new ConfigButtonKey("RUN_KEY", this),
				new ConfigButtonKey("ENTER_KEY", this),
				new ConfigButtonKey("EXIT_KEY", this),
				new ConfigButtonKey("INVENTORY_KEY", this),
				new ConfigButtonKey("GRABUSE_KEY", this),
				new ConfigButtonKey("CHAT_KEY", this),
				}*/));
		
		configTabs.add(new ConfigTab("Sound", new ConfigButton[] {}));
		
		if(RenderingConfig.isDebugAllowed)
		{
			configTabs.add(new ConfigTab("Debug", new ConfigButton[] { 
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
			configTabs.add(new ConfigTab("Debug", new ConfigButton[] { 
					
					//No cheat-allowing debug functions
					new ConfigButtonToggle("showDebugInfo"),
					new ConfigButtonToggle("frametimeGraph"),
					new ConfigButtonMultiChoice("log-policy",new String[] { "send", "dont" }),
					}));
		}

		for (ConfigTab tab : configTabs)
		{
			for (GuiElement f : tab.configButtons)
				guiHandler.add(f);
			String txt = tab.name;
			int txtlen = FontRenderer2.getTextLengthUsingFont(32, txt, BitmapFont.SMALLFONTS);
			Button tabButton = new Button(0, 0, txtlen + 32, 32, txt, BitmapFont.SMALLFONTS, 1);
			tabsButtons.add(tabButton);
			guiHandler.add(tabButton);
		}
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int x, int y, int w, int h)
	{
		int optionsPanelSize = 320 * 2 + 32 + 64;
		
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(0, 0, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight, 0, 0, 0, 0, null, false, true, new Vector4fm(0.0, 0.0, 0.0, 0.5));
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(GameWindowOpenGL.windowWidth / 2.0f - optionsPanelSize / 2, 0, GameWindowOpenGL.windowWidth  / 2 + optionsPanelSize / 2, GameWindowOpenGL.windowHeight, 0, 0, 0, 0, null, false, true, new Vector4fm(0.0, 0.0, 0.0, 0.5));
		
		//ObjectRenderer.renderColoredRect(GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2, GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight, 0, "000000", 0.5f);
		//ObjectRenderer.renderColoredRect(GameWindowOpenGL.windowWidth / 2, GameWindowOpenGL.windowHeight / 2, optionsPanelSize, GameWindowOpenGL.windowHeight, 0, "000000", 0.25f);

		int dekal = 0;
		int i = 0;
		for (Button b : tabsButtons)
		{
			dekal += b.getWidth() + 32 + 16;
			b.setPosition(GameWindowOpenGL.windowWidth / 2 - optionsPanelSize / 2 + dekal, GameWindowOpenGL.windowHeight - 128);
			b.draw();
			dekal += b.getWidth();
			if (b.clicked())
				selectedConfigTab = i;
			i++;
		}

		ConfigTab currentConfigTab = configTabs.get(selectedConfigTab);
		int a = 0, b = 0;
		int startPosX = GameWindowOpenGL.windowWidth / 2 - optionsPanelSize / 2 + 160 + 32;
		int startPosY = GameWindowOpenGL.windowHeight - 128 - 64;
		for (ConfigButton c : currentConfigTab.configButtons)
		{
			c.setPosition(startPosX + b * (320 + 32), startPosY - (float) Math.floor(a / 2) * 64);
			c.updateText();
			c.draw();
			if(c instanceof ConfigButtonScale && c.isMouseOver() && Mouse.isButtonDown(0))
			{
				ConfigButtonScale cs = (ConfigButtonScale)c;
				cs.onClick(Mouse.getX(), Mouse.getY(), 0);
				cs.apply();
				//applyAndSave();
			}
			
			a++;
			b = a % 2;
		}

		FontRenderer2.drawTextUsingSpecificFont(GameWindowOpenGL.windowWidth / 2 - optionsPanelSize / 2 + 32, GameWindowOpenGL.windowHeight - 48 * 2, 0, 48, "Options menu", BitmapFont.SMALLFONTS);

		exitButton.setPosition(GameWindowOpenGL.windowWidth / 2, 48);
		exitButton.draw();

		if(currentConfigTab.name.equals("Rendering") || currentConfigTab.name.equals("") || currentConfigTab.name.equals("Debug"))
			shouldReload = true;
		
		if (exitButton.clicked())
		{
			//applyAndSave();
			Client.getConfig().save();
			mainScene.changeOverlay(parent);
		}
	}

	@Override
	public boolean handleKeypress(int k)
	{
		//TODO handleKeypress to take KeyBind
		
		//if (Client.getInstance().getInputsManager().getInputByName("exit").isPressed())
		if (Client.getInstance().getInputsManager().getInputByName("exit").isPressed())
		{
			Client.getConfig().save();
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
	}
	
	boolean shouldReload = false;
}
