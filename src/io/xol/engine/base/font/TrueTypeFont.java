package io.xol.engine.base.font;

import io.xol.engine.gui.GuiDrawer;

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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.awt.GraphicsEnvironment;

import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector4f;

/**
 * A TrueType font implementation originally for Slick, edited for Bobjob's
 * Engine
 * 
 * @original author James Chambers (Jimmy)
 * @original author Jeremy Adams (elias4444)
 * @original author Kevin Glass (kevglass)
 * @original author Peter Korzuszek (genail)
 * 
 * @new version edited by David Aaron Muhar (bobjob)
 */
public class TrueTypeFont
{
	// public static TrueTypeFont haettenschweiler = new
	// TrueTypeFont(Font.createFont(Font.TRUETYPE_FONT, new
	// FileInputStream("res/font/haettenschweiler.ttf")), false);
	public static TrueTypeFont smallfonts = new TrueTypeFont("res/font/smallfonts.ttf", false, 12F);
	public static TrueTypeFont arial12 = new TrueTypeFont("res/font/arial.ttf", false, 8F);
	public static TrueTypeFont haettenschweiler = new TrueTypeFont("res/font/haettenschweiler.ttf", false, 16F);

	public final static int ALIGN_LEFT = 0, ALIGN_RIGHT = 1, ALIGN_CENTER = 2;
	/** Array that holds necessary information about the font characters */
	
	public int glTexIds[];
	public Glyph glyphs[];

	// private IntObject[] charArray = new IntObject[256];

	/** Map of user defined font characters (Character <-> IntObject) */
	// private HashMap<Character, IntObject> customChars = new
	// HashMap<Character, IntObject>();

	/** Boolean flag on whether AntiAliasing is enabled or not */
	private boolean antiAlias;

	/** Font's size */
	private int fontSize = 0;

	/** Font's height */
	private int fontHeight = 0;

	/** Texture used to cache the font 0-255 characters */
	// private int fontTextureID;

	/** Default font texture width */
	private int textureWidth = 512;

	/** Default font texture height */
	private int textureHeight = 512;

	/** A reference to Java's AWT Font that we create our font texture from */
	private Font font;

	/** The font metrics for our Java AWT font */
	private FontMetrics fontMetrics;

	private int correctL = 9;//, correctR = 8;

	/*
	 * public class IntObject { // Character's width public int width;
	 * 
	 * //* Character's height public int height;
	 * 
	 * //* Character's stored x position public int storedX;
	 * 
	 * //* Character's stored y position public int storedY; }
	 */

	TrueTypeFont()
	{
		glTexIds = new int[256];
		for (int i = 0; i < 256; i++)
			glTexIds[i] = -1;
		glyphs = new Glyph[65536];
	}

	public TrueTypeFont(Font font, boolean antiAlias)
	{
		this();
		this.font = font;
		this.fontSize = font.getSize() + 3;
		this.antiAlias = antiAlias;

		createSet(0);

		fontHeight -= 1;
		if (fontHeight <= 0)
			fontHeight = 1;
	}

