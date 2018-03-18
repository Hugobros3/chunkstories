//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.net.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;



/** 
 * Hilariously ugly helper methods I wrote in 2014 for another, unrelated game and kept using
 * like the filthy degenerate that I am
 */
public class HttpRequests
{
	public static String sendPost(String address, String params)
	{
		try
		{
			URL url = new URL(address);
			if (!address.startsWith("https://"))
			{
				HttpURLConnection htc = (HttpURLConnection) url.openConnection();
				htc.setRequestMethod("POST");
				htc.setRequestProperty("Accept-Charset", "UTF-8");
				htc.setRequestProperty("charset", "UTF-8");
				htc.setDoOutput(true);
				htc.setConnectTimeout(5000);
				htc.setReadTimeout(15000);
				DataOutputStream out = new DataOutputStream(
						htc.getOutputStream());
				out.writeBytes(params);
				out.flush();
				out.close();
				// get response
				BufferedReader in = new BufferedReader(new InputStreamReader(
						htc.getInputStream()));
				StringBuffer rslt = new StringBuffer();
				String line;
				while ((line = in.readLine()) != null)
				{
					rslt.append(line);
				}
				in.close();
				return rslt.toString();
			} else
			{
				HttpsURLConnection htc = (HttpsURLConnection) url
						.openConnection();
				htc.setRequestMethod("POST");
				htc.setRequestProperty("Accept-Charset", "UTF-8");
				htc.setRequestProperty("charset", "UTF-8");
				htc.setDoOutput(true);
				htc.setConnectTimeout(5000);
				htc.setReadTimeout(15000);
				DataOutputStream out = new DataOutputStream(htc.getOutputStream());
				out.writeBytes(params);
				out.flush();
				out.close();
				// get response
				BufferedReader in = new BufferedReader(new InputStreamReader(
						htc.getInputStream()));
				StringBuffer rslt = new StringBuffer();
				String line;
				while ((line = in.readLine()) != null)
				{
					rslt.append(line);
				}
				in.close();
				return rslt.toString();
			}
		} catch (Exception e)
		{
			System.out.println("Coudln't perform POST request on " + address
					+ ", (" + e.getMessage() + ") stack trace above :");
			return "request failed : " + e.getMessage() + "";
		}
	}
}
