//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.tools;

/**
 * Testing my 1-2 byte packets Ids thing
 * @author gobrosse
 *
 */
public class TestVariableWidthPackets
{

	public static void main(String[] args)
	{
		parsePackets(new byte[]{2, 0});
		parsePackets(new byte[]{(byte) 0x81, (byte) 0x00});
		
		//Test creation
		parsePackets(writePackets((short)0));
		parsePackets(writePackets((short)1));

		parsePackets(writePackets((short)127));
		parsePackets(writePackets((short)128));

		parsePackets(writePackets((short)255));
		parsePackets(writePackets((short)256));
		parsePackets(writePackets((short)1024));
	}

	private static byte[] writePackets(short i)
	{
		byte first = 0x00;
		byte second = 0x00;
		if(i < 127)
			first = (byte)i;
		else
		{
			first = (byte)(0x80 | i >> 8);
			second = (byte)(i % 256);
		}
		return new byte[]{first, second};
	}

	private static void parsePackets(byte[] bs)
	{
		int i = 0;
		int firstByte = bs[i];
		i++;
		int packetType = 0;
		//If it is under 127 unsigned it's a 1-byte packet [0.firstByte(1.7)]
		if ((firstByte & 0x80) == 0)
			packetType = firstByte;
		else
		{
			//It's a 2-byte packet [0.firstByte(1.7)][secondByte(0.8)]
			int secondByte = bs[i];
			secondByte = secondByte & 0xFF;
			packetType = secondByte | (firstByte & 0x7F) << 8;
		}
		System.out.println("packetType:"+packetType);
	}

}
