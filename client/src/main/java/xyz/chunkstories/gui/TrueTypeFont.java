//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.gui;

import xyz.chunkstories.api.content.Asset;
import xyz.chunkstories.api.gui.Font;
import xyz.chunkstories.api.math.HexTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * A TrueType font implementation originally for Slick, edited for Bobjob's
 * Engine, edited for Chunk Stories engine
 *
 * @original author James Chambers (Jimmy)
 * @original author Jeremy Adams (elias4444)
 * @original author Kevin Glass (kevglass)
 * @original author Peter Korzuszek (genail)
 * @new version edited by David Aaron Muhar (bobjob)
 * @new version edited by Hugo Devillers (gobrosse)
 */
//TODO: rip out this bullshit and just use stb_truetype like every other cool kid
public class TrueTypeFont implements Font {
	/**
	 * Array that holds necessary information about the font characters
	 */

	//public Texture2DGL glTextures[];
	private boolean pageCreated[] = new boolean[256];
	public Glyph glyphs[];

	/**
	 * Boolean flag on whether AntiAliasing is enabled or not
	 */
	private boolean antiAlias;

	/**
	 * Font's size
	 */
	private int fontSize = 0;

	/**
	 * Font's height
	 */
	private int fontHeight = 0;

	/**
	 * Default font texture width
	 */
	public static int textureWidth = 512;

	/**
	 * Default font texture height
	 */
	public static int textureHeight = 512;

	/**
	 * A reference to Java's AWT Font that we create our font texture from
	 */
	private java.awt.Font font;

	/**
	 * The font metrics for our Java AWT font
	 */
	private FontMetrics fontMetrics;

	TrueTypeFont() {
		//glTextures = new Texture2DGL[256];
		glyphs = new Glyph[65536];
	}

	private static final Logger logger = LoggerFactory.getLogger("rendering.fonts");

	public Logger logger() {
		return logger;
	}

	public TrueTypeFont(Asset fontAsset, float sizeInPX, boolean antiAlias) throws FontFormatException, IOException {
		this();
		logger().debug("Loading font:" + fontAsset + "fontSize: " + sizeInPX);
		font = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontAsset.read()).deriveFont(sizeInPX);

		this.fontSize = font.getSize();

		this.antiAlias = antiAlias;

		createPage(0);

		fontHeight -= 1;
		if (fontHeight <= 0)
			fontHeight = 1;
	}

	private BufferedImage getFontImage(char ch) {
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
		if (charwidth <= 0) {
			charwidth = 7;
		}
		int charheight = fontMetrics.getHeight() + 3;
		if (charheight <= 0) {
			charheight = fontSize;
		}

		// if(font.getFontName().contains("LiberationSans-Regular"))
		// System.out.println("Glyph "+ch+" width:" + charwidth + "
		// height:"+(charheight));

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

	public BufferedImage createPage(int offset) {
		// If there are custom chars then I expand the font texture twice

		// In any case this should be done in other way. Texture with size
		// 512x512
		// can maintain only 256 characters with resolution of 32x32. The
		// texture
		// size should be calculated dynamicaly by looking at character sizes.
		try {
			pageCreated[offset] = true;

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

			for (int i = offset * 256; i < offset * 256 + 256; i++) {
				// get 0-255 characters and then custom characters
				char ch = (char) i;

				BufferedImage fontImage = getFontImage(ch);

				Glyph glyph = new Glyph(ch);

				glyph.width = fontImage.getWidth();
				glyph.height = fontImage.getHeight();

				if (positionX + glyph.width + 1 >= textureWidth) {
					positionX = 0;
					positionY += rowHeight;
					rowHeight = 0;
				}

				glyph.x = positionX;
				glyph.y = positionY;

				if (glyph.height > fontHeight) {
					fontHeight = glyph.height;
				}

				if (glyph.height > rowHeight) {
					rowHeight = glyph.height;
				}

				// Draw it here
				g.drawImage(fontImage, positionX, positionY, null);

				positionX += glyph.width + 1;

				glyphs[i] = glyph;

				fontImage = null;
			}

			return imgTemp;

		} catch (Exception e) {
			System.err.println("Failed to create font.");
			e.printStackTrace();
		}
		return null;
	}

	public int getWidth(String whatchars) {
		if(whatchars == null)
			return 0;

		if(whatchars.contains("\n")) {
			int max = 0;
			for(String line : whatchars.split("\n")) {
				int l = getWidth(line);
				if(l > max)
					max = l;
			}
			return max;
		}

		int totalwidth = 0;
		Glyph glyph = null;
		int currentChar = 0;
		for (int i = 0; i < whatchars.length(); i++) {
			currentChar = whatchars.charAt(i);

			glyph = glyphs[currentChar];

			if (glyph != null) {
				if (glyph.width < 3)
					totalwidth += 1;
				totalwidth += glyph.width;
			}
		}
		return totalwidth;
	}

	public int getLinesHeight(String whatchars) {
		return getLinesHeight(whatchars, -1);
	}

	public int getLinesHeight(String whatchars, float clipX) {
		if (whatchars == null)
			return 0;

		boolean clip = clipX != -1;
		int lines = 1;
		int i = 0;
		char charCurrent;
		Glyph glyph;
		int totalwidth = 0;
		while (i < whatchars.length()) {
			charCurrent = whatchars.charAt(i);

			if (!pageCreated[charCurrent / 256])
				createPage(charCurrent / 256);

			glyph = glyphs[charCurrent];

			if (glyph != null) {
				if (charCurrent == '#' && whatchars.length() - i - 1 >= 6 && (whatchars.toCharArray()[i + 1] != '#') && HexTools
						.isHexOnly(whatchars.substring(i + 1, i + 7))) {
					if (!(i > 1 && whatchars.toCharArray()[i - 1] == '#')) {
						i += 7;
						continue;
					}
				} else if (charCurrent == '\n') {
					totalwidth = 0;
					lines++;
				} else {
					if (clip && (totalwidth + (glyph.width)) > clipX) {
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

	public int getHeight() {
		return fontHeight;
	}

	public int getHeight(String HeightString) {
		return fontHeight;
	}

	public int getLineHeight() {
		return fontHeight;
	}

	public static boolean isSupported(String fontname) {
		java.awt.Font[] font = getFonts();
		for (int i = font.length - 1; i >= 0; i--) {
			if (font[i].getName().equalsIgnoreCase(fontname))
				return true;
		}
		return false;
	}

	public static java.awt.Font[] getFonts() {
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
	}

	public static byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
	}

	public void destroy() {

	}

	@Override public float size() {
		return fontSize;
	}
}