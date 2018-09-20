//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.config;

import static org.lwjgl.glfw.GLFW.glfwGetKeyName;

import java.util.ArrayList;
import java.util.List;

import io.xol.chunkstories.client.ClientImplementation;
import org.joml.Vector4f;

import io.xol.chunkstories.api.content.Content.LocalizationManager;
import io.xol.chunkstories.api.gui.GuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.BaseButton;
import io.xol.chunkstories.api.gui.elements.LargeButtonIcon;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.util.Configuration.Option;
import io.xol.chunkstories.api.util.Configuration.OptionBoolean;
import io.xol.chunkstories.input.lwjgl3.Lwjgl3KeyBind.Lwjgl3KeyBindOption;
import io.xol.chunkstories.renderer.opengl.util.ObjectRenderer;
import io.xol.chunkstories.util.config.OptionChoiceImplementation;
import io.xol.chunkstories.util.config.OptionScaleImplementation;
import io.xol.chunkstories.util.config.OptionToggleImplementation;

public class OptionsScreen extends Layer {
	LargeButtonIcon exitButton = new LargeButtonIcon(this, "back");
	List<ConfigTab> configTabs = new ArrayList<ConfigTab>();

	List<TabButton> tabsButtons = new ArrayList<TabButton>();
	int selectedConfigTab = 0;
	private final LocalizationManager locMgr;

	// private RenderingInterface renderer;

	abstract class ConfigButton extends BaseButton {
		Runnable run = null;

		public ConfigButton setApplyAction(Runnable run) {
			this.run = run;
			return this;
		}

		public void apply() {
			if (run != null)
				run.run();
		}

		final Option option;

		public ConfigButton(Option o) {
			super(OptionsScreen.this, 0, 0, o.getName());
			this.option = o;

			this.height = 24;
			this.width = 160;
		}

		public void updateText() {
			this.text = locMgr.getLocalizedString(option.getName()) + " : " + option.getValue();
		}

		public abstract void onClick(float posx, float posy, int button);
	}

	class ConfigButtonToggle extends ConfigButton {
		final OptionBoolean option;

		public ConfigButtonToggle(OptionBoolean o) {
			super(o);
			option = o;
		}

		@Override
		public void onClick(float posx, float posy, int button) {
			option.toggle();
		}

	}

	class ConfigButtonMultiChoice extends ConfigButton {
		final OptionChoiceImplementation option;

		String values[];
		int cuVal = 0;

		public ConfigButtonMultiChoice(OptionChoiceImplementation o) {
			super(o);
			this.option = o;
			this.values = new String[o.getPossibleChoices().size()];
			o.getPossibleChoices().toArray(values);

			if (o.getValue() != null) {
				for (int i = 0; i < values.length; i++) {
					if (values[i].equals(o.getValue()))
						cuVal = i;
				}
			}
		}

		@Override
		public void onClick(float posx, float posy, int button) {
			if (button == 0)
				cuVal++;
			else
				cuVal--;
			if (cuVal < 0)
				cuVal = values.length - 1;
			if (cuVal >= values.length)
				cuVal = 0;
			option.trySetting(values[cuVal]);
		}

	}

	class ConfigButtonKey extends ConfigButton {
		public ConfigButtonKey(Lwjgl3KeyBindOption kbi) {
			super(kbi);
		}

		@Override
		public void updateText() {
			this.text = locMgr.getLocalizedString(option.getName()) + " : "
					+ glfwGetKeyName(Integer.parseInt(option.getValue()), 0);// Keyboard.getKeyName(Integer.parseInt(value));
		}

		@Override
		public void onClick(float posx, float posy, int button) {
			OptionsScreen.this.gameWindow
					.setLayer(new KeyBindSelectionOverlay(OptionsScreen.this.gameWindow, OptionsScreen.this, this));
		}

		public void callBack(int key) {
			option.trySetting("" + key);
			apply();
		}

	}

	class ConfigButtonScale extends ConfigButton {
		final OptionScaleImplementation option;

		public ConfigButtonScale(OptionScaleImplementation o) {
			super(o);
			this.option = o;
		}

