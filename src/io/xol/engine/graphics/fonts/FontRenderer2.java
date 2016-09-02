package io.xol.engine.graphics.fonts;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import static org.lwjgl.opengl.GL11.*;

import io.xol.engine.math.lalgb.Vector4f;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.HexTools;
import io.xol.engine.misc.ColorsTools;

public class FontRenderer2
{

	public static int drawTextUsingSpecificFont(float xpos, float ypos, float rot, float size, String text, BitmapFont font)
	{
		return drawTextUsingSpecificFontRVBA(xpos, ypos, rot, size, text, font, 1, 1, 1, 1);
	}

	public static int drawTextUsingSpecificFont(float xpos, float ypos, float rot, float size, String text, BitmapFont font, float alpha)
	{
		return drawTextUsingSpecificFontRVBA(xpos, ypos, rot, size, text, font, alpha, 1, 1, 1);
	}

	public static int drawTextUsingSpecificFontHex(float xpos, float ypos, float rot, float size, String text, BitmapFont font, String hex, float alpha)
	{
		int[] rgb = ColorsTools.hexToRGB(hex);
		return drawTextUsingSpecificFontRVBA(xpos, ypos, rot, size, text, font, alpha, rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f);
	}

	public static int drawTextUsingSpecificFontRVBA(float xpos, float ypos, float rot, float size, String text, BitmapFont font, float alpha, float r, float v, float b)
	{
		glDisable(GL_CULL_FACE);
		glEnable(GL_TEXTURE_2D);
		
		Texture2D fontTexture = TexturesHandler.getTexture("./textures/font/" + font.name + ".png");
		fontTexture.setLinearFiltering(false);
		
		Vector4f color = new Vector4f(r, v, b, alpha);

		float baseX = xpos;
		float baseY = ypos;

		float translateX = 0;
		float translateY = 0;

		float fontsize = size;
		// float fontsizeB = size;
		float ratio = 16f * 256f / font.texSize;// 16f * 12f; // 192
		int i = 0;
		int l = 0;
		int skip = 0;

		if (cutLongText)
		{
			int totalLength = getTextLengthUsingFont(size, text, font);
			String[] words = text.split(" ");
			int wordsToKeep = words.length - 1;
			while (totalLength > lengthCutoff)
			{
				wordsToKeep--;
				if (wordsToKeep <= 0)
				{
					text = "...";
					break;
				}
				else
				{
					text = "";
					for (int j = 0; j < wordsToKeep; j++)
					{
						//System.out.println(j+"w"+words.length+"--"+wordsToKeep);
						text += words[j] + " ";
					}
					text += "...";
					totalLength = getTextLengthUsingFont(size, text, font);
				}
			}
		}

		for (char c : text.toCharArray())
		{
			if (skip > 0)
			{
				skip--;
			}
			else
			{
				if (c >= 256)
					c = '?'; // Support for UTF-8 is coming later.
				float tx = (c % 16) / ratio;
				float ty = (c / 16) / ratio;
				float cellSize = 1 / ratio;
				int charW = (int) (font.fontWidthData[getIntForChar(c)] * (size / 16));
				// float cellSizeW = (float) (charW/16.0) / ratio;
				// color handling
				if (c == '#' && text.length() - i - 1 >= 6 && (text.toCharArray()[i + 1] != '#') && HexTools.isHexOnly(text.substring(i + 1, i + 7)))
				{
					if (!(i > 1 && text.toCharArray()[i - 1] == '#'))
					{

						String colorCode = text.substring(i + 1, i + 7);
						int rgb[] = ColorsTools.hexToRGB(colorCode);
						// System.out.println("colorcode found ! - "+colorCode
						// +" rgb:"+rgb[1]);
						color = new Vector4f(rgb[0] / 255.0f, rgb[1] / 255.0f, rgb[2] / 255.0f, alpha);
						skip = 6;
					}
				}
				else if (c == '\n')
				{
					translateX += -l;
					translateY += -size / 1.5;
					// glTranslated(-l, -size/1.5, 0);
					l = 0;
				}
				else
				{

					float border = size / 16;
					GameWindowOpenGL.getInstance().getRenderingContext().getGuiRenderer().drawBoxWindowsSpace(baseX + (translateX), baseY + (translateY), baseX + (translateX + charW + border), baseY + (translateY + fontsize), tx, ty + cellSize, tx + (charW + border) / 16f / size, ty,
							fontTexture.getId(), false, true, color);
					
					translateX += charW;
					l += charW;
				}
			}

			i++;
		}
		return l;
	}

	public static int getTextLengthUsingFont(float size, String text, BitmapFont font)
	{
		int len = 0;
		int i = 0;
		int skip = 0;
		if (text == null)
			return 0;
		for (char c : text.toCharArray())
		{
			if (skip > 0)
			{
				skip--;
			}
			else
			{
				int charW = (int) (font.fontWidthData[getIntForChar(c)] * (size / 16));
				if (c == '#' && text.length() - i - 1 >= 6 && (text.toCharArray()[i + 1] != '#') && HexTools.isHexOnly(text.substring(i + 1, i + 7)) && !(i > 1 && text.toCharArray()[i - 1] == '#'))
				{
					skip = 6;
				}
				else
					len += charW;
			}
			i++;
		}
		return len;
	}
	
	public static int getTextHeightUsingFont(float size, String text, BitmapFont font)
	{
		//nique
		return -1;
	}

	static int getIntForChar(int c)
	{
		int i = c;
		if (i > 65532)
			i = 65532;
		return i;
	}

	static boolean cutLongText = false;
	static int lengthCutoff = 0;

	public static void setLengthCutoff(boolean on, int width)
	{
		cutLongText = on;
		lengthCutoff = width;
	}
}
