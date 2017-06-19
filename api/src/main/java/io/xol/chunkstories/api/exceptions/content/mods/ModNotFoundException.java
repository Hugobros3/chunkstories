package io.xol.chunkstories.api.exceptions.content.mods;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ModNotFoundException extends ModLoadFailureException
{
	String modName;
	
	public ModNotFoundException(String modName)
	{
		super(null, null);
		this.modName = modName;
	}

	public String getMessage()
	{
		return "Mod '"+modName+"' was not found.";
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -5671040280199985929L;

}