	public TrueTypeFont(String string, boolean antiAlias, float sizeF)
	{
		this();
		try
		{
			font = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(string));
			font = font.deriveFont(sizeF);
			System.out.println("Le gros fun bien dur " + font.getSize());
			this.fontSize = font.getSize() + 3;
			this.antiAlias = antiAlias;

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

	/*public void setCorrection(boolean on)
	{
		if (on)
		{
			correctL = 2;
			//correctR = 1;
		}
		else
		{
			correctL = 0;
			//correctR = 0;
		}
	}*/

	private BufferedImage getFontImage(char ch)
	{
		// Create a temporary image to extract the character's size
		BufferedImage tempfontImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = (Graphics2D) tempfontImage.getGraphics();
		if (antiAlias == true)
		{
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		g.setFont(font);
		fontMetrics = g.getFontMetrics();
		int charwidth = fontMetrics.charWidth(ch) + 8;

		if (charwidth <= 0)
		{
			charwidth = 7;
		}
		int charheight = fontMetrics.getHeight() + 3;
		if (charheight <= 0)
		{
			charheight = fontSize;
		}

		// Create another image holding the character we are creating
		BufferedImage fontImage;
		fontImage = new BufferedImage(charwidth, charheight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D gt = (Graphics2D) fontImage.getGraphics();
		if (antiAlias == true)
		{
			gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		gt.setFont(font);

		gt.setColor(Color.WHITE);
		int charx = 3;
		int chary = 1;
		gt.drawString(String.valueOf(ch), (charx), (chary) + fontMetrics.getAscent());

		return fontImage;

	}

	private int createSet(int offset)
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

				glyph.width = fontImage.getWidth() + 1;
				glyph.height = fontImage.getHeight();

				if (positionX + glyph.width >= textureWidth)
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

				positionX += glyph.width;

				glyphs[i] = glyph;
				/*
				 * if (i < 256) { // standard characters charArray[i] =
				 * newIntObject; } else { // custom characters
				 * customChars.put(new Character(ch), newIntObject); }
				 */

				fontImage = null;
			}

			glTexIds[offset] = loadImage(offset, imgTemp);

			return glTexIds[offset];
			// .getTexture(font.toString(), imgTemp);

		}
		catch (Exception e)
		{
			System.err.println("Failed to create font.");
			e.printStackTrace();
		}
		return -1;
	}

	private void drawQuad(float drawX, float drawY, float drawX2, float drawY2, float srcX, float srcY, float srcX2, float srcY2)
	{
		float DrawWidth = drawX2 - drawX;
		float DrawHeight = drawY2 - drawY;
		float TextureSrcX = srcX / textureWidth;
		float TextureSrcY = srcY / textureHeight;
		float SrcWidth = srcX2 - srcX;
		float SrcHeight = srcY2 - srcY;
		float RenderWidth = (SrcWidth / textureWidth);
		float RenderHeight = (SrcHeight / textureHeight);

		/*
		 * glTexCoord2f(TextureSrcX, TextureSrcY); glVertex2f(drawX, drawY);
		 * glTexCoord2f(TextureSrcX, TextureSrcY + RenderHeight);
		 * glVertex2f(drawX, drawY + DrawHeight); glTexCoord2f(TextureSrcX +
		 * RenderWidth, TextureSrcY + RenderHeight); glVertex2f(drawX +
		 * DrawWidth, drawY + DrawHeight); glTexCoord2f(TextureSrcX +
		 * RenderWidth, TextureSrcY); glVertex2f(drawX + DrawWidth, drawY);
		 */

		GuiDrawer.drawBoxWindowsSpace(drawX, drawY, drawX + DrawWidth, drawY + DrawHeight, TextureSrcX, TextureSrcY, TextureSrcX + RenderWidth, TextureSrcY + RenderHeight, -1, false, true, null);

		// GuiDrawer.drawBoxWindowsSpace(0, 0, 512, 512, 0, 0, 1, 1, -1, true,
		// null);

		// System.out.println(drawX);
	}

	public int getWidth(String whatchars)
	{
		int totalwidth = 0;
		Glyph glyph = null;
		int currentChar = 0;
		for (int i = 0; i < whatchars.length(); i++)
		{
			currentChar = whatchars.charAt(i);
			/*
			 * if (currentChar < 256) { intObject = charArray[currentChar]; }
			 * else { intObject = (IntObject) customChars.get(new
			 * Character((char) currentChar)); }
			 */
			glyph = glyphs[currentChar];

			if (glyph != null)
				totalwidth += glyph.width - correctL;
		}
		return totalwidth;
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

	public void drawString(float x, float y, String whatchars, float scaleX, float scaleY)
	{
		drawString(x, y, whatchars, 0, whatchars.length() - 1, scaleX, scaleY, ALIGN_LEFT, new Vector4f(1,1,1,1));
	}

	public void drawString(float x, float y, String whatchars, float scaleX, float scaleY, Vector4f color)
	{
		drawString(x, y, whatchars, 0, whatchars.length() - 1, scaleX, scaleY, ALIGN_LEFT, color);
	}
	
	public void drawStringWithShadow(float x, float y, String whatchars, float scaleX, float scaleY, Vector4f color)
	{
		Vector4f colorDarkened = new Vector4f(color);
		colorDarkened.x*=0.0f;
		colorDarkened.y*=0.0f;
		colorDarkened.z*=0.0f;
		drawString(x+1*scaleX, y-1*scaleY, whatchars, 0, whatchars.length() - 1, scaleX, scaleY, ALIGN_LEFT, colorDarkened);
		drawString(x, y, whatchars, 0, whatchars.length() - 1, scaleX, scaleY, ALIGN_LEFT, color);
	}
	
	public void drawString(float x, float y, String whatchars, float scaleX, float scaleY, int format)
	{
		drawString(x, y, whatchars, 0, whatchars.length() - 1, scaleX, scaleY, format, new Vector4f(1,1,1,1));
	}
	
	public void drawString(float x, float y, String whatchars, int startIndex, int endIndex, float scaleX, float scaleY, int format, Vector4f color)
	{
		Glyph glyph;
		// IntObject intObject = null;
		int charCurrent;

		int totalwidth = 0;
		int i = startIndex, d, c;
		float startY = 0;

		/*switch (format)
		{
		case ALIGN_RIGHT:
		{
			d = -1;
			c = correctR;

			while (i < endIndex)
			{
				if (whatchars.charAt(i) == '\n')
					startY -= fontHeight;
				i++;
			}
			break;
		}
		case ALIGN_CENTER:
		{
			for (int l = startIndex; l <= endIndex; l++)
			{
				charCurrent = whatchars.charAt(l);
				if (charCurrent == '\n')
					break;
				
				glyph = glyphs[charCurrent];
				totalwidth += glyph.width - correctL;
			}
			totalwidth /= -2;
		}
		case ALIGN_LEFT:
		default:
		{
			d = 1;
			c = correctL;
			break;
		}

		}*/
		
		d = 1;
		c = correctL;

		// GuiDrawer.setState(fontTextureID, true, new Vector4f(1, 1, 1, 1));

		// glBindTexture(GL_TEXTURE_2D, fontTextureID);
		// glBegin(GL_QUADS);
		
		while (i >= startIndex && i <= endIndex)
		{
			charCurrent = whatchars.charAt(i);

			/*
			 * if (charCurrent < 256) { intObject = charArray[charCurrent]; }
			 * else { intObject = (IntObject) customChars.get(new
			 * Character((char) charCurrent)); }
			 */

			int pageId = glTexIds[charCurrent / 256];
			if(pageId == -1)
			{
				System.out.println("Uncached unicode page, generating");
				pageId = createSet(charCurrent / 256);
			}
			
			glyph = glyphs[charCurrent];
			
			if (glyph != null)
			{
				if (d < 0)
					totalwidth += (glyph.width - c) * d;
				if (charCurrent == '\n')
				{
					startY -= fontHeight * d;
					totalwidth = 0;
					/*if (format == ALIGN_CENTER)
					{
						for (int l = i + 1; l <= endIndex; l++)
						{
							charCurrent = whatchars.charAt(l);
							if (charCurrent == '\n')
								break;


							glyph = glyphs[charCurrent];

							totalwidth += glyph.width - correctL;
						}
						totalwidth /= -2;
					}
					*/
					// if center get next lines total width/2;
				}
				else
				{
					/*int pageId = glTexIds[charCurrent / 256];
					if(pageId == -1)
					{
						System.out.println("Uncached unicode page, generating");
						pageId = createSet(charCurrent / 256);
					}*/
					GuiDrawer.setState(pageId, true, true, color);
					drawQuad((totalwidth + glyph.width) * scaleX + x, startY * scaleY + y, totalwidth * scaleX + x, (startY + glyph.height) * scaleY + y, glyph.x + glyph.width, glyph.y + glyph.height, glyph.x, glyph.y);
					if (d > 0)
						totalwidth += (glyph.width - c) * d;
				}
				i += d;
			}
		}
		
		// glEnd();
	}

	public static int loadImage(int offset, BufferedImage bufferedImage)
	{
		try
		{
			short width = (short) bufferedImage.getWidth();
			short height = (short) bufferedImage.getHeight();
			// textureLoader.bpp = bufferedImage.getColorModel().hasAlpha() ?
			// (byte)32 : (byte)24;
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

			int internalFormat = GL_RGBA8, format = GL_RGBA;
			// IntBuffer textureId = BufferUtils.createIntBuffer(1);

			int textureId;

			textureId = glGenTextures();
			glBindTexture(GL_TEXTURE_2D, textureId);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

			// glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);

			GLU.gluBuild2DMipmaps(GL_TEXTURE_2D, internalFormat, width, height, format, GL_UNSIGNED_BYTE, byteBuffer);
			return textureId;

		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}

		return -1;
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
		// IntBuffer scratch = BufferUtils.createIntBuffer(1);
		// scratch.put(0, fontTextureID);
		// glBindTexture(GL_TEXTURE_2D, 0);
		// glDeleteTextures(scratch);
	}
}