//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.input.lwjgl3;

import io.xol.chunkstories.api.client.ClientInputsManager;
import io.xol.chunkstories.api.client.IngameClient;
import io.xol.chunkstories.api.client.LocalPlayer;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.traits.Trait;
import io.xol.chunkstories.api.entity.traits.serializable.TraitControllable;
import io.xol.chunkstories.api.events.client.ClientInputPressedEvent;
import io.xol.chunkstories.api.events.client.ClientInputReleasedEvent;
import io.xol.chunkstories.api.events.player.PlayerInputPressedEvent;
import io.xol.chunkstories.api.events.player.PlayerInputReleasedEvent;
import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.client.ClientImplementation;
import io.xol.chunkstories.client.glfw.GLFWWindow;
import io.xol.chunkstories.client.net.ServerConnection;
import io.xol.chunkstories.gui.layer.config.KeyBindSelectionOverlay;
import io.xol.chunkstories.input.InputVirtual;
import io.xol.chunkstories.input.InputsLoaderHelper;
import io.xol.chunkstories.input.InputsManagerLoader;
import io.xol.chunkstories.input.Pollable;
import io.xol.chunkstories.net.packets.PacketInput;
import io.xol.chunkstories.world.WorldClientRemote;
import org.lwjgl.glfw.GLFWCharCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.lwjgl.glfw.GLFW.*;

public class Lwjgl3ClientInputsManager implements ClientInputsManager, InputsManagerLoader {
    protected final GLFWWindow gameWindow;
    private final Gui gui;

    Collection<Input> inputs = new ArrayList<>();
    Map<Long, Input> inputsMap = new HashMap<>();

    public Lwjgl3Mouse mouse;
    public Lwjgl3MouseButton LEFT;
    public Lwjgl3MouseButton RIGHT;
    public Lwjgl3MouseButton MIDDLE;

    private final GLFWKeyCallback keyCallback;
    private final GLFWMouseButtonCallback mouseButtonCallback;
    private final GLFWScrollCallback scrollCallback;
    private final GLFWCharCallback characterCallback;

    private static final Logger logger = LoggerFactory.getLogger("client.workers");

