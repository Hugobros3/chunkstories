//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.util.config;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.xol.chunkstories.api.util.ConfigDeprecated;

/** TODO this is shit and should die */
public class OldStyleConfigFile implements ConfigDeprecated
{
	String path;
	Map<String, String> props = new HashMap<String, String>();

	public OldStyleConfigFile(String p)
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

	@Override
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

	@Override
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


	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#getProp(java.lang.String, java.lang.String)
	 */
	@Override
	public String getString(String s, String a)
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

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#getString(java.lang.String)
	 */
	@Override
	public String getString(String s)
	{
		return getString(s, "");
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#getInteger(java.lang.String, int)
	 */
	@Override
	public int getInteger(String s, int intProp)
	{
		return Integer.parseInt(getString(s, intProp + ""));
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#getBoolean(java.lang.String, boolean)
	 */
	@Override
	public boolean getBoolean(String string, boolean booleanProp)
	{
		return getString(string, booleanProp + "").equals("true");
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#getFloat(java.lang.String)
	 */
	@Override
	public float getFloat(String s)
	{
		return Float.parseFloat(getString(s, "0.0"));
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#getFloat(java.lang.String, float)
	 */
	@Override
	public float getFloat(String s, float f)
	{
		return Float.parseFloat(getString(s, "" + f));
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#getDouble(java.lang.String)
	 */
	@Override
	public double getDouble(String s)
	{
		return Double.parseDouble(getString(s, "0.0"));
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#getDouble(java.lang.String, double)
	 */
	@Override
	public double getDouble(String s, double d)
	{
		return Double.parseDouble(getString(s, d + ""));
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#getLong(java.lang.String, long)
	 */
	@Override
	public long getLong(String s, long l)
	{
		try
		{
			return Long.parseLong(getString(s, l + ""));
		}
		catch (NumberFormatException e)
		{
			return (long) Double.parseDouble(getString(s, l + ""));
		}
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#setString(java.lang.String, java.lang.String)
	 */
	@Override
	public void setString(String p, String d)
	{
		if (props.containsKey(p))
			props.remove(p);
		props.put(p, d);
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#setInteger(java.lang.String, int)
	 */
	@Override
	public void setInteger(String p, int i)
	{
		setString(p, i + "");
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#setLong(java.lang.String, long)
	 */
	@Override
	public void setLong(String p, long l)
	{
		setString(p, l + "");
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#setDouble(java.lang.String, double)
	 */
	@Override
	public void setDouble(String p, double d)
	{
		setString(p, d + "");
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#setFloat(java.lang.String, float)
	 */
	@Override
	public void setFloat(String p, float f)
	{
		setString(p, f + "");
	}

	/* (non-Javadoc)
	 * @see io.xol.engine.misc.ConfigDeprecated#isFieldSet(java.lang.String)
	 */
	@Override
	public boolean isFieldSet(String string)
	{
		return props.containsKey(string);
	}

	@Override
	public int getInteger(String property) {
		return getInteger(property, 0);
	}

	@Override
	public boolean getBoolean(String property) {
		return getBoolean(property, false);
	}

	@Override
	public long getLong(String property) {
		return getLong(property, 0);
	}

	@Override
	public void removeFieldValue(String string) {
		props.remove(string);
	}

	@Override
	public Iterator<String> getFieldsSet() {
		List<String> fields = new ArrayList<String>(props.keySet());
		return fields.iterator();
	}
}
