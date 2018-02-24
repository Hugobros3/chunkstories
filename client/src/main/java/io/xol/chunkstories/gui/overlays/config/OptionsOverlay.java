//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.overlays.config;

import static org.lwjgl.glfw.GLFW.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import io.xol.chunkstories.api.content.Content.LocalizationManager;
import io.xol.chunkstories.api.gui.GuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import org.joml.Vector4f;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.gui.Ingame;
import io.xol.chunkstories.gui.ng.BaseNgButton;
import io.xol.chunkstories.gui.ng.LargeButtonIcon;
import io.xol.chunkstories.input.lwjgl3.Lwjgl3KeyBind;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.graphics.util.CorneredBoxDrawer;
import io.xol.engine.graphics.util.ObjectRenderer;

public class OptionsOverlay extends Layer 
{
	LargeButtonIcon exitButton = new LargeButtonIcon(this, "back");
	List<ConfigTab> configTabs = new ArrayList<ConfigTab>();

	List<TabButton> tabsButtons = new ArrayList<TabButton>();
	int selectedConfigTab = 0;
	private final LocalizationManager locMgr;
	
	private RenderingInterface renderer;

	abstract class ConfigButton extends BaseNgButton
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
			super(OptionsOverlay.this, 0, 0, n);
			this.parameter = n;
			this.value = Client.getInstance().getConfig().getString(parameter, value);
			
			this.height = 24;
			this.width = 160;
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
			float relPos = posx - this.xPosition;
			float scaled = (float) (0.0f + Math.min(320, Math.max(0.0, relPos))) / 320.0f;
			scaled *= (max-min);
			scaled += min;
			scaled /= steps;
			scaled = (float) (Math.floor(scaled)) * steps;
				
			//scaled -= scaled % 0.01f;
			
