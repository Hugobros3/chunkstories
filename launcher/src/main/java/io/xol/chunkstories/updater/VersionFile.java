package io.xol.chunkstories.updater;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

/** Represents the version.txt file used to identify the game */
public class VersionFile {
	final String version;
	final Map<String, String> informations = new HashMap<>();
	
	public VersionFile(String string) {
		string.replace("\r", "");
		if(string.startsWith("version:")) {
			for(String line : string.split("\n")) {
				if(line.contains(": ")) {
					String propertyName = line.substring(0, line.indexOf(": "));
					String propertyValue = line.substring(line.indexOf(": ") + 2);
					informations.put(propertyName, propertyValue);
				}
					
			}
			
			version = informations.get("version");
			
		} else {
			version = string.replace("\n", "");
			System.out.println("fail"+version);
		}
	}
	
	public static VersionFile loadFromFile(File f) {
		String read = "";
		if (f.exists()) {
			try {
				InputStream ips = new FileInputStream(f);
				InputStreamReader ipsr = new InputStreamReader(ips, "UTF-8");
				BufferedReader br = new BufferedReader(ipsr);
				String ligne;
				while ((ligne = br.readLine()) != null) {
					read += ligne + "\n";
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else
			read = "unknown";
		
		return new VersionFile(read);
	}
	
	public static VersionFile loadFromOnline() throws IOException {
		URL url = new URL("https://chunkstories.xyz/api/updater/current/version.txt");
		HttpsURLConnection htc = (HttpsURLConnection) url.openConnection();
		htc.setRequestMethod("GET");
		htc.setRequestProperty("Accept-Charset", "UTF-8");
		htc.setRequestProperty("charset", "UTF-8");
		htc.setDoOutput(true);
		htc.setConnectTimeout(5000);
		htc.setReadTimeout(15000);
		DataOutputStream out = new DataOutputStream(htc.getOutputStream());
		//out.writeBytes(params);
		out.flush();
		out.close();
		//get response
		BufferedReader in = new BufferedReader(new InputStreamReader(htc.getInputStream()));
		StringBuffer rslt = new StringBuffer();
		String line;
		while((line = in.readLine()) != null)
		{
			rslt.append(line);
			rslt.append("\n");
		}
		in.close();
		
		return new VersionFile(rslt.toString());
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof VersionFile) {
			VersionFile ver = (VersionFile)obj;
			if(ver.informations.get("commit") != null) {
				if(!ver.informations.get("commit").equals(this.informations.get("commit")))
					return false;
			}
			
			return ver.version.equals(version);
		}
		return false;
	}
	
	
}
