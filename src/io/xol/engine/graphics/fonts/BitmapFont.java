package io.xol.engine.graphics.fonts;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class BitmapFont
{
	public static BitmapFont EDITUNDO = new BitmapFont("editundo", 256, 16);
	public static BitmapFont SMALLFONTS = new BitmapFont("smallfonts", 256, 16);
	public static BitmapFont TINYFONTS = new BitmapFont("tinyfonts", 256, 16);

	public String name = "";
	public int texSize = 256;
	public int cellSize = 16;
	public int fontWidthData[] = new int[65533];

	public BitmapFont(String n, int t, int c)
	{
		name = n;
		texSize = t;
		cellSize = c;
		load();
	}

	public void load()
	{
		try
		{
			//TODO this is ancient, remake it !
			InputStream ips = new FileInputStream(new File(System.getProperty("user.dir") + "/res/textures/font/" + name + ".xfd"));
			InputStreamReader ipsr = new InputStreamReader(ips, "UTF-8");
			BufferedReader br = new BufferedReader(ipsr);
			String ligne;
			while ((ligne = br.readLine()) != null)
			{
				if (!ligne.contains("#") && !ligne.equals(""))
					fontWidthData[Integer.parseInt(ligne.split(":")[0])] = Integer.parseInt(ligne.split(":")[1]);
			}
			br.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
