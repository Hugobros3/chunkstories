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
		synchronized(this)
		{
			isOpen = true;
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
				if(isOpen)
					break;
				
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
