package io.xol.chunkstories.content.mods;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.xol.chunkstories.api.utils.IterableIterator;
import io.xol.chunkstories.content.mods.exceptions.ModLoadFailureException;
import io.xol.engine.math.HexTools;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class Mod
{
	protected ModInfo modInfo;
	protected String md5hash;
	
	public static MessageDigest md;

	static
	{
		try
		{
			md = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
	}
	
	Mod() throws ModLoadFailureException
	{
		
	}
	
	public abstract Asset getAssetByName(String name);
	
	public abstract IterableIterator<Asset> assets();
	
	public ModInfo getModInfo()
	{
		return modInfo;
	}
	
	public String getMD5Hash()
	{
		if(md5hash == null)
			computeMD5Hash();
		return md5hash;
	}
	
	private void computeMD5Hash()
	{
		//Makes a sorted list of the names of all the assets
		List<String> assetsSorted = new ArrayList<String>();
		for(Asset asset : assets())
		{
			assetsSorted.add(asset.getName());
		}
		assetsSorted.sort(new Comparator<String>() {

			@Override
			public int compare(String o1, String o2)
			{
				return o1.compareTo(o2);
			}
			
		});
		//for(String s : assetsSorted)
		//	System.out.println(s);

		//Concatenate their names...
		String completeNamesString = "";
		for(String s : assetsSorted)
			completeNamesString += s + ";";
		
		//MD5 it
		String hashedNames = HexTools.byteArrayAsHexString(md.digest(completeNamesString.getBytes()));
		
		//Iterate over each asset, hash it then add that to the sb
		StringBuilder sb = new StringBuilder();
		for(String s : assetsSorted)
		{
			Asset a = this.getAssetByName(s);
			byte[] buffer = new byte[4096];
			DigestInputStream eater = new DigestInputStream(a.read(), md);
			try {
				while(eater.read(buffer) != -1);
				eater.close();
			}
			catch(IOException e)
			{
				
			}
			//Append
			sb.append(HexTools.byteArrayAsHexString(md.digest()));
		}
		//Append hash of list of names
		sb.append(hashedNames);
		
		//Hash the whole stuff again
		md5hash = HexTools.byteArrayAsHexString(md.digest(sb.toString().getBytes()));
	}

	public abstract void close();

	public abstract String getLoadString();
}
