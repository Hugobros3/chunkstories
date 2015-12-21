package io.xol.engine.misc;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class ColorsTools
{

	static char[] hexTable = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
			'9', 'A', 'B', 'C', 'D', 'E', 'F' };

	public static int hexToInt(String hex)
	{
		int value = 0;
		for (char c : hex.toCharArray())
		{
			value *= 16;
			int index = 0;
			for (char d : hexTable)
			{
				if (c == d)
				{
					value += index;
				}
				index++;
			}
		}
		return value;
	}

	public static int[] hexToRGB(String hex)
	{
		int[] rgb = new int[3];
		char[] bytes = hex.toCharArray();

		if (bytes.length != 6)
			return null;

		rgb[0] = hexToInt(hex.substring(0, 2));
		rgb[1] = hexToInt(hex.substring(2, 4));
		rgb[2] = hexToInt(hex.substring(4, 6));

		return rgb;
	}

	public static String intToHex(int i)
	{
		// System.out.println("wtf "+i);
		if (i < 0)
			return "";
		String hex = "" + hexTable[(i / 16) % 16] + hexTable[i % 16];
		return hex;
	}

	public static String rgbToHex(int i)
	{
		i += 256 * 256 * 256;
		// System.out.println("cc ^^ "+i);
		String returnMeh = "";

		int r = i / (256 * 256);
		int v = ((i) / 256) % 256;
		int b = i % 256;

		returnMeh += intToHex(r);
		returnMeh += intToHex(v);
		returnMeh += intToHex(b);

		return returnMeh;
	}

	public static int[] rgbSplit(int i)
	{
		i += 256 * 256 * 256;
		int[] rgb = new int[3];
		rgb[0] = i / (256 * 256);
		rgb[1] = ((i) / 256) % 256;
		rgb[2] = i % 256;
		return rgb;
	}

	public static boolean isHexOnly(String str)
	{
		boolean yes = true;
		for (char c : str.toCharArray())
		{
			boolean found = false;
			for (char d : hexTable)
			{
				if (c == d)
					found = true;
			}
			if (!found)
				yes = false;
		}
		return yes;
	}

	// HEXADECIMAL TO ANSI CONVERTER
	// Usefull ? Quite not. But it does display in color !

	static int[][] ansiRGB = { { 0, 0, 0 }, { 187, 0, 0 }, { 0, 187, 0 },
			{ 187, 187, 0 }, { 0, 0, 187 }, { 187, 0, 187 }, { 0, 187, 187 },
			{ 187, 187, 187 }, };

	static String[] ansiEscape = { "\u001B[0m", "\u001B[31m", "\u001B[32m",
			"\u001B[33m", "\u001B[34m", "\u001B[35m", "\u001B[36m",
			"\u001B[37m" };

	public static String convertToAnsi(String text)
	{
		boolean doConvert = !System.getProperty("os.name").toLowerCase()
				.contains("windows");
		// As windows don't support ansi codes in terminal we disable it.

		String result = "";
		int i = 0;
		int skip = 0; // skips a few characters when it founds a hex code so it
						// doesn't appear
		for (char c : text.toCharArray())
		{
			if (skip > 0)
			{
				skip--;
			} else
			{
				if (c == '#' && text.length() - i - 1 >= 6
						&& ColorsTools.isHexOnly(text.substring(i + 1, i + 7)))
				{
					String colorCode = text.substring(i + 1, i + 7);
					int rgb[] = ColorsTools.hexToRGB(colorCode);
					if (doConvert)
						result += getNearestAnsiOfRgb(rgb);
					skip = 6;
				} else
					result += c;
			}
			i++;
		}
		if (doConvert)
			result += "\u001B[0m"; // don't forget to reset the input !
		return result;
	}

	private static String getNearestAnsiOfRgb(int[] rgb)
	{
		int distance = 999999999; // <- should do it
		int best = 0;
		for (int i = 0; i < ansiRGB.length; i++)
		{
			int distance2 = Math.abs(ansiRGB[i][0] - rgb[0])
					+ Math.abs(ansiRGB[i][1] - rgb[1])
					+ Math.abs(ansiRGB[i][2] - rgb[2]);
			if (distance2 < distance)
			{
				best = i;
				distance = distance2;
			}
		}
		return ansiEscape[best];
	}
}
