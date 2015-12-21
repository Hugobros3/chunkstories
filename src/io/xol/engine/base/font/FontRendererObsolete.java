package io.xol.engine.base.font;

import io.xol.engine.base.TexturesHandler;
import io.xol.engine.misc.ColorsTools;

import org.lwjgl.opengl.GL11;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class FontRendererObsolete
{

	/*
	 * public static void drawText(float xpos, float ypos, float rot, float
	 * size, String text) { drawTextUsingSpecificFontOld(xpos, ypos, rot, size,
	 * text, "oldfont"); }
	 */

	/*
	 * public static void drawTextUsingSpecificFontOld(float xpos, float ypos,
	 * float rot, float size, String text, String font) {
	 * TexturesHandler.bindTexture("./res/textures/font/" + font + ".png");
	 * GL11.glColor3f(1f, 1f, 1.0f); GL11.glTexParameteri(GL11.GL_TEXTURE_2D,
	 * GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
	 * GL11.glEnable(GL11.GL_TEXTURE_2D); GL11.glPushMatrix();
	 * GL11.glEnable(GL11.GL_BLEND); GL11.glBlendFunc(GL11.GL_SRC_ALPHA,
	 * GL11.GL_ONE_MINUS_SRC_ALPHA); // draw quad GL11.glTranslated(xpos, ypos,
	 * 0); GL11.glRotatef(rot, 0, 0, 1); float fontsize = size; float fontsizeB
	 * = size;
	 * 
	 * float ratio = 16f*256f/192f;//16f * 12f; // 192 for (char c :
	 * text.toCharArray()) { float tx = ((int) c % 16) / ratio; float ty =
	 * ((int) c / 16) / ratio; float cellSize = 1 / ratio;
	 * GL11.glBegin(GL11.GL_QUADS); GL11.glTexCoord2d(tx + cellSize, ty +
	 * cellSize); GL11.glVertex2f(+fontsize, 0); GL11.glTexCoord2d(tx, ty +
	 * cellSize); GL11.glVertex2f(0, 0); GL11.glTexCoord2d(tx, ty);
	 * GL11.glVertex2f(0, +fontsize); GL11.glTexCoord2d(tx + cellSize, ty);
	 * GL11.glVertex2f(+fontsize, +fontsize); GL11.glEnd();
	 * GL11.glTranslated(fontsizeB, 0, 0);
	 * 
	 * //
	 * System.out.println("c: "+c+" tx:"+tx+"ty:"+ty+" -- "+(c%16)+"/"+(c/16));
	 * } // GL11.glDisable(GL11.GL_BLEND); GL11.glPopMatrix(); }
	 */

	public static int drawTextUsingSpecificFont(float xpos, float ypos,
			float rot, float size, String text, BitmapFont font)
	{
		return drawTextUsingSpecificFontRVBA(xpos, ypos, rot, size, text, font,
				1, 1, 1, 1);
	}

	public static int drawTextUsingSpecificFont(float xpos, float ypos,
			float rot, float size, String text, BitmapFont font, float alpha)
	{
		return drawTextUsingSpecificFontRVBA(xpos, ypos, rot, size, text, font,
				alpha, 1, 1, 1);
	}

	public static int drawTextUsingSpecificFontHex(float xpos, float ypos,
			float rot, float size, String text, BitmapFont font, String hex,
			float alpha)
	{
		int[] rgb = ColorsTools.hexToRGB(hex);
		return drawTextUsingSpecificFontRVBA(xpos, ypos, rot, size, text, font,
				alpha, rgb[0] / 255f, rgb[1] / 255f, rgb[2] / 255f);
	}

	public static int drawTextUsingSpecificFontRVBA(float xpos, float ypos,
			float rot, float size, String text, BitmapFont font, float alpha,
			float r, float v, float b)
	{
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		TexturesHandler
				.bindTexture("./res/textures/font/" + font.name + ".png");
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
				GL11.GL_NEAREST);
		GL11.glColor4f(r, v, b, alpha);
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		// draw quad
		GL11.glTranslated(xpos, ypos, 0);
		GL11.glRotatef(rot, 0, 0, 1);
		float fontsize = size;
		// float fontsizeB = size;
		float ratio = 16f * 256f / font.texSize;// 16f * 12f; // 192
		int i = 0;
		int l = 0;
		int skip = 0;
		for (char c : text.toCharArray())
		{
			if (skip > 0)
			{
				skip--;
			} else
			{
				float tx = ((int) c % 16) / ratio;
				float ty = ((int) c / 16) / ratio;
				float cellSize = 1 / ratio;
				int charW = (int) (font.fontWidthData[getIntForChar(c)] * (size / 16));
				// float cellSizeW = (float) (charW/16.0) / ratio;
				// color handling
				if (c == '#' && text.length() - i - 1 >= 6
						&& (text.toCharArray()[i + 1] != '#')
						&& ColorsTools.isHexOnly(text.substring(i + 1, i + 7)))
				{
					if (!(i > 1 && text.toCharArray()[i - 1] == '#'))
					{

						String colorCode = text.substring(i + 1, i + 7);
						int rgb[] = ColorsTools.hexToRGB(colorCode);
						// System.out.println("colorcode found ! - "+colorCode
						// +" rgb:"+rgb[1]);
						GL11.glColor4f(rgb[0] / 255.0f, rgb[1] / 255.0f,
								rgb[2] / 255.0f, alpha);
						skip = 6;
					}
				} else if (c == '\n')
				{
					GL11.glTranslated(-l, -size / 1.5, 0);
					l = 0;
				} else
				{
					// System.out.println(charW+":"+fontsizeB+":"+cellSizeW);
					GL11.glBegin(GL11.GL_QUADS);
					GL11.glTexCoord2d(tx + cellSize, ty + cellSize);
					GL11.glVertex2f(+fontsize, 0);
					GL11.glTexCoord2d(tx, ty + cellSize);
					GL11.glVertex2f(0, 0);
					GL11.glTexCoord2d(tx, ty);
					GL11.glVertex2f(0, +fontsize);
					GL11.glTexCoord2d(tx + cellSize, ty);
					GL11.glVertex2f(+fontsize, +fontsize);
					GL11.glEnd();
					GL11.glTranslated(charW, 0, 0);
					l += charW;
					// System.out.println("c: "+c+" tx:"+tx+"ty:"+ty+" -- "+(c%16)+"/"+(c/16));
				}
			}

			i++;
		}
		// GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1f, 1f, 1f, 1f);
		GL11.glPopMatrix();

		return l;
	}

	public static int getTextLengthUsingFont(float size, String text,
			BitmapFont font)
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
			} else
			{
				int charW = (int) (font.fontWidthData[getIntForChar(c)] * (size / 16));
				if (c == '#' && text.length() - i - 1 >= 6
						&& (text.toCharArray()[i + 1] != '#')
						&& ColorsTools.isHexOnly(text.substring(i + 1, i + 7))
						&& !(i > 1 && text.toCharArray()[i - 1] == '#'))
				{
					skip = 6;
				} else
					len += charW;
			}
			i++;
		}
		return len;
	}

	static int getIntForChar(int c)
	{
		int i = (int) c;
		if (i > 65532)
			i = 65532;
		return i;
	}

	// 3d

	public static int drawTextUsingSpecificFont3D(float size, String text,
			BitmapFont font, float alpha, float r, float v, float b)
	{
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		TexturesHandler
				.bindTexture("./res/textures/font/" + font.name + ".png");
		GL11.glColor4f(r, v, b, alpha);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER,
				GL11.GL_NEAREST);
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		// draw quad
		float fontsize = size;
		// float fontsizeB = size;
		float ratio = 16f * 256f / font.texSize;// 16f * 12f; // 192
		int i = 0;
		int l = 0;
		int skip = 0;
		for (char c : text.toCharArray())
		{
			if (skip > 0)
			{
				skip--;
			} else
			{
				float tx = ((int) c % 16) / ratio;
				float ty = ((int) c / 16) / ratio;
				float cellSize = 1 / ratio;
				int charW = (int) (font.fontWidthData[getIntForChar(c)] * (size / 16));
				// float cellSizeW = (float) (charW/16.0) / ratio;
				// color handling
				if (c == '#' && text.length() - i - 1 >= 6
						&& (text.toCharArray()[i + 1] != '#')
						&& ColorsTools.isHexOnly(text.substring(i + 1, i + 7)))
				{
					if (!(i > 1 && text.toCharArray()[i - 1] == '#'))
					{

						String colorCode = text.substring(i + 1, i + 7);
						int rgb[] = ColorsTools.hexToRGB(colorCode);
						// System.out.println("colorcode found ! - "+colorCode
						// +" rgb:"+rgb[1]);
						GL11.glColor4f(rgb[0] / 255.0f, rgb[1] / 255.0f,
								rgb[2] / 255.0f, alpha);
						skip = 6;
					}
				} else if (c == '\n')
				{
					GL11.glTranslated(-l, -size / 1.5, 0);
					l = 0;
				} else
				{
					// System.out.println(charW+":"+fontsizeB+":"+cellSizeW);
					GL11.glBegin(GL11.GL_QUADS);
					GL11.glTexCoord2d(tx + cellSize, ty + cellSize);
					GL11.glVertex2f(+fontsize, 0);
					GL11.glTexCoord2d(tx, ty + cellSize);
					GL11.glVertex2f(0, 0);
					GL11.glTexCoord2d(tx, ty);
					GL11.glVertex2f(0, +fontsize);
					GL11.glTexCoord2d(tx + cellSize, ty);
					GL11.glVertex2f(+fontsize, +fontsize);
					GL11.glEnd();
					GL11.glTranslated(charW, 0, l * 0.01f);
					l += charW;
					// System.out.println("c: "+c+" tx:"+tx+"ty:"+ty+" -- "+(c%16)+"/"+(c/16));
				}
			}

			i++;
		}
		// GL11.glDisable(GL11.GL_BLEND);
		GL11.glColor4f(1f, 1f, 1f, 1f);
		GL11.glPopMatrix();

		return l;
	}

}
