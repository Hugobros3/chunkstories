package io.xol.chunkstories.net;

import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketPrepared;
import io.xol.chunkstories.api.net.PacketSynchPrepared;
import io.xol.chunkstories.api.net.PacketSynch;
import io.xol.chunkstories.net.packets.PacketDummy;
import io.xol.chunkstories.net.packets.PacketText;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.net.packets.UnknowPacketException;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SendQueue extends Thread
{
	BlockingQueue<Packet> sendQueue = new LinkedBlockingQueue<Packet>();
	AtomicBoolean die = new AtomicBoolean(false);

	PacketsProcessor processor;
	DataOutputStream out;

	//Reference to what we are sending stuff to, used in packet creation logic to look for implemented interfaces ( remote server, unlogged in client, logged in client etc )
	PacketDestinator destinator;

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
		if (die.get())
			return;

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

		//Synch packets have to be built when submitted
		/*if (packet instanceof PacketSynch)
		{
			//Get our thread's buffers
			SynchBuffer synchBuffer = SendQueue.synchBuffer.get();
		
			ByteArrayOutputStream baos = synchBuffer.baos;
			DataOutputStream outSynch = synchBuffer.outSynch;
		
			//Reset it
			baos.reset();
			try
			{
				//Send the packet in the buffer
				packet.send(destinator, outSynch);
				outSynch.flush();
		
				//How many bytes were written ?
				int packetSize = baos.size();
		
				//Make a dummy packet out of the stuff we got
				PacketSynchSendable sendablePacket = new PacketSynchSendable(packet.isSentFromClient());
				sendablePacket.data = baos.toByteArray();
				sendablePacket.packetLength = packetSize;
				sendablePacket.packetType = processor.getPacketId(packet);
		
				//Add that one instead of the real one
				sendQueue.add(sendablePacket);
			}
			catch (IOException e)
			{
				ChunkStoriesLogger.getInstance().error("Error : unable to buffer PacketSynch " + packet);
				e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
			}
			catch (UnknowPacketException e)
			{
				//Definitly not supposed to happen
				e.printStackTrace();
			}
		}
		else*/
		sendQueue.add(packet);
	}

	public void flush()
	{
		sendQueue.add(FLUSH);
	}

	@Override
	public void run()
	{
		while (!die.get())
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
				}
				catch (UnknowPacketException e)
				{
					//We care about that
					ChunkStoriesLogger.getInstance().error("Error : Unknown packet exception");
					e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
				}
		}
	}

	public void kill()
	{
		try
		{
			out.close();
		}
		catch (IOException e)
		{
			//Really that's just disconnection
		}
		die.set(true);
		sendQueue.add(DIE);

		synchronized (this)
		{
			notifyAll();
		}
	}
}
