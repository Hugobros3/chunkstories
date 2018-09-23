//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer.ingame;

import io.xol.chunkstories.api.client.IngameClient;
import io.xol.chunkstories.api.gui.Font;
import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.Button;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.math.HexTools;
import io.xol.chunkstories.api.net.packets.PacketText;
import io.xol.chunkstories.api.world.WorldClientNetworkedRemote;
import io.xol.chunkstories.api.world.WorldMaster;
import io.xol.chunkstories.client.ClientImplementation;
import io.xol.chunkstories.gui.layer.MainMenu;
import org.joml.Vector4f;

/**
 * Childishly taunts you when you die and offers you the option to ragequit the game
 */
public class DeathScreen extends Layer {
    private Button respawnButton = new Button(this, 0, 0, 160, "#{ingame.respawn}");
    private Button exitButton = new Button(this, 0, 0, 160, "#{ingame.exit}");

    private IngameClient ingameClient;

    public DeathScreen(Gui gui, Layer parent) {
        super(gui, parent);

        this.ingameClient = gui.getClient().getIngame();
        assert ingameClient != null;

        this.respawnButton.setAction(() -> {
            if (ingameClient.getWorld() instanceof WorldMaster)
                ((WorldMaster) ingameClient.getWorld()).spawnPlayer(ingameClient.getPlayer());
            else
                ((WorldClientNetworkedRemote) ingameClient.getWorld()).getRemoteServer().pushPacket(new PacketText("world/respawn"));

            gui.popTopLayer();
        });

        this.exitButton.setAction(() -> gui.setTopLayer(new MainMenu(gui, null)));

        elements.add(respawnButton);
        elements.add(exitButton);
    }

    @Override
    public void render(GuiDrawer drawer) {
        parentLayer.render(drawer);

        drawer.drawBox(0, 0, gui.getViewportWidth(), gui.getViewportHeight(), new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));

        String color = "#";
        color += HexTools.intToHex((int) (Math.random() * 255));
        color += HexTools.intToHex((int) (Math.random() * 255));
        color += HexTools.intToHex((int) (Math.random() * 255));

        Font font = drawer.getFonts().getFont("LiberationSans-Regular", 11);

        drawer.drawStringWithShadow(font, gui.getViewportWidth() / 2 - font.getWidth("YOU DIEDED") / 2, gui.getViewportHeight() / 2 + 48 * 3, "#FF0000YOU DIEDED", -1, new Vector4f(1));
        drawer.drawStringWithShadow(font, gui.getViewportWidth() / 2 - font.getWidth("git --gud scrub") / 2, gui.getViewportHeight() / 2 + 36 * 3, color + "git --gud scrub", -1, new Vector4f(1));

        respawnButton.setPosition(gui.getViewportWidth() / 2 - respawnButton.getWidth() / 2, gui.getViewportHeight() / 2 + 48);
        exitButton.setPosition(gui.getViewportWidth() / 2 - exitButton.getWidth() / 2, gui.getViewportHeight() / 2 - 24);

        respawnButton.render(drawer);
        exitButton.render(drawer);

        // When the new entity arrives, pop this
        if (ingameClient.getPlayer().getControlledEntity() != null)
            gui.popTopLayer();

        // Make sure to ungrab the mouse
        Mouse mouse = gui.getMouse();
        if (mouse.isGrabbed())
            mouse.setGrabbed(false);
    }
}