			value = scaled+"";
			//options.mainScene.changeOverlay(new KeyBindSelectionOverlay(options.mainScene, options, this));
		}
		
		@Override
		public void render(RenderingInterface renderer)
		{
			String localizedText = Client.getInstance().getContent().localization().localize(text);
			int textWidth = Client.getInstance().getContent().fonts().defaultFont().getWidth(localizedText) * scale();
			if (width < 0)
			{
				width = textWidth;
			}
			float textDekal = getWidth() / 2 - textWidth / 2;
			TexturesHandler.getTexture("./textures/gui/scalableField.png").setLinearFiltering(false);
			CorneredBoxDrawer.drawCorneredBoxTiled(xPosition + getWidth() / 2, yPosition + getHeight() / 2, getWidth(), getHeight(), 8, "./textures/gui/scalableField.png", 32, scale());
			
			ObjectRenderer.renderTexturedRect(xPosition + getWidth() * (Float.parseFloat(value)-min)/(max-min), yPosition + 12 * scale(), 32 * scale(), 32 * scale(), 0, 0, 32, 32, 32, "./textures/gui/barCursor.png");
			
			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), xPosition + textDekal, yPosition + 4 * scale(), localizedText, scale(), scale(), new Vector4f(1.0f));
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
						renderer.shaders().reloadShader("reflections");
						renderer.shaders().reloadShader("postprocess");
					}
				}),
				new ConfigButtonToggle("doDynamicCubemaps").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						renderer.shaders().reloadShader("shadows_apply");
						renderer.shaders().reloadShader("terrain");
						renderer.shaders().reloadShader("terrain_blocky");
						renderer.shaders().reloadShader("postprocess");
						renderer.shaders().reloadShader("reflections");
					}
				}),
				new ConfigButtonToggle("doShadows").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						renderer.shaders().reloadShader("shadows_apply");
						renderer.shaders().reloadShader("terrain");
						renderer.shaders().reloadShader("terrain_blocky");
					}
				}),
				new ConfigButtonMultiChoice("shadowMapResolutions", new String[] { "512", "1024", "2048", "4096" }).setApplyAction(new Runnable(){
						@Override
						public void run()
						{
							if (parentLayer.getRootLayer() instanceof Ingame){
								Client.getInstance().getWorld().getWorldRenderer().resizeShadowMaps();
						}
					}
				}),
				new ConfigButtonToggle("dynamicGrass").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						renderer.shaders().reloadShader("blocks_opaque");
						renderer.shaders().reloadShader("postprocess");
						renderer.shaders().reloadShader("shadows");
					}
				}),
				new ConfigButtonToggle("hqTerrain").setApplyAction(new Runnable(){
					@Override
					public void run()
					{
						renderer.shaders().reloadShader("terrain");
						renderer.shaders().reloadShader("terrain_blocky");
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
						{
							if (parentLayer.getRootLayer() instanceof Ingame){
								Client.getInstance().getWorld().getWorldRenderer().setupRenderSize();
							renderer.shaders().reloadShader("postprocess");
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

		
		//Make a list of the available translations before creating the button
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
						String code = Client.getInstance().getConfig().getString("language", "en");
						Client.getInstance().configDeprecated().setString("language", code);
						Client.getInstance().getContent().localization().loadTranslation(code);
					}
				}),
				}));

		
		List<ConfigButton> controlsButtons = new ArrayList<ConfigButton>();
		controlsButtons.add(new ConfigButtonScale("mouseSensitivity", 0.5f, 2f, 0.05f));
		
		//List all the configurable inputs
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
		
		//Turn that into an array and make a tab from it
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
		
		//Make the debug tab dependant on runtime perms
		if(RenderingConfig.isDebugAllowed)
		{
			configTabs.add(new ConfigTab("#{Debug}", new ConfigButton[] { 
					new ConfigButtonToggle("debugGBuffers").setApplyAction(new Runnable(){
						@Override
						public void run()
						{
							renderer.shaders().reloadShader("postprocess");
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
			//Add all these elements to the Gui handler
			for (GuiElement f : tab.configButtons)
				elements.add(f);
			
			//String txt = tab.name;
			TabButton tabButton = new TabButton(this, tab);
			
			//Make the action of the tab buttons switching tab effectively
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
	
	class TabButton extends BaseNgButton {
		TabButton(OptionsOverlay layer, ConfigTab tab) {
			super(layer, 0, 0, tab.name);
			
			this.height = 24;
		}
		
		public float getWidth() {
			return super.getWidth() + 8 * scale();
		}
	}

	@Override
	public void render(RenderingInterface renderer)
	{
		this.renderer = renderer;
		
		parentLayer.getRootLayer().render(renderer);
		
		int optionsPanelSize = (160 * 2 + 16 + 32) * this.getGuiScale();
		
		//Shades the BG
		renderer.getGuiRenderer().drawBoxWindowsSpace(0, 0, renderer.getWindow().getWidth(), renderer.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));
		renderer.getGuiRenderer().drawBoxWindowsSpace(renderer.getWindow().getWidth() / 2.0f - optionsPanelSize / 2, 0, renderer.getWindow().getWidth()  / 2 + optionsPanelSize / 2, renderer.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));
		
		//Render the tabs buttons
		float dekal = 16 * this.getGuiScale();
		for (TabButton b : tabsButtons)
		{
			b.setPosition(renderer.getWindow().getWidth() / 2 - optionsPanelSize / 2 + dekal, renderer.getWindow().getHeight() - 64 * this.getGuiScale());
			b.render(renderer);
			dekal += b.getWidth() / 2f;
			dekal += b.getWidth() / 2f;
		}
		
		//Display the current tab
		ConfigTab currentConfigTab = configTabs.get(selectedConfigTab);
		int a = 0, b = 0;
		int startPosX = renderer.getWindow().getWidth() / 2 - optionsPanelSize / 2 + 0 + 16 * this.getGuiScale();
		int startPosY = renderer.getWindow().getHeight() - (64 + 32) * this.getGuiScale();
		
		Mouse mouse = gameWindow.getInputsManager().getMouse();
		
		for (ConfigButton c : currentConfigTab.configButtons)
		{
			c.setPosition(startPosX + b * (160 + 16) * this.getGuiScale(), startPosY - (float) Math.floor(a / 2) * 32 * this.getGuiScale());
			c.updateText();
			c.render(renderer);
			
			//Scale buttons work a bit hackyshly
			if(c instanceof ConfigButtonScale && c.isMouseOver() && mouse.getMainButton().isPressed())
			{
				ConfigButtonScale cs = (ConfigButtonScale)c;
				cs.onClick((float)mouse.getCursorX(), (float)mouse.getCursorY(), 0);
				cs.apply();
			}
			
			a++;
			b = a % 2;
		}

		renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().getFont("LiberationSans-Regular", 11), renderer.getWindow().getWidth() / 2 - optionsPanelSize / 2 + 16 * this.getGuiScale(), renderer.getWindow().getHeight() - 32 * this.getGuiScale(), Client.getInstance().getContent().localization().getLocalizedString("options.title"), 3, 3, new Vector4f(1));
		
		exitButton.setPosition(8, 8);
		exitButton.render(renderer);
	}
	
	@Override
	public boolean handleInput(Input input) {
		if(input.getName().equals("exit"))
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
					b.onClick((float)mb.getMouse().getCursorX(), (float)mb.getMouse().getCursorY(), mb.getName().equals("mouse.left") ? 0 : 1);
					b.apply();
				}
			}
		}
		
		super.handleInput(input);
		
		return true;
	}
}
