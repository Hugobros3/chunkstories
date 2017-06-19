package io.xol.engine.graphics.fonts;

import java.awt.FontFormatException;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import io.xol.chunkstories.api.math.HexTools;
import io.xol.chunkstories.api.math.vector.sp.Vector4fm;
import io.xol.chunkstories.api.rendering.text.FontRenderer;
import io.xol.chunkstories.api.rendering.text.TextMesh;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.TextMeshObject;
import io.xol.engine.graphics.textures.Texture2DGL;
import io.xol.engine.misc.ColorsTools;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

/** Very messy and shitty way of spitting text */
public class TrueTypeFontRenderer implements FontRenderer
{
	//private static TrueTypeFontRenderer trueTypeFontRenderer;
	
	private final Map<String, TrueTypeFont> loadedFonts = new TreeMap<String, TrueTypeFont>();
	private final RenderingContext renderingContext;

	/** Default font texture width */
	private int textureWidth = 512;

	/** Default font texture height */
	private int textureHeight = 512;
	
	/*public static TrueTypeFontRenderer get()
	{
		return trueTypeFontRenderer;
	}*/

	public TrueTypeFontRenderer(RenderingContext renderingContext)
	{
		this.renderingContext = renderingContext;
		//TrueTypeFontRenderer.trueTypeFontRenderer = this;
	}

	public final static int ALIGN_LEFT = 0, ALIGN_RIGHT = 1, ALIGN_CENTER = 2;

	public void drawString(Font trueTypeFont, float x, float y, String whatchars, float scale, int clipX)
	{
		drawString(trueTypeFont, x, y, whatchars, scale, scale, ALIGN_LEFT, clipX, new Vector4fm(1, 1, 1, 1));
	}
	
	//TODO not public this crap
	public void drawStringIngame(TrueTypeFont trueTypeFont, float x, float y, String whatchars, float scale, int clipX, TextMeshObject target)
	{
		drawString(trueTypeFont, x, y, whatchars, scale, scale, ALIGN_CENTER, clipX, new Vector4fm(1, 1, 1, 1), target);
	}

	public void drawString(Font trueTypeFont, float x, float y, String whatchars, float scale)
	{
		drawString(trueTypeFont, x, y, whatchars, scale, scale, ALIGN_LEFT, -1, new Vector4fm(1, 1, 1, 1));
	}
	
	public void drawString(Font trueTypeFont, float x, float y, String whatchars, float scale, Vector4fm color)
	{
		drawString(trueTypeFont, x, y, whatchars, scale, scale, ALIGN_LEFT, -1, color);
	}

	public void drawString(Font trueTypeFont, float x, float y, String whatchars, float scaleX, float scaleY, int clipX, Vector4fm color)
	{
		drawString(trueTypeFont, x, y, whatchars, scaleX, scaleY, ALIGN_LEFT, clipX, color);
	}

	public void drawStringWithShadow(Font trueTypeFont, float x, float y, String whatchars, float scaleX, float scaleY, Vector4fm color)
	{
		drawStringWithShadow(trueTypeFont, x, y, whatchars, scaleX, scaleY, -1, color);
	}

	public void drawStringWithShadow(Font trueTypeFont, float x, float y, String whatchars, float scaleX, float scaleY, int clipX, Vector4fm color)
	{
		Vector4fm colorDarkened = new Vector4fm(color);
		colorDarkened.setX(colorDarkened.getX() * 0.2f);
		colorDarkened.setY(colorDarkened.getY() * 0.2f);
		colorDarkened.setZ(colorDarkened.getZ() * 0.2f);
		drawString(trueTypeFont, x + 1 * scaleX, y - 1 * scaleY, whatchars, scaleX, scaleY, ALIGN_LEFT, clipX, colorDarkened);
		drawString(trueTypeFont, x, y, whatchars, scaleX, scaleY, ALIGN_LEFT, clipX, color);
	}

	public void drawString(Font trueTypeFont, float x, float y, String whatchars, float scaleX, float scaleY, int format)
	{
		drawString(trueTypeFont, x, y, whatchars, scaleX, scaleY, format, -1, new Vector4fm(1, 1, 1, 1));
	}

	private void drawString(Font trueTypeFont, float x, float y, String whatchars, float scaleX, float scaleY, int alignement, int clipX, Vector4fm color)
	{
		drawString(trueTypeFont, x, y, whatchars, scaleX, scaleY, alignement, clipX, color, null);
	}
	
