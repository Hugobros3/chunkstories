//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.client;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import io.xol.chunkstories.IngameClientImplementation;
import io.xol.chunkstories.api.GameContext;
import io.xol.chunkstories.api.client.Client;
import io.xol.chunkstories.api.client.IngameClient;
import io.xol.chunkstories.api.content.Content;
import io.xol.chunkstories.api.graphics.Window;
import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.client.glfw.GLFWWindow;
import io.xol.chunkstories.content.GameContentStore;
import io.xol.chunkstories.sound.ALSoundManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.Constants;
import io.xol.chunkstories.api.client.ClientSoundManager;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.item.inventory.Inventory;
import io.xol.chunkstories.api.plugin.ClientPluginManager;
import io.xol.chunkstories.api.workers.Tasks;
import io.xol.chunkstories.api.world.WorldClient;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.gui.layer.LoginPrompt;
import io.xol.chunkstories.gui.layer.MainMenu;
import io.xol.chunkstories.gui.layer.MessageBox;
import io.xol.chunkstories.gui.layer.SkyBoxBackground;
import io.xol.chunkstories.gui.layer.ingame.ConnectionOverlay;
import io.xol.chunkstories.gui.layer.ingame.Ingame;
import io.xol.chunkstories.gui.layer.ingame.InventoryView;
import io.xol.chunkstories.input.lwjgl3.Lwjgl3ClientInputsManager;
import io.xol.chunkstories.server.commands.InstallServerCommands;
import io.xol.chunkstories.server.commands.content.ReloadContentCommand;
import io.xol.chunkstories.util.LogbackSetupHelper;
import io.xol.chunkstories.util.VersionInfo;
import io.xol.chunkstories.util.config.ConfigurationImplementation;
import io.xol.chunkstories.world.WorldClientCommon;
import io.xol.chunkstories.world.WorldClientLocal;

import javax.annotation.Nonnull;

public class ClientImplementation implements Client, GameContext {
    private final Logger logger;
    private final Logger chatLogger = LoggerFactory.getLogger("game.chat");

    private final ConfigurationImplementation configuration;

    private final GameContentStore gameContent;

    private final GLFWWindow gameWindow;
    private final ALSoundManager soundManager;
    private final ClientTasksPool workers;

    public String username = "Undefined";
    public String session_key = "";
    public boolean offline = false;

    private IngameClient ingameClient;

    public static void main(String[] launchArguments) {
        // Check for folder
        GameDirectory.check();
        GameDirectory.initClientPath();

        //osx dirty fix
        System.setProperty("java.awt.headless", "true");

        File coreContentLocation = new File("core_content.zip");

        String modsStringArgument = null;
        for (String launchArgument : launchArguments) {
            if (launchArgument.equals("--forceobsolete")) {

                ClientLimitations.ignoreObsoleteHardware = false;
                System.out.println(
                        "Ignoring OpenGL detection. This is absolutely definitely not going to make the game run, proceed at your own risk of imminent failure."
                                + "You are stripped of any tech support rights when running the game using this.");
            } else if (launchArgument.contains("--mods")) {
                modsStringArgument = launchArgument.replace("--mods=", "");
            } else if (launchArgument.contains("--dir")) {
                GameDirectory.set(launchArgument.replace("--dir=", ""));
            } else if (launchArgument.contains("--core")) {
                String coreContentLocationPath = launchArgument.replace("--core=", "");
                coreContentLocation = new File(coreContentLocationPath);
            } else {
                String helpText = "Chunk Stories client " + VersionInfo.version + "\n";

                if (launchArgument.equals("-h") || launchArgument.equals("--help"))
                    helpText += "Command line help: \n";
                else
                    helpText += "Unrecognized command: " + launchArgument + "\n";

                helpText += "--forceobsolete Forces the game to run even if requirements aren't met. No support will be offered when using this! \n";
                helpText += "--mods=xxx,yyy | -mods=* Tells the game to start with those mods enabled\n";
                helpText += "--dir=whatever Tells the game not to look for .chunkstories at it's normal location and instead use the argument\n";
                helpText += "--core=whaterverfolder/ or --core=whatever.zip Tells the game to use some specific folder or archive as it's base content.\n";

                System.out.println(helpText);
                return;
            }
        }

        new ClientImplementation(coreContentLocation, modsStringArgument);

        // Not supposed to happen, gets there when ClientImplementation crashes badly.
        System.exit(-1);
    }

