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
import io.xol.chunkstories.api.gui.elements.Button;
import io.xol.chunkstories.api.gui.elements.InputText;
import io.xol.chunkstories.api.gui.elements.LargeButtonWithIcon;
import io.xol.chunkstories.api.gui.elements.ScrollableContainer;
import io.xol.chunkstories.api.gui.elements.ScrollableContainer.ContainerElement;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.util.Configuration;
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

    private LargeButtonWithIcon backOption = new LargeButtonWithIcon(this, "back");
    private InputText ipForm = new InputText(this, 0, 0, 250);
    private Button connectButton = new Button(this, 0, 0, 0, "#{connection.connect}");
    private ServerSelectionZone serverSelectionZone = new ServerSelectionZone(this);

    private boolean automaticLogin;
    private boolean movedInList = false;

    private final static Logger logger = LoggerFactory.getLogger("gui.serverselection");

    ServerSelection(Gui gui, Layer parent, boolean a) {
        super(gui, parent);
        elements.add(ipForm);

        this.setFocusedElement(ipForm);

        this.connectButton.setAction(() -> login());
        this.backOption.setAction(() -> gui.setTopLayer(parentLayer));

        elements.add(connectButton);
        elements.add(serverSelectionZone);
        elements.add(backOption);

        automaticLogin = a;
        String lastServer = gui.getClient().getConfiguration().getValue("client.game.lastServer");
        if (!lastServer.equals(""))
            ipForm.setText(lastServer);

        // Create the HTTP request to poll actives servers.
        new HttpRequestThread(this, "serversList", "http://chunkstories.xyz/api/listServers.php", "");
    }

    @Override
    public void render(GuiDrawer drawer) {
        if (parentLayer != null) {
            parentLayer.render(drawer);
        }

        if (automaticLogin && !ipForm.getText().equals(""))
            login();

        String instructions = "Select a server from the list or type in the address directly";
        Font titleFont = drawer.getFonts().getFont("LiberationSans-Regular", 33);
        drawer.drawStringWithShadow(titleFont, 32, gui.getViewportHeight() - 32 * 2, instructions, -1, new Vector4f(1));

        // gui
        int ipTextboxSize = gui.getViewportWidth() - connectButton.getWidth() - 48 - 8;
        ipForm.setPosition(25, gui.getViewportHeight() - 100);
        ipForm.setWidth(ipTextboxSize);
        ipForm.render(drawer);

        connectButton.setPosition(ipForm.getPositionX() + ipForm.getWidth() + 4,
                gui.getViewportHeight() - 100);

        connectButton.render(drawer);

        backOption.setPosition(8, 8);
        backOption.render(drawer);

        updateServers();

        int offsetForButtons = backOption.getPositionY() + backOption.getHeight() + 8;
        int offsetForHeaderText = 32 + ipForm.getHeight();
        serverSelectionZone.setPosition((width - 480) / 2, offsetForButtons);
        serverSelectionZone.setSize(480, height - (offsetForButtons + offsetForHeaderText));
        serverSelectionZone.render(drawer);
    }

    @Override
    public boolean handleInput(Input input) {
        if (input.equals("enter"))
            login();
        else if (input.equals("refreshServers")) // F5
            new HttpRequestThread(this, "serversList", "http://chunkstories.xyz/api/listServers.php", "");
        else if (input.equals("repingServers")) // F6 ?
            f6();
        else if (input.equals("exit"))
            gui.popTopLayer();
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

        ((Configuration.OptionString)gui.getClient().getConfiguration().get("client.game.lastServer")).trySetting(ip);
        gui.getClient().getConfiguration().save();

        if (ip.contains(":")) {
            port = Integer.parseInt(ip.split(":")[1]);
            ip = ip.split(":")[0];
        }

        //TODO create connection sequence (ongoing refactor of that)
        //gui.setTopLayer(new RemoteConnectionGuiLayer(gui, this, ip, port));
    }

    private int currentServer = 0;

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

    class ServerSelectionZone extends ScrollableContainer {
        ServerSelectionZone(Layer layer) {
            super(layer);
        }

        class ServerGuiItem extends ContainerElement {

            ServerDataLoader sd;
            String ip;
            int port;

            ServerGuiItem(String ip, int port) {
                super("Loading server info for " + ip + ":" + port, "");
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

            void reload() {
                if (sd.infoError || sd.infoLoaded)
                    this.sd = new ServerDataLoader(this, ip, port);
            }
        }
    }

    // Sub-class for server data loading
    public class ServerDataLoader extends Thread {
        ServerGuiItem parent;

        String ip;
        int port;
        String name = "Loading...";
        String description = "Loading...";
        String gameMode = "Loading...";
        String version = "Loading...";
        boolean infoLoaded = false;
        boolean infoError = false;
        long connectStart;
        long ping = 42;

        ServerDataLoader(ServerGuiItem parent, String ip, int port) {
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
                out.writeInt("info".getBytes("UTF-8").length + 2);
                out.writeUTF("info");
                out.flush();

                ping = System.currentTimeMillis() - connectStart;
                String lineRead = "";

                while (!lineRead.startsWith("info/done")) {
                    // Discard first byte, assummed to be packed id
                    in.readByte();
                    // Discard one more byte, assumed to be packet length
                    in.readInt();
                    lineRead = in.readUTF();
                    // System.out.println("red:"+lineRead);
                    if (lineRead.startsWith("info/")) {
                        String data[] = lineRead.replace("info/", "").split(":");
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
            } catch (Exception ignored) { }
        }
    }
}
