package io.xol.chunkstories.gui;

import io.xol.engine.graphics.fonts.BitmapFont;
import io.xol.engine.gui.elements.InputText;
import io.xol.engine.misc.ColorsTools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

import io.xol.chunkstories.api.gui.Layer;
import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.Mouse.MouseScroll;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.mods.Mod;
import io.xol.chunkstories.api.plugin.ChunkStoriesPlugin;
import io.xol.chunkstories.api.rendering.GameWindow;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.client.RenderingConfig;
import io.xol.chunkstories.renderer.WorldRendererImplementation;
import io.xol.chunkstories.world.WorldClientLocal;
import io.xol.chunkstories.world.WorldClientRemote;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/**
 * KILL ME
 */
public class Chat
{
	private final Ingame ingame;

	int chatHistorySize = 150;
	InputText inputBox;// = new InputText(ingame, 0, 0, 500, 32, BitmapFont.SMALLFONTS);

	public boolean chatting = false;
	Deque<ChatLine> chat = new ArrayDeque<ChatLine>();
	List<String> sent = new ArrayList<String>();
	int sentMessages = 0;
	int sentHistory = 0;

	public Chat(Ingame ingame)
	{
		this.ingame = ingame;
		this.inputBox = new InputText(ingame, 0, 0, 500, 32, BitmapFont.SMALLFONTS);
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

		public void clickRelative(int x, int y)
		{
			//TODO clickable text, urls etc
		}
	}

	public class ChatPanelOverlay extends Layer
	{
		long delay;
		
		public ChatPanelOverlay(GameWindow scene, Layer parent)
		{
			super(scene, parent);
			openChatbox();
			this.elements.add(inputBox);
			this.setFocusedElement(inputBox);
			
			//150ms of delay to avoid typing in by mistake
			this.delay = System.currentTimeMillis() + 150;
		}

		@Override
		public void render(RenderingInterface renderer) {
			parentLayer.render(renderer);
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
				chatting = false;
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
				chatting = false;
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

	private void openChatbox()
	{
		inputBox.text = "";
		chatting = true;
		scroll = 0;
	}

	int scroll = 0;

	public void draw(RenderingInterface renderer)
	{	
		while (chat.size() > chatHistorySize)
			chat.removeLast();
		if (scroll < 0 || !chatting)
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
			if (alpha > 1 || chatting)
				alpha = 1;
			renderer.getFontRenderer().drawStringWithShadow(renderer.getFontRenderer().defaultFont(), 9, (linesDrew - 1) * 26 + 180 + (chatting ? 50 : 0), localizedLine, 2, 2, chatWidth, new Vector4fm(1, 1, 1, alpha));
		}
		inputBox.setPosition(12, 192);
		
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
			inputBox.text = "<Press T to chat> lol no one can ever see dis!!!!��";
		
		//inputBox.setFocus(true);
	}

	public void insert(String t)
	{
		chat.addFirst(new ChatLine(t));
	}
	
