//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.config;

import static org.lwjgl.glfw.GLFW.glfwGetKeyName;

import java.util.ArrayList;
import java.util.List;

import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.api.util.Configuration;
import io.xol.chunkstories.client.ClientImplementation;
import org.joml.Vector4f;

import io.xol.chunkstories.api.gui.GuiElement;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.BaseButton;
import io.xol.chunkstories.api.gui.elements.LargeButtonIcon;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.util.Configuration.Option;
import io.xol.chunkstories.api.util.Configuration.OptionBoolean;
import io.xol.chunkstories.input.lwjgl3.Lwjgl3KeyBind.Lwjgl3KeyBindOption;

public class OptionsScreen extends Layer {
	private LargeButtonIcon exitButton = new LargeButtonIcon(this, "back");
	private List<ConfigTab> configTabs = new ArrayList<>();

	private List<BaseButton> tabsButtons = new ArrayList<>();
	private int selectedConfigTab = 0;

	private final Configuration clientConfiguration;

	abstract class ConfigButton extends BaseButton {
		Runnable run = null;

		public ConfigButton setApplyAction(Runnable run) {
			this.run = run;
			return this;
		}

		void apply() {
			if (run != null)
				run.run();
		}

		final Option option;

		ConfigButton(Option o) {
			super(OptionsScreen.this, 0, 0, o.getName());
			this.option = o;

			this.setHeight(24);
			this.setWidth(160);
		}

		public void updateText() {
			this.text = gui.localization().getLocalizedString(option.getName()) + " : " + option.getValue();
		}

		public abstract void onClick(float posx, float posy, int button);
	}

	class ConfigButtonToggle extends ConfigButton {
		final OptionBoolean option;

		ConfigButtonToggle(OptionBoolean o) {
			super(o);
			option = o;
		}

		@Override
		public void onClick(float posx, float posy, int button) {
			option.toggle();
		}

	}

	class ConfigButtonMultiChoice extends ConfigButton {
		final Configuration.OptionMultiChoice option;

		String values[];
		int cuVal = 0;

		ConfigButtonMultiChoice(Configuration.OptionMultiChoice o) {
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
		ConfigButtonKey(Lwjgl3KeyBindOption kbi) {
			super(kbi);
		}

		@Override
		public void updateText() {
			this.text = gui.localization().getLocalizedString(option.getName()) + " : "
					+ glfwGetKeyName(Integer.parseInt(option.getValue()), 0);// Keyboard.getKeyName(Integer.parseInt(value));
		}

		@Override
		public void onClick(float posx, float posy, int button) {
			gui.setTopLayer(new KeyBindSelectionOverlay(gui, OptionsScreen.this, this));
		}

		void callBack(int key) {
			option.trySetting("" + key);
			apply();
		}

	}

	class ConfigButtonScale extends ConfigButton {
		final Configuration.OptionDoubleRange option;

		ConfigButtonScale(Configuration.OptionDoubleRange o) {
			super(o);
			this.option = o;
		}

		@Override
		public void onClick(float mouseX, float mouseY, int button) {
			float relativeMouseXPosition = mouseX - this.getPositionX();
			float newValue = (float) (0.0f + Math.min(320, Math.max(0.0, relativeMouseXPosition))) / 320.0f;
			newValue *= (option.getMaximumValue() - option.getMinimumValue());
			newValue += option.getMinimumValue();

			option.trySetting(newValue);
		}

		@Override
		public void render(GuiDrawer renderer) {
			double scaled = option.getValue();

			scaled /= option.getGranularity();
			scaled = (float) (Math.floor(scaled)) * option.getGranularity();

			this.text = gui.localization().getLocalizedString(option.getName()) + " : " + String.format("%." + 3 + "G", scaled);// Keyboard.getKeyName(Integer.parseInt(value));

			String localizedText = gui.localization().localize(text);
			int textWidth = gui.getFonts().defaultFont().getWidth(localizedText);
			if (width < 0) {
				width = textWidth;
			}
			float textDekal = getWidth() / 2 - textWidth / 2;
			String texture = "./textures/gui/scalableField.png";

			renderer.drawCorneredBoxTiled(xPosition, yPosition, getWidth(), getHeight(), 8, texture, 32, scale());

			//TODO modernize
			/*
			ObjectRenderer.renderTexturedRect(
					xPosition + this.width * scale() * (float) (option.getDoubleValue() - option.getMinimumValue())
							/ (float) (option.getMaximumValue() - option.getMinimumValue()),
					yPosition + 12 * scale(), 32 * scale(), 32 * scale(), 0, 0, 32, 32, 32,
					"./textures/gui/barCursor.png");*/

			renderer.drawStringWithShadow(renderer.getFonts().defaultFont(),
					xPosition + textDekal, yPosition + 4, localizedText, -1,
					new Vector4f(1.0f));
		}

	}

	class ConfigTab {
		String name;
		List<ConfigButton> configButtons;

		ConfigTab(String name) {
			this.name = name;
			this.configButtons = new ArrayList<>();
		}

		public ConfigTab(String name, ConfigButton[] buttons) {
			this(name);
			for (ConfigButton b : buttons)
				configButtons.add(b);
		}
	}

	public OptionsScreen(Gui gui, Layer parent) {
		super(gui, parent);

		exitButton.setAction(new Runnable() {

			@Override
			public void run() {
				gui.getClient().getConfiguration().save();
				gui.setTopLayer(parentLayer);
			}

		});
		elements.add(exitButton);

		clientConfiguration = gui.getClient().getConfiguration();

		for (Option option : clientConfiguration.getOptions()) {
			String name = option.getName();
			String category = name.substring("client.".length());
			category = category.substring(0, category.indexOf("."));

			category = category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase();

			ConfigButton optionButton;

			if (option instanceof Configuration.OptionMultiChoice)
				optionButton = new ConfigButtonMultiChoice((Configuration.OptionMultiChoice) option);
			else if (option instanceof Configuration.OptionDoubleRange)
				optionButton = new ConfigButtonScale((Configuration.OptionDoubleRange) option);
			else if (option instanceof OptionBoolean)
				optionButton = new ConfigButtonToggle((OptionBoolean) option);
			else
				continue;

			//TODO rethink how input works
			/*if (option instanceof Configuration.OptionInput) {
				Configuration.OptionInput keyOption = (Lwjgl3KeyBindOption) option;
				if (keyOption.getInput().isEditable())
					optionButton = new ConfigButtonKey(keyOption);
			}*/

			//if (optionButton == null || option.resolveProperty("hidden", "false").equals("true"))
			//	continue;

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
			elements.addAll(tab.configButtons);

			BaseButton tabButton = new BaseButton(this, tab);

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

			this.setHeight(24);
		}
	}

	@Override
	public void render(RenderingInterface renderer) {
		// this.renderer = renderer;
		parentLayer.getRootLayer().render(renderer);

		int optionsPanelSize = (160 * 2 + 16 + 32) * this.getGuiScale();

		// Shades the BG
		renderer..drawBoxWindowsSpace(0, 0, renderer.getWindow().getWidth(),
				renderer.getWindow().getHeight(), 0, 0, 0, 0, null, false, true, new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));
		renderer..drawBoxWindowsSpace(renderer.getWindow().getWidth() / 2.0f - optionsPanelSize / 2, 0,
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