		@Override
		public void onClick(float posx, float posy, int button) {
			float relPos = posx - this.xPosition;
			float scaled = (float) (0.0f + Math.min(320, Math.max(0.0, relPos))) / 320.0f;
			scaled *= (option.getMaximumValue() - option.getMinimumValue());
			scaled += option.getMinimumValue();

			option.trySetting("" + scaled);
			/*
			 * scaled /= steps; scaled = (float) (Math.floor(scaled)) * steps;
			 * 
			 * value = scaled+"";
			 */
		}

		@Override
		public void render(RenderingInterface renderer) {
			double scaled = option.getDoubleValue();

			scaled /= option.getGranularity();
			scaled = (float) (Math.floor(scaled)) * option.getGranularity();

			this.text = locMgr.getLocalizedString(option.getName()) + " : " + String.format("%." + 3 + "G", scaled);// Keyboard.getKeyName(Integer.parseInt(value));

			String localizedText = ClientImplementation.getInstance().getContent().localization().localize(text);
			int textWidth = ClientImplementation.getInstance().getContent().fonts().defaultFont().getWidth(localizedText) * scale();
			if (width < 0) {
				width = textWidth;
			}
			float textDekal = getWidth() / 2 - textWidth / 2;
			Texture2D texture = renderer.textures().getTexture("./textures/gui/scalableField.png");
			texture.setLinearFiltering(false);

			renderer.getGuiRenderer().drawCorneredBoxTiled(xPosition, yPosition, getWidth(), getHeight(), 8, texture,
					32, scale());

			ObjectRenderer.renderTexturedRect(
					xPosition + this.width * scale() * (float) (option.getDoubleValue() - option.getMinimumValue())
							/ (float) (option.getMaximumValue() - option.getMinimumValue()),
					yPosition + 12 * scale(), 32 * scale(), 32 * scale(), 0, 0, 32, 32, 32,
					"./textures/gui/barCursor.png");

			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(),
					xPosition + textDekal, yPosition + 4 * scale(), localizedText, scale(), scale(),
					new Vector4f(1.0f));
		}

	}

	class ConfigTab {
		String name;
		List<ConfigButton> configButtons;

		public ConfigTab(String name) {
			this.name = name;
			this.configButtons = new ArrayList<>();
		}

		public ConfigTab(String name, ConfigButton[] buttons) {
			this(name);
			for (ConfigButton b : buttons)
				configButtons.add(b);
		}
	}

	public OptionsScreen(GameWindow scene, Layer parent) {
		super(scene, parent);

		locMgr = ClientImplementation.getInstance().getContent().localization();

		exitButton.setAction(new Runnable() {

			@Override
			public void run() {
				ClientImplementation.getInstance().getConfiguration().save();
				gameWindow.setLayer(parentLayer);
			}

		});
		elements.add(exitButton);

		for (Option option : gameWindow.getClient().getConfiguration().allOptions()) {
			if (!option.getName().startsWith("client."))
				continue;

			String name = option.getName();
			String category = name.substring("client.".length());
			category = category.substring(0, category.indexOf("."));

			category = category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase();

			ConfigButton optionButton = null;

			if (option instanceof OptionChoiceImplementation)
				optionButton = new ConfigButtonMultiChoice((OptionChoiceImplementation) option);
			if (option instanceof OptionScaleImplementation)
				optionButton = new ConfigButtonScale((OptionScaleImplementation) option);
			if (option instanceof OptionToggleImplementation)
				optionButton = new ConfigButtonToggle((OptionToggleImplementation) option);
			if (option instanceof Lwjgl3KeyBindOption) {

				Lwjgl3KeyBindOption keyOption = (Lwjgl3KeyBindOption) option;
				if (keyOption.getInput().isEditable())
					optionButton = new ConfigButtonKey(keyOption);
			}

			if (optionButton == null || option.resolveProperty("hidden", "false").equals("true"))
				continue;

			ConfigTab relevantTab = null;
			for (ConfigTab tab : configTabs) {
				if (tab.name.equals(category)) {
					relevantTab = tab;
					break;
				}
			}
			if (relevantTab == null) {
				relevantTab = new ConfigTab(category);
				configTabs.add(0, relevantTab);
			}

			relevantTab.configButtons.add(optionButton);
		}

		int configTabIndex = 0;
		for (ConfigTab tab : configTabs) {
			// Add all these elements to the Gui handler
			for (GuiElement f : tab.configButtons)
				elements.add(f);

			// String txt = tab.name;
			TabButton tabButton = new TabButton(this, tab);

			// Make the action of the tab buttons switching tab effectively
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

	class TabButton extends BaseButton {
		TabButton(OptionsScreen layer, ConfigTab tab) {
			super(layer, 0, 0, tab.name);

			this.height = 24;
		}

		public float getWidth() {
			return super.getWidth();// + 8 * scale();
		}
	}

	@Override
	public void render(RenderingInterface renderer) {
		// this.renderer = renderer;
		parentLayer.getRootLayer().render(renderer);

		int optionsPanelSize = (160 * 2 + 16 + 32) * this.getGuiScale();

		// Shades the BG
		renderer.getGuiRenderer().drawBoxWindowsSpace(0, 0, renderer.getWindow().getWidth(),
				renderer.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));
		renderer.getGuiRenderer().drawBoxWindowsSpace(renderer.getWindow().getWidth() / 2.0f - optionsPanelSize / 2, 0,
				renderer.getWindow().getWidth() / 2 + optionsPanelSize / 2, renderer.getWindow().getHeight(), 0, 0, 0,
				0, null, false, true, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));

		// Render the tabs buttons
		float dekal = 16 * this.getGuiScale();
		for (TabButton b : tabsButtons) {
			b.setPosition(renderer.getWindow().getWidth() / 2 - optionsPanelSize / 2 + dekal,
					renderer.getWindow().getHeight() - 64 * this.getGuiScale());
			b.render(renderer);
			dekal += b.getWidth() / 2f;
			dekal += b.getWidth() / 2f;
		}

		// Display the current tab
		ConfigTab currentConfigTab = configTabs.get(selectedConfigTab);
		int a = 0, b = 0;
		int startPosX = renderer.getWindow().getWidth() / 2 - optionsPanelSize / 2 + 0 + 16 * this.getGuiScale();
		int startPosY = renderer.getWindow().getHeight() - (64 + 32) * this.getGuiScale();

		Mouse mouse = gameWindow.getInputsManager().getMouse();

		for (ConfigButton c : currentConfigTab.configButtons) {
			c.setPosition(startPosX + b * (160 + 16) * this.getGuiScale(),
					startPosY - (float) Math.floor(a / 2) * 32 * this.getGuiScale());
			c.updateText();
			c.render(renderer);

			// Scale buttons work a bit hackyshly
			if (c instanceof ConfigButtonScale && c.isMouseOver() && mouse.getMainButton().isPressed()) {
				ConfigButtonScale cs = (ConfigButtonScale) c;
				cs.onClick((float) mouse.getCursorX(), (float) mouse.getCursorY(), 0);
				cs.apply();
			}

			a++;
			b = a % 2;
		}

		renderer.getFontRenderer().drawStringWithShadow(
				renderer.getFontRenderer().getFont("LiberationSans-Regular", 11),
				renderer.getWindow().getWidth() / 2 - optionsPanelSize / 2 + 16 * this.getGuiScale(),
				renderer.getWindow().getHeight() - 32 * this.getGuiScale(),
				ClientImplementation.getInstance().getContent().localization().getLocalizedString("options.title"), 3, 3,
				new Vector4f(1));

		exitButton.setPosition(8, 8);
		exitButton.render(renderer);
	}

	@Override
	public boolean handleInput(Input input) {
		if (input.getName().equals("exit")) {
			ClientImplementation.getInstance().getConfiguration().save();
			gameWindow.setLayer(parentLayer);
			return true;
		} else if (input instanceof MouseButton) {
			MouseButton mb = (MouseButton) input;
			for (ConfigButton b : configTabs.get(selectedConfigTab).configButtons) {
				if (b.isMouseOver()) {
					b.onClick((float) mb.getMouse().getCursorX(), (float) mb.getMouse().getCursorY(),
							mb.getName().equals("mouse.left") ? 0 : 1);
					b.apply();
				}
			}
		}

		super.handleInput(input);

		return true;
	}
}