    // private final IngameLayer scene;
    public Lwjgl3ClientInputsManager(GLFWWindow gameWindow) {
        this.gameWindow = gameWindow;
        this.gui = gameWindow.getClient().getGui();

        mouse = new Lwjgl3Mouse(this);
        LEFT = new Lwjgl3MouseButton(mouse, "mouse.left", 0);
        RIGHT = new Lwjgl3MouseButton(mouse, "mouse.right", 1);
        MIDDLE = new Lwjgl3MouseButton(mouse, "mouse.middle", 2);

        glfwSetKeyCallback(gameWindow.getGlfwWindowHandle(), (keyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (gui.getTopLayer() instanceof KeyBindSelectionOverlay) {
                    KeyBindSelectionOverlay kbs = (KeyBindSelectionOverlay) gui.getTopLayer();
                    kbs.setKeyTo(key);
                }

                // Try first the compound shortcuts
                Lwjgl3KeyBindCompound keyBindCompound = getKeyCompoundFulForLWJGL3xKey(key);
                if (keyBindCompound != null) {
                    if (action == GLFW_PRESS)
                        if (onInputPressed(keyBindCompound))
                            return;
                }

                // If unsuccessfull pass to normal keyboard input
                Lwjgl3KeyBind keyboardInput = getKeyBoundForLWJGL3xKey(key);

                if (keyboardInput != null) {
                    if (action == GLFW_PRESS)
                        onInputPressed(keyboardInput);
                    else if (action == GLFW_REPEAT && keyboardInput.repeat)
                        onInputPressed(keyboardInput);
                    else if (action == GLFW_RELEASE)
                        onInputReleased(keyboardInput);
                }

                // Unhandled character
            }
        }));

        glfwSetMouseButtonCallback(gameWindow.getGlfwWindowHandle(), (mouseButtonCallback = new GLFWMouseButtonCallback() {
            @Override
            public void invoke(long window, int button, int action, int mods) {
                MouseButton mButton = null;
                switch (button) {
                    case 0:
                        mButton = LEFT;
                        break;
                    case 1:
                        mButton = RIGHT;
                        break;
                    case 2:
                        mButton = MIDDLE;
                        break;
                }

                if (mButton != null) {
                    if (action == GLFW_PRESS)
                        onInputPressed(mButton);
                    else if (action == GLFW_RELEASE)
                        onInputReleased(mButton);
                }
            }

        }));

        glfwSetScrollCallback(gameWindow.getGlfwWindowHandle(), scrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double xoffset, double yoffset) {

                MouseScroll ms = mouse.scroll(yoffset);
                onInputPressed(ms);

                // gameWindow.getCurrentScene().onScroll((int)yoffset);
            }
        });

        glfwSetCharCallback(gameWindow.getGlfwWindowHandle(), characterCallback = new GLFWCharCallback() {

            @Override
            public void invoke(long window, int codepoint) {
                char[] chars = Character.toChars(codepoint);
                for (char c : chars) {
                    Layer layer = gui.getTopLayer();
                    if (layer != null)
                        layer.handleTextInput(c);
                }
            }

        });

        //reload();
    }

    public Iterator<Input> getAllInputs() {
        return inputs.iterator();
    }

    /**
     * Returns null or a KeyBind matching the name
     */
    public Input getInputByName(String bindName) {
        if (bindName.equals("mouse.left"))
            return LEFT;
        if (bindName.equals("mouse.right"))
            return RIGHT;
        if (bindName.equals("mouse.middle"))
            return MIDDLE;

        //TODO hash map !!!
        for (Input keyBind : inputs) {
            if (keyBind.getName().equals(bindName))
                return keyBind;
        }
        return null;
    }

    /**
     * Returns null or a KeyBind matching the pressed key
     *
     * @param keyCode
     * @return
     */
    protected Lwjgl3KeyBind getKeyBoundForLWJGL3xKey(int keyCode) {
        for (Input keyBind : inputs) {
            if (keyBind instanceof Lwjgl3KeyBind && ((Lwjgl3KeyBind) keyBind).getLWJGL3xKey() == keyCode)
                return (Lwjgl3KeyBind) keyBind;
        }
        return null;
    }

    protected Lwjgl3KeyBindCompound getKeyCompoundFulForLWJGL3xKey(int key) {
        inputs:
        for (Input keyBind : inputs) {
            if (keyBind instanceof Lwjgl3KeyBindCompound) {
                Lwjgl3KeyBindCompound keyCombinaison = (Lwjgl3KeyBindCompound) keyBind;

                // Check getAllVoxelComponents other keys were pressed
                for (int glfwKey : keyCombinaison.glfwKeys) {
                    if (glfwGetKey(gameWindow.getGlfwWindowHandle(), glfwKey) != GLFW_PRESS)
                        continue inputs;
                }

                return keyCombinaison;
            }
        }

        return null;
    }

    public Input getInputFromHash(long hash) {
        if (hash == 0)
            return LEFT;
        else if (hash == 1)
            return RIGHT;
        else if (hash == 2)
            return MIDDLE;

        return inputsMap.get(hash);
    }

    public void reload() {
        inputs.clear();
        inputsMap.clear();

        InputsLoaderHelper.loadKeyBindsIntoManager(this, gameWindow.getClient().getContent().modsManager());

        // Add physical mouse buttons
        inputs.add(LEFT);
        inputsMap.put(LEFT.getHash(), LEFT);
        inputs.add(RIGHT);
        inputsMap.put(RIGHT.getHash(), RIGHT);
        inputs.add(MIDDLE);
        inputsMap.put(MIDDLE.getHash(), MIDDLE);
    }

    public void insertInput(String type, String name, String value, Collection<String> arguments) {
        Input input;
        if (type.equals("keyBind")) {
            Lwjgl3KeyBind key = new Lwjgl3KeyBind(this, name, value);
            input = key;
            if (arguments.contains("hidden"))
                key.editable = false;
            if (arguments.contains("repeat"))
                key.repeat = true;
            // keyboardInputs.add(key);
        } else if (type.equals("virtual")) {
            input = new InputVirtual(name);
        } else if (type.equals("keyBindCompound")) {
            Lwjgl3KeyBindCompound keyCompound = new Lwjgl3KeyBindCompound(this, name, value);
            input = keyCompound;
        } else
            return;

        inputs.add(input);
        inputsMap.put(input.getHash(), input);
    }

    public void pollLWJGLInputs() {
        glfwPollEvents();

        for (Input input : this.inputs) {
            if (input instanceof Pollable)
                ((Pollable) input).updateStatus();
        }
    }

    public boolean onInputPressed(Input input) {
        if (input.equals("fullscreen")) {
            //TODO
            //gameWindow.toggleFullscreen();
            return true;
        }

        IngameClient ingameClient = gameWindow.getClient().getIngame();
        if(ingameClient == null)
            return false;

        // Try the client-side event press
        ClientInputPressedEvent inputPressedEvent = new ClientInputPressedEvent(gameWindow.getClient(), input);
        ingameClient.getPluginManager().fireEvent(inputPressedEvent);
        if (inputPressedEvent.isCancelled())
            return false;

        // Try the GUI handling
        Layer layer = gameWindow.getClient().getGui().getTopLayer();
        if (layer != null && layer.handleInput(input))
            return true;

        final LocalPlayer player = ingameClient.getPlayer();
        final Entity playerEntity = player.getControlledEntity();

        // There has to be a controlled entity for sending inputs to make sense.
        if (playerEntity == null)
            return false;

        World world = playerEntity.getWorld();

        // Send input to server
        if (world instanceof WorldClientRemote) {
            // MouseScroll inputs are strictly client-side
            if (!(input instanceof MouseScroll)) {
                ServerConnection connection = ((WorldClientRemote) playerEntity.getWorld()).getConnection();
                PacketInput packet = new PacketInput(world);
                packet.input = input;
                packet.isPressed = true;
                connection.pushPacket(packet);
            }

            return playerEntity.traits.tryWithBoolean(TraitControllable.class, t -> t.onControllerInput(input));
        } else {
            PlayerInputPressedEvent playerInputPressedEvent = new PlayerInputPressedEvent(player, input);
            ingameClient.getPluginManager().fireEvent(playerInputPressedEvent);

            if (playerInputPressedEvent.isCancelled())
                return false;
        }

        // Handle interaction locally
        return playerEntity.traits.tryWithBoolean(TraitControllable.class, t -> t.onControllerInput(input));
    }

    @Override
    public boolean onInputReleased(Input input) {
        IngameClient ingameClient = gameWindow.getClient().getIngame();
        if(ingameClient == null)
            return false;

        ClientInputReleasedEvent event = new ClientInputReleasedEvent(gameWindow.getClient(), input);
        ingameClient.getPluginManager().fireEvent(event);

        final LocalPlayer player = ingameClient.getPlayer();
        final Entity entityControlled = player.getControlledEntity();

        // There has to be a controlled entity for sending inputs to make sense.
        if (entityControlled == null)
            return false;

        // Send input to server
        World world = entityControlled.getWorld();
        if (world instanceof WorldClientRemote) {
            ServerConnection connection = ((WorldClientRemote) entityControlled.getWorld()).getConnection();
            PacketInput packet = new PacketInput(world);
            packet.input = input;
            packet.isPressed = false;
            connection.pushPacket(packet);
            return true;
        } else {
            PlayerInputReleasedEvent event2 = new PlayerInputReleasedEvent(player, input);
            ingameClient.getPluginManager().fireEvent(event2);
            return true;
        }
    }

    @Override
    public Mouse getMouse() {
        return mouse;
    }

    public void destroy() {
        this.keyCallback.free();
        this.mouseButtonCallback.free();
        this.scrollCallback.free();
        this.characterCallback.free();
    }

    public Logger logger() {
        return logger;
    }
}
