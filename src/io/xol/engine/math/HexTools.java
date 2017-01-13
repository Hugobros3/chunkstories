package io.xol.engine.math;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class HexTools
{
	public static int parseHexValue(String text)
	{
		//Get rid of the x
		if(text.contains("x"))
			text = text.substring(text.indexOf('x')+1, text.length());
		return hexToInt(text);
	}
	
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
	
	public static String byteArrayAsHexString(byte[] array)
	{
		String s = "";
		for(byte b : array)
		{
			s += hexTable[(b >> 4) & 0xF];
			s += hexTable[(b >> 0) & 0xF];
		}
		return s.toLowerCase();
	}
	
	public static String intToHex(int i)
	{
		// System.out.println("wtf "+i);
		if (i < 0)
			return "";
		String hex = "" + hexTable[(i / 16) % 16] + hexTable[i % 16];
		return hex;
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
}
