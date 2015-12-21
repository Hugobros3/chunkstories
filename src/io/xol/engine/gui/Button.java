package io.xol.engine.gui;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import org.lwjgl.input.Mouse;

import io.xol.engine.base.ObjectRenderer;

public class Button
{

	public int posx;
	public int posy;
	int width;
	int height;
	String tex;
	int offset;
	Runnable todo = null;
	boolean enabled = false;

	public Button(int x, int y, int w, int h, int o, String t, Runnable todo)
	{
		posx = x;
		posy = y;
		width = w;
		height = h;
		tex = t;
		offset = o;
		this.todo = todo;
	}

	public void render()
	{
		if (Mouse.getX() > posx && Mouse.getX() < posx + width
				&& Mouse.getY() > posy && Mouse.getY() < posy + height)
			ObjectRenderer.renderTexturedRect(posx + 16, posy + 16, width,
					height, offset, 16, offset + 16, 32, 64, tex);
		else
			ObjectRenderer.renderTexturedRect(posx + 16, posy + 16, width,
					height, offset, 0, offset + 16, 16, 64, tex);
	}

	public boolean isOver()
	{
		return (Mouse.getX() > posx && Mouse.getX() < posx + width
				&& Mouse.getY() > posy && Mouse.getY() < posy + height);
	}

	public void update()
	{
		if (isOver() && Mouse.isButtonDown(0) && !enabled)
		{
			enabled = true;
			todo.run();
		}
		if (!Mouse.isButtonDown(0))
			enabled = false;
	}
}
