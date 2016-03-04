package io.xol.engine.locale;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * Loads .locale files and is used for internationalization of the game
 * @author Gobrosse
 *
 */
public class Localizer
{
	static Map<String, String> locale = new HashMap<String, String>();

	public static void loadLocale(String locale)
	{
		File localeFile = new File("./res/locale/" + locale + ".locale.txt");
		if (localeFile.exists())
		{
			loadLocale(localeFile);
			System.out.println("Sucessfully loaded locale \"" + locale + "\".");
		} else
		{
			loadLocale(new File("./res/locale/en.locale.txt"));
			System.out.println("Loaded fallback locale \"en\".");
		}
	}

	private static void loadLocale(File localeFile)
	{
		locale.clear();
		try
		{
			InputStream ips = new FileInputStream(localeFile);
			InputStreamReader ipsr = new InputStreamReader(ips, "UTF-8");
			BufferedReader br = new BufferedReader(ipsr);
			String ligne;
			while ((ligne = br.readLine()) != null)
			{
				if (ligne.contains("="))
				{
					String[] meh = ligne.split("=");
					locale.put(meh[0], meh[1]);
				}
			}
			br.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static String getText(String key, String[] args)
	{
		String txt = getText(key);
		String txt2 = "";
		int modifs = 0;
		for (int i = 0; i < txt.length(); i++)
		{
			char c = txt.charAt(i);
			if (c == '%' && !(i >= 1 && txt.charAt(i) == '\\'))
			{
				if (modifs < args.length)
					txt2 += args[modifs];
				modifs++;
			} else
				txt2 += c;
		}
		return txt2;
	}

	public static String getText(String key)
	{
		String txt = locale.get(key);
		if (txt == null)
			txt = "Translation error";
		return txt;
	}
}
