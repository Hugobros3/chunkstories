package io.xol.chunkstories.server;

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
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class UsersPrivileges
{
	// Takes care of the admins/banned/whitelisted people lists

	public static List<String> admins = new ArrayList<String>();

	public static List<String> whitelist = new ArrayList<String>();

	public static List<String> banned_users = new ArrayList<String>();

	public static List<String> banned_ips = new ArrayList<String>();

	public static boolean isUserAdmin(String username)
	{
		return admins.contains(username);
	}

	public static boolean isUserWhitelisted(String username)
	{
		return whitelist.contains(username);
	}

	public static boolean isUserBanned(String username)
	{
		return banned_users.contains(username);
	}

	public static boolean isIpBanned(String username)
	{
		return banned_ips.contains(username);
	}

	// Ugly load/save methods

	public static void load()
	{
		admins = loadListFile(new File(System.getProperty("user.dir") + "/config/server-admins.txt"));
		whitelist = loadListFile(new File(System.getProperty("user.dir") + "/config/server-whitelist.txt"));
		banned_users = loadListFile(new File(System.getProperty("user.dir") + "/config/server-bans.txt"));
		banned_ips = loadListFile(new File(System.getProperty("user.dir") + "/config/server-banips.txt"));
	}

	public static void save()
	{
		saveListFile(new File(System.getProperty("user.dir") + "/config/server-admins.txt"), admins);
		saveListFile(new File(System.getProperty("user.dir") + "/config/server-whitelist.txt"), whitelist);
		saveListFile(new File(System.getProperty("user.dir") + "/config/server-bans.txt"), banned_users);
		saveListFile(new File(System.getProperty("user.dir") + "/config/server-banips.txt"), banned_ips);
	}

	// Uglier file loading/saving routines

	@SuppressWarnings("deprecation")
	static void saveListFile(File f, List<String> list)
	{
		check4Folder(f);
		try
		{
			Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
			out.write("# File generated on " + new Time(System.currentTimeMillis()).toGMTString() + "\n");
			for (String s : list)
			{
				out.write(s + "\n");
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

	static List<String> loadListFile(File f)
	{
		check4Folder(f);
		List<String> list = new ArrayList<String>();
		try
		{
			InputStream ips = new FileInputStream(f);
			InputStreamReader ipsr = new InputStreamReader(ips, "UTF-8");
			BufferedReader br = new BufferedReader(ipsr);
			String ligne;
			while ((ligne = br.readLine()) != null)
			{
				if (!ligne.startsWith("#"))
					list.add(ligne);
			}
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return list;
	}

	static void check4Folder(File file)
	{
		File folder = null;
		if (!file.isDirectory())
			folder = file.getParentFile();
		if (folder != null && !folder.exists())
			folder.mkdir();
		if (!file.exists())
			try
			{
				file.createNewFile();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
	}
}
