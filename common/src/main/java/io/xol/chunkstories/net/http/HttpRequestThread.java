//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.net.http;

public class HttpRequestThread extends Thread
{
	HttpRequester requester;
	String info;
	String address;
	String params;
	
	boolean done = false;

	public HttpRequestThread(HttpRequester requester, String info,
			String address, String params)
	{
		this.requester = requester;
		this.info = info;
		this.address = address;
		this.params = params;
		this.setName("Http Request Thread (" + info + "/" + address + ")");
		
		this.start();
	}

	@Override
	public void run()
	{
		String result = HttpRequests.sendPost(address, params);
		if (result == null)
			result = "null";
		requester.handleHttpRequest(info, result);
		
		//Tell anyone listening we are done
		done = true;
		synchronized(this)
		{
			notifyAll();
		}
	}

	/**
	 * Wait() until thread is done with the request.
	 */
	public void waitUntilTermination()
	{
		while(!done)
		{
			synchronized(this)
			{
				try
				{
					wait(100L);
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