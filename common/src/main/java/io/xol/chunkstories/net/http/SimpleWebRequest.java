//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.net.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Semaphore;

import javax.net.ssl.HttpsURLConnection;

/** Does a GET request and wraps the result into a boilerplate-less fashion */
public class SimpleWebRequest extends Thread {
	
	final String url;
	final Semaphore doneFence = new Semaphore(0);
	String result = null;
	
	final RequestResultAction postRequestAction;

	public SimpleWebRequest(String url) {
		this(url, null);
	}
	
	public SimpleWebRequest(String url, RequestResultAction postRequestAction) {
		this.url = url;
		this.postRequestAction = postRequestAction;
		this.start();
	}
	
	public void run() {
		try {
			String line;
			URL url = new URL(this.url);
			
			if (!this.url.startsWith("https://")) {
				HttpURLConnection htc = (HttpURLConnection) url.openConnection();
				htc.setRequestMethod("GET");
				htc.setRequestProperty("Accept-Charset", "UTF-8");
				htc.setRequestProperty("charset", "UTF-8");
				htc.setConnectTimeout(5000);
				htc.setReadTimeout(2000);
				
				BufferedReader in = new BufferedReader(new InputStreamReader(htc.getInputStream()));
				StringBuffer buffer = new StringBuffer();
				while ((line = in.readLine()) != null) {
					buffer.append(line);
				}
				in.close();
				result = buffer.toString();
			} else {
				HttpsURLConnection htc = (HttpsURLConnection) url.openConnection();
				htc.setRequestMethod("GET");
				htc.setRequestProperty("Accept-Charset", "UTF-8");
				htc.setRequestProperty("charset", "UTF-8");
				htc.setConnectTimeout(5000);
				htc.setReadTimeout(2000);
				
				BufferedReader in = new BufferedReader(new InputStreamReader(htc.getInputStream()));
				StringBuffer buffer = new StringBuffer();
				while ((line = in.readLine()) != null) {
					buffer.append(line);
				}
				in.close();
				result = buffer.toString();
				if(postRequestAction != null)
					postRequestAction.action(result);
			}
		} catch (IOException e) {
			
		} finally {
			doneFence.release();
		}
	}
	
	public String result() {
		doneFence.acquireUninterruptibly();
		return result;
	}
}
