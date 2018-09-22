//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.layer;

import io.xol.chunkstories.api.gui.Font;
import io.xol.chunkstories.api.gui.Gui;
import io.xol.chunkstories.api.gui.GuiDrawer;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.gui.elements.BaseButton;
import io.xol.chunkstories.api.gui.elements.InputText;
import io.xol.chunkstories.api.gui.elements.LargeButtonIcon;
import io.xol.chunkstories.api.gui.elements.ScrollableContainer;
import io.xol.chunkstories.api.gui.elements.ScrollableContainer.ContainerElement;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.gui.layer.ServerSelection.ServerSelectionZone.ServerGuiItem;
import io.xol.chunkstories.gui.layer.ingame.RemoteConnectionGuiLayer;
import io.xol.chunkstories.net.http.HttpRequestThread;
import io.xol.chunkstories.net.http.HttpRequester;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;

public class ServerSelection extends Layer implements HttpRequester {
    InputText ipForm = new InputText(this, 0, 0, 250);

    LargeButtonIcon backOption = new LargeButtonIcon(this, "back");
    BaseButton connectButton = new BaseButton(this, 0, 0, 0, "#{connection.connect}");

    ServerSelectionZone serverSelectionZone = new ServerSelectionZone(this);

    boolean autologin;
    private boolean movedInList = false;

    private final static Logger logger = LoggerFactory.getLogger("gui.serverselection");

    public ServerSelection(Gui gui, Layer parent, boolean a) {
        super(gui, parent);
        elements.add(ipForm);

        this.setFocusedElement(ipForm);

        this.connectButton.setAction(() -> login());
        this.backOption.setAction(() -> gui.setTopLayer(parentLayer));

        elements.add(connectButton);
        elements.add(serverSelectionZone);
        elements.add(backOption);

        autologin = a;
        String lastServer = gui.getClient().getConfiguration().getStringOption("client.game.last-server");
        if (!lastServer.equals(""))
            ipForm.setText(lastServer);

        // Create the HTTP request to poll actives servers.
        new HttpRequestThread(this, "serversList", "http://chunkstories.xyz/api/listServers.php", "");
    }

    @Override
    public void render(GuiDrawer renderer) {
        parentLayer.getRootLayer().render(renderer);

        if (autologin && !ipForm.getText().equals(""))
            login();

        String instructions = "Select a server from the list or type in the address directly";
        Font font = renderer.getFontRenderer().getFont("LiberationSans-Regular", 11);
        renderer.getFontRenderer().drawStringWithShadow(font, 32, renderer.getWindow().getHeight() - 32 * 2,
                instructions, 3, 3, new Vector4f(1));

        // gui
        float txtbox = renderer.getWindow().getWidth() - connectButton.getWidth() - 48 - 8;
        ipForm.setPosition(25, renderer.getWindow().getHeight() - 100);
        ipForm.setWidth(txtbox / this.getGuiScale());
        ipForm.render(renderer);

        connectButton.setPosition(ipForm.getPositionX() + ipForm.getWidth() + 4,
                renderer.getWindow().getHeight() - 100);

        connectButton.render(renderer);

        backOption.setPosition(8, 8);
        backOption.render(renderer);

        updateServers();

        float offsetForButtons = backOption.getPositionY() + backOption.getHeight() + 8;
        float offsetForHeaderText = 32 + ipForm.getHeight();
        serverSelectionZone.setPosition((width - 480) / 2, offsetForButtons);
        serverSelectionZone.setDimensions(480, height - (offsetForButtons + offsetForHeaderText));
        serverSelectionZone.render(renderer);
    }

    // Controls handling
    @Override
    public boolean handleInput(Input input) {
        if (input.equals("enter"))
            login();
        else if (input.equals("refreshServers")) // F5
            new HttpRequestThread(this, "serversList", "http://chunkstories.xyz/api/listServers.php", "");
        else if (input.equals("repingServers")) // F6 ?
            f6();
        else if (input.equals("exit"))
            gameWindow.setLayer(parentLayer);
        else if (serverSelectionZone.isFocused() && input.equals("uiUp")) {
            movedInList = true;
            currentServer--;
        } else if (serverSelectionZone.isFocused() && input.equals("uiDown")) {
            movedInList = true;
            currentServer++;
        } else
            // else
            // guiHandler.handleInput(k);
            return super.handleInput(input);

        return true;
    }

    // Takes care of connecting to a server
    private void login() {
        String ip = ipForm.getText();
        int port = 30410;

        if (ip.length() == 0)
            return;

        gui.getClient().getConfiguration().getOption("client.game.last-server").trySetting(ip);
        gui.getClient().getConfiguration().save();

        if (ip.contains(":")) {
            port = Integer.parseInt(ip.split(":")[1]);
            ip = ip.split(":")[0];
        }

        // ClientImplementation.world = null;

        gui.setTopLayer(new RemoteConnectionGuiLayer(gui, this, ip, port));
    }

    int currentServer = 0;
    int oldServer = 0;

