package io.xol.chunkstories.gui;

import io.xol.engine.gui.elements.InputText;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import org.joml.Vector4f;
import io.xol.chunkstories.api.mods.Mod;
import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.util.ColorsTools;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.renderer.WorldRendererImplementation;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.chunkstories.world.WorldClientRemote;
import io.xol.chunkstories.world.WorldImplementation;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Chat
{
	private final Ingame ingame;

	private int chatHistorySize = 150;
	
	private Deque<ChatLine> chat = new ArrayDeque<ChatLine>();
	private List<String> sent = new ArrayList<String>();
	
	private int sentMessages = 0;
	private int sentHistory = 0;

	public Chat(Ingame ingame)
	{
		this.ingame = ingame;
	}

	private class ChatLine
	{
		public ChatLine(String text)
		{
			if (text == null)
				text = "";
			this.text = text;
			time = System.currentTimeMillis();
		}

		public long time;
		public String text;

		@SuppressWarnings("unused")
		public void clickRelative(int x, int y)
		{
			//TODO clickable text, urls etc
		}
	}

	public class ChatPanelOverlay extends Layer
	{
		InputText inputBox;
		
		long delay;
		
		public ChatPanelOverlay(GameWindow scene, Layer parent)
		{
			super(scene, parent);

			//Add the inputBox
			this.inputBox = new InputText(this, 0, 0, 500);
			this.elements.add(inputBox);
			this.setFocusedElement(inputBox);
			
			//Reset the scroll
			Chat.this.scroll = 0;
			
			//150ms of delay to avoid typing in by mistake
			this.delay = System.currentTimeMillis() + 30;
		}

		@Override
		public void render(RenderingInterface renderer) {
			parentLayer.render(renderer);

			inputBox.setPosition(12, 192);
			inputBox.drawWithBackGroundTransparent(renderer);
		}

		@Override
		public void onResize(int newWidth, int newHeight) {
			parentLayer.onResize(newWidth, newHeight);
		}

		@Override
		public boolean handleInput(Input input)
		{
			if (input.equals("exit"))
			{
				gameWindow.setLayer(parentLayer);
				return true;
			}
			else if (input.equals("uiUp"))
			{
				//sentHistory = 0 : empty message, = 1 last message, 2 last message before etc
				if (sentMessages > sentHistory)
				{
					sentHistory++;
				}
				if (sentHistory > 0)
					inputBox.text = sent.get(sentHistory - 1);
				else
					inputBox.text = "";
			}
			else if (input.equals("uiDown"))
			{
				//sentHistory = 0 : empty message, = 1 last message, 2 last message before etc
				if (sentHistory > 0)
				{
					sentHistory--;
				}
				if (sentHistory > 0)
					inputBox.text = sent.get(sentHistory - 1);
				else
					inputBox.text = "";
			}
			else if (input.equals("enter"))
			{
				processTextInput(inputBox.text);
				inputBox.text = "";
				sentHistory = 0;
				gameWindow.setLayer(parentLayer);
				return true;
			}
			else if(input instanceof MouseScroll) {
				MouseScroll ms = (MouseScroll)input;
				if (ms.amount() > 0)
					scroll++;
				else
					scroll--;
			}
			/*else
			{
				sentHistory = 0;
				inputBox.input(k);
			}*/
			inputBox.handleInput(input);
			
			return true;
		}
		
		@Override
		public boolean handleTextInput(char c) {
			if(System.currentTimeMillis() <= delay)
				return false;
			else
				return super.handleTextInput(c);
		}
	}

	int scroll = 0;

	public void draw(RenderingInterface renderer)
	{	
		while (chat.size() > chatHistorySize)
			chat.removeLast();
		if (scroll < 0)// || !chatting)
			scroll = 0;
		if (scroll > chatHistorySize)
			scroll = chatHistorySize;
		int scrollLinesSkip = scroll;
		int linesDrew = 0;
		int maxLines = 14;
		Iterator<ChatLine> i = chat.iterator();
		while (linesDrew < maxLines && i.hasNext())
		{
			//if (a >= chatHistorySize - lines)
			ChatLine line = i.next();
			if (scrollLinesSkip > 0)
			{
				scrollLinesSkip--;
				continue;
			}

			int chatWidth = Math.max(750, Client.getInstance().getGameWindow().getWidth() / 2 - 10);

			String localizedLine = Client.getInstance().getContent().localization().localize(line.text);
			
			int actualLines = renderer.getFontRenderer().defaultFont().getLinesHeight(localizedLine, chatWidth / 2);
			linesDrew += actualLines;
			float alpha = (line.time + 10000L - System.currentTimeMillis()) / 1000f;
			if (alpha < 0)
				alpha = 0;
			if (alpha > 1 || ingame.getGameWindow().getLayer() instanceof ChatPanelOverlay)
				alpha = 1;
			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), 9, (linesDrew - 1) * renderer.getFontRenderer().defaultFont().getLineHeight() * 2 + 180 + (50), localizedLine, 2, 2, chatWidth, new Vector4f(1, 1, 1, alpha));
		}
		
	}

	/**
	 * Fetches message from the connection
	 */
	public void update()
	{
		String m;
		//Grabs messages from the remote server connection buffer
		if (ingame.getWorld() instanceof WorldClientRemote)
			while ((m = ((WorldClientRemote) ingame.getWorld()).getConnection().getLastChatMessage()) != null)
				insert(m);
		
		//if (!chatting)
		//	inputBox.text = "<Press T to chat> lol no one can ever see dis!!!!ï¿½ï¿½";
		
		//inputBox.setFocus(true);
	}

	public void insert(String t)
	{
		chat.addFirst(new ChatLine(t));
	}
	
	private void processTextInput(String input) {
		
		String username = ingame.getGameWindow().getClient().username();
		
		if (input.startsWith("/"))
		{
			String chatMsg = input;

			chatMsg = chatMsg.substring(1, chatMsg.length());

			String cmdName = chatMsg.toLowerCase();
			String[] args = {};
			if (chatMsg.contains(" "))
			{
				cmdName = chatMsg.substring(0, chatMsg.indexOf(" "));
				args = chatMsg.substring(chatMsg.indexOf(" ") + 1, chatMsg.length()).split(" ");
			}

			if (ingame.getPluginManager().dispatchCommand(Client.getInstance().getPlayer(), cmdName, args))
			{
				if (sent.size() == 0 || !sent.get(0).equals(input))
				{
					sent.add(0, input);
					sentMessages++;
				}
				return;
			}
			else if (cmdName.equals("plugins"))
			{
				String list = "";
				
				Iterator<ChunkStoriesPlugin> i = ingame.getPluginManager().activePlugins();
				while(i.hasNext()) {
					ChunkStoriesPlugin plugin = i.next();
					list += plugin.getName() + (i.hasNext() ? ", " : "");
				}
				
				if(Client.getInstance().getWorld() instanceof WorldClientLocal)
					insert("#00FFD0" + i + " active client [master] plugins : " + list);
				else
					insert("#74FFD0" + i + " active client [remote] plugins : " + list);
				
				if (sent.size() == 0 || !sent.get(0).equals(input))
				{
					sent.add(0, input);
					sentMessages++;
				}
			}
			else if (cmdName.equals("mods"))
			{
				String list = "";
				int i = 0;
				for (Mod mod : Client.getInstance().getContent().modsManager().getCurrentlyLoadedMods())
				{
					i++;
					list += mod.getModInfo().getName() + (i == Client.getInstance().getContent().modsManager().getCurrentlyLoadedMods().size() ? "" : ", ");
				}
				
				if(Client.getInstance().getWorld() instanceof WorldClientLocal)
					insert("#FF0000" + i + " active client [master] mods : " + list);
				else
					insert("#FF7070" + i + " active client [remote] mods : " + list);
				
				if (sent.size() == 0 || !sent.get(0).equals(input))
				{
					sent.add(0, input);
					sentMessages++;
				}
			}
		}

		if (input.equals("/locclear"))
		{
			chat.clear();
		}
		else if (input.equals("I am Mr Debug"))
		{
			RenderingConfig.isDebugAllowed = true;
		}
		else if (input.equals("_-"))
		{
			Entity e = Client.getInstance().getPlayer().getControlledEntity();
			int cx = ((int)(double)e.getLocation().x())/32;
			int cy = ((int)(double)e.getLocation().y())/32;
			int cz = ((int)(double)e.getLocation().z())/32;
			
			insert("No fuck you"+((WorldImplementation)Client.getInstance().getWorld()).getRegionsHolder().getRegionChunkCoordinates(cx, cy, cz));
		}
		else if(input.equals("/reloadLocalContent"))
		{
			//Rebuild the mod FS
			Client.getInstance().reloadAssets();
			//Mark some caches dirty
			((WorldRendererImplementation) Client.getInstance().getWorld().getWorldRenderer()).reloadContentSpecificStuff();
		}
		else if (ingame.getWorld() instanceof WorldClientRemote)
			((WorldClientRemote) ingame.getWorld()).getConnection().sendTextMessage("chat/" + input);
		else
			insert(ColorsTools.getUniqueColorPrefix(username) + username + "#FFFFFF > " + input);

		System.out.println(username + " > " + input);

		if (sent.size() == 0 || !sent.get(0).equals(input))
		{
			sent.add(0, input);
			sentMessages++;
		}
	}
}