	private void drawString(Font font, float x, float y, String whatchars, float scaleX, float scaleY, int alignement, int clipX, Vector4fm color, TextMeshObject target)
	{
		TrueTypeFont trueTypeFont = (TrueTypeFont)font;
		
		boolean clip = clipX != -1;

		Glyph glyph;
		int charCurrent;

		int totalwidth = 0;
		int i = 0;
		float startY = 0;

		Vector4fm colorModified = new Vector4fm(color);
		String lines[] = whatchars.split("\n");
		if(lines.length == 0)
			return;
		int lineI = 0;
		int currentLineTotalLength = trueTypeFont.getWidth(lines[lineI]);
		if(alignement == ALIGN_CENTER)
		{
			//System.out.println(clipX + " " + currentLineTotalLength + " -> " + (clipX / scaleX - currentLineTotalLength) / 2);
			if(currentLineTotalLength < clipX / scaleX )
				totalwidth += (clipX / scaleX - currentLineTotalLength) / 2;
		}
		while (i < whatchars.length())
		{
			charCurrent = whatchars.charAt(i);

			Texture2DGL pageTexture = trueTypeFont.glTextures[charCurrent / 256];

			//Generates any required unicode page
			if (pageTexture == null)
				pageTexture = trueTypeFont.createPage(charCurrent / 256);

			glyph = trueTypeFont.glyphs[charCurrent];

			if (glyph != null)
			{
				//Detects and parses #C0L0R codes
				if (charCurrent == '#' && whatchars.length() - i - 1 >= 6 && (whatchars.toCharArray()[i + 1] != '#') && HexTools.isHexOnly(whatchars.substring(i + 1, i + 7)))
				{
					if (!(i > 1 && whatchars.toCharArray()[i - 1] == '#'))
					{
						String colorCode = whatchars.substring(i + 1, i + 7);
						int rgb[] = ColorsTools.hexToRGB(colorCode);
						colorModified = new Vector4fm(rgb[0] / 255.0f * color.getX(), rgb[1] / 255.0f * color.getY(), rgb[2] / 255.0f * color.getZ(), color.getW());
						i += 7;
						continue;
					}
				}
				else if (charCurrent == '\n')
				{
					startY -= trueTypeFont.getHeight();
					totalwidth = 0;
					
					if(lineI < lines.length - 1)
						lineI++;
					currentLineTotalLength = trueTypeFont.getWidth(lines[lineI]);
					if(alignement == ALIGN_CENTER)
					{
						if(currentLineTotalLength < clipX / scaleX )
							totalwidth += (clipX / scaleX - currentLineTotalLength) / 2;
					}
				}
				else
				{
					if (clip && (totalwidth + (glyph.width)) > clipX / scaleX)
					{
						startY -= trueTypeFont.getHeight();
						totalwidth = 0;
						continue;
					}
					
					if(target == null)
					{
						//renderingContext.getGuiRenderer().setState(pageTexture, true, true, colorModified);
						drawQuad(totalwidth * scaleX + x, startY * scaleY + y, (glyph.width) * scaleX, glyph.height * scaleY, ((float)glyph.x) / textureWidth, ((float)(glyph.y + glyph.height)) / textureHeight, ((float)(glyph.x + glyph.width)) / textureWidth, ((float)glyph.y) / textureHeight, pageTexture, colorModified);
					}
					else
					{
						target.setState(pageTexture, colorModified);
						target.drawQuad(totalwidth * scaleX + x, startY * scaleY + y, (glyph.width) * scaleX, glyph.height * scaleY, ((float)glyph.x) / textureWidth, ((float)(glyph.y + glyph.height)) / textureHeight, ((float)(glyph.x + glyph.width)) / textureWidth, ((float)glyph.y) / textureHeight);
					}
					
					if (glyph.width < 3)
						totalwidth += 1;
					totalwidth += glyph.width;
				}
				i++;
			}
		}
		
		if(target != null)
			target.done();
	}

	private void drawQuad(float startX, float startY, float width, float height, float srcX, float srcY, float srcX2, float srcY2, Texture2DGL pageTexture, Vector4fm colorModified)
	{
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(startX, startY, startX + width, startY + height, srcX, srcY, srcX2, srcY2, pageTexture, false, true, colorModified);
	}

	@Override
	public TextMesh newTextMeshObject(Font font, String text) {
		return new TextMeshObject(this, (TrueTypeFont) font, text);
	}

	
	@Override
	public Font getFont(String fontName, float sizeInPX) {
		String combinedName = fontName + ":" + sizeInPX;
		TrueTypeFont font = this.loadedFonts.get(combinedName);
		if(font == null) {
			try {
				font = new TrueTypeFont(this.renderingContext.getClient().getContent().getAsset("./font/"+fontName+".ttf"), sizeInPX);
			}
			catch(IOException | FontFormatException e) {
				return defaultFont();
			}
			this.loadedFonts.put(combinedName, font);
		}
		return font;
	}

	@Override
	public Font defaultFont() {
		return getFont("arial", 11.0F);
	}

	public void reloadFonts() {
		for(TrueTypeFont f : loadedFonts.values())
			f.destroy();
		loadedFonts.clear();
	}
}
