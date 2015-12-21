package io.xol.engine.misc;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import io.xol.engine.misc.HttpRequester;

public class HttpRequestThread extends Thread
{
	HttpRequester requester;
	String info;
	String address;
	String params;

	public HttpRequestThread(HttpRequester requester, String info,
			String address, String params)
	{
		this.requester = requester;
		this.info = info;
		this.address = address;
		this.params = params;
		this.setName("Http Request Thread (" + info + "/" + address + ")");
	}

	public void run()
	{
		String result = HttpRequests.sendPost(address, params);
		if (result == null)
			result = "null";
		requester.handleHttpRequest(info, result);
	}
}