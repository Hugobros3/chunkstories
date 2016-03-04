package io.xol.engine.net;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class HttpRequests
{

	/*
	 * Small tools for getting http(s) pages
	 */

	public static String sendPost(String address, String params)
	{

		boolean https = false;
		try
		{
			URL url = new URL(address);
			https = address.startsWith("https://");
			if (!https)
			{
				HttpURLConnection htc = (HttpURLConnection) url
						.openConnection();
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
			}
		} catch (Exception e)
		{
			System.out.println("Coudln't perform POST request on " + address
					+ ", (" + e.getMessage() + ") stack trace above :");
			return "request failed : " + e.getMessage() + "";
		}
	}
}
