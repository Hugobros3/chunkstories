package io.xol.engine.graphics.fonts;

import io.xol.chunkstories.tools.ChunkStoriesLogger;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.textures.TextureType;
import io.xol.engine.graphics.util.GuiRenderer;
import io.xol.engine.math.HexTools;
import io.xol.engine.misc.ColorsTools;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.imageio.ImageIO;

import java.awt.GraphicsEnvironment;

import io.xol.engine.math.lalgb.Vector4f;

/**
 * A TrueType font implementation originally for Slick, edited for Bobjob's Engine, edited for Chunk Stories engine
 * 
 * @original author James Chambers (Jimmy)
 * @original author Jeremy Adams (elias4444)
 * @original author Kevin Glass (kevglass)
 * @original author Peter Korzuszek (genail)
 * @new version edited by David Aaron Muhar (bobjob)
 */
public class TrueTypeFont
{
	//public static TrueTypeFont smallfonts = new TrueTypeFont("res/font/smallfonts.ttf", 12F);
	public static TrueTypeFont arial11px = new TrueTypeFont("res/font/arial.ttf", 8F);
	public static TrueTypeFont haettenschweiler = new TrueTypeFont("res/font/haettenschweiler.ttf", 16f);

	public final static int ALIGN_LEFT = 0, ALIGN_RIGHT = 1, ALIGN_CENTER = 2;
	/** Array that holds necessary information about the font characters */

	public Texture2D glTextures[];
	public Glyph glyphs[];

	/** Boolean flag on whether AntiAliasing is enabled or not */
	private boolean antiAlias;

	/** Font's size */
	private int fontSize = 0;

	/** Font's height */
	private int fontHeight = 0;

	/** Default font texture width */
	private int textureWidth = 512;

	/** Default font texture height */
	private int textureHeight = 512;

	/** A reference to Java's AWT Font that we create our font texture from */
	private Font font;

	/** The font metrics for our Java AWT font */
	private FontMetrics fontMetrics;

	TrueTypeFont()
	{
		glTextures = new Texture2D[256];
		glyphs = new Glyph[65536];
	}

