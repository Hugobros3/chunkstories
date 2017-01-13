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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//(c) 2015-2017 XolioWare Interactive
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
				System.out.println("Creating config file " + file);
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
			File file = new File(path);
			if (!file.exists())
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
	}

	public void save()
	{
		check4Folder(path);
		try
		{
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
			Set<String> unsortedKeys = props.keySet();
			List<String> sortedKeys = new ArrayList<String>(unsortedKeys);
			sortedKeys.sort(new Comparator<String>()
			{
				@Override
				public int compare(String arg0, String arg1)
				{
					return arg0.compareTo(arg1);
				}

			});
			for (String key : sortedKeys)
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
		{
			if (a != null)
				props.put(s, a);
		}
		return a;
	}

	public String getString(String s)
	{
		return getProp(s, "");
	}

	public int getInteger(String s, int intProp)
	{
		return Integer.parseInt(getProp(s, intProp + ""));
	}

	public boolean getBoolean(String string, boolean booleanProp)
	{
		return getProp(string, booleanProp + "").equals("true");
	}

	public float getFloat(String s)
	{
		return Float.parseFloat(getProp(s, "0.0"));
	}

	public float getFloat(String s, float f)
	{
		return Float.parseFloat(getProp(s, "" + f));
	}

	public double getDouble(String s)
	{
		return Double.parseDouble(getProp(s, "0.0"));
	}

	public double getDouble(String s, double d)
	{
		return Double.parseDouble(getProp(s, d + ""));
	}

	public long getLong(String s, long l)
	{
		try
		{
			return Long.parseLong(getProp(s, l + ""));
		}
		catch (NumberFormatException e)
		{
			return (long) Double.parseDouble(getProp(s, l + ""));
		}
	}

	public void setString(String p, String d)
	{
		if (props.containsKey(p))
			props.remove(p);
		props.put(p, d);
	}

	public void setInteger(String p, int i)
	{
		setString(p, i + "");
	}

	public void setLong(String p, long l)
	{
		setString(p, l + "");
	}

	public void setDouble(String p, double d)
	{
		setString(p, d + "");
	}

	public void setFloat(String p, float f)
	{
		setString(p, f + "");
	}

	public boolean isFieldSet(String string)
	{
		return props.containsKey(string);
	}
}
