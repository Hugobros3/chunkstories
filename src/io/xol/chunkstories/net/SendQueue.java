package io.xol.chunkstories.net;

import io.xol.chunkstories.api.net.PacketDestinator;
import io.xol.chunkstories.net.packets.Packet;
import io.xol.chunkstories.net.packets.PacketsProcessor;
import io.xol.chunkstories.net.packets.UnknowPacketException;
import io.xol.chunkstories.tools.ChunkStoriesLogger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class SendQueue extends Thread
{
	Queue<Packet> sendQueue = new ConcurrentLinkedQueue<Packet>();
	AtomicBoolean die = new AtomicBoolean(false);
	AtomicBoolean sleepy = new AtomicBoolean(false);
	
	PacketsProcessor processor;
	DataOutputStream out;
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
	
	public void queue(Packet packet)
	{
		if(die.get())
			return;
		//System.out.println("Queued packet "+packet.toString());
		sendQueue.add(packet);
		
		if(sleepy.get())
		{
			synchronized (this)
			{
				notifyAll();
			}
		}
	}
	
	@Override
	public void run()
	{
		while(!die.get())
		{
			Packet packet = null;
			//synchronized(sendQueue)
			{
				if(sendQueue.size() > 0)
					packet = sendQueue.poll();
			}
			if(packet == null)
				try
				{
					synchronized (this)
					{
						//Wait if no more job to do.
						sleepy.set(true);
						wait();
					}
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			else
				try
				{
					processor.sendPacketHeader(out, packet);
					packet.send(destinator, out);
					out.flush();
				}
				catch (IOException e)
				{
					//We don't care about that
				}
				catch (UnknowPacketException e)
				{
					//We care about that
					e.printStackTrace(ChunkStoriesLogger.getInstance().getPrintWriter());
				}
			sleepy.set(false);
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
		synchronized (this)
		{
			notifyAll();
		}
	}
}
