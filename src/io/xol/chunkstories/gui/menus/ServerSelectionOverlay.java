package io.xol.chunkstories.gui.menus;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.gui.ConnectScene;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.input.KeyBinds;
import io.xol.engine.base.InputAbstractor;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.graphics.util.ObjectRenderer;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.gui.GuiElementsHandler;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.GuiElement;
import io.xol.engine.gui.elements.InputText;
import io.xol.engine.net.HttpRequestThread;
import io.xol.engine.net.HttpRequester;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerSelectionOverlay extends Overlay implements HttpRequester
{

	GuiElementsHandler guiHandler = new GuiElementsHandler();
	InputText ipForm = new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS);
	Button backOption = new Button(0, 0, 300, 32, ("Back"), BitmapFont.SMALLFONTS, 1);
	Button connectButton = new Button(0, 0, 128, 32, "Connect", BitmapFont.SMALLFONTS, 1);
	ServerSelectionZone serverSelectionZone = new ServerSelectionZone();
	boolean autologin;
	private boolean movedInList = false;

	public ServerSelectionOverlay(OverlayableScene scene, Overlay parent, boolean a)
	{
		super(scene, parent);
		guiHandler.add(ipForm);
		ipForm.setFocus(true);
		guiHandler.add(connectButton);
		guiHandler.add(serverSelectionZone);
		guiHandler.add(backOption);
		
		autologin = a;
		String lastServer = Client.clientConfig.getProp("last-server", "");
		if (!lastServer.equals(""))
		{
			ipForm.setText(lastServer);
			// System.out.println("ls-load:"+autologin);
		}
		//Create the HTTP request to poll actives servers.
		new HttpRequestThread(this, "serversList", "http://chunkstories.xyz/api/listServers.php", "").start();
	}

	@Override
	public void drawToScreen(int x, int y, int w, int h)
	{
		if (autologin && !ipForm.text.equals(""))
			login();

		// title
		FontRenderer2.drawTextUsingSpecificFontRVBA(32, GameWindowOpenGL.windowHeight - 32 * (1 + 1), 0, 32 + 1 * 16, "Select a server", BitmapFont.SMALLFONTS, 1f, 1f, 1f, 1f);
		// gui
		int txtbox = GameWindowOpenGL.windowWidth - 50 - guiHandler.getButton(1).getWidth() * 2 - 75;
		ipForm.setPosition(25, GameWindowOpenGL.windowHeight - 50 * (1 + 1));
		ipForm.setMaxLength(txtbox);
		ipForm.drawWithBackGround();
		
		guiHandler.getButton(1).setPosition(txtbox + 96 + 12, GameWindowOpenGL.windowHeight - 50 - 16 - 18);
		
		guiHandler.getButton(1).draw();
		if (guiHandler.getButton(1).clicked)
			login();


		backOption.setPosition(x + 192, 96);
		backOption.draw();

		if (backOption.clicked())
		{
			this.mainScene.changeOverlay(this.parent);
		}
		
		updateServers();
	}

	// Controls handling
	@Override
	public boolean handleKeypress(int k)
	{
		if (k == Keyboard.KEY_TAB)// FastConfig.keyTab)
			guiHandler.next();
		//TODO Move special text edition tools to inputText class
		else if (k == 47 && (InputAbstractor.isKeyDown(29) || InputAbstractor.isKeyDown(157))) // Copy/paste
		{
			Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
			if (clip.isDataFlavorAvailable(DataFlavor.stringFlavor))
			{
				String newTxt = null;
				try
				{
					newTxt = (String) clip.getData(DataFlavor.stringFlavor);
				}
				catch (UnsupportedFlavorException | IOException e)
				{
					e.printStackTrace();
				}
				if (newTxt != null)
					ipForm.text = newTxt;
			}
		}
		else if (k == 14 && (InputAbstractor.isKeyDown(29) || InputAbstractor.isKeyDown(157))) // CTR-DELETE
		{
			ipForm.text = "";
		}
		else if (KeyBinds.getKeyBind("enter").isPressed())
			login();
		else if (k == 63) // F5
			new HttpRequestThread(this, "serversList", "http://chunkstories.xyz/api/listServers.php", "").start();
		else if (k == 64) // F6 ?
			f6();
		else if (KeyBinds.getKeyBind("exit").isPressed())
			this.mainScene.changeOverlay(parent);
		else if (serverSelectionZone.hasFocus() && KeyBinds.getKeyBind("forward").isPressed())
		{
			movedInList = true;
			currentServer--;
		}
		else if (serverSelectionZone.hasFocus() && KeyBinds.getKeyBind("back").isPressed())
		{
			movedInList = true;
			currentServer++;
		}
		else
			guiHandler.handleInput(k);
		return false;
	}

	// Takes care of connecting to a server
	private void login()
	{
		String ip = ipForm.text;
		int port = 30410;
		
		if (ip.length() == 0)
			return;
		
		Client.clientConfig.setProp("last-server", ip);
		Client.clientConfig.save();
		
		if (ip.contains(":"))
		{
			port = Integer.parseInt(ip.split(":")[1]);
			ip = ip.split(":")[0];
		}
		
		Client.world = null;
		this.mainScene.eng.changeScene(new ConnectScene(mainScene.eng, ip, port));
	}

	@Override
	public boolean onClick(int posx, int posy, int button)
	{
		ServerData sd = serverSelectionZone.click();
		if(sd != null)
		{
			ipForm.text = sd.ip + (sd.port == 30410 ? "" : sd.port);
			login();
		}
		else
			guiHandler.handleClick(posx, posy);
		return true;
	}

	void drawRightedText(String t, float decx, float height, int basesize, float r, float v, float b, float a)
	{
		FontRenderer2.drawTextUsingSpecificFontRVBA(GameWindowOpenGL.windowWidth - decx - FontRenderer2.getTextLengthUsingFont(basesize, t, BitmapFont.SMALLFONTS), height, 0, basesize, t, BitmapFont.SMALLFONTS, a, r, v, b);
	}

	// Load-save server's list
	List<ServerData> servers = new ArrayList<ServerData>();
	int currentServer = 0;
	int oldServer = 0;

	private void updateServers()
	{
		if (servers.size() == 0)
			return;

		if (currentServer < 0)
			currentServer = 0;
		if (currentServer > servers.size() - 1)
			currentServer = servers.size() - 1;

		if (movedInList)
		{
			movedInList = false;
			ipForm.text = servers.get(currentServer).ip + (servers.get(currentServer).port == 30410 ? "" : servers.get(currentServer).port);
		}
		serverSelectionZone.render();
	}

	private void f6()
	{
		int index = 0;
		for (ServerData sd : servers)
		{
			try
			{
				ServerData lel = new ServerData(sd.ip, sd.port);
				servers.set(index, lel);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			index++;
		}
	}
	
	public class ServerSelectionZone extends GuiElement
	{
		public void render()
		{
			int posy = GameWindowOpenGL.windowHeight - 100 * (1 + 1);
			int i = 0;
			boolean focus;
			for (ServerData sd : servers)
			{
				focus = i == currentServer;
				//focus = false;
				posy +=  - i * 70;
				if(Mouse.getX() > 20 && Mouse.getX() < GameWindowOpenGL.windowWidth - 52
						&& Mouse.getY() > posy - 32 && Mouse.getY() < posy + 32)
				{
					focus = true;
				}
				sd.render(posy, focus);
				i++;
			}
		}
		
		public ServerData click()
		{
			int posy = GameWindowOpenGL.windowHeight - 100 * (1 + 1);
			int i = 0;
			for (ServerData sd : servers)
			{
				//focus = false;
				posy +=  - i * 70;
				if(Mouse.getX() > 20 && Mouse.getX() < GameWindowOpenGL.windowWidth - 52
						&& Mouse.getY() > posy - 32 && Mouse.getY() < posy + 32)
				{
					return sd;
				}
				i++;
			}
			return null;
		}
	}

	// Sub-class for server data
	public class ServerData extends Thread
	{
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

		public ServerData(String ip, int port)
		{
			this.ip = ip;
			this.port = port;
			this.setName("ServerData updater " + ip + port);
			this.start();
		}

		public void render(int posy, boolean focus)
		{
			int offset = focus ? 32 : 0;
			ObjectRenderer.renderTexturedRect(52, posy, 64, 64, 0, 0 + offset, 31, 32 + offset, 64f, "gui/server_data");
			int width = GameWindowOpenGL.windowWidth - 52 - 32 - 64;
			ObjectRenderer.renderTexturedRect(36 + width / 2, posy, width, 64, 16, 0 + offset, 31, 32 + offset, 64f, "gui/server_data");
			ObjectRenderer.renderTexturedRect(width + 64, posy, 33 * 2, 64, 31, 0 + offset, 64, 32 + offset, 64f, "gui/server_data");
			// text
			FontRenderer2.drawTextUsingSpecificFont(28, posy, 0, 32, (infoError ? "#FF0000" : "") + name + " #AAAAAA- " + ip + (port == 30410 ? "" : ":" + port) + "- " + version + (infoError ? "" : " - " + ping + "ms"), BitmapFont.SMALLFONTS);
			if (infoLoaded)
				FontRenderer2.drawTextUsingSpecificFont(28, posy - 26, 0, 32, (infoError ? "#FF0000" : "") + description + " #AAAAAA (" + gameMode + ")", BitmapFont.SMALLFONTS);
		}

		@Override
		public synchronized void run()
		{
			try
			{
				connectStart = System.currentTimeMillis();
				Socket socket = new Socket(ip, port);
				DataInputStream in = new DataInputStream(socket.getInputStream());
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				out.write((byte)0x00);
				out.writeUTF("info");
				ping = System.currentTimeMillis() - connectStart;
				String mdr = "";
				//System.out.println("kek:"+mdr+"port:"+port);
				while (!mdr.startsWith("info/done"))
				{
					in.readByte();
					mdr = in.readUTF();
					if (mdr.startsWith("info/"))
					{
						String data[] = mdr.replace("info/", "").split(":");
						if (data[0].equals("name"))
							name = data[1];
						if (data[0].equals("version"))
							version = data[1];
						if (data[0].equals("motd"))
							description = data[1];
						if (data[0].equals("connected"))
							gameMode = data[1] + " / " + data[2];
					}
					//System.out.println("server_prompter:"+mdr);
				}
				//System.out.println("kok:"+mdr);
				infoLoaded = true;
				in.close();
				out.close();
				socket.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				description = "Couldn't update.";
				gameMode = "Couldn't update.";
				version = "Unkwnow version";
				infoError = true;
				infoLoaded = true;
			}
		}
	}

	@Override
	public void handleHttpRequest(String info, String result)
	{
		// Will load fucking servers !
		servers.clear();
		if (info.equals("serversList"))
		{
			try
			{
				for (String line : result.split(";"))
				{
					String address = line.split(":")[2];
					servers.add(new ServerData(address, 30410));
				}
			}
			catch (Exception e)
			{

			}
		}
	}
}
