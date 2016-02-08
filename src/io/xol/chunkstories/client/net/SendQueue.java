package io.xol.chunkstories.client.net;

import io.xol.chunkstories.net.packets.Packet;

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
	
	DataOutputStream out;

	public SendQueue(DataOutputStream out)
	{
		this.out = out;
		this.setName("Send queue thread");
	}
	
	public void queue(Packet packet)
	{
		if(die.get())
			return;
		//System.out.println("Queued packet "+packet.toString());
		sendQueue.add(packet);
		
		synchronized (this)
		{
			notifyAll();
		}
	}
	
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
					//System.out.println("de-Queued packet "+packet.toString());
					packet.send(out);
					out.flush();
				}
				catch (IOException e)
				{
					e.printStackTrace();
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
			//e.printStackTrace();
		}
		die.set(true);
		synchronized (this)
		{
			notifyAll();
		}
	}
}
