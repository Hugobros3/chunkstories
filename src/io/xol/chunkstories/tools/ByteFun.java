package io.xol.chunkstories.tools;

public class ByteFun
{

	public static void main(String args[])
	{
		float z = 32.0f;
		int kek = fromFloat(z);

		System.out.println(z + ":" + kek + ":" + ((byte) ((kek) & 0xFF)) + ":"
				+ ((byte) (((kek) >> 8) & 0x00FF)));
		System.out.println(((byte) (((kek) >> 8) & 0x00FF)));
	}

	public static int fromFloat(float fval)
	{
		int fbits = Float.floatToIntBits(fval);
		int sign = fbits >>> 16 & 0x8000; // sign only
		int val = (fbits & 0x7fffffff) + 0x1000; // rounded value

		if (val >= 0x47800000) // might be or become NaN/Inf
		{ // avoid Inf due to rounding
			if ((fbits & 0x7fffffff) >= 0x47800000)
			{ // is or must become NaN/Inf
				if (val < 0x7f800000) // was value but too large
					return sign | 0x7c00; // make it +/-Inf
				return sign | 0x7c00 | // remains +/-Inf or NaN
						(fbits & 0x007fffff) >>> 13; // keep NaN (and Inf) bits
			}
			return sign | 0x7bff; // unrounded not quite Inf
		}
		if (val >= 0x38800000) // remains normalized value
			return sign | val - 0x38000000 >>> 13; // exp - 127 + 15
		if (val < 0x33000000) // too small for subnormal
			return sign; // becomes +/-0
		val = (fbits & 0x7fffffff) >>> 23; // tmp exp for subnormal calc
		return sign | ((fbits & 0x7fffff | 0x800000) // add subnormal bit
				+ (0x800000 >>> val - 102) // round depending on cut off
		>>> 126 - val); // div by 2^(1-(exp-127+15)) and >> 13 | exp=0
	}
	/*
	 * public static void main(String a[]) { short blockData =
	 * block2short(4,(short)13,(short)7);
	 * 
	 * System.out.println("short:"+blockData);
	 * System.out.println("id:"+short2id(blockData));
	 * System.out.println("meta:"+short2meta(blockData));
	 * System.out.println("light:"+short2light(blockData)); }
	 * 
	 * public static int short2id(short s){ int s2 = s+ 256*256/2; return s2 &
	 * 0xFF; }
	 * 
	 * public static int short2meta(short s){ int s2 = s+ 256*256/2; return (s2
	 * / 256)%16; }
	 * 
	 * public static int short2light(short s){ int s2 = s+ 256*256/2; return (s2
	 * / 256)/16%16; }
	 * 
	 * public static short block2short(int blockID, short metaData, short
	 * lightLevel) { int result = blockID+metaData*256+lightLevel*4096-32768;
	 * return (short)result; }
	 * 
	 * public static short block2short(int blockID, int i, int j) { return
	 * block2short(blockID,(short)i,(short)j); }
	 * 
	 * public static short changeLightning(short s, int i) { return
	 * block2short(short2id(s),short2meta(s),i); }
	 */
}
