package io.xol.chunkstories.net;

import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketPrepared;
import io.xol.chunkstories.net.packets.PacketDummy;
import io.xol.chunkstories.net.packets.PacketText;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.net.packets.UnknowPacketException;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SendQueue extends Thread
{
	private final BlockingQueue<Packet> sendQueue = new LinkedBlockingQueue<Packet>();

	private final PacketsProcessor processor;
	private final DataOutputStream out;

	//Reference to what we are sending stuff to, used in packet creation logic to look for implemented interfaces ( remote server, unlogged in client, logged in client etc )
	private PacketDestinator destinator;

	public SendQueue(PacketDestinator destinator, DataOutputStream out, PacketsProcessor processor)
	{
		this.destinator = destinator;
		this.out = out;
		this.processor = processor;

		this.setName("Send queue thread");
	}

	public void setDestinator(PacketDestinator destinator)
	{
		this.destinator = destinator;
	}

	Packet DIE = new PacketText();
	Packet FLUSH = new PacketText();

	/**
	 * Queue a packet for sending, no synchronisation needed
	 */
	public void queue(Packet packet)
	{
		//Prepare packets when queuing them
		try
		{
			if (packet instanceof PacketPrepared)
				((PacketPrepared) packet).prepare(destinator);
		}
		catch (IOException e)
		{
			ChunkStoriesLogger.getInstance().error("Error : unable to buffer PacketPrepared " + packet);
			e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
		}
		
		sendQueue.add(packet);
	}

	public void flush()
	{
		sendQueue.add(FLUSH);
	}

	@Override
	public void run()
	{
		while (true)
		{
			Packet packet = null;

			try
			{
				packet = sendQueue.take();
			}
			catch (InterruptedException e1)
			{
				e1.printStackTrace();
			}

			if (packet == DIE)
				break;

			if (packet == null)
			{
				System.out.println("ASSERTION FAILED : THE SEND QUEUE CAN'T CONTAIN NULL PACKETS.");
				System.exit(-1);
			}
			else if (packet == FLUSH)
			{
				try
				{
					out.flush();
				}
				catch (IOException e)
				{
					//That's basically terminated connection exceptions
					destinator.disconnect("Broken pipe: Unable to flush: "+e.getMessage());
				}
			}
			else
				try
				{
					if (!(packet instanceof PacketDummy))
						processor.sendPacketHeader(out, packet);
					packet.send(destinator, out);
				}
				catch (IOException e)
				{
					//We don't care about that, it's the motd thing mostly
					//ChunkStoriesLogger.getInstance().error("Error : unable to send Packet");
					destinator.disconnect("Broken pipe: Unable to send packet: "+e.getMessage());
					break;
				}
				catch (UnknowPacketException e)
				{
					//We care about that
					ChunkStoriesLogger.getInstance().error("Error : Unknown packet exception");
					e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
				}
		}

		try
		{
			out.close();
		}
		catch (IOException e)
		{
			//Really that's just disconnection
		}
	}

	public void kill()
	{
		sendQueue.add(DIE);

		synchronized (this)
		{
			notifyAll();
		}
	}
}
