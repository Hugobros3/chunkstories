package io.xol.engine.graphics.fonts;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class Glyph
{
	public Glyph(char c)
	{
		this.c = c;
	}
	
	public char c;
	
	public int width;
	public int height;
	
	public int x;
	public int y;
}
