package io.xol.chunkstories.api.exceptions;

import java.io.File;

import io.xol.chunkstories.content.mods.Asset;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SyntaxErrorException extends Exception
{
	Asset asset;
	
	File file;
	int line;
	String customMsg = null;
	
	public SyntaxErrorException(int ln, File f)
	{
		file = f;
		line = ln;
	}

	public SyntaxErrorException(int ln, File f, String msg)
	{
		this(ln, f);
		customMsg = msg;
	}
	
	public SyntaxErrorException(int ln, Asset f, String msg)
	{
		asset = f;
		line = ln;
		customMsg = msg;
	}

	@Override
	public String getMessage()
	{
		if(asset != null)
			return "Parse error "+(customMsg == null ? "" : ("( "+customMsg+" )"))+" at line "+line+" of asset "+asset;
		
		return "Parse error "+(customMsg == null ? "" : ("( "+customMsg+" )"))+" at line "+line+" of file "+file.getAbsolutePath();
	}
	
	private static final long serialVersionUID = 4922242735691620666L;

}
