//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.graphics.fonts;



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
