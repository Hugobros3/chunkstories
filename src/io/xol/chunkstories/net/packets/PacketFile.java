package io.xol.chunkstories.net.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketSender;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketFile extends Packet
{

	public PacketFile(boolean client)
	{
		super(client);
		// TODO Auto-generated constructor stub
	}

	public String fileTag;
	public File file;

	@Override
	public void send(PacketDestinator destinator, DataOutputStream out) throws IOException
	{
		out.writeUTF(fileTag);

		if (file.exists())
		{
			out.writeLong(file.length());
			FileInputStream fis = new FileInputStream(file);
			byte[] buffer = new byte[4096];
			int read;
			while(true)
			{
				read = fis.read(buffer);
				//System.out.println("read"+read);
				if(read > 0)
					out.write(buffer, 0, read);
				else
					break;
			}
			fis.close();
		}
		else
			out.writeLong(0L);
	}

	@Override
	public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException
	{
		String fileTag = in.readUTF();
		long fileLength = in.readLong();

		if (fileLength > 0)
		{
			if(fileTag.equals(processor.getClientToServerConnection().getExpectedFileTag()))
			{
				file = processor.getClientToServerConnection().getExpectedFileLocationToSaveAt();
			}
			else
				throw new IOException("Unexpected file, tag="+fileTag);
			
			System.out.println("Receiving file "+fileTag+" ... ");
			FileOutputStream fos = new FileOutputStream(file);
			long remaining = fileLength;
			byte[] buffer = new byte[4096];
			while (remaining > 0)
			{
				long toRead = Math.min(4096, remaining);
				System.out.println("Working ! ...");
				int actuallyRead = in.read(buffer, 0, (int) toRead);
				fos.write(buffer, 0, (int) actuallyRead);
				remaining -= actuallyRead;
			}
			fos.close();
			
			processor.getClientToServerConnection().fileReceived(fileTag);
			System.out.println("Done !");
		}
	}

}
