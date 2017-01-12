package io.xol.chunkstories.api.exceptions.rendering;

import io.xol.engine.graphics.textures.Texture;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class TextureBindException extends RenderingException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7397862685160889891L;
	
	Texture texture;
	String extraInformation;
	
	public TextureBindException(Texture texture)
	{
		this.texture = texture;
	}

	public TextureBindException(Texture texture, String extraInformation)
	{
		this.texture = texture;
		this.extraInformation = extraInformation;
	}

	@Override
	public String getMessage()
	{
		if(extraInformation != null)
			return "Texture "+texture+" couldn't be bound : "+extraInformation;
		return "Texture "+texture+" couldn't be bound.";
	}
}
