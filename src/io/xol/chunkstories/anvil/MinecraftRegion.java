package io.xol.chunkstories.anvil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.zip.Inflater;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class MinecraftRegion {
	
	/**
	 * Unit test
	 * @param a
	 */
	public static void main(String a[])
	{
		MinecraftRegion r = new MinecraftRegion(new File("world/region/r.0.1.mca"));//"r.0.0.mca.in"));
		MinecraftChunk c = r.getChunk(22, 1);
		System.out.println("------------");
		int block = c.getBlockID(5, 6, 9);
		System.out.println(block);
		r.close();
	}
	
	int[] locations = new int[1024];
	int[] sizes = new int[1024];
	
	RandomAccessFile is;
	
	public MinecraftRegion(File regionFile) {
		try{
			is = new RandomAccessFile(regionFile,"r");
			//First read the 1024 chunks offsets
			//int n = 0;
			for(int i = 0; i < 1024; i++)
			{
				locations[i] += is.read() << 16;
				locations[i] += is.read() << 8;
				locations[i] += is.read();
				
				sizes[i] += is.read();
			}
			//Discard the timestamp bytes, we don't care.
			byte[] osef = new byte[4];
			for(int i = 0; i < 1024; i++)
			{
				is.read(osef);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	int offset(int x, int z)
	{
		return ((x & 31) + (z & 31) * 32);
	}
	
	public MinecraftChunk getChunk(int x, int z)
	{
		int l = offset(x,z);
		if(sizes[l] > 0)
		{
			try{
				//Chunk non-void, load it
				is.seek(locations[l]*4096);
				//Read 4-bytes of data length
				int compressedLength = 0;
				compressedLength += is.read() << 24;
				compressedLength += is.read() << 16;
				compressedLength += is.read() << 8;
				compressedLength += is.read();
				//Read compression mode
				int compression = is.read();
				if(compression != 2)
				{
					System.out.println("Fatal error : compression scheme not Zlib. ("+compression+") at "+is.getFilePointer()+" l = "+l+" s= "+sizes[l]);
					Runtime.getRuntime().exit(1);
				}
				else
				{
					byte[] compressedData = new byte[compressedLength];
					is.read(compressedData);
					
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					
					Inflater inflater = new Inflater();
					inflater.setInput(compressedData);
					
					byte[] buffer = new byte[4096];
					while(!inflater.finished())
					{
						int c = inflater.inflate(buffer);
						baos.write(buffer, 0, c);
					}
					baos.close();
					
					return new MinecraftChunk(x,z, baos.toByteArray());
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		return new MinecraftChunk(x,z);
	}
	
	public void close()
	{
		try {
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
