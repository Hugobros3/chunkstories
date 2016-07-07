package io.xol.chunkstories.api.input;

import java.util.Iterator;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface InputsManager
{
	public Input getInputByName(String inputName);

	public Iterator<Input> getAllInputs();
}
