package io.xol.chunkstories.workers;

public class WorkerTest
{
	public static void main(String a[]) throws InterruptedException {
		int nbCores = Runtime.getRuntime().availableProcessors();
		System.out.println(nbCores+" cores detected.");
		
		int nbThreads = nbCores;
		if(a.length >= 1)
		{
			nbThreads = Integer.parseInt(a[0]);
		}
		
		System.out.println("Creating thread pool with "+nbThreads+"threads");
		WorkerThreadPool pool = new WorkerThreadPool(nbThreads);
		
		int nbTask = 50000;
		System.out.println("Filling it with "+nbTask+" dummy tasks");
		for(int i = 0; i < nbTask; i++)
		{
			pool.scheduleTask(new DummyTask());
		}
		
		System.out.println("Working ... ");
		System.out.flush();
		while(pool.size() > 0)
		{
			synchronized(WorkerTest.class) {
				Thread.sleep(500L);
			}
			
			System.out.print(pool.size() + " left. ");
			System.out.flush();
		}
		
		System.out.println();
		System.out.println("Stats: "+pool.toString());
		
		System.out.println("Done.");
		pool.destroy();
	}
}
