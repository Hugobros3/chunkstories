//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.config;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.Mod;
import io.xol.chunkstories.api.exceptions.content.mods.ModLoadFailureException;
import io.xol.chunkstories.api.gui.Font;
import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.Button;
import io.xol.chunkstories.api.gui.elements.LargeButtonWithIcon;
import io.xol.chunkstories.api.gui.elements.ScrollableContainer;
import io.xol.chunkstories.api.gui.elements.ScrollableContainer.ContainerElement;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.client.ClientImplementation;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.content.mods.ModFolder;
import io.xol.chunkstories.content.mods.ModImplementation;
import io.xol.chunkstories.content.mods.ModZip;
import io.xol.chunkstories.gui.layer.config.ModsSelection.ModsScrollableContainer.ModItem;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

public class ModsSelection extends Layer {
    private static final Logger logger = LoggerFactory.getLogger("client.mods");

    private LargeButtonWithIcon applyMods = new LargeButtonWithIcon(this, "validate");
    private LargeButtonWithIcon backOption = new LargeButtonWithIcon(this, "back");

    private Button locateExtMod = new Button(this, 0, 0, "Locate external mod");
    private Button openModsFolder = new Button(this, 0, 0, "Open mods folder");

    private ModsScrollableContainer modsContainer = new ModsScrollableContainer(this);

    public ModsSelection(Gui window, Layer parent) {
        super(window, parent);

        elements.add(modsContainer);

        elements.add(locateExtMod);
        elements.add(openModsFolder);
        elements.add(backOption);
        elements.add(applyMods);

        this.backOption.setAction(() -> gui.setTopLayer(parentLayer));

        this.applyMods.setAction(() -> {
            List<String> modsEnabled = new ArrayList<String>();
            for (ContainerElement e : modsContainer.elements) {
                ModItem modItem = (ModItem) e;
                if (modItem.enabled) {
                    logger.info("Adding " + ((ModImplementation) modItem.mod).getLoadString() + " to mod path");
                    modsEnabled.add(((ModImplementation) modItem.mod).getLoadString());
                }
            }

            String[] ok = new String[modsEnabled.size()];
            modsEnabled.toArray(ok);
            gui.getClient().getContent().modsManager().setEnabledMods(ok);

            ((ClientImplementation) gui.getClient()).reloadAssets();
            buildModsList();
        });

        buildModsList();
    }

