package io.xol.chunkstories.gui;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.engine.base.font.BitmapFont;
import io.xol.engine.base.font.FontRenderer2;
import io.xol.engine.gui.InputText;
import io.xol.chunkstories.client.Client;

public class ChatPanel
{
	int chatHistorySize = 50;
	String[] chatHistory = new String[chatHistorySize];
	InputText inputBox = new InputText(0, 0, 500, 32, BitmapFont.SMALLFONTS);
	public boolean chatting = false;

	public ChatPanel()
	{
		java.util.Arrays.fill(chatHistory, "");
	}

	public void key(int k)
	{
		if (k == 28)
		{
			chatting = false;
			if(inputBox.text.equals("/clear"))
			{
				java.util.Arrays.fill(chatHistory, "");
				return;
			}
			
			
			if(Client.connection != null)
				Client.connection.sendTextMessage("chat/" + inputBox.text);
			else
				insert("#00CC22"+Client.username+"#FFFFFF > "+inputBox.text);
			inputBox.text = "";

		}
		else if (k == 1)
			chatting = false;
		else
			inputBox.input(k);

	}

	public void openChatbox()
	{
		inputBox.text = "";
		chatting = true;
	}
	
	public void update()
	{
		String m;
		if(Client.connection != null)
			while ((m = Client.connection.getLastChatMessage()) != null)
				insert(m);
		if (!chatting)
			inputBox.text = "<Press T to chat>";
		// inputBox.drawWithBackGroundTransparent(12,25, 32,
		// BitmapFont.SMALLFONTS, XolioWindow.frameW/3*2);
		inputBox.focus = true;
	}

	public void draw(int lines)
	{
		int a = 0;
		for (String text : chatHistory)
		{
			a++;
			if (a >= chatHistorySize - lines)
				FontRenderer2.drawTextUsingSpecificFont(9, (-a + chatHistorySize + 3) * 24, 0, 32, text, BitmapFont.SMALLFONTS, 0.75f);
		}
		inputBox.setPos(12, 25);
		inputBox.drawWithBackGroundTransparent();

	}

	public void insert(String t)
	{
		for (int i = 0; i < chatHistorySize - 1; i++)
		{
			chatHistory[i] = chatHistory[i + 1];
		}
		chatHistory[chatHistorySize - 1] = t;
	}
}