    private void updateServers() {
        if (serverSelectionZone.elements.size() == 0)
            return;

        if (currentServer < 0)
            currentServer = 0;
        if (currentServer > serverSelectionZone.elements.size() - 1)
            currentServer = serverSelectionZone.elements.size() - 1;

        if (movedInList) {
            movedInList = false;
            ipForm.setText(((ServerGuiItem) serverSelectionZone.elements.get(currentServer)).ip
                    + (((ServerGuiItem) serverSelectionZone.elements.get(currentServer)).port == 30410 ? ""
                    : ((ServerGuiItem) serverSelectionZone.elements.get(currentServer)).port));
        }
    }

    private void f6() {
        for (ContainerElement ce : serverSelectionZone.elements) {
            try {
                ((ServerGuiItem) ce).reload();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public class ServerSelectionZone extends ScrollableContainer {
        protected ServerSelectionZone(Layer layer) {
            super(layer);
        }

        class ServerGuiItem extends ContainerElement {

            ServerDataLoader sd;
            public String ip;
            public int port;

            public ServerGuiItem(String ip, int port) {
                super("Loading server worldInfo for " + ip + ":" + port, "");
                this.ip = ip;
                this.port = port;
                this.sd = new ServerDataLoader(this, ip, port);
                this.iconTextureLocation = GameDirectory.getGameFolderPath() + "/cache/server-icon-" + ip + "-" + port
                        + ".png";
            }

            @Override
            public boolean handleClick(MouseButton mouseButton) {
                if (sd != null && sd.infoLoaded) {
                    ipForm.setText(sd.ip + (sd.port == 30410 ? "" : sd.port));
                    login();
                }

                return true;
            }

            public void reload() {
                if (sd.infoError || sd.infoLoaded)
                    this.sd = new ServerDataLoader(this, ip, port);
            }
        }
    }

    // Sub-class for server data loading
    public class ServerDataLoader extends Thread {
        ServerGuiItem parent;

        public String ip;
        public int port;
        String name = "Loading...";
        String description = "Loading...";
        String gameMode = "Loading...";
        String version = "Loading...";
        boolean infoLoaded = false;
        boolean infoError = false;
        long connectStart;
        long ping = 42;

        public ServerDataLoader(ServerGuiItem parent, String ip, int port) {
            this.parent = parent;

            this.ip = ip;
            this.port = port;
            this.setName("ServerData updater " + ip + port);
            this.start();
        }

        @Override
        public void run() {
            try {
                connectStart = System.currentTimeMillis();
                Socket socket = new Socket(ip, port);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                out.write((byte) 0x00);
                out.writeInt("worldInfo".getBytes("UTF-8").length + 2);
                out.writeUTF("worldInfo");
                out.flush();

                ping = System.currentTimeMillis() - connectStart;
                String lineRead = "";

                while (!lineRead.startsWith("worldInfo/done")) {
                    // Discard first byte, assummed to be packed id
                    in.readByte();
                    // Discard one more byte, assumed to be packet length
                    in.readInt();
                    lineRead = in.readUTF();
                    // System.out.println("red:"+lineRead);
                    if (lineRead.startsWith("worldInfo/")) {
                        String data[] = lineRead.replace("worldInfo/", "").split(":");
                        if (data[0].equals("name"))
                            name = data[1];
                        if (data[0].equals("version"))
                            version = data[1];
                        if (data[0].equals("motd"))
                            description = data[1];
                        if (data[0].equals("connected"))
                            gameMode = data[1] + " / " + data[2];
                    }
                }

                // Requests icon file
                out.write((byte) 0x00);
                out.writeInt("icon-file".getBytes("UTF-8").length + 2);
                out.writeUTF("icon-file");
                out.flush();
                // Expect reply immediately
                byte expect = in.readByte();
                logger.info("Expected:" + expect);
                // Read and discard tag, we know what we are expecting
                in.readUTF();
                long fileLength = in.readLong();
                logger.info("fileLength:" + fileLength);

                if (fileLength > 0) {
                    File file = new File(
                            GameDirectory.getGameFolderPath() + "/cache/server-icon-" + ip + "-" + port + ".png");
                    FileOutputStream fos = new FileOutputStream(file);
                    long remaining = fileLength;
                    byte[] buffer = new byte[4096];
                    while (remaining > 0) {
                        long toRead = Math.min(4096, remaining);
                        int actuallyRead = in.read(buffer, 0, (int) toRead);
                        fos.write(buffer, 0, (int) actuallyRead);
                        remaining -= actuallyRead;
                    }
                    fos.close();
                }

                infoLoaded = true;
                in.close();
                out.close();
                socket.close();
            } catch (Exception e) {
                // e.printStackTrace();
                description = e.toString();
                gameMode = "Couldn't update.";
                version = "Unkwnow version";

                infoError = true;
                infoLoaded = true;
            }

            parent.name = name;
            parent.descriptionLines = description + "\n " + gameMode;
            parent.topRightString = version;
        }
    }

    @Override
    public void handleHttpRequest(String info, String result) {
        // Will load fucking servers !
        serverSelectionZone.elements.clear();
        if (info.equals("serversList")) {
            try {
                for (String line : result.split(";")) {
                    String address = line.split(":")[2];
                    serverSelectionZone.elements.add(serverSelectionZone.new ServerGuiItem(address, 30410));
                }
            } catch (Exception e) {

            }
        }
    }
}
