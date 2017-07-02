package io.xol.engine.concurrency;

import io.xol.chunkstories.api.util.concurrency.Fence;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SimpleFence implements Fence
{
	boolean isOpen = false;
	
	public void signal()
	{
		isOpen = true;
		synchronized(this)
		{
			notifyAll();
		}
	}

	@Override
	public void traverse()
	{
		while(true)
		{
			if(isOpen)
				break;
			
			synchronized(this)
			{
				try
				{
					wait();
				}
				catch (InterruptedException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
