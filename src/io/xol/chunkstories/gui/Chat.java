package io.xol.chunkstories.gui;

import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.fonts.TrueTypeFontRenderer;
import io.xol.engine.gui.elements.InputText;
import io.xol.engine.misc.ColorsTools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import org.lwjgl.input.Keyboard;

import io.xol.engine.math.lalgb.Vector4f;
import io.xol.chunkstories.api.entity.Entity;
import io.xol.chunkstories.api.entity.interfaces.EntityCreative;
import io.xol.chunkstories.api.entity.interfaces.EntityFlying;
import io.xol.chunkstories.api.entity.interfaces.EntityRotateable;
import io.xol.chunkstories.api.gui.Overlay;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.entity.Entities;
import io.xol.chunkstories.world.WorldClientRemote;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class Chat
{
	Ingame ingame;
	
	public Chat(Ingame ingame)
	{
		this.ingame = ingame;
	}
	
	int chatHistorySize = 150;
	InputText inputBox = new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS);

	public boolean chatting = false;
	Deque<ChatLine> chat = new ArrayDeque<ChatLine>();
	List<String> sent = new ArrayList<String>();
	int sentMessages = 0;
	int sentHistory = 0;

	private class ChatLine
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
			//TODO clickable text, urls etc
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
		public void drawToScreen(RenderingContext renderingContext, int positionStartX, int positionStartY, int width, int height)
		{

		}

		@Override
		public boolean handleKeypress(int k)
		{
			if (Client.getInstance().getInputsManager().getInputByName("exit").isPressed())
			{
				chatting = false;
				mainScene.changeOverlay(parent);
				return true;
			}
			else if (k == Keyboard.KEY_UP)
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
			else if (k == Keyboard.KEY_DOWN)
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
				else if (inputBox.text.startsWith("/locw"))
				{
					try
					{
						float overcastFactor = Float.parseFloat(inputBox.text.split(" ")[1]);
						Client.world.setWeather(overcastFactor);
					}
					catch (Exception e)
					{

					}
				}
				else if (inputBox.text.startsWith("/locspawn"))
				{
					if (inputBox.text.contains(" "))
					{
						int id = Integer.parseInt(inputBox.text.split(" ")[1]);
						Entity test = Entities.newEntity(Client.world, (short) id);
						Entity player = Client.getInstance().getClientSideController().getControlledEntity();
						test.setLocation(player.getLocation());
						Client.world.addEntity(test);
					}
				}
				else if (inputBox.text.startsWith("/locbutcher"))
				{
					Iterator<Entity> ie = Client.world.getAllLoadedEntities();
					while (ie.hasNext())
					{
						Entity e = ie.next();
						System.out.println("checking " + e);
						if (!e.equals(Client.getInstance().getClientSideController().getControlledEntity()))
						{
							System.out.println("removing");
							ie.remove();
						}
					}
				}
				else if (inputBox.text.startsWith("/locsave"))
				{
					Client.world.saveEverything();
				}
				else if (inputBox.text.startsWith("/locfly"))
				{
					Entity controlledEntity = Client.getInstance().getClientSideController().getControlledEntity();
					if (controlledEntity != null && controlledEntity instanceof EntityFlying)
					{
						boolean state = ((EntityFlying) controlledEntity).getFlyingComponent().isFlying();
						state = !state;
						Client.getInstance().printChat("flying : " + state);
						((EntityFlying) controlledEntity).getFlyingComponent().setFlying(state);
						//return;
					}
				}
				else if (inputBox.text.startsWith("/loccrea"))
				{
					Entity controlledEntity = Client.getInstance().getClientSideController().getControlledEntity();
					if (controlledEntity != null && controlledEntity instanceof EntityCreative)
					{
						boolean state = ((EntityCreative) controlledEntity).getCreativeModeComponent().isCreativeMode();
						state = !state;
						Client.getInstance().printChat("creative : " + state);
						((EntityCreative) controlledEntity).getCreativeModeComponent().setCreativeMode(true);
						//return;
					}
				}
				else if (inputBox.text.startsWith("/kkk"))
				{
					Entity controlledEntity = Client.getInstance().getClientSideController().getControlledEntity();
					if (controlledEntity != null && controlledEntity instanceof EntityRotateable)
					{
						((EntityRotateable) controlledEntity).getEntityRotationComponent().setRotation(180, 0);
					}
				}
				else if (ingame.getWorld() instanceof WorldClientRemote)
					((WorldClientRemote) ingame.getWorld()).getConnection().sendTextMessage("chat/" + inputBox.text);
				else
					insert(ColorsTools.getUniqueColorPrefix(Client.username) + Client.username + "#FFFFFF > " + inputBox.text);

				System.out.println(Client.username + " > " + inputBox.text);

				if (sent.size() == 0 || !sent.get(0).equals(inputBox.text))
				{
					sent.add(0, inputBox.text);
					sentMessages++;
				}

				inputBox.text = "";
				chatting = false;
				sentHistory = 0;
				mainScene.changeOverlay(parent);
			}
			else
			{
				sentHistory = 0;
				inputBox.input(k);
			}
			return true;
		}

		@Override
		public boolean onClick(int posx, int posy, int button)
		{
			return false;
		}

		@Override
		public boolean onScroll(int dy)
		{
			if (dy > 0)
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
		if (scroll < 0 || !chatting)
			scroll = 0;
		if (scroll > chatHistorySize)
			scroll = chatHistorySize;
		int ST = scroll;
		int linesDrew = 0;
		int maxLines = 14;
		Iterator<ChatLine> i = chat.iterator();
		while (linesDrew < maxLines && i.hasNext())
		{
			//if (a >= chatHistorySize - lines)
			ChatLine line = i.next();
			if (ST > 0)
			{
				ST--;
				continue;
			}
			//System.out.println("added" +line.text);
			int actualLines = TrueTypeFont.arial11px.getLinesHeight(line.text, 250);
			linesDrew += actualLines;
			float alpha = (line.time + 10000L - System.currentTimeMillis()) / 1000f;
			if (alpha < 0)
				alpha = 0;
			if (alpha > 1 || chatting)
				alpha = 1;
			TrueTypeFontRenderer.get().drawStringWithShadow(TrueTypeFont.arial11px, 9, (linesDrew - 1) * 26 + 100 + (chatting ? 50 : 0), line.text, 2, 2, 500, new Vector4f(1, 1, 1, alpha));
		}
		inputBox.setPosition(12, 112);
		if (chatting)
			inputBox.drawWithBackGroundTransparent();
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
		if (!chatting)
			inputBox.text = "<Press T to chat> lol no one can ever see dis!!!!ии";
		inputBox.setFocus(true);
	}

	public void insert(String t)
	{
		chat.addFirst(new ChatLine(t));
	}
}
