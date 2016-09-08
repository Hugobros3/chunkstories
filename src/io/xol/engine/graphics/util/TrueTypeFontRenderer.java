package io.xol.engine.graphics.util;

import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.fonts.Glyph;
import io.xol.engine.graphics.fonts.TrueTypeFont;
import io.xol.engine.graphics.geometry.TextMeshObject;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.math.HexTools;
import io.xol.engine.math.lalgb.Vector4f;
import io.xol.engine.misc.ColorsTools;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class TrueTypeFontRenderer
{
	private static TrueTypeFontRenderer trueTypeFontRenderer;
	
	public static TrueTypeFontRenderer get()
	{
		return trueTypeFontRenderer;
	}
	
	private RenderingContext renderingContext;

	public TrueTypeFontRenderer(RenderingContext renderingContext)
	{
		this.renderingContext = renderingContext;
		TrueTypeFontRenderer.trueTypeFontRenderer = this;
	}

	public final static int ALIGN_LEFT = 0, ALIGN_RIGHT = 1, ALIGN_CENTER = 2;

	public void drawString(TrueTypeFont trueTypeFont, float x, float y, String whatchars, float scaleX, int clipX)
	{
		drawString(trueTypeFont, x, y, whatchars, scaleX, scaleX, ALIGN_LEFT, clipX, new Vector4f(1, 1, 1, 1));
	}
	
	public void drawStringIngame(TrueTypeFont trueTypeFont, float x, float y, String whatchars, float scaleX, int clipX, TextMeshObject target)
	{
		drawString(trueTypeFont, x, y, whatchars, scaleX, scaleX, ALIGN_CENTER, clipX, new Vector4f(1, 1, 1, 1), target);
	}

	public void drawString(TrueTypeFont trueTypeFont, float x, float y, String whatchars, float scale, float scaleY)
	{
		drawString(trueTypeFont, x, y, whatchars, scale, scaleY, ALIGN_LEFT, -1, new Vector4f(1, 1, 1, 1));
	}

	public void drawString(TrueTypeFont trueTypeFont, float x, float y, String whatchars, float scaleX, float scaleY, int clipX, Vector4f color)
	{
		drawString(trueTypeFont, x, y, whatchars, scaleX, scaleY, ALIGN_LEFT, clipX, color);
	}

	public void drawStringWithShadow(TrueTypeFont trueTypeFont, float x, float y, String whatchars, float scaleX, float scaleY, Vector4f color)
	{
		drawStringWithShadow(trueTypeFont, x, y, whatchars, scaleX, scaleY, -1, color);
	}

	public void drawStringWithShadow(TrueTypeFont trueTypeFont, float x, float y, String whatchars, float scaleX, float scaleY, int clipX, Vector4f color)
	{
		Vector4f colorDarkened = new Vector4f(color);
		colorDarkened.x *= 0.2f;
		colorDarkened.y *= 0.2f;
		colorDarkened.z *= 0.2f;
		drawString(trueTypeFont, x + 1 * scaleX, y - 1 * scaleY, whatchars, scaleX, scaleY, ALIGN_LEFT, clipX, colorDarkened);
		drawString(trueTypeFont, x, y, whatchars, scaleX, scaleY, ALIGN_LEFT, clipX, color);
	}

	public void drawString(TrueTypeFont trueTypeFont, float x, float y, String whatchars, float scaleX, float scaleY, int format)
	{
		drawString(trueTypeFont, x, y, whatchars, scaleX, scaleY, format, -1, new Vector4f(1, 1, 1, 1));
	}

	private void drawString(TrueTypeFont trueTypeFont, float x, float y, String whatchars, float scaleX, float scaleY, int alignement, int clipX, Vector4f color)
	{
		drawString(trueTypeFont, x, y, whatchars, scaleX, scaleY, alignement, clipX, color, null);
	}
	
	private void drawString(TrueTypeFont trueTypeFont, float x, float y, String whatchars, float scaleX, float scaleY, int alignement, int clipX, Vector4f color, TextMeshObject target)
	{
		boolean clip = clipX != -1;

		Glyph glyph;
		int charCurrent;

		int totalwidth = 0;
		int i = 0;
		float startY = 0;

		Vector4f colorModified = new Vector4f(color);
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

			Texture2D pageTexture = trueTypeFont.glTextures[charCurrent / 256];

			//Generates any required unicode page
			if (pageTexture == null)
				pageTexture = trueTypeFont.createSet(charCurrent / 256);

			glyph = trueTypeFont.glyphs[charCurrent];

			if (glyph != null)
			{
				if (clip && (totalwidth + (glyph.width)) > clipX / scaleX)
				{
					startY -= trueTypeFont.getHeight();
					totalwidth = 0;
					continue;
				}
				//Detects and parses #C0L0R codes
				if (charCurrent == '#' && whatchars.length() - i - 1 >= 6 && (whatchars.toCharArray()[i + 1] != '#') && HexTools.isHexOnly(whatchars.substring(i + 1, i + 7)))
				{
					if (!(i > 1 && whatchars.toCharArray()[i - 1] == '#'))
					{
						String colorCode = whatchars.substring(i + 1, i + 7);
						int rgb[] = ColorsTools.hexToRGB(colorCode);
						colorModified = new Vector4f(rgb[0] / 255.0f * color.x, rgb[1] / 255.0f * color.y, rgb[2] / 255.0f * color.z, color.w);
						i += 6;
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
					if(target == null)
					{
						renderingContext.getGuiRenderer().setState(pageTexture, true, true, colorModified);
						drawQuad(totalwidth * scaleX + x, startY * scaleY + y, (glyph.width) * scaleX, glyph.height * scaleY, ((float)glyph.x) / textureWidth, ((float)(glyph.y + glyph.height)) / textureHeight, ((float)(glyph.x + glyph.width)) / textureWidth, ((float)glyph.y) / textureHeight, pageTexture);
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

	/** Default font texture width */
	private int textureWidth = 512;

	/** Default font texture height */
	private int textureHeight = 512;

	private void drawQuad(float startX, float startY, float width, float height, float srcX, float srcY, float srcX2, float srcY2, Texture2D pageTexture)
	{
		renderingContext.getGuiRenderer().drawBoxWindowsSpace(startX, startY, startX + width, startY + height, srcX, srcY, srcX2, srcY2, pageTexture, false, true, null);
	}
}