	public TrueTypeFont(String string, float sizeF)
	{
		this();
		try
		{
			ChunkStoriesLogger.getInstance().info("Loading font " + string);
			font = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(string)).deriveFont(sizeF);
			this.fontSize = font.getSize();
			System.out.println(font.getFontName() + "fontSize: " + fontSize);

			this.antiAlias = false;

			createSet(0);

			fontHeight -= 1;
			if (fontHeight <= 0)
				fontHeight = 1;
		}
		catch (FontFormatException | IOException e)
		{
			e.printStackTrace();
		}
	}

	private BufferedImage getFontImage(char ch)
	{
		// Create a temporary image to extract the character's size
		BufferedImage tempfontImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D tempFontGraphics = (Graphics2D) tempfontImage.getGraphics();

		if (antiAlias == true)
			tempFontGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		else
			tempFontGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		tempFontGraphics.setFont(font);
		fontMetrics = tempFontGraphics.getFontMetrics();
		int charwidth = fontMetrics.charWidth(ch);
		if (charwidth <= 0)
		{
			charwidth = 7;
		}
		int charheight = fontMetrics.getHeight() + 3;
		if (charheight <= 0)
		{
			charheight = fontSize;
		}

		//if(font.getFontName().contains("Arial"))
		//	System.out.println("Glyph "+ch+" width:" + charwidth + " height:"+(charheight));

		// Create another image holding the character we are creating
		BufferedImage fontImage;
		fontImage = new BufferedImage(charwidth, charheight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gt = (Graphics2D) fontImage.getGraphics();
		if (antiAlias == true)
			gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		else
			gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		gt.setFont(font);
		gt.setColor(Color.WHITE);
		int charx = 0;
		int chary = 1;
		gt.drawString(String.valueOf(ch), (charx), (chary) + fontMetrics.getAscent());

		return fontImage;

	}

	public Texture2D createSet(int offset)
	{
		// If there are custom chars then I expand the font texture twice

		/*
		 * if (customCharsArray != null && customCharsArray.length > 0) {
		 * textureWidth *= 2; }
		 */

		// In any case this should be done in other way. Texture with size
		// 512x512
		// can maintain only 256 characters with resolution of 32x32. The
		// texture
		// size should be calculated dynamicaly by looking at character sizes.
		try
		{
			BufferedImage imgTemp = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = (Graphics2D) imgTemp.getGraphics();

			// WHY Y U DO DIS
			g.setColor(new Color(0, 0, 0, 0));
			g.fillRect(0, 0, textureWidth, textureHeight);

			int rowHeight = 0;
			int positionX = 0;
			int positionY = 0;

			// int customCharsLength = (customCharsArray != null) ?
			// customCharsArray.length : 0;

			for (int i = offset * 256; i < offset * 256 + 256; i++)
			{
				// get 0-255 characters and then custom characters
				char ch = (char) i;

				BufferedImage fontImage = getFontImage(ch);

				Glyph glyph = new Glyph(ch);

				glyph.width = fontImage.getWidth();
				glyph.height = fontImage.getHeight();

				if (positionX + glyph.width + 1 >= textureWidth)
				{
					positionX = 0;
					positionY += rowHeight;
					rowHeight = 0;
				}

				glyph.x = positionX;
				glyph.y = positionY;

				if (glyph.height > fontHeight)
				{
					fontHeight = glyph.height;
				}

				if (glyph.height > rowHeight)
				{
					rowHeight = glyph.height;
				}

				// Draw it here
				g.drawImage(fontImage, positionX, positionY, null);

				positionX += glyph.width + 1;

				glyphs[i] = glyph;

				fontImage = null;
			}

			glTextures[offset] = loadImageIntoOpenGLTexture(offset, imgTemp);
			/*File outputfile = new File(font.getFontName() + "saved.png");
			ImageIO.write(imgTemp, "png", outputfile);*/

			return glTextures[offset];

		}
		catch (Exception e)
		{
			System.err.println("Failed to create font.");
			e.printStackTrace();
		}
		return null;
	}

	public int getWidth(String whatchars)
	{
		int totalwidth = 0;
		Glyph glyph = null;
		int currentChar = 0;
		for (int i = 0; i < whatchars.length(); i++)
		{
			currentChar = whatchars.charAt(i);

			glyph = glyphs[currentChar];

			if (glyph != null)
			{
				if (glyph.width < 3)
					totalwidth += 1;
				totalwidth += glyph.width;
			}
		}
		return totalwidth;
	}

	public int getLinesHeight(String whatchars)
	{
		return getLinesHeight(whatchars, -1);
	}

	public int getLinesHeight(String whatchars, int clipX)
	{
		boolean clip = clipX != -1;
		int lines = 1;
		int i = 1;
		char charCurrent;
		Glyph glyph;
		int totalwidth = 0;
		while (i < whatchars.length())
		{
			charCurrent = whatchars.charAt(i);

			Texture2D pageTexture = glTextures[charCurrent / 256];
			if (pageTexture == null)
				pageTexture = createSet(charCurrent / 256);

			glyph = glyphs[charCurrent];

			if (glyph != null)
			{
				if (charCurrent == '#' && whatchars.length() - i - 1 >= 6 && (whatchars.toCharArray()[i + 1] != '#') && HexTools.isHexOnly(whatchars.substring(i + 1, i + 7)))
				{
					if (!(i > 1 && whatchars.toCharArray()[i - 1] == '#'))
					{
						i += 6;
					}
				}
				else if (charCurrent == '\n')
				{
					totalwidth = 0;
					lines++;
				}
				else
				{
					if (clip && (totalwidth + (glyph.width)) > clipX)
					{
						lines++;
						totalwidth = 0;
						continue;
					}

					if (glyph.width < 3)
						totalwidth += 1;
					totalwidth += (glyph.width);
				}
				i++;
			}
		}
		return lines;
	}

	public int getHeight()
	{
		return fontHeight;
	}

	public int getHeight(String HeightString)
	{
		return fontHeight;
	}

	public int getLineHeight()
	{
		return fontHeight;
	}

	public static Texture2D loadImageIntoOpenGLTexture(int offset, BufferedImage bufferedImage)
	{
		try
		{
			short width = (short) bufferedImage.getWidth();
			short height = (short) bufferedImage.getHeight();

			int bpp = (byte) bufferedImage.getColorModel().getPixelSize();
			ByteBuffer byteBuffer;
			DataBuffer db = bufferedImage.getData().getDataBuffer();
			if (db instanceof DataBufferInt)
			{
				int intI[] = ((DataBufferInt) (bufferedImage.getData().getDataBuffer())).getData();
				byte newI[] = new byte[intI.length * 4];
				for (int i = 0; i < intI.length; i++)
				{
					byte b[] = intToByteArray(intI[i]);
					int newIndex = i * 4;

					newI[newIndex] = b[1];
					newI[newIndex + 1] = b[2];
					newI[newIndex + 2] = b[3];
					newI[newIndex + 3] = b[0];
				}

				byteBuffer = ByteBuffer.allocateDirect(width * height * (bpp / 8)).order(ByteOrder.nativeOrder()).put(newI);
			}
			else
			{
				byteBuffer = ByteBuffer.allocateDirect(width * height * (bpp / 8)).order(ByteOrder.nativeOrder()).put(((DataBufferByte) (bufferedImage.getData().getDataBuffer())).getData());
			}
			byteBuffer.flip();

			Texture2D texture = new Texture2D(TextureType.RGBA_8BPP);

			texture.uploadTextureData(width, height, byteBuffer);
			texture.setLinearFiltering(false);
			texture.setTextureWrapping(false);

			return texture;

		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}

		return null;
	}

	public static boolean isSupported(String fontname)
	{
		Font font[] = getFonts();
		for (int i = font.length - 1; i >= 0; i--)
		{
			if (font[i].getName().equalsIgnoreCase(fontname))
				return true;
		}
		return false;
	}

	public static Font[] getFonts()
	{
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
	}

	public static byte[] intToByteArray(int value)
	{
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
	}

	public void destroy()
	{

	}
}