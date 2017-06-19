package io.xol.chunkstories.gui.overlays;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.lwjgl.input.Keyboard;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.gui.OverlayableScene;
import io.xol.chunkstories.gui.ng.ScrollableContainer;
import io.xol.chunkstories.gui.ng.ScrollableContainer.ContainerElement;
import io.xol.chunkstories.gui.overlays.ServerSelectionOverlayNg.ServerSelectionZone.ServerGuiItem;
import io.xol.chunkstories.gui.overlays.ingame.ConnectionOverlay;
import io.xol.engine.base.InputAbstractor;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.FontRenderer2;
import io.xol.engine.gui.Overlay;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.InputText;
import io.xol.engine.net.HttpRequestThread;
import io.xol.engine.net.HttpRequester;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ServerSelectionOverlayNg extends Overlay implements HttpRequester
{
	InputText ipForm = new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS);
	Button backOption = new Button(0, 0, 300, 32, ("Back"), BitmapFont.SMALLFONTS, 1);
	Button connectButton = new Button(0, 0, 128, 32, "Connect", BitmapFont.SMALLFONTS, 1);
	
	ServerSelectionZone serverSelectionZone = new ServerSelectionZone();
	
	boolean autologin;
	private boolean movedInList = false;

	public ServerSelectionOverlayNg(OverlayableScene scene, Overlay parent, boolean a)
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
		new HttpRequestThread(this, "serversList", "http://chunkstories.xyz/api/listServers.php", "");
	}

	@Override
	public void drawToScreen(RenderingContext renderingContext, int x, int y, int w, int h)
	{
		if (autologin && !ipForm.text.equals(""))
			login();

		// title
		FontRenderer2.drawTextUsingSpecificFontRVBA(32, renderingContext.getWindow().getHeight() - 32 * (1 + 1), 0, 32 + 1 * 16, "Select a server", BitmapFont.SMALLFONTS, 1f, 1f, 1f, 1f);
		// gui
		int txtbox = renderingContext.getWindow().getWidth() - 50 - guiHandler.getButton(1).getWidth() * 2 - 75;
		ipForm.setPosition(25, renderingContext.getWindow().getHeight() - 50 * (1 + 1));
		ipForm.setMaxLength(txtbox);
		ipForm.drawWithBackGround();
		
		guiHandler.getButton(1).setPosition(txtbox + 96 + 12, renderingContext.getWindow().getHeight() - 50 - 16 - 18);
		
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
		int s = Client.getInstance().getGameWindow().getScalingFactor();
		
		serverSelectionZone.setPosition((w - 480 * s) / 2, 32);
		serverSelectionZone.setDimensions(480 * s, h - 32 - 128);
		serverSelectionZone.render();
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
		else if (Client.getInstance().getInputsManager().getInputByName("enter").isPressed())
			login();
		else if (k == 63) // F5
			new HttpRequestThread(this, "serversList", "http://chunkstories.xyz/api/listServers.php", "");
		else if (k == 64) // F6 ?
			f6();
		else if (Client.getInstance().getInputsManager().getInputByName("exit").isPressed())
			this.mainScene.changeOverlay(parent);
		else if (serverSelectionZone.hasFocus() && Client.getInstance().getInputsManager().getInputByName("forward").isPressed())
		{
			movedInList = true;
			currentServer--;
		}
		else if (serverSelectionZone.hasFocus() && Client.getInstance().getInputsManager().getInputByName("back").isPressed())
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
		
		Client.clientConfig.setString("last-server", ip);
		Client.clientConfig.save();
		
		if (ip.contains(":"))
		{
			port = Integer.parseInt(ip.split(":")[1]);
			ip = ip.split(":")[0];
		}
		
		Client.world = null;
		
		this.mainScene.changeOverlay(new ConnectionOverlay(mainScene, mainScene.currentOverlay, ip, port));
		//this.mainScene.gameWindow.changeScene(new ConnectScene(mainScene.gameWindow, ip, port));
	}

	@Override
	public boolean onClick(int posx, int posy, int button)
	{
			guiHandler.handleClick(posx, posy);
		return true;
	}

	void drawRightedText(RenderingInterface renderingContext, String t, float decx, float height, int basesize, float r, float v, float b, float a)
	{
		FontRenderer2.drawTextUsingSpecificFontRVBA(renderingContext.getWindow().getWidth() - decx - FontRenderer2.getTextLengthUsingFont(basesize, t, BitmapFont.SMALLFONTS), height, 0, basesize, t, BitmapFont.SMALLFONTS, a, r, v, b);
	}

	int currentServer = 0;
	int oldServer = 0;

	private void updateServers()
	{
		if (serverSelectionZone.elements.size() == 0)
			return;

		if (currentServer < 0)
			currentServer = 0;
		if (currentServer > serverSelectionZone.elements.size() - 1)
			currentServer = serverSelectionZone.elements.size() - 1;

		if (movedInList)
		{
			movedInList = false;
			ipForm.text = ((ServerGuiItem) serverSelectionZone.elements.get(currentServer)).ip + (((ServerGuiItem) serverSelectionZone.elements.get(currentServer)).port == 30410 ? "" : ((ServerGuiItem) serverSelectionZone.elements.get(currentServer)).port);
		}
	}

	private void f6()
	{
		for (ContainerElement ce : serverSelectionZone.elements)
		{
			try
			{
				((ServerGuiItem) ce).reload();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public class ServerSelectionZone extends ScrollableContainer
	{
		class ServerGuiItem extends ContainerElement {

			ServerDataLoader sd;
			public String ip;
			public int port;
			
			public ServerGuiItem(String ip, int port)
			{
				super("Loading server info for "+ip+":"+port, "");
				this.ip = ip;
				this.port = port;
				this.sd = new ServerDataLoader(this, ip, port);
				this.iconTextureLocation = GameDirectory.getGameFolderPath()+"/cache/server-icon-"+ip+"-"+port+".png";
			}

			@Override
			public void clicked()
			{
				if(sd != null && sd.infoLoaded)
				{
					ipForm.text = sd.ip + (sd.port == 30410 ? "" : sd.port);
					login();
				}
			}
			
			public void reload()
			{
				if(sd.infoError || sd.infoLoaded)
					this.sd = new ServerDataLoader(this, ip, port);
			}
		}
	}

	// Sub-class for server data loading
	public class ServerDataLoader extends Thread
	{
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

		public ServerDataLoader(ServerGuiItem parent, String ip, int port)
		{
			this.parent = parent;
			
			this.ip = ip;
			this.port = port;
			this.setName("ServerData updater " + ip + port);
			this.start();
		}

		@Override
		public void run()
		{
			try
			{
				connectStart = System.currentTimeMillis();
				Socket socket = new Socket(ip, port);
				DataInputStream in = new DataInputStream(socket.getInputStream());
				DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				out.write((byte)0x00);
				out.writeUTF("info");
				out.flush();
				
				ping = System.currentTimeMillis() - connectStart;
				String lineRead = "";
				
				while (!lineRead.startsWith("info/done"))
				{
					//Discard first byte, assummed to be packed id
					in.readByte();
					lineRead = in.readUTF();
					//System.out.println("red:"+lineRead);
					if (lineRead.startsWith("info/"))
					{
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
				
				//Requests icon file
				out.write((byte)0x00);
				out.writeUTF("icon-file");
				out.flush();
				//Expect reply immediately
				byte expect = in.readByte();
				System.out.println("Expected:"+expect);
				//Read and discard tag, we know what we are expecting
				in.readUTF();
				long fileLength = in.readLong();
				System.out.println("fileLength:"+fileLength);
				
				if (fileLength > 0)
				{
					File file = new File(GameDirectory.getGameFolderPath()+"/cache/server-icon-"+ip+"-"+port+".png");
					FileOutputStream fos = new FileOutputStream(file);
					long remaining = fileLength;
					byte[] buffer = new byte[4096];
					while (remaining > 0)
					{
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
			
			parent.name = name;
			parent.descriptionLines = description + "\n "+gameMode;
			parent.topRightString = version;
		}
	}

	@Override
	public void handleHttpRequest(String info, String result)
	{
		// Will load fucking servers !
		serverSelectionZone.elements.clear();
		if (info.equals("serversList"))
		{
			try
			{
				for (String line : result.split(";"))
				{
					String address = line.split(":")[2];
					serverSelectionZone.elements.add(serverSelectionZone.new ServerGuiItem(address, 30410));
				}
			}
			catch (Exception e)
			{

			}
		}
	}
}