	private void processTextInput(String input) {
		if (inputBox.text.startsWith("/"))
		{
			String chatMsg = inputBox.text;

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
				if (sent.size() == 0 || !sent.get(0).equals(inputBox.text))
				{
					sent.add(0, inputBox.text);
					sentMessages++;
				}
				return;
			}
			else if (cmdName.equals("plugins"))
			{
				String list = "";
				int i = 0;
				for (ChunkStoriesPlugin csp : ingame.getPluginManager().loadedPlugins())
				{
					i++;
					list += csp.getName() + (i == ingame.getPluginManager().loadedPlugins().size() ? "" : ", ");
				}
				
				if(Client.getInstance().getWorld() instanceof WorldClientLocal)
					insert("#00FFD0" + i + " active client [master] plugins : " + list);
				else
					insert("#74FFD0" + i + " active client [remote] plugins : " + list);
				
				if (sent.size() == 0 || !sent.get(0).equals(inputBox.text))
				{
					sent.add(0, inputBox.text);
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
				
				if (sent.size() == 0 || !sent.get(0).equals(inputBox.text))
				{
					sent.add(0, inputBox.text);
					sentMessages++;
				}
			}
		}

		if (inputBox.text.equals("/locclear"))
		{
			//java.util.Arrays.fill(chatHistory, "");
			chat.clear();
		}
		else if (inputBox.text.equals("I am Mr Debug"))
		{
			RenderingConfig.isDebugAllowed = true;
		}
		else if(inputBox.text.equals("/reloadLocalContent"))
		{
			//Rebuild the mod FS
			Client.getInstance().reloadAssets();
			//Mark some caches dirty
			((WorldRendererImplementation) Client.getInstance().getWorld().getWorldRenderer()).reloadContentSpecificStuff();
		}
		/*else if (inputBox.text.startsWith("/loctime"))
		{
			try
			{
				int time = Integer.parseInt(inputBox.text.split(" ")[1]);
				Client.world.setTime(time);
			}
			catch (Exception e)
			{

			}
		}
		else if (inputBox.text.startsWith("/locfood"))
		{
			try
			{
				float foodLevel = Float.parseFloat(inputBox.text.split(" ")[1]);
				EntityPlayer player = (EntityPlayer) Client.getInstance().getPlayer().getControlledEntity();
				player.setFoodLevel(foodLevel);
				insert("Food set to " + foodLevel);
			}
			catch (Exception e)
			{

			}
		}
		else if (inputBox.text.startsWith("/locspeed"))
		{
			try
			{
				float flySpeed = Float.parseFloat(inputBox.text.split(" ")[1]);
				EntityPlayer.flySpeed = flySpeed;
				insert("Flying speed set to " + flySpeed);
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
				int count = 1;
				if (inputBox.text.split(" ").length > 2)
					count = Integer.parseInt(inputBox.text.split(" ")[2]);

				for (int ii = 0; ii < count; ii++)
					for (int jj = 0; jj < count; jj++)
					{
						Entity test = Client.world.getGameContext().getContent().entities().getEntityTypeById((short) id).create(Client.world);// Entities.newEntity(Client.world, (short) id);
						Entity player = Client.getInstance().getPlayer().getControlledEntity();
						test.setLocation(new Location(Client.world, player.getLocation().clone().add(ii * 3.0, 0.0, jj * 3.0)));
						Client.world.addEntity(test);
					}
			}
		}
		else if (inputBox.text.startsWith("/locbutcher"))
		{
			Iterator<Entity> ie = Client.world.getAllLoadedEntities();
			while (ie.hasNext())
			{
				Entity e = ie.next();
				System.out.println("checking " + e);
				if (!e.equals(Client.getInstance().getPlayer().getControlledEntity()))
				{
					System.out.println("removing");
					ie.remove();
				}
			}
		}
		else if (inputBox.text.startsWith("/locgive"))
		{

			try
			{
				Entity controlledEntity = Client.getInstance().getPlayer().getControlledEntity();
				String itemName = inputBox.text.split(" ")[1];

				int c = 1;
				if (inputBox.text.split(" ").length >= 3)
					c = Integer.parseInt(inputBox.text.split(" ")[2]);

				ItemPile it = new ItemPile(Client.getInstance().getContent().items().getItemTypeByName(itemName).newItem());
				it.setAmount(c);

				((EntityPlayer) controlledEntity).getInventory().addItemPile(it);
			}
			catch (Throwable npe)
			{

			}
		}
		else if (inputBox.text.startsWith("/locclearinv"))
		{
			Entity controlledEntity = Client.getInstance().getPlayer().getControlledEntity();
			((EntityPlayer) controlledEntity).getInventory().clear();
			System.out.println("CLEARED");
		}
		else if (inputBox.text.startsWith("/locsave"))
		{
			Client.world.saveEverything();
		}
		else if (inputBox.text.startsWith("/locfly"))
		{
			Entity controlledEntity = Client.getInstance().getPlayer().getControlledEntity();
			if (controlledEntity != null && controlledEntity instanceof EntityFlying)
			{
				boolean state = ((EntityFlying) controlledEntity).getFlyingComponent().get();
				state = !state;
				Client.getInstance().printChat("flying : " + state);
				((EntityFlying) controlledEntity).getFlyingComponent().set(state);
				//return;
			}
		}
		else if (inputBox.text.startsWith("/loccrea"))
		{
			Entity controlledEntity = Client.getInstance().getPlayer().getControlledEntity();
			if (controlledEntity != null && controlledEntity instanceof EntityCreative)
			{
				boolean state = ((EntityCreative) controlledEntity).getCreativeModeComponent().get();
				state = !state;
				Client.getInstance().printChat("creative : " + state);
				((EntityCreative) controlledEntity).getCreativeModeComponent().set(true);
				//return;
			}
		}
		else if (inputBox.text.startsWith("/kkk"))
		{
			Entity controlledEntity = Client.getInstance().getPlayer().getControlledEntity();
			if (controlledEntity != null && controlledEntity instanceof EntityRotateable)
			{
				((EntityRotateable) controlledEntity).getEntityRotationComponent().setRotation(180, 0);
			}
		}*/
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
	}
}