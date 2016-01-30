package io.xol.engine.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ConfigFile
{

	String path;
	Map<String, String> props = new HashMap<String, String>();

	public ConfigFile(String p)
	{
		path = p;
		load();
	}

	void check4Folder(String f)
	{
		File file = new File(f);
		File folder = null;
		if (!file.isDirectory())
			folder = file.getParentFile();
		if (folder != null && !folder.exists())
			folder.mkdirs();
		if (!file.exists())
			try
			{
				System.out.println("creating file "+file);
				file.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
	}

	public void load()
	{
		check4Folder(path);
		props.clear();
		try
		{
			File file = new File(System.getProperty("user.dir") + "/" + path);
			if(!file.exists())
				return;
			InputStream ips = new FileInputStream(file);
			InputStreamReader ipsr = new InputStreamReader(ips, "UTF-8");
			BufferedReader br = new BufferedReader(ipsr);
			String ligne;
			while ((ligne = br.readLine()) != null)
			{
				if (ligne.contains("=") && !ligne.endsWith("="))
					props.put(ligne.split("=")[0], ligne.split("=")[1]);
			}
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		// /*Logger.getLogger("misc").info*/System.out.println("Config file " +
		// path + " loaded OK. (" + props.size()+ ") elements inside it.");
		/*
		 * if(path.contains("null")) { int lol = 0/0; }
		 */
	}

	public void save()
	{
		check4Folder(path);
		try
		{
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(System.getProperty("user.dir") + "/" + path), "UTF-8"));
			for (String key : props.keySet())
			{
				out.write(key + "=" + props.get(key) + "\n");
			}
			out.close();
		}
		catch (FileNotFoundException e)
		{
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		// /*Logger.getLogger("misc").info*/System.out.println("Config file " +
		// path + " saved OK.");
	}

	public int getIntProp(String s, String a)
	{
		return Integer.parseInt(getProp(s, a));
	}

	public String getProp(String s, String a)
	{
		if (props.containsKey(s))
			return props.get(s);
		else
			props.put(s, a);
		// System.out.println("getting prop"+s+"size:"+this.props.size());
		return a;
	}

	public String getProp(String s)
	{
		return getProp(s, "");
	}

	public void setProp(String p, String d)
	{
		if (props.containsKey(p))
			props.remove(p);
		props.put(p, d);
	}

	public void setProp(String p, int i)
	{
		setProp(p, i + "");
	}

	public int getIntProp(String s, int intProp)
	{
		return Integer.parseInt(getProp(s, intProp + ""));
	}

	public boolean getBooleanProp(String string, boolean booleanProp)
	{
		return getProp(string, booleanProp + "").equals("true");
	}

	public float getFloatProp(String s)
	{
		return Float.parseFloat(getProp(s, "0.0"));
	}

	public float getFloatProp(String s, float f)
	{
		return Float.parseFloat(getProp(s, ""+f));
	}

	public double getDoubleProp(String s)
	{
		return Double.parseDouble(getProp(s, "0.0"));
	}

	public double getDoubleProp(String s, float d)
	{
		return Double.parseDouble(getProp(s, d+""));
	}

	public void setProp(String p, double d)
	{
		setProp(p, d + "");
	}

	public void setProp(String p, float f)
	{
		setProp(p, f + "");
	}
}
