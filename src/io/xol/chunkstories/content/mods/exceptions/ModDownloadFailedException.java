package io.xol.chunkstories.content.mods.exceptions;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ModDownloadFailedException extends ModLoadFailureException
{
	private static final long serialVersionUID = -8878214806405897338L;
	
	String modName;
	String failMessage;
	
	public ModDownloadFailedException(String modName, String failMessage)
	{
		super(null, null);
		this.modName = modName;
		this.failMessage = failMessage;
	}

	public String getMessage()
	{
		return "Mod '"+modName+"' could not be downloaded : "+failMessage;
	}

}
