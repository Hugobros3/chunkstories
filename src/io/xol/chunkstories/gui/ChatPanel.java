package io.xol.chunkstories.gui;

import io.xol.engine.font.BitmapFont;
import io.xol.engine.font.TrueTypeFont;
import io.xol.engine.gui.InputText;
import io.xol.engine.misc.ColorsTools;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.lwjgl.util.vector.Vector4f;

import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.FastConfig;
import io.xol.chunkstories.entity.EntitiesList;
import io.xol.chunkstories.input.KeyBinds;

public class ChatPanel
{
	int chatHistorySize = 150;
	//String[] chatHistory = new String[chatHistorySize];
	InputText inputBox = new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS);
	public boolean chatting = false;

	Deque<ChatLine> chat = new ArrayDeque<ChatLine>();

	class ChatLine
	{
		public ChatLine(String text)
		{
			this.text = text;
			time = System.currentTimeMillis();
		}

		public long time;
		public String text;

		public void clickRelative(int x, int y)
		{
			//TODO clickable text
		}
	}
	
	public class ChatPanelOverlay extends Overlay
	{
		public ChatPanelOverlay(OverlayableScene scene, Overlay parent)
		{
			super(scene, parent);
			openChatbox();
		}

		@Override
		public void drawToScreen(int positionStartX, int positionStartY, int width, int height)
		{
			
		}
		
		public boolean handleKeypress(int k)
		{
			if (KeyBinds.getKeyBind("exit").isPressed())
			{
				chatting = false;
				mainScene.changeOverlay(parent);
				return true;
			}
			else if (k == 28)
			{
				if (inputBox.text.equals("/locclear"))
				{
					//java.util.Arrays.fill(chatHistory, "");
					chat.clear();
				}
				else if (inputBox.text.startsWith("/loctime"))
				{
					try
					{
						int time = Integer.parseInt(inputBox.text.split(" ")[1]);
						Client.world.worldTime = time;
					}
					catch (Exception e)
					{

					}
				}
				else if (inputBox.text.startsWith("/locspawn"))
				{
					if(inputBox.text.contains(" "))
					{
						int id = Integer.parseInt(inputBox.text.split(" ")[1]);
						Entity test = EntitiesList.newEntity(Client.world, (short) id);
						Entity player = Client.controlledEntity;
						test.setLocation(player.getLocation());
						Client.world.addEntity(test);
					}
				}
				else if (inputBox.text.startsWith("/locbutcher"))
				{
					Iterator<Entity> ie = Client.world.getAllLoadedEntities();
					while(ie.hasNext())
					{
						Entity e = ie.next();
						System.out.println("checking "+e);
						if(!e.equals(Client.controlledEntity))
						{	
							System.out.println("removing");
							ie.remove();
						}
					}
				}
				else if (inputBox.text.startsWith("/locsave"))
				{
					Client.world.save();
				}
				else if (Client.connection != null)
					Client.connection.sendTextMessage("chat/" + inputBox.text);
				else
					insert(ColorsTools.getUniqueColorPrefix(Client.username) + Client.username + "#FFFFFF > " + inputBox.text);
				inputBox.text = "";

				chatting = false;
				mainScene.changeOverlay(parent);
			}
			else
				inputBox.input(k);
			return true;
		}

		public boolean onClick(int posx, int posy, int button)
		{
			return false;
		}
		
		public boolean onScroll(int dy)
		{
			if(dy > 0)
				scroll++;
			else
				scroll--;
			return true;
		}
	}
	
	private void openChatbox()
	{
		inputBox.text = "";
		chatting = true;
		scroll = 0;
	}

	int scroll = 0;
	
	public void draw()
	{
		while (chat.size() > chatHistorySize)
			chat.removeLast();
		if(scroll < 0 || !chatting)
			scroll = 0;
		if(scroll > chatHistorySize)
			scroll = chatHistorySize;
		int ST = scroll;
		int linesDrew = 0;
		int maxLines = 14;
		Iterator<ChatLine> i = chat.iterator();
		while (linesDrew < maxLines && i.hasNext())
		{
			//if (a >= chatHistorySize - lines)
			ChatLine line = i.next();
			if(ST > 0)
			{
				ST--;
				continue;
			}
			//System.out.println("added" +line.text);
			int actualLines = TrueTypeFont.arial12.getLinesHeight(line.text, 250);
			linesDrew += actualLines;
			float a = (line.time + 10000L - System.currentTimeMillis()) / 1000f;
			if (a < 0)
				a = 0;
			if (a > 1 || chatting)
				a = 1;
			//FontRenderer2.drawTextUsingSpecificFont(9, (linesDrew + 0 * maxLines - 1) * 24 + 100 + (chatting ? 50 : 0), 0, 32, line.text, BitmapFont.SMALLFONTS, a);
			//TrueTypeFont.arial12.drawString(9, (-linesDrew + 1) * 24 + 100 + (chatting ? 50 : 0), line.text, 2, 2, 500, new Vector4f(1,1,1,a));
			TrueTypeFont.arial12.drawStringWithShadow(9, (linesDrew - 1) * 26 + 100 + (chatting ? 50 : 0), line.text, 2, 2, 500, new Vector4f(1,1,1,a));
		}
		inputBox.setPos(12, 112);
		if (chatting)
			inputBox.drawWithBackGroundTransparent();
	}
	
	/**
	 * Fetches message from the connection
	 */
	public void update()
	{
		String m;
		if (Client.connection != null)
			while ((m = Client.connection.getLastChatMessage()) != null)
				insert(m);
		if (!chatting)
			inputBox.text = "<Press T to chat>";
		inputBox.focus = true;
	}

	public void insert(String t)
	{
		chat.addFirst(new ChatLine(t));
	}
}
