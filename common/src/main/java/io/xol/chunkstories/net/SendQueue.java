package io.xol.chunkstories.net;

import io.xol.chunkstories.api.exceptions.PacketProcessingException;
import io.xol.chunkstories.api.exceptions.net.UnknowPacketException;
import io.xol.chunkstories.api.net.Packet;
import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.api.net.PacketPrepared;
import io.xol.chunkstories.api.net.PacketSender;
import io.xol.chunkstories.api.net.PacketsProcessor;
import io.xol.chunkstories.api.net.packets.PacketDummy;
import io.xol.chunkstories.api.net.packets.PacketText;
import io.xol.chunkstories.api.util.concurrency.Fence;
import io.xol.chunkstories.tools.ChunkStoriesLoggerImplementation;
import io.xol.engine.concurrency.SimpleFence;
import io.xol.engine.concurrency.TrivialFence;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SendQueue extends Thread
{
	private final BlockingQueue<Packet> sendQueue = new LinkedBlockingQueue<Packet>();

	private final PacketsProcessor processor;
	private final DataOutputStream outputStream;

	//Reference to what we are sending stuff to, used in packet creation logic to look for implemented interfaces ( remote server, unlogged in client, logged in client etc )
	private PacketDestinator destinator;

	public SendQueue(PacketDestinator destinator, DataOutputStream out, PacketsProcessorActual processor)
	{
		this.destinator = destinator;
		this.outputStream = out;
		this.processor = processor;

		this.setName("Send queue thread");
	}

	public void setDestinator(PacketDestinator destinator)
	{
		this.destinator = destinator;
	}

	Packet DIE = new PacketText();
	
	class Flush extends Packet {
		SimpleFence fence = new SimpleFence();

		public void send(PacketDestinator destinator, DataOutputStream out) throws IOException {}
		public void process(PacketSender sender, DataInputStream in, PacketsProcessor processor) throws IOException, PacketProcessingException {}
	}
	//Packet FLUSH = new PacketText();

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
			ChunkStoriesLoggerImplementation.getInstance().error("Error : unable to buffer PacketPrepared " + packet);
			e.printStackTrace(ChunkStoriesLoggerImplementation.getInstance().getPrintWriter());
		}
		
		sendQueue.add(packet);
	}
	
	Lock deathLock = new ReentrantLock();
	boolean dead = false;

	public Fence flush()
	{
		deathLock.lock();
		if(dead) {
			deathLock.unlock();
			return new TrivialFence();
		}
		
		Flush flush = new Flush();
		sendQueue.add(flush);
		
		deathLock.unlock();
		return flush.fence;
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

			if (packet == DIE) {
				//Kill request ? accept gracefully our fate
				break;
			}

			if (packet == null)
			{
				System.out.println("ASSERTION FAILED : THE SEND QUEUE CAN'T CONTAIN NULL PACKETS.");
				System.exit(-1);
			}
			else if (packet instanceof Flush)
			{
				try
				{
					outputStream.flush();
					((Flush) packet).fence.signal();
				}
				catch (IOException e)
				{
					//That's basically terminated connection exceptions
					((Flush) packet).fence.signal();
					destinator.disconnect("Broken pipe: Unable to flush: "+e.getMessage());
					break;
				}
			}
			else
				try
				{
					if (!(packet instanceof PacketDummy))
						processor.sendPacketHeader(outputStream, packet);
					packet.send(destinator, outputStream);
				}
				catch (IOException e)
				{
					//We don't care about that, it's the motd thing mostly
					destinator.disconnect("Broken pipe: Unable to send packet: "+e.getMessage());
					break;
				}
				catch (UnknowPacketException e)
				{
					//We care about that
					ChunkStoriesLoggerImplementation.getInstance().error("Error : Unknown packet exception : "+packet.getClass().getName());
					e.printStackTrace(ChunkStoriesLoggerImplementation.getInstance().getPrintWriter());
				}
		}

		deathLock.lock();
		dead = true;
		deathLock.unlock();

		//We do a final round of discarding flush requests, after we made sure to never accept any more.
		Packet packet = null;
		while(true) {
			packet = sendQueue.poll();
			if(packet == null)
				break;
			
			//We signal all the remaining flush fluff
			if (packet instanceof Flush)
				((Flush) packet).fence.signal();
			
			packet = null;
		}
		
		try
		{
			outputStream.close();
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
