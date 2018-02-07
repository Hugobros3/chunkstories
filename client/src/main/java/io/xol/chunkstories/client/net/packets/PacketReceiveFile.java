package io.xol.chunkstories.client.net.packets;

import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketReceptionContext;
import io.xol.chunkstories.client.net.ClientPacketsProcessorImplementation;
import io.xol.chunkstories.net.packets.PacketSendFile;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class PacketReceiveFile extends PacketSendFile
{
	@Override
	public void process(PacketSender sender, DataInputStream in, PacketReceptionContext processor) throws IOException, PacketProcessingException
	{
		if(!(processor instanceof ClientPacketsProcessorImplementation))
			return;
		ClientPacketsProcessorImplementation cppi = (ClientPacketsProcessorImplementation)processor;
		
		String fileTag = in.readUTF();
		long fileLength = in.readLong();

		if (fileLength > 0)
		{
			if(fileTag.equals(cppi.getConnection().getExpectedFileTag()))
			{
				file = cppi.getConnection().getExpectedFileLocationToSaveAt();
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
				//System.out.println("Working ! ...");
				cppi.getConnection().getCurrentlyDownloadedFileProgress().setStepText("Downloading "+fileTag+", "+(fileLength - remaining)/1024+"/"+fileLength/1024+"kb");
				int actuallyRead = in.read(buffer, 0, (int) toRead);
				fos.write(buffer, 0, (int) actuallyRead);
				remaining -= actuallyRead;
			}
			fos.close();
			
			cppi.getConnection().fileReceived(fileTag);
			System.out.println("Done !");
		}
	}

}
