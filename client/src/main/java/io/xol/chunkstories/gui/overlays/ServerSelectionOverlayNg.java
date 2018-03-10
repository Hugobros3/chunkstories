//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.gui.overlays;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.Socket;

import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseButton;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.text.FontRenderer.Font;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.content.GameDirectory;
import io.xol.chunkstories.gui.ng.LargeButtonIcon;
import io.xol.chunkstories.gui.ng.ScrollableContainer;
import io.xol.chunkstories.gui.ng.ScrollableContainer.ContainerElement;
import io.xol.chunkstories.gui.overlays.ServerSelectionOverlayNg.ServerSelectionZone.ServerGuiItem;
import io.xol.chunkstories.gui.overlays.ingame.ConnectionOverlay;
import io.xol.engine.gui.elements.Button;
import io.xol.engine.gui.elements.InputText;
import io.xol.engine.net.HttpRequestThread;
import io.xol.engine.net.HttpRequester;

public class ServerSelectionOverlayNg extends Layer implements HttpRequester
{
	InputText ipForm = new InputText(this, 0, 0, 500);

	LargeButtonIcon backOption = new LargeButtonIcon(this, "back");
	Button connectButton = new Button(this, 0, 0, 128, "#{connection.connect}");
	
	ServerSelectionZone serverSelectionZone = new ServerSelectionZone(this);
	
	boolean autologin;
	private boolean movedInList = false;
	
	private final static Logger logger = LoggerFactory.getLogger("gui.serverselection");

	public ServerSelectionOverlayNg(GameWindow scene, Layer parent, boolean a)
	{
		super(scene, parent);
		elements.add(ipForm);
		
		this.setFocusedElement(ipForm);
		//ipForm.setFocus(true);
		
		this.connectButton.setAction(new Runnable() {

			@Override
			public void run() {
				login();
			}
			
		});
		
		this.backOption.setAction(new Runnable() {

			@Override
			public void run() {
				gameWindow.setLayer(parentLayer);
				//this.mainScene.changeOverlay(this.parent);
			}
			
		});
		
		elements.add(connectButton);
		elements.add(serverSelectionZone);
		elements.add(backOption);
		
		autologin = a;
		String lastServer = Client.getInstance().configDeprecated().getString("last-server", "");
		if (!lastServer.equals(""))
		{
			ipForm.setText(lastServer);
			// System.out.println("ls-load:"+autologin);
		}
		//Create the HTTP request to poll actives servers.
		new HttpRequestThread(this, "serversList", "http://chunkstories.xyz/api/listServers.php", "");
	}

	@Override
	public void render(RenderingInterface renderer)
	{
		parentLayer.getRootLayer().render(renderer);
		
		if (autologin && !ipForm.text.equals(""))
			login();

		String instructions = "Select a server from the list or type in the address directly";
		Font font = renderer.getFontRenderer().getFont("LiberationSans-Regular", 11);
		renderer.getFontRenderer().drawStringWithShadow(font, 32, renderer.getWindow().getHeight() - 32 * 2, instructions, 3, 3, new Vector4f(1));
		//FontRenderer2.drawTextUsingSpecificFontRVBA(32, renderer.getWindow().getHeight() - 32 * (1 + 1), 0, 32 + 1 * 16, , BitmapFont.SMALLFONTS, 1f, 1f, 1f, 1f);
		
		// gui
		float txtbox = renderer.getWindow().getWidth() - connectButton.getWidth();
		ipForm.setPosition(25, renderer.getWindow().getHeight() - 50 * (1 + 1));
		ipForm.setWidth(txtbox);
		ipForm.drawWithBackGround(renderer);
		
		connectButton.setPosition(txtbox + 96 + 12, renderer.getWindow().getHeight() - 50 - 16 - 18);
		
		connectButton.render(renderer);

		backOption.setPosition(8, 8);
		backOption.render(renderer);

		updateServers();
		int s = Client.getInstance().getGameWindow().getGuiScale();
		
		serverSelectionZone.setPosition((width - 480 * s) / 2, 32);
		serverSelectionZone.setDimensions(480 * s, height - 32 - 128);
		serverSelectionZone.render(renderer);
	}

	// Controls handling
	@Override
	public boolean handleInput(Input input)
	{
		if (input.equals("enter"))
			login();
		else if (input.equals("refreshServers")) // F5
			new HttpRequestThread(this, "serversList", "http://chunkstories.xyz/api/listServers.php", "");
		else if (input.equals("repingServers")) // F6 ?
			f6();
		else if (input.equals("exit"))
			gameWindow.setLayer(parentLayer);
		else if (serverSelectionZone.isFocused() && input.equals("uiUp"))
		{
			movedInList = true;
			currentServer--;
		}
		else if (serverSelectionZone.isFocused() && input.equals("uiDown"))
		{
			movedInList = true;
			currentServer++;
		}
		else
		//else
		//	guiHandler.handleInput(k);
			return super.handleInput(input);
		
		return true;
	}

	// Takes care of connecting to a server
	private void login()
	{
		String ip = ipForm.text;
		int port = 30410;
		
		if (ip.length() == 0)
			return;
		
		Client.getInstance().configDeprecated().setString("last-server", ip);
		Client.getInstance().configDeprecated().save();
		
		if (ip.contains(":"))
		{
			port = Integer.parseInt(ip.split(":")[1]);
			ip = ip.split(":")[0];
		}
		
		//Client.world = null;
		
		gameWindow.setLayer(new ConnectionOverlay(gameWindow, this, ip, port));
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
		protected ServerSelectionZone(Layer layer) {
			super(layer);
		}

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
			public boolean handleClick(MouseButton mouseButton) {
				if(sd != null && sd.infoLoaded)
				{
					ipForm.text = sd.ip + (sd.port == 30410 ? "" : sd.port);
					login();
				}
				
				return true;
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
				out.writeInt("info".getBytes("UTF-8").length+2);
				out.writeUTF("info");
				out.flush();
				
				ping = System.currentTimeMillis() - connectStart;
				String lineRead = "";
				
				while (!lineRead.startsWith("info/done"))
				{
					//Discard first byte, assummed to be packed id
					in.readByte();
					//Discard one more byte, assumed to be packet length
					in.readInt();
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
				out.writeInt("icon-file".getBytes("UTF-8").length+2);
				out.writeUTF("icon-file");
				out.flush();
				//Expect reply immediately
				byte expect = in.readByte();
				logger.info("Expected:"+expect);
				//Read and discard tag, we know what we are expecting
				in.readUTF();
				long fileLength = in.readLong();
				logger.info("fileLength:"+fileLength);
				
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
				//e.printStackTrace();
				description = e.toString();
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
