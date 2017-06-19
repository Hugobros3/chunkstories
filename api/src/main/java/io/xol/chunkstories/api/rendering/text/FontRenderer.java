package io.xol.chunkstories.api.rendering.text;

import io.xol.chunkstories.api.math.vector.sp.Vector4fm;

public interface FontRenderer {
	/** Will try to create the requested font in said size, if not found or fails will return defaultFont */
	public Font getFont(String fontName, float sizeInPX);
	
	/** Returns Arial in 11px */
	public Font defaultFont();
	
	public void drawString(Font font, float x, float y, String whatchars, float scale, int clipX);
	
	public void drawString(Font font, float x, float y, String whatchars, float scale);
	
	public void drawString(Font font, float x, float y, String whatchars, float scale, Vector4fm color);
	
	public void drawStringWithShadow(Font font, float x, float y, String whatchars, float scaleX, float scaleY, Vector4fm color);
	
	public void drawStringWithShadow(Font font, float x, float y, String whatchars, float scaleX, float scaleY, int clipX, Vector4fm color);
	
	//public void drawString(Font font, float x, float y, String whatchars, float scaleX, float scaleY, int format);
	
	public TextMesh newTextMeshObject(Font font, String text);
	
	public interface Font {
		public float size();
		
		public int getWidth(String whatchars);
		
		public int getLineHeight();
		
		public int getLinesHeight(String whatchars, int clipX);
	}
}