    private void buildModsList() {
        modsContainer.elements.clear();
        Collection<String> currentlyEnabledMods = Arrays
                .asList(gui.getClient().getContent().modsManager().getEnabledModsString());

        Set<String> uniqueMods = new HashSet<String>();
        // First put in already loaded mods
        for (Mod mod : gui.getClient().getContent().modsManager().getCurrentlyLoadedMods()) {
            // Should use md5 hash instead ;)
            if (uniqueMods.add(mod.getModInfo().getName().toLowerCase()))
                modsContainer.elements.add(modsContainer.new ModItem(mod, true));
        }

        // Then look for mods in folder fashion
        for (File f : new File(GameDirectory.getGameFolderPath() + "/mods/").listFiles()) {
            if (f.isDirectory()) {
                File txt = new File(f.getAbsolutePath() + "/mod.txt");
                if (txt.exists()) {
                    try {
                        ModFolder mod = new ModFolder(f);
                        // Should use md5 hash instead ;)
                        if (uniqueMods.add(mod.getModInfo().getName().toLowerCase()))
                            modsContainer.elements.add(modsContainer.new ModItem(mod,
                                    currentlyEnabledMods.contains(mod.getModInfo().getName())));

                        System.out.println("mod:" + mod.getModInfo().getName() + " // "
                                + currentlyEnabledMods.contains(mod.getModInfo().getName()));
                    } catch (ModLoadFailureException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // Then look for .zips
        // First look for mods in folder fashion
        for (File f : new File(GameDirectory.getGameFolderPath() + "/mods/").listFiles()) {
            if (f.getName().endsWith(".zip")) {
                try {
                    ModZip mod = new ModZip(f);
                    // Should use md5 hash instead ;)
                    if (uniqueMods.add(mod.getModInfo().getName().toLowerCase()))
                        modsContainer.elements.add(modsContainer.new ModItem(mod,
                                currentlyEnabledMods.contains(mod.getModInfo().getName())));
                } catch (ModLoadFailureException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void render(GuiDrawer renderer) {
        parentLayer.getRootLayer().render(renderer);
        int scale = 1;

        String instructions = "Select the mods you want to use";
        Font font = renderer.getFonts().getFont("LiberationSans-Regular", 16 * scale);
        renderer.drawStringWithShadow(font, 32, gui.getViewportHeight() - 24 * scale,
                instructions, -1, new Vector4f(1));

        backOption.setPosition(xPosition + 8, 8);
        backOption.render(renderer);

        // Display buttons

        int totalLengthOfButtons = 0;
        int spacing = 2 * scale;

        totalLengthOfButtons += applyMods.getWidth();
        totalLengthOfButtons += spacing;

        totalLengthOfButtons += locateExtMod.getWidth();
        totalLengthOfButtons += spacing;

        int buttonDisplayX = gui.getViewportWidth() / 2 - totalLengthOfButtons / 2;
        int buttonDisplayY = 8;

        locateExtMod.setPosition(buttonDisplayX, buttonDisplayY);
        buttonDisplayX += locateExtMod.getWidth() + spacing;
        locateExtMod.render(renderer);

        openModsFolder.setPosition(buttonDisplayX, buttonDisplayY);
        buttonDisplayX += openModsFolder.getWidth() + spacing;
        openModsFolder.render(renderer);

        applyMods.setPosition(this.getWidth() - applyMods.getWidth() - 8, 8);
        buttonDisplayX += applyMods.getWidth() + spacing;
        applyMods.render(renderer);

        int offsetForButtons = applyMods.getPositionY() + applyMods.getHeight() + 8 * scale;
        int offsetForHeaderText = 32 * scale;
        modsContainer.setPosition((width - 480 * scale) / 2, offsetForButtons);
        modsContainer.setSize(480 * scale, height - (offsetForButtons + offsetForHeaderText));
        modsContainer.render(renderer);
    }

    @Override
    public boolean handleInput(Input input) {
        if (input instanceof MouseScroll) {
            MouseScroll ms = (MouseScroll) input;
            modsContainer.scroll(ms.amount() > 0);
            return true;
        }

        return super.handleInput(input);
    }

    class ModsScrollableContainer extends ScrollableContainer {

        ModsScrollableContainer(Layer layer) {
            super(layer);
        }

        public void render(GuiDrawer renderer) {
            super.render(renderer);

            String text = "Showing elements ";

            text += scroll;
            text += "-";
            text += scroll;

            text += " out of " + elements.size();
            int dekal = renderer.getFonts().getFont("LiberationSans-Regular", 12).getWidth(text) / 2;

            renderer.drawString(renderer.getFonts().getFont("LiberationSans-Regular", 12),
                    xPosition + width / 2 - dekal, yPosition - 128, text, -1,
                    new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));
        }

        class ModItem extends ContainerElement {

            boolean enabled;

            String icon;
            Mod mod;

            ModItem(Mod mod2, boolean enabled) {
                super(mod2.getModInfo().getName(), mod2.getModInfo().getDescription().replace("\\n", "\n"));
                this.mod = mod2;
                this.enabled = enabled;
                this.topRightString = mod2.getModInfo().getVersion();

                Asset asset = mod2.getAssetByName("modicon.png");
                if (asset != null)
                    icon = "@"+mod2.getModInfo().getInternalName()+":modicon.png";
                else
                    icon = "nomodicon.png";
            }

            @Override
            public boolean handleClick(MouseButton mouseButton) {
                Mouse mouse = mouseButton.getMouse();
                if (isOverUpButton(mouse)) {
                    int indexInList = ModsScrollableContainer.this.elements.indexOf(this);
                    if (indexInList > 0) {
                        int newIndex = indexInList - 1;
                        ModsScrollableContainer.this.elements.remove(indexInList);
                        ModsScrollableContainer.this.elements.add(newIndex, this);
                    }
                } else if (isOverDownButton(mouse)) {
                    int indexInList = ModsScrollableContainer.this.elements.indexOf(this);
                    if (indexInList < ModsScrollableContainer.this.elements.size() - 1) {
                        int newIndex = indexInList + 1;
                        ModsScrollableContainer.this.elements.remove(indexInList);
                        ModsScrollableContainer.this.elements.add(newIndex, this);
                    }
                } else if (isOverEnableDisableButton(mouse)) {
                    // TODO: Check for conflicts when enabling
                    enabled = !enabled;
                } else
                    return false;
                return true;
            }

            boolean isOverUpButton(Mouse mouse) {
                int s = 1;
                double mx = mouse.getCursorX();
                double my = mouse.getCursorY();

                float positionX = this.positionX + 460 * s;
                float positionY = this.positionY + 37 * s;
                int width = 18;
                int height = 17;
                return mx >= positionX && mx <= positionX + width * s && my >= positionY
                        && my <= positionY + height * s;
            }

            boolean isOverEnableDisableButton(Mouse mouse) {
                int s = 1;
                double mx = mouse.getCursorX();
                double my = mouse.getCursorY();

                float positionX = this.positionX + 460 * s;
                float positionY = this.positionY + 20 * s;
                int width = 18;
                int height = 17;
                return mx >= positionX && mx <= positionX + width * s && my >= positionY
                        && my <= positionY + height * s;
            }

            boolean isOverDownButton(Mouse mouse) {
                int s = 1;
                double mx = mouse.getCursorX();
                double my = mouse.getCursorY();

                float positionX = this.positionX + 460 * s;
                float positionY = this.positionY + 2 * s;
                int width = 18;
                int height = 17;
                return mx >= positionX && mx <= positionX + width * s && my >= positionY
                        && my <= positionY + height * s;
            }

            @Override
            public void render(GuiDrawer drawer) {
                Mouse mouse = gui.getMouse();

                // Setup textures
                String bgTexture = isMouseOver(mouse) ? "textures/gui/modsOver.png" : "textures/gui/mods.png";

                String upArrowTexture = "textures/gui/modsArrowUp.png";
                String downArrowTexture = "textures/gui/modsArrowDown.png";

                String enableDisableTexture =(enabled ? "textures/gui/modsDisable.png" : "textures/gui/modsEnable.png");

                // Render graphical base
                drawer.drawBox(positionX, positionY, width, height, 0, 1, 1, 0, bgTexture,
                        enabled ? new Vector4f(1.0f, 1.0f, 1.0f, 1.0f) : new Vector4f(1f, 1f, 1f, 0.5f));
                if (enabled) {
                    String enabledTexture = "textures/gui/modsEnabled.png";
                    drawer.drawBox(positionX, positionY, width, height,
                            0, 1, 1, 0, enabledTexture, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
                }

                // Render subbuttons
                if (isOverUpButton(mouse))
                    drawer.drawBox(positionX, positionY, width, height,
                            0, 1, 1, 0, upArrowTexture, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
                if (isOverEnableDisableButton(mouse))
                    drawer.drawBox(positionX, positionY, width, height,
                            0, 1, 1, 0, enableDisableTexture, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
                if (isOverDownButton(mouse))
                    drawer.drawBox(positionX, positionY, width, height,
                            0, 1, 1, 0, downArrowTexture, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));

                // Render icon
                drawer.drawBox(positionX + 4, positionY + 4, 64,
                        64, 0, 1, 1, 0, icon, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
                // Text !
                if (name != null)
                    drawer.drawString(
                            drawer.getFonts().getFont("LiberationSans-Regular", 12), positionX + 70,
                            positionY + 54, name, -1, new Vector4f(0.0f, 0.0f, 0.0f, 1.0f));

                if (topRightString != null) {
                    int dekal = width - drawer.getFonts().getFont("LiberationSans-Regular", 12).getWidth(topRightString) - 4;
                    drawer.drawString(
                            drawer.getFonts().getFont("LiberationSans-Regular", 12), positionX + dekal,
                            positionY + 54, topRightString, -1, new Vector4f(0.25f, 0.25f, 0.25f, 1.0f));
                }

                if (descriptionLines != null)
                    drawer.drawString(
                            drawer.getFonts().getFont("LiberationSans-Regular", 12), positionX + 70,
                            positionY + 38, descriptionLines, -1, new Vector4f(0.25f, 0.25f, 0.25f, 1.0f));

            }

        }
    }

}
