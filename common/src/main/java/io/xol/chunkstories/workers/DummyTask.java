package io.xol.chunkstories.workers;

public class DummyTask extends Task
{
	
	long a, b;
	
	public DummyTask() {
		a = (long) (1 + Math.random() * (Long.MAX_VALUE - 256));
		b = (long) (1 + Math.random() * (Long.MAX_VALUE - 256));
	}
	
	@Override
	protected boolean task()
	{
		//int c = pgcd(a, b);
		
		while(a * b > 0)
		{
			if(a == b)
				break;
			
			if(a > b)
				b -= a;
			else
				a -= b;
			
		}
		
		int d = 500;
		while(d > 0)
		{
			d -= Math.random() > 0.5 ? 1 : 0;
		}
		
		return Math.random() > 0.5;
	}

	int pgcd(int a, int b)
	{
		if(a == b)
			return a;
		else if(a == 0)
			return b;
		else if(b == 0)
			return a;
		else if(a > b)
			return pgcd(a % b, b);
		else
			return pgcd(a, b % a);
	}
}
