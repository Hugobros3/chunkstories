package io.xol.chunkstories.world;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

public class DequePool
{
	Deque<Deque<Integer>> avaibleDeques = new ConcurrentLinkedDeque<Deque<Integer>>();
	//BlockingQueue<Deque<Integer>> avaibleDeques = new LinkedBlockingQueue<Deque<Integer>>();

	public DequePool()
	{
		for(int i = 0; i < 16; i++)
			avaibleDeques.push(new ArrayDeque<Integer>());
	}
	
	public Deque<Integer> grab()
	{
		Deque<Integer> mdr = null;
		mdr = avaibleDeques.pop();
		while (mdr == null)
		{
			mdr = avaibleDeques.pop();
			try
			{
				Thread.sleep(20L);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		return mdr;
	}

	public void back(Deque<Integer> deque)
	{
		deque.clear();
		avaibleDeques.push(deque);
	}

}