    ClientImplementation(File coreContentLocation, String modsStringArgument) {

        // Name the thread
        Thread.currentThread().setName("Main OpenGL Rendering thread");
        Thread.currentThread().setPriority(Constants.MAIN_GL_THREAD_PRIORITY);

        // Start logging system
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY.MM.dd HH.mm.ss");
        String time = sdf.format(cal.getTime());

        logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        String loggingFilename = GameDirectory.getGameFolderPath() + "/logs/" + time + ".log";
        new LogbackSetupHelper(loggingFilename);

        soundManager = new ALSoundManager();

        // Creates game window, no use of any user content up to this point
        gameWindow = new GLFWWindow(this, "Chunk Stories " + VersionInfo.version);

        // Create game content manager
        gameContent = new GameContentStore(this, coreContentLocation, modsStringArgument);
        gameContent.reload();

        configuration = new ClientConfigurationImplementation(this, new File("./config/client.cfg"));
        //gameWindow.stage_2_init();
        configuration.load();

        // Spawns worker threads
        int nbThreads = -1;
        String configThreads = getConfiguration().getStringOption("workersThreads");
        if (!configThreads.equals("auto")) {
            try {
                nbThreads = Integer.parseInt(configThreads);
            } catch (NumberFormatException e) {
            }
        }

        if (nbThreads <= 0) {
            nbThreads = Runtime.getRuntime().availableProcessors() / 2;

            // Fail-safe
            if (nbThreads < 1)
                nbThreads = 1;
        }

        workers = new ClientTasksPool(this, nbThreads);
        workers.start();

        // Load the correct language
        String lang = getConfiguration().getStringOption("client.game.language");
        if (!lang.equals("undefined"))
            gameContent.localization().loadTranslation(lang);

        // Initlializes windows screen to main menu ( and ask for login )
        getGui().setTopLayer(new LoginPrompt(gameWindow, new SkyBoxBackground(gameWindow)));

        gameWindow.mainLoop();
        cleanup();
    }

    @Override
    public ClientSoundManager getSoundManager() {
        return soundManager;
    }

    public void cleanup() {
        workers.destroy();
        configuration.save();
    }

    public void reloadAssets() {
        configuration.save();
        gameContent.reload();
        configuration.reload();
        gameWindow.getInputsManager().reload();
        //TODO hook some rendering stuff in here
        configuration.load();
    }

    public void openInventories(Inventory... inventories) {
        if (gameWindow.getLayer().getRootLayer() instanceof Ingame) {
            Ingame gmp = (Ingame) gameWindow.getLayer().getRootLayer();
            gameWindow.setLayer(new InventoryView(gameWindow, gmp, inventories));
            gmp.focus(false);
        }
    }

    @Override
    public void changeWorld(WorldClient worldClient) {
        assert worldClient instanceof WorldClientCommon;
        WorldClientCommon world = (WorldClientCommon) worldClient;

        // Setup the new world and make a controller for it
        PlayerClientImplementation player = new PlayerClientImplementation(ClientImplementation.this, world);

        pluginManager.reloadPlugins();
        new ReloadContentCommand(ClientImplementation.this);
        if (world instanceof WorldClientLocal) {
            new InstallServerCommands(((WorldClientLocal) world).getLocalServer());
        }

        // Change the scene
        Ingame ingameScene = new Ingame(getGui(), world);

        // We want to keep the connection overlay when getting into a server
        // TODO that logic doesn't belong there
        if (getGui().getTopLayer() instanceof ConnectionOverlay) {
            ConnectionOverlay overlay = (ConnectionOverlay) getGui().getTopLayer();
            // If that happen, we want this connection overlay to forget he was originated
            // from a server browser or whatever by changing that reference
            overlay.setParentScene(ingameScene);
        } else
            getGui().setTopLayer(ingameScene);

        ingameClient = new IngameClientImplementation(this, world, player, ingameScene);

        // Start only the logic after all that
        world.startLogic();
    }

    @Override
    public void exitToMainMenu() {
        Layer currentRootLayer = getGui().getTopLayer().getRootLayer();
        if (currentRootLayer != null && currentRootLayer instanceof Ingame) {
            currentRootLayer.destroy();
        }

        gameWindow.setLayer(new MainMenu(gameWindow, new SkyBoxBackground(gameWindow)));

        if (world != null) {
            ClientImplementation.this.world.destroy();
            ClientImplementation.this.world = null;
        }
        player = null;

        ClientImplementation.this.getSoundManager().stopAnySound();
    }

    public void exitToMainMenu(String errorMessage) {
        getGui().setTopLayer(new MessageBox(gameWindow, new SkyBoxBackground(gameWindow), errorMessage));

        ClientImplementation.this.getSoundManager().stopAnySound();
    }

    public void print(String message) {
        chatLogger.info(message);
    }

    public Content getContent() {
        return gameContent;
    }

    private ClientPluginManager pluginManager = null;

    // We have to set a reference from Ingame via a callback since stuff called from
    // within it's very constructor rely on this global reference.
    // TODO it shouldn't I guess ?
    public void setClientPluginManager(ClientPluginManager pl) {
        this.pluginManager = pl;
    }

    public ClientPluginManager getPluginManager() {
        return pluginManager;
    }

    @Override
    public Lwjgl3ClientInputsManager getInputsManager() {
        return gameWindow.getInputsManager();
    }

    @Nonnull
    public Window getGameWindow() {
        return gameWindow;
    }

    public Logger logger() {
        return this.logger;
    }

    public Tasks getTasks() {
        return workers;
    }

    @Override
    public ConfigurationImplementation getConfiguration() {
        return configuration;
    }

    @NotNull
    @Override
    public Gui getGui() {
        return null;
    }

    @Nullable
    @Override
    public IngameClient getIngame() {
        return ingameClient;
    }
}
