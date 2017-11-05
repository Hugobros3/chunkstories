package io.xol.chunkstories.content.mods;

import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;

import io.xol.chunkstories.api.exceptions.content.mods.ModLoadFailureException;
import io.xol.chunkstories.api.math.HexTools;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.mods.Mod;
import io.xol.chunkstories.api.content.mods.ModInfo;
import io.xol.chunkstories.api.util.IterableIterator;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public abstract class ModImplementation implements Mod
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
	
	ModImplementation() throws ModLoadFailureException
	{
		
	}
	
	@Override
	public abstract Asset getAssetByName(String name);
	
	@Override
	public abstract IterableIterator<Asset> assets();
	
	@Override
	public ModInfo getModInfo()
	{
		return modInfo;
	}
	
	@Override
	public String getMD5Hash()
	{
		if(md5hash == null)
			computeMD5Hash();
		return md5hash;
	}
	
	private synchronized void computeMD5Hash()
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

	protected Logger logger;
	public Logger logger() {
		return logger;
	}
}
